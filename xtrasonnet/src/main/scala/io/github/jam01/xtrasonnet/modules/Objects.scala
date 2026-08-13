package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.spi.Library.{emptyObj, keyFrom}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Val.Obj
import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, EvalScope, FileScope, Position, TailstrictModeDisabled, Val}

import java.util
import scala.collection.mutable.ArrayBuffer

object Objects extends AbstractFunctionModule {
  override def name: String = "objects"
  
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("all", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) =>
        obj.visibleKeyNames.toSeq.forall(key => func.apply2(obj.value(key, pos)(ev), Val.Str(pos, key), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True])
    },

    builtin("any", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) =>
        obj.visibleKeyNames.exists(
          item => func.apply2(obj.value(item, pos)(ev), Val.Str(pos, item), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]
        )
    },

    builtin("distinctBy", "container", "func") {
      (_, ev, obj: Val.Obj, func: Val.Func) =>
        distinctBy(obj, func, ev)
    },

    builtinWithDefaults("innerEqJoin",
      "arrL" -> null,
      "arrR" -> null,
      "funcIdL" -> null,
      "funcIdR" -> null,
      "funcJoin" -> Val.False(position)) { (args, pos, ev) =>
      eqJoin(args, pos, ev, JoinKind.Inner)
    },

    builtinWithDefaults("leftEqJoin",
      "arrL" -> null,
      "arrR" -> null,
      "funcIdL" -> null,
      "funcIdR" -> null,
      "funcJoin" -> Val.False(position)) { (args, pos, ev) =>
      eqJoin(args, pos, ev, JoinKind.Left)
    },

    builtinWithDefaults("fullEqJoin",
      "arrL" -> null,
      "arrR" -> null,
      "funcIdL" -> null,
      "funcIdR" -> null,
      "funcJoin" -> Val.False(position)) { (args, pos, ev) =>
      eqJoin(args, pos, ev, JoinKind.Full)
    },

    builtinWithDefaults("fromArray",
      "arr" -> null,
      "keyF" -> null,
      "valueF" -> Val.False(position)) { (args, pos, ev) =>
      val lzyArr = args(0) match {
        case arr: Val.Arr => arr.asLazyArray
        case x => Error.fail("Expected Array, got: " + x.prettyName)
      }
      val kFunc = args(1) match {
        case f: Val.Func => f.asFunc
        case x => Error.fail("Expected Function, got: " + x.prettyName)
      }
      val vFunc = args(2)
      val fArgs = kFunc.params.names.length

      val m = new util.LinkedHashMap[String, Val.Obj.Member](lzyArr.length)
      var i = 0
      if (fArgs == 2) {
        while (i < lzyArr.length) {
          val k = kFunc.apply2(lzyArr(i), Val.Num(kFunc.pos, i), pos.noOffset)(ev, TailstrictModeDisabled)
          if (!k.isInstanceOf[Val.Str]) Error.fail("Key Function should return a String, got: " + k.prettyName)
          val j = i.intValue // ints are objects in Scala??, so we set a 'final' reference

          m.put(k.asString,
            if (vFunc.isInstanceOf[Val.False]) new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = lzyArr(j).force
            } else new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = vFunc.asFunc.apply1(lzyArr(j), pos.noOffset)(ev, TailstrictModeDisabled)
            })
          i = i + 1
        }
      } else if (fArgs == 1) {
        while (i < lzyArr.length) {
          val k = kFunc.apply1(lzyArr(i), pos.noOffset)(ev, TailstrictModeDisabled)
          if (!k.isInstanceOf[Val.Str]) Error.fail("Key Function should return a String, got: " + k.prettyName)
          val j = i.intValue // ints are objects in Scala??, so we set a 'final' reference

          m.put(k.asString,
            if (vFunc.isInstanceOf[Val.False]) new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = lzyArr(j).force
            } else new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = vFunc.asFunc.apply1(lzyArr(j), pos.noOffset)(ev, TailstrictModeDisabled)
            })
          i = i + 1
        }
      } else {
        Error.fail("Expected function to take 1 or 2 parameters, received: " + fArgs)
      }

      new Val.Obj(pos, m, false, null, null).asInstanceOf[Val]
    }
  )

  /** Which rows eqJoin emits besides the matched pairs; the only three combinations any builtin defines. */
  private sealed trait JoinKind
  private object JoinKind {
    case object Inner extends JoinKind // matched pairs only
    case object Left extends JoinKind // + unmatched left rows
    case object Full extends JoinKind // + unmatched left and right rows
  }

  /**
   * The hash-join core behind inner/left/fullEqJoin, differing only in which unmatched rows they emit.
   *
   * Rows come out in a deterministic order: left rows in their original order -- each expanded to its
   * right matches in right order, one row per match via a shallow merge or funcJoin -- and, when
   * unmatched right rows are emitted, those appended in their original order.
   */
  private def eqJoin(args: Array[Val], pos: Position, ev: EvalScope, kind: JoinKind): Val = {
    val unmatchedLeft = kind != JoinKind.Inner
    val unmatchedRight = kind == JoinKind.Full

    val left = args(0).asArr
    val right = args(1).asArr
    val funcIdL = args(2).asFunc
    val funcIdR = args(3).asFunc
    val funcJoin = args(4) match {
      case _: Val.False => null
      case f: Val.Func => f
      case x => Error.fail("Expected function, got: " + x.prettyName)
    }

    // 1. Index the RIGHT side by key; unmatchedRight also needs each row's key for the unmatched pass
    val rightHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
    val rightKeys = if (unmatchedRight) new Array[String](right.length) else null
    var i = 0
    while (i < right.length) {
      val k = keyFrom(funcIdR.apply1(right.asLazyArray(i), funcIdR.pos)(ev, TailstrictModeDisabled))
      rightHash.computeIfAbsent(k, newArrBuff).addOne(right.force(i).asObj)
      if (unmatchedRight) rightKeys(i) = k
      i += 1
    }

    val result = new ArrayBuffer[Val]()
    val joinedKeys = if (unmatchedRight) new util.HashSet[String]() else null

    // 2. Iterate the LEFT side in original order
    i = 0
    while (i < left.length) {
      val leftObj = left.force(i).asObj
      val k = keyFrom(funcIdL.apply1(left.asLazyArray(i), funcIdL.pos)(ev, TailstrictModeDisabled))

      val matches = rightHash.get(k)
      if (matches != null) {
        if (unmatchedRight) joinedKeys.add(k)
        matches.foreach { rightObj =>
          result.addOne(
            if (funcJoin == null) leftObj.addSuper(pos, rightObj).asObj
            else funcJoin.apply2(leftObj, rightObj, funcJoin.pos)(ev, TailstrictModeDisabled).asObj)
        }
      } else if (unmatchedLeft) {
        result.addOne(
          if (funcJoin == null) leftObj
          else funcJoin.apply2(leftObj, emptyObj, funcJoin.pos)(ev, TailstrictModeDisabled).asObj)
      }
      i += 1
    }

    // 3. Append the unmatched RIGHT rows in their original order
    if (unmatchedRight) {
      i = 0
      while (i < right.length) {
        if (!joinedKeys.contains(rightKeys(i))) {
          val rightObj = right.force(i).asObj
          result.addOne(
            if (funcJoin == null) rightObj
            else funcJoin.apply2(emptyObj, rightObj, funcJoin.pos)(ev, TailstrictModeDisabled).asObj)
        }
        i += 1
      }
    }

    Val.Arr(pos, result.toArray)
  }

  private def distinctBy(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val tests = new ArrayBuffer[Val]()
    val m = new ArrayBuffer[(String, Val.Obj.Member)]()

    var i = 0
    if (args == 2) { // 2 args
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        val test = func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled)
        if (!tests.exists(uniq => ev.equal(uniq, test))) {
          tests.append(test)
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else if (args == 1) { //1 arg
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        val test = func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled)
        if (!tests.exists(uniq => ev.equal(uniq, test))) {
          tests.append(test)
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.toArray: _*)
  }

  private val newArrBuff: java.util.function.Function[String, ArrayBuffer[Val.Obj]] = _ => ArrayBuffer[Val.Obj]()
}
