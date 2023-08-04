package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.spi.Library.keyFrom
import io.github.jam01.xtrasonnet.spi.Library.{dummyPosition, emptyObj, memberOf}
import os.Generator
import sjsonnet.Expr.Member.Visibility
import io.github.jam01.xtrasonnet.spi.Library.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val.Obj
import sjsonnet.{Error, EvalScope, FileScope, Lazy, Val}

import java.util
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

object Objects {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("all", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) =>
        obj.visibleKeyNames.toSeq.forall(key => func.apply2(obj.value(key, pos)(ev), Val.Str(pos, key), pos.noOffset)(ev).isInstanceOf[Val.True])
    },

    builtin("any", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) =>
        obj.visibleKeyNames.exists(
          item => func.apply2(obj.value(item, pos)(ev), Val.Str(pos, item), pos.noOffset)(ev).isInstanceOf[Val.True]
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
      "funcJoin" -> Val.False(dummyPosition)) { (args, pos, ev) =>

      val left = args(0).asArr
      val right = args(1).asArr
      val funcIdL = args(2).asFunc
      val funcIdR = args(3).asFunc
      val funcJoin = args(4) match {
        case _: Val.False => null
        case f: Val.Func => f
        case x => Error.fail("Expected function, got: " + x.prettyName)
      }
      val leftHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
      val out = new ArrayBuffer[Val.Obj]()

      var i = 0
      while (i < left.length) {
        val k = keyFrom(funcIdL.apply1(left.asLazy(i), funcIdL.pos)(ev))
        if (leftHash.containsKey(k)) leftHash.put(k, leftHash.get(k).addOne(left.force(i).asObj))
        else leftHash.put(k, ArrayBuffer[Obj](left.force(i).asObj))
        i = i + 1
      }

      i = 0
      while (i < right.length) {
        val k = keyFrom(funcIdR.apply1(right.asLazy(i), funcIdL.pos)(ev))
        if (leftHash.containsKey(k)) {
          leftHash.get(k).foreach(obj =>
            out.addOne(
              if (funcJoin == null) obj.asObj.addSuper(pos, right.force(i).asObj).asObj
              else funcJoin.asFunc.apply2(obj, right.force(i), funcJoin.pos)(ev).asObj
            )
          )
        }

        i = i + 1
      }

      new Val.Arr(pos, out.toArray)
    },

    builtinWithDefaults("leftEqJoin",
      "arrL" -> null,
      "arrR" -> null,
      "funcIdL" -> null,
      "funcIdR" -> null,
      "funcJoin" -> Val.False(dummyPosition)) { (args, pos, ev) =>

      val left = args(0).asArr
      val right = args(1).asArr
      val funcIdL = args(2).asFunc
      val funcIdR = args(3).asFunc
      val funcJoin = args(4) match {
        case _: Val.False => null
        case f: Val.Func => f
        case x => Error.fail("Expected function, got: " + x.prettyName)
      }
      val leftHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
      val bothHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
      val leftUnjoined = new util.HashSet[String]()

      var i = 0
      while (i < left.length) { // computing keys on the left with the corresponding array of values
        val k = keyFrom(funcIdL.apply1(left.asLazy(i), funcIdL.pos)(ev))
        val toAdd = left.force(i).asObj
        val arr = leftHash.computeIfAbsent(k, newArrBuff).addOne(toAdd)

        // no custom func, already moving to both in case not to be joined
        if (funcJoin == null) bothHash.put(k, arr)
        leftUnjoined.add(k)
        i = i + 1
      }

      i = 0
      while (i < right.length) { // computing keys on the right
        val k = keyFrom(funcIdR.apply1(right.asLazy(i), funcIdL.pos)(ev))
        if (leftHash.containsKey(k)) { // if also in left join them into bothHash
          leftHash.get(k).foreach(obj => {
            if (leftUnjoined.remove(k)) bothHash.remove(k) // 1st time seeing this one, clear it to join
            bothHash.computeIfAbsent(k, newArrBuff).addOne(
              if (funcJoin == null) obj.asObj.addSuper(pos, right.force(i).asObj).asObj
              else funcJoin.asFunc.apply2(obj, right.force(i), funcJoin.pos)(ev).asObj
            )
          })
        }

        i = i + 1
      }

      // if custom join func, apply to the remaining unjoined ones (otherwise already in bothHash)
      if (funcJoin != null) leftUnjoined.forEach(k => bothHash.put(k, leftHash.get(k).map(obj =>
        funcJoin.asFunc.apply2(obj, emptyObj, funcJoin.pos)(ev).asObj
      )))

      new Val.Arr(pos, bothHash.values().asScala.flatten.toArray)
    },

    builtinWithDefaults("fullEqJoin",
      "arrL" -> null,
      "arrR" -> null,
      "funcIdL" -> null,
      "funcIdR" -> null,
      "funcJoin" -> Val.False(dummyPosition)) { (args, pos, ev) =>

      val left = args(0).asArr
      val right = args(1).asArr
      val funcIdL = args(2).asFunc
      val funcIdR = args(3).asFunc
      val funcJoin = args(4) match {
        case _: Val.False => null
        case f: Val.Func => f
        case x => Error.fail("Expected function, got: " + x.prettyName)
      }
      val leftHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
      val bothHash = new util.HashMap[String, ArrayBuffer[Val.Obj]]()
      val leftUnjoined = new util.HashSet[String]()

      var i = 0
      while (i < left.length) { // computing keys on the left with the corresponding array of values
        val k = keyFrom(funcIdL.apply1(left.asLazy(i), funcIdL.pos)(ev))
        val toAdd = left.force(i).asObj
        val arr = leftHash.computeIfAbsent(k, newArrBuff).addOne(toAdd)

        // no custom func, already moving to both in case not to be joined
        if (funcJoin == null) bothHash.put(k, arr)
        leftUnjoined.add(k)
        i = i + 1
      }

      i = 0
      while (i < right.length) { // computing keys on the right
        val k = keyFrom(funcIdR.apply1(right.asLazy(i), funcIdL.pos)(ev))
        if (leftHash.containsKey(k)) { // if also in left join them into bothHash
          leftHash.get(k).foreach(obj => {
            if (leftUnjoined.remove(k)) bothHash.remove(k) // 1st time seeing this one, clear it to join
            bothHash.computeIfAbsent(k, newArrBuff).addOne(
              if (funcJoin == null) obj.asObj.addSuper(pos, right.force(i).asObj).asObj
              else funcJoin.asFunc.apply2(obj, right.force(i), funcJoin.pos)(ev).asObj
            )
          })
        } else { // else add it to bothHash
          val toAdd =
            if (funcJoin == null) right.force(i).asObj
            else funcJoin.asFunc.apply2(emptyObj, right.force(i).asObj, funcJoin.pos)(ev).asObj
          bothHash.computeIfAbsent(k, _ => new ArrayBuffer[Obj]()).addOne(toAdd)
        }

        i = i + 1
      }

      // if custom join func, apply to the remaining unjoined ones (otherwise already in bothHash)
      if (funcJoin != null) leftUnjoined.forEach(k => bothHash.put(k, leftHash.get(k).map(obj =>
        funcJoin.asFunc.apply2(obj, emptyObj, funcJoin.pos)(ev).asObj
      )))

      new Val.Arr(pos, bothHash.values().asScala.flatten.toArray)
    },

    builtinWithDefaults("fromArray",
      "arr" -> null,
      "keyF" -> null,
      "valueF" -> Val.False(dummyPosition)) { (args, pos, ev) =>
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
          val k = kFunc.apply2(lzyArr(i), Val.Num(kFunc.pos, i), pos.noOffset)(ev)
          if (!k.isInstanceOf[Val.Str]) Error.fail("Key Function should return a String, got: " + k.prettyName)
          val j = i.intValue // ints are objects in Scala??, so we set a 'final' reference

          m.put(k.asString,
            if (vFunc.isInstanceOf[Val.False]) new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = lzyArr(j).force
            } else new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = vFunc.asFunc.apply1(lzyArr(j), pos.noOffset)(ev)
            })
          i = i + 1
        }
      } else if (fArgs == 1) {
        while (i < lzyArr.length) {
          val k = kFunc.apply1(lzyArr(i), pos.noOffset)(ev)
          if (!k.isInstanceOf[Val.Str]) Error.fail("Key Function should return a String, got: " + k.prettyName)
          val j = i.intValue // ints are objects in Scala??, so we set a 'final' reference

          m.put(k.asString,
            if (vFunc.isInstanceOf[Val.False]) new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = lzyArr(j).force
            } else new Obj.Member(false, Visibility.Normal) {
              override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = vFunc.asFunc.apply1(lzyArr(j), pos.noOffset)(ev)
            })
          i = i + 1
        }
      } else {
        Error.fail("Expected function to take 1 or 2 parameters, received: " + fArgs)
      }

      new Val.Obj(pos, m, false, null, null).asInstanceOf[Val]
    }
  )

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
        val test = func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev)
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
        val test = func.apply1(v, pos.noOffset)(ev)
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
