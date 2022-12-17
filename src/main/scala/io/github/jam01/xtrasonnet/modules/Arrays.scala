package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-
 * Adopted:
 * - 386223447f864492ca4703a4d9eaa49eea9b64a3: Converted util functions to scala
 *      Functions: duplicates, deepFlatten, occurrences
 *
 * Changed:
 * - d37ba4c860723b42cecfe20e381c302eef75b49e - 2213fec224b8cbd1302f0b15542d1699308d3d08: removed null support from adopted functions
 * - 8a009fe5d1e7c0994a18a32faefae197c06f1168: refactor arrays module
 *    added identity func to duplicates, now duplicatesBy
 *    renamed occurrences to occurrencesBy
 * - 05edbc0165aff4849b9f142e153141d7a8204efd: rename deepflatten for flat
 */

import io.github.jam01.xtrasonnet.spi.Library.keyFrom
import io.github.jam01.xtrasonnet.spi.Library.{dummyPosition, memberOf}
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.{Error, EvalScope, Lazy, Val}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import java.util

object Arrays {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("countBy", "arr", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) =>
        var total = 0
        for (x <- arr.asLazyArray) {
          if (func.apply1(x, pos.noOffset)(ev).isInstanceOf[Val.True]) {
            total += 1
          }
        }
        total
    },

    builtin("chunksOf", "array", "size") { // TODO: better name?
      (pos, ev, array: Val.Arr, size: Int) =>
        new Val.Arr(pos, array.asLazyArray.sliding(size, size).map(item => new Val.Arr(pos, item)).toArray)
    },

    builtin("drop", "arr", "num") {
      (pos, ev, arr: Val.Arr, num: Int) =>
        new Val.Arr(pos, arr.asLazyArray.drop(num))
    },

    builtin("dropWhile", "arr", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) =>
        new Val.Arr(pos, arr.asLazyArray.dropWhile(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True]))
    },

    builtin("duplicatesBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        val out = mutable.ArrayBuffer[Lazy]()
        array.asLazyArray.collect({
          case item if array.asLazyArray.count(lzy => ev.equal(lzy.force, func.apply1(item.force, func.pos)(ev))) >= 2 &&
            !out.exists(lzy => ev.equal(lzy.force, item.force)) => out.append(item)
        })
        new Val.Arr(pos, out.toArray)
    },

    builtin("all", "value", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) => arr.forall(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
    },

    builtin("find", "arr", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) =>
        val pos = func.pos
        val args = func.params.names.length

        if (args == 2) {
          val found = arr.asLazyArray.zipWithIndex
            .find(item => func.apply2(item._1, Val.Num(pos, item._2), pos.noOffset)(ev).isInstanceOf[Val.True])
            .map(_._1)
          if (found.nonEmpty) new Val.Arr(pos, Array(found.get))
          else new Val.Arr(pos, Array.empty)
        } else if (args == 1) {
          val found = arr.asLazyArray.find(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
          if (found.nonEmpty) new Val.Arr(pos, Array(found.get))
          else new Val.Arr(pos, Array.empty)
        } else {
          Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
        }
    },

    builtin("flat", "arr") {
      (pos, ev, arr: Val.Arr) =>
        new Val.Arr(pos, flat(arr.asLazyArray))
    },

    builtin("indexWhere", "arr", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        array.asLazyArray.indexWhere(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
    },

    builtin("lastIndexWhere", "arr", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        array.asLazyArray.lastIndexWhere(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
    },

    builtin("indicesWhere", "arr", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        val pos = func.pos
        val args = func.params.names.length
        val out = new ArrayBuffer[Val.Num]()
        val lazyArr = array.asLazyArray

        var i = 0
        if (args == 1) { // 1 arg
          while (i < array.length) {
            val test = func.apply1(lazyArr(i), pos.noOffset)(ev)
            if (test.asBoolean.booleanValue()) {
              out.append(Val.Num(pos, i))
            }
            i = i + 1
          }
        } else {
          Error.fail("Expected embedded function to have 1 parameters, received: " + args)
        }
        new Val.Arr(pos, out.toArray)
    },

    builtin("occurrencesBy", "arr", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        // no idea why, but this sorts the result in the correct order
        val ordered = mutable.Map.from(
          array.asLazyArray
            .groupBy(item => keyFrom(func.apply1(item, pos.noOffset)(ev)))
            .map(item => item._1 -> memberOf(Val.Num(pos, item._2.length)))
        )

        Val.Obj.mk(pos, ordered.toSeq: _*)
    },

    builtin("partition", "arr", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        val out = new util.LinkedHashMap[String, Val.Obj.Member]()
        val part = array.asLazyArray.partition(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
        out.put("pass", memberOf(new Val.Arr(pos, part._1)))
        out.put("fail", memberOf(new Val.Arr(pos, part._2)))
        new Val.Obj(pos, out, false, null, null)
    },

    builtin("any", "value", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) => array.asLazyArray.exists(item => func.apply1(item, pos.noOffset)(ev).isInstanceOf[Val.True])
    },

    builtin("splitAt", "array", "index") {
      (pos, ev, array: Val.Arr, index: Int) =>
        val split = array.asLazyArray.splitAt(index)
        val out = new util.LinkedHashMap[String, Val.Obj.Member]()

        out.put("left", memberOf(new Val.Arr(pos, split._1)))
        out.put("right", memberOf(new Val.Arr(pos, split._2)))
        new Val.Obj(pos, out, false, null, null)
    },

    builtin("break", "arr", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) =>
        val split = arr.asLazyArray.splitAt(arr.asLazyArray.indexWhere(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True]))
        val out = new util.LinkedHashMap[String, Val.Obj.Member]()

        out.put("left", memberOf(new Val.Arr(pos, split._1)))
        out.put("right", memberOf(new Val.Arr(pos, split._2)))
        new Val.Obj(pos, out, false, null, null)
    },

    builtin("sumBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        array.asLazyArray.foldLeft(0.0)((sum, num) => sum + func.apply1(num, pos.noOffset)(ev).asDouble)
    },

    builtin("take", "array", "index") {
      (pos, ev, array: Val.Arr, index: Int) =>
        new Val.Arr(pos, array.asLazyArray.splitAt(index)._1)
    },

    builtin("takeWhile", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        new Val.Arr(pos, array.asLazyArray.takeWhile(item => func.apply1(item, pos.noOffset)(ev).isInstanceOf[Val.True]))
    },

    builtin("distinctBy", "container", "func") {
      (_, ev, arr: Val.Arr, func: Val.Func) =>
        distinctBy(arr.asLazyArray, func, ev)
    },

    builtin("unzip", "array") {
      (pos, _, array: Val.Arr) =>
        val lazyArr = array.asLazyArray
        val out = new ArrayBuffer[Lazy]
        val maxSize = lazyArr.map(
          _.force match {
            case arr: Val.Arr => arr.asLazyArray.length
            case x => Error.fail("Expected Array of Arrays, got inner: " + x.prettyName)
          })
          .max

        var i = 0
        while (i < maxSize) {
          val current = new ArrayBuffer[Lazy]
          var j = 0
          while (j < lazyArr.length) {
            current.append(lazyArr(j).force.asArr.asLazyArray(i))
            j = j + 1
          }
          out.append(new Val.Arr(pos, current.toArray))
          i = i + 1
        }

        new Val.Arr(pos, out.toArray)
    },

    builtinWithDefaults("zip",
      "arr1" -> null,
      "arr2" -> null,
      "arr3" -> Val.False(dummyPosition),
      "arr4" -> Val.False(dummyPosition),
      "arr5" -> Val.False(dummyPosition)) { (args, pos, ev) =>
      val lazyArr = args.filter {
        case _: Val.Arr => true
        case _: Val.False => false
        case x => Error.fail("Expected Array, got: " + x.prettyName) // give param index?
      }
      val out = new ArrayBuffer[Lazy]
      val maxSize = lazyArr.map(
        _.force match {
          case arr: Val.Arr => arr.asLazyArray.length
          case x => Error.fail("Expected Array of Arrays, got inner: " + x.prettyName)
        })
        .max

      var i = 0
      while (i < maxSize) {
        val current = new ArrayBuffer[Lazy]
        var j = 0
        while (j < lazyArr.length) {
          current.append(lazyArr(j).force.asArr.asLazyArray(i))
          j = j + 1
        }
        out.append(new Val.Arr(pos, current.toArray))
        i = i + 1
      }

      new Val.Arr(pos, out.toArray)
    }
  )

  private def distinctBy(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
    // alternative implementation here https://stackoverflow.com/a/9982455
    val pos = func.pos
    val args = func.params.names.length
    val tests = new ArrayBuffer[Val]()
    val out = new ArrayBuffer[Lazy]()

    var i = 0
    if (args == 2) { // 2 args
      while (i < array.length) {
        val test = func.apply2(array(i), Val.Num(pos, i), pos.noOffset)(ev)
        if (!tests.exists(uniq => ev.equal(uniq, test))) {
          tests.append(test)
          out.append(array(i))
        }
        i = i + 1
      }
    } else if (args == 1) { // 1 arg
      while (i < array.length) {
        val test = func.apply1(array(i), pos.noOffset)(ev)
        if (!tests.exists(uniq => ev.equal(uniq, test))) {
          tests.append(test)
          out.append(array(i))
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
    new Val.Arr(pos, out.toArray)
  }

  // TODO: add depth parameter, guard for stackoverflow
  private def flat(array: Array[Lazy]): Array[Lazy] = {
    array.foldLeft(new ArrayBuffer[Lazy])((agg, curr) => {
      curr.force match {
        case inner: Val.Arr => agg.appendAll(flat(inner.asLazyArray))
        case _ => agg.append(curr)
      }
    }).toArray
  }
}
