package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
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
/*
 * Work covered:
 * - 6b92da38753b0f8d00f12dc5859c644027d92cd1: Added operations for concatenation and removal
 *      Functions: remove, removeMatch
 * - 482b67a18b29a331cbb6366c81885ee35d9c9075: Fixed orderBy Functionality
 *      Functions: orderBy, toString
 * - 5bb242721f728c00432234cd24f7256e21c4caac: Added some expanded functionality
 *      Functions: indexOf, lastIndexOf
 * - 5f7619dea8ac4e04e0d7e527999095d6bbac6029: Added String option to reverse function
 *      Functions: reverse
 * - 482b67a18b29a331cbb6366c81885ee35d9c9075: Fixed orderBy Functionality
 *      Functions: orderBy
 *
 * Changes made:
 * - d19a57dfcf4382669d55ac4427916c8440c1bac3: fixes orderBy comparison
 * - 2662d96cdfbd613d766830420a0b2a6920d07b52: changed remove to rmKey and filterNotEq, and removeMatch to rmKeyIn and filterNotIn
 * - d37ba4c860723b42cecfe20e381c302eef75b49e - 2213fec224b8cbd1302f0b15542d1699308d3d08: removed null support from adopted functions
 * - bb160ee733a2770629935e8573c6c77574a9d8f7: rename orderBy to sortBy
 */

import io.github.jam01.xtrasonnet.document.{Document, MediaType}
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.modules.{Arrays, Base64, Crypto, Datetime, Duration, Math, Numbers, Objects, Strings, URL}
import io.github.jam01.xtrasonnet.spi.Library
import io.github.jam01.xtrasonnet.spi.Library.{emptyObj, keyFrom}
import sjsonnet.ReadWriter.{ArrRead, ObjRead, ValRead}
import sjsonnet.functions.FunctionModule
import sjsonnet.{Error, EvalScope, Lazy, Materializer, Position, TailstrictModeDisabled, Val}
import ujson.{Bool, Null, Num, Str}

import java.util
import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

// re: performance of while-loops vs foreach see:
// https://www.theguardian.com/info/2021/oct/19/pondering-some-scala-performance-questions
// https://www.lihaoyi.com/post/MicrooptimizingyourScalacode.html#speed-through-while-loops
// re: performance of "unswitched loops" see:
// https://stackoverflow.com/a/2242246
// http://lampwww.epfl.ch/~hmiller/scala2013/resources/pdfs/paper9.pdf
// https://www.geeksforgeeks.org/loop-optimization-techniques-set-2/ item #7

// further optimizations possible:
// consider replacing memberOf(s) with lazy-invoke
// prefer Val.Obj() than Val.Obj.mk
final class Xtr(dataFormats: DataFormatService, header: Header) extends Library {
  override def name: String = "xtr"

  private val functions: Map[String, Val.Func] = Map(
    builtin("contains", "container", "value") {
      (_, ev, container: Val, value: Val) =>
        container match {
          // See: scala.collection.IterableOnceOps.exists
          case str: Val.Str => str.value.contains(value.asString)
          case array: Val.Arr => array.asLazyArray.exists(v => ev.equal(v.force, value))
          case x => Error.fail("Expected Array or String, got: " + x.prettyName)
        }
    },

    builtin("entries", "obj") {
      (pos, ev, obj: Val.Obj) =>
        Val.Arr(pos, obj.visibleKeyNames.collect({
          case key => Val.Obj.mk(pos,
            ("key", memberOf(Val.Str(pos, key))),
            ("value", memberOf(obj.value(key, pos)(ev)))
          )
        }))
    },

    builtin("filter", "array", "func") {
      (_, ev, arr: Val.Arr, func: Val.Func) => filter(arr.asLazyArray, func, ev)
    },

    builtin("filterObject", "array", "func") {
      (_, ev, obj: Val.Obj, func: Val.Func) => filterObject(obj, func, ev).asInstanceOf[Val]
    },

    builtin("indicesOf", "container", "value") {
      (pos, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str =>
            val sub = value.cast[Val.Str].value
            Val.Arr(pos, sub.r.findAllMatchIn(str.value).map(_.start).map(item => Val.Num(pos, item)).toArray)
          case array: Val.Arr =>
            val out = new ArrayBuffer[Val.Num]()
            val lazArr = array.asLazyArray
            var i = 0
            while (i < lazArr.length) {
              if (ev.equal(lazArr(i).force, value)) {
                out.append(Val.Num(pos, i))
              }
              i = i + 1
            }
            Val.Arr(pos, out.toArray)
          case x => Error.fail("Expected Array or String, got: " + x.prettyName)
        }
    },

    builtin("flatMap", "array", "func") {
      (_, ev, value: Val.Arr, func: Val.Func) => flatMap(value.asLazyArray, func, ev)
    },

    builtin("flatMapObject", "value", "func") {
      (_, ev, obj: Val.Obj, func: Val.Func) => flatMapObject(obj, func, ev)
    },

    builtin("flatten", "array") {
      (pos, _, value: Val.Arr) =>
        val out = new ArrayBuffer[Lazy]
        var i = 0
        while (i < value.length) {
          out.appendAll(value.asLazyArray(i).force.asArr.asLazyArray) // should we report we expected arr[arr]?
          i = i + 1
        }
        Val.Arr(pos, out.toArray)
    },

    builtin("endsWith", "main", "sub") {
      (_, _, main: String, sub: String) =>
        main.toLowerCase().endsWith(sub.toLowerCase());
    },

    builtin("groupBy", "container", "func") {
      (_, ev, container: Val, func: Val.Func) =>
        container match {
          case array: Val.Arr => groupBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => groupBy(obj, func, ev)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("isBlank", "value") {
      (_, _, str: String) => !Utils.hasText(str)
    },

    builtin("isDecimal", "value") {
      (pos, _, num: Val.Num) =>
        num match {
          case Val.Int64(pos, value) => false
          case Val.Float64(pos, value) => !value.isWhole
          case Val.Dec128(pos, value) => !value.isWhole
        }
    },

    builtin("isEmpty", "container") {
      (_, _, container: Val) =>
        container match {
          case s: Val.Obj => !s.hasKeys.booleanValue()
          case s: Val.Str => s.value.isEmpty.booleanValue()
          case array: Val.Arr => array.asLazyArray.isEmpty.booleanValue()
          case x => Error.fail("Expected String, Array, or Object, got: " + x.prettyName)
        }
    },

    builtin("isInteger", "value") {
      (_, _, num: Val.Num) =>
        num match {
          case Val.Int64(pos, value) => true
          case Val.Float64(pos, value) => value.isWhole
          case Val.Dec128(pos, value) => value.isWhole
        }
    },

    builtin("join", "array", "sep") {
      (_, _, array: Val.Arr, sep: String) =>
        array.asLazyArray.map(v => keyFrom(v.force)).mkString(sep)
    },

    builtin("keys", "obj") {
      (pos, _, obj: Val.Obj) =>
        Val.Arr(pos, obj.visibleKeyNames.map(item => Val.Str(pos, item)))
    },

    builtin("toLowerCase", "str") {
      (_, _, str: String) => str.toLowerCase();
    },

    builtin("map", "value", "func") {
      (_, ev, arr: Val.Arr, func: Val.Func) => map(arr.asLazyArray, func, ev)
    },

    builtin("mapObject", "value", "func") {
      (_, ev, obj: Val.Obj, func: Val.Func) => mapObject(obj, func, ev)
    },

    builtin("mapEntries", "value", "func") {
      (_, ev, obj: Val.Obj, func: Val.Func) => mapEntries(obj, func, ev)
    },

    // TODO: optimize with while-loop
    builtin("max", "array") {
      (_, ev, array: Val.Arr) => array.asLazyArray.view.map(_.force).max(ev)
    },

    builtin("maxBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        array.asLazyArray.view.map(_.force).maxBy(it => func.apply1(it, pos.noOffset)(ev, TailstrictModeDisabled))(ev)
    },

    // TODO: optimize with while-loop
    builtin("min", "array") {
      (_, ev, array: Val.Arr) => array.asLazyArray.view.map(_.force).min(ev)
    },

    builtin("minBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        array.asLazyArray.view.map(_.force).minBy(it => func.apply1(it, pos.noOffset)(ev, TailstrictModeDisabled))(ev)
    },

    builtin("sortBy", "value", "func") {
      (_, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => sortBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => sortBy(obj, func, ev)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    // TODO: add step param
    builtin("range", "begin", "end") {
      (pos, _, begin: Int, end: Int) =>
        Val.Arr(pos, (begin to end).map(i => Val.Num(pos, i)).toArray)
    },

    builtin("replace", "str1", "str2", "replacement") {
      (_, _, str: String, str2: String, replacement: String) =>
        str.replace(str2, replacement)
    },

    builtinWithDefaults("read",
      "data" -> null,
      "mimeType" -> null,
      "params" -> Val.False(position)) {
      (args, pos, ev) =>
        val data = args(0).cast[Val.Str].value
        val mimeType = args(1).cast[Val.Str].value
        val params = if (args(2).isInstanceOf[Val.False]) {
          emptyObj
        } else {
          args(2).cast[Val.Obj]
        }
        read(dataFormats, data, mimeType, params, ev, pos)
    },

    builtinWithDefaults("readUrl",
      "url" -> null,
      "mimeType" -> null,
      "params" -> Val.False(position)) {
      (args, pos, ev) =>
        val url = args(0).cast[Val.Str].value
        val mimeType = args(1).cast[Val.Str].value
        val params = if (args(2).isInstanceOf[Val.False]) {
          emptyObj
        } else {
          args(2).cast[Val.Obj]
        }
        val data = ResourceResolver.asString(url, null)
        read(dataFormats, data, mimeType, params, ev, pos)
    },

    builtin("length", "value") {
      (_, _, value: Val) =>
        value match {
          case s: Val.Str => s.value.length()
          case s: Val.Obj => s.visibleKeyNames.length
          case array: Val.Arr => array.asLazyArray.length
          case s: Val.Func => s.params.names.length
          case x => Error.fail("Expected Array, String, or Object, got: " + x.prettyName)
        }
    },

    builtin("split", "str1", "str2") {
      (pos, _, str1: String, str2: String) =>
        Val.Arr(pos, Utils.split(str1, str2).map(s => Val.Str(pos, s)))
    },

    builtin("startsWith", "str1", "str2") {
      (_, _, str1: String, str2: String) => str1.startsWith(str2);
    },

    builtin("toString", "value") {
      (_, ev, value: Val) => Materializer.stringify(value)(ev)
    },

    builtin("trim", "str") {
      (_, _, str: String) => str.trim()
    },

    builtin("type", "value") {
      (_, _, value: Val) => value.prettyName
    },

    builtin("toUpperCase", "str") {
      (_, _, str: String) =>
        str.toUpperCase()
    },

    builtin("uuid") {
      (_, _) =>
        UUID.randomUUID().toString
    },

    builtin("values", "obj") {
      (pos, ev, obj: Val.Obj) =>
        Val.Arr(pos, obj.visibleKeyNames.map(key => obj.value(key, pos)(ev)))
    },

    builtinWithDefaults("write",
      "data" -> Val.Null(position),
      "mimeType" -> Val.Null(position),
      "params" -> emptyObj) { (args, pos, ev) =>
      val data = args(0)
      val mimeType = args(1).cast[Val.Str].value
      val params = args(2).cast[Val.Obj]
      write(dataFormats, data, mimeType, params, ev, pos)
    },

    // funcs below taken from Std
    builtin("isString", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Str]
    },

    builtin("isBoolean", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.True] || v.isInstanceOf[Val.False]
    },

    builtin("isNumber", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Num]
    },

    builtin("isObject", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Obj]
    },

    builtin("isArray", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Arr]
    },

    builtin("isFunction", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Func]
    },

    builtin("foldLeft", "arr", "init", "func") {
      (pos, ev, arr0: Val.Arr, init: Val, func: Val.Func) =>
        val arr = arr0.asLazyArray
        var current = init
        var i = 0
        while (i < arr.length) {
          val c = current
          current = func.apply2(arr(i), c, pos.noOffset)(ev, TailstrictModeDisabled)
          i = i + 1
        }
        current
    },

    builtin("foldRight", "arr", "init", "func") {
      (pos, ev, arr0: Val.Arr, init: Val, func: Val.Func) =>
        val arr = arr0.asLazyArray
        var current = init
        var i = arr.length - 1
        while (i >= 0) {
          val c = current
          current = func.apply2(arr(i), c, pos.noOffset)(ev, TailstrictModeDisabled)
          i = i - 1
        }
        current
    },

    builtin("rmKey", "collection", "value") {
      (pos, ev, obj: Val.Obj, value: Val) =>
        Val.Obj.mk(pos,
          (value match {
            case str: Val.Str =>
              obj.visibleKeyNames.toSeq.collect({
                case key if !key.equals(str.value) => key -> memberOf(obj.value(key, pos)(ev))
              })
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }): _*).asInstanceOf[Val]
    },

    builtin("rmKeyIn", "first", "second") {
      (pos, ev, obj: Val.Obj, second: Val) =>
        Val.Obj.mk(pos,
          (second match {
            case arr: Val.Arr =>
              obj.visibleKeyNames.toSeq.collect({
                case key if !arr.asLazyArray.exists(item => item.force.asString.equals(key)) =>
                  key -> memberOf(obj.value(key, pos)(ev))
              })
            case x => Error.fail("Expected Array, got: " + x.prettyName)
          }): _*).asInstanceOf[Val]
    },

    builtin("filterNotEq", "collection", "value") {
      (pos, ev, arr: Val.Arr, value: Val) =>
        Val.Arr(pos, arr.asLazyArray.filter(x => !ev.equal(x.force, value)))
    },

    builtin("filterNotIn", "first", "second") {
      (pos, ev, first: Val, second: Val) =>
        first match {
          case arr: Val.Arr =>
            second match {
              case arr2: Val.Arr =>
                // unfortunately cannot use diff here because of lazy values
                Val.Arr(pos, arr.asLazyArray
                  .filter(arrItem => !arr2.asLazyArray.exists(arr2Item => ev.equal(arrItem.force, arr2Item.force)))).asInstanceOf[Val]
              case x => Error.fail("Expected Array, got: " + x.prettyName)
            }
          case obj: Val.Obj =>
            Val.Obj.mk(pos, (second match {
              case arr: Val.Arr =>
                obj.visibleKeyNames.toSeq.collect({
                  case key if !arr.asLazyArray.exists(item => item.force.asString.equals(key)) =>
                    key -> memberOf(obj.value(key, pos)(ev))
                })
              case x => Error.fail("Expected Array, got: " + x.prettyName)
            }): _*).asInstanceOf[Val]
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("reverse", "collection") {
      (pos, ev, collection: Val) =>
        collection match {
          case str: Val.Str => Val.Str(pos, str.value.reverse).asInstanceOf[Val]
          case arr: Val.Arr => Val.Arr(pos, arr.asLazyArray.reverse).asInstanceOf[Val]
          case obj: Val.Obj =>
            var result: Seq[(String, Val.Obj.Member)] = Seq()
            obj.visibleKeyNames.foreach(key => result = result.prepended(
              key -> memberOf(obj.value(key, pos)(ev))
            ))
            Val.Obj.mk(pos, result: _*).asInstanceOf[Val]
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("indexOf", "container", "value") {
      (_, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str => str.value.indexOf(value.cast[Val.Str].value)
          case array: Val.Arr => array.asLazyArray.indexWhere(lzy => ev.equal(lzy.force, value))
          case x => Error.fail("Expected String or Array, got: " + x.prettyName)
        }
    },

    builtin("lastIndexOf", "container", "value") {
      (_, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str => str.value.lastIndexOf(value.cast[Val.Str].value)
          case array: Val.Arr => array.asLazyArray.lastIndexWhere(lzy => ev.equal(lzy.force, value))
          case x => Error.fail("Expected String or Array, got: " + x.prettyName)
        }
    },

    builtin("parseNum", "str") { (_, _, str: String) =>
      str.toDouble
    }
  )

  override def module: Val.Obj = {
    Val.Obj.mk(position,
      functions.toSeq.map{ case (name, func) => (name, memberOf(func)) } ++
        Xtr.allModules.map(mod => (mod.name, memberOf(mod.module))): _*)
  }

  def read(dataFormats: DataFormatService, data: String, mimeType: String, params: Val.Obj, ev: EvalScope, pos: Position): Val = {
    val paramsAsJava = ujson.read(Materializer.apply(params)(ev)).obj.map(keyVal => {
      (keyVal._1, keyVal._2 match {
        case Str(value) => value
        case Num(value) => String.valueOf(value)
        case bool: Bool => String.valueOf(bool)
        case Null => "null"
        case x => Error.fail("function expected to return Number, String, Null, or Boolean, got: " + x, pos)(ev)
      })
    }).asJava

    val doc = Document.of(data, MediaType.parseMediaType(mimeType).withParameters(paramsAsJava))
    dataFormats.mandatoryRead(doc, pos)
  }

  def write(dataFormats: DataFormatService, json: Val, mimeType: String, params: Val.Obj, ev: EvalScope, pos: Position): String = {
    val paramsAsJava = ujson.read(Materializer.apply(params)(ev)).obj.map(keyVal => {
      (keyVal._1, keyVal._2 match {
        case Str(value) => value
        case Num(value) => String.valueOf(value)
        case bool: Bool => String.valueOf(bool)
        case Null => "null"
        case x => Error.fail("function expected to return Number, String, Null, or Boolean, got: " + x, pos)(ev)
      })
    }).asJava
    val mediaType = MediaType.parseMediaType(mimeType).withParameters(paramsAsJava)
    dataFormats.mandatoryWrite(json, mediaType, classOf[String], ev).getContent
  }

  // TODO: can we reference std.filter?
  private def filter(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val.Arr = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new ArrayBuffer[Lazy]()

    var i = 0
    if (args == 2) {
      while (i < array.length) {
        val item = array(i)
        if (func.apply2(array(i), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]) {
          out.append(item)
        }
        i = i + 1
      }
    } else if (args == 1) {
      while (i < array.length) {
        val item = array(i)
        if (func.apply1(array(i), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]) {
          out.append(item)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Arr(pos, out.toArray)
  }

  private def map(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val.Arr = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new Array[Lazy](array.length)

    var i = 0
    if (args == 2) { //2 args
      while (i < array.length) {
        out(i) = func.apply2(array(i), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled)
        i = i + 1
      }
    } else if (args == 1) { // 1 arg
      while (i < array.length) {
        out(i) = func.apply1(array(i), pos.noOffset)(ev, TailstrictModeDisabled)
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Arr(pos, out)
  }

  private def filterObject(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val.Obj = {
    val pos = func.pos
    val args = func.params.names.length
    val m = new ArrayBuffer[(String, Val.Obj.Member)]()

    var i = 0
    if (args == 3) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        if (func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]) {
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else if (args == 2) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        if (func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]) {
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else if (args == 1) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        if (func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled).isInstanceOf[Val.True]) {
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.toArray: _*)
  }

  private def flatMap(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new ArrayBuffer[Lazy]()

    var i = 0
    if (args == 2) { // 2 args
      while (i < array.length) {
        val inner = func.apply2(array(i).force, Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled).asArr
        var j = 0
        while (j < inner.length) {
          out.append(inner.asLazyArray(j))
          j = j + 1
        }
        i = i + 1
      }
    } else if (args == 1) { //  1 arg
      while (i < array.length) {
        val inner = func.apply1(array(i).force, pos.noOffset)(ev, TailstrictModeDisabled).asArr
        var j = 0
        while (j < inner.length) {
          out.append(inner.asLazyArray(j))
          j = j + 1
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Arr(pos, out.toArray)
  }

  private def groupBy(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val m: util.LinkedHashMap[String, mutable.ArrayBuffer[Lazy]] = new util.LinkedHashMap[String, mutable.ArrayBuffer[Lazy]]()
    val mScala = m.asScala // to allow getOrElseUpdate

    var i = 0
    if (args == 2) {
      while (i < array.length) {
        val v = array(i)
        val k = keyFrom(func.apply2(v, Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled))
        mScala.getOrElseUpdate(k, mutable.ArrayBuffer[Lazy]()).addOne(v)
        i = i + 1
      }
    } else if (args == 1) {
      while (i < array.length) {
        val v = array(i)
        val k = keyFrom(func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled))
        mScala.getOrElseUpdate(k, mutable.ArrayBuffer[Lazy]()).addOne(v)
        i = i + 1
      }
    }
    else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.asScala.toSeq.map { case (k, buff) => (k, memberOf(Val.Arr(pos, buff.toArray))) }.toArray: _*)
  }

  private def groupBy(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val m: util.LinkedHashMap[String, util.LinkedHashMap[String, Val.Obj.Member]] = new util.LinkedHashMap[String, util.LinkedHashMap[String, Val.Obj.Member]]()
    val mScala = m.asScala // to allow getOrElseUpdate

    var i = 0
    if (args == 2) { // 2 args
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos)(ev)
        val funcKey = keyFrom(func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled))
        mScala.getOrElseUpdate(funcKey, new util.LinkedHashMap[String, Val.Obj.Member]()).put(k, memberOf(v))
        i = i + 1
      }
    } else if (args == 1) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos)(ev)
        val funcKey = keyFrom(func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled))
        mScala.getOrElseUpdate(funcKey, new util.LinkedHashMap[String, Val.Obj.Member]()).put(k, memberOf(v))
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.asScala.toSeq.map { case (k, map) => (k, memberOf(Val.Obj(pos, map, false, null, null))) }.toArray: _*)
  }

  private def mapEntries(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new ArrayBuffer[Lazy](obj.visibleKeyNames.length)

    var i = 0
    var k: String = null
    var v: Val = null
    if (args.equals(3)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        out.append(func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled))
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        out.append(func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled))
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        out.append(func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled))
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }

    Val.Arr(pos, out.toArray)
  }

  private def mapObject(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new util.LinkedHashMap[String, Val.Obj.Member]()

    var i = 0
    var k: String = null
    var v: Val = null
    if (args.equals(3)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, position)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, position)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, position)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }
    Val.Obj(pos, out, false, null, null)
  }

  private def flatMapObject(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new util.LinkedHashMap[String, Val.Obj.Member]()

    var i = 0
    var k: String = null
    var v: Val = null
    if (args.equals(3)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, position)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, position)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply1(v, pos.noOffset)(ev, TailstrictModeDisabled) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, position)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }
    Val.Obj(pos, out, false, null, null)
  }

  // TODO: optimize with while-loop
  private def sortBy(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length

    if (args == 2) {
      Val.Arr(pos,
        array.zipWithIndex.sortBy(
          it => func.apply2(it._1, Val.Num(pos, it._2), pos.noOffset)(ev, TailstrictModeDisabled)
        )(ord = ev).map(_._1))
    } else if (args == 1) {
      Val.Arr(pos, array.sortBy(it => func.apply1(it, pos.noOffset)(ev, TailstrictModeDisabled))(ord = ev))
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
  }

  // TODO: we're traversing the object twice, needed?
  private def sortBy(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length
    val m = new mutable.LinkedHashMap[String, Val.Obj.Member]

    var i = 0
    var k: String = null
    var v: Val = null
    while (i < obj.visibleKeyNames.length) {
      k = obj.visibleKeyNames(i)
      v = obj.value(k, pos)(ev)

      m.put(k, memberOf(v))
      i = i + 1
    }

    if (args.equals(2)) {
      Val.Obj.mk(pos,
        m.toSeq.sortBy { case (k1, _) => func.apply2(obj.value(k1, pos)(ev), Val.Str(pos, k1), pos.noOffset)(ev, TailstrictModeDisabled) }(ord = ev): _*
      )
    } else if (args == 1) {
      Val.Obj.mk(pos,
        m.toSeq.sortBy { case (k1, _) => func.apply1(obj.value(k1, pos)(ev), pos.noOffset)(ev, TailstrictModeDisabled) }(ord = ev): _*
      )
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
  }
}

object Xtr {
  private val allModules: Seq[FunctionModule] = Seq(
    Datetime,
    Duration,
    Crypto,
    URL,
    Math,
    Arrays,
    Objects,
    Numbers,
    Strings, // TODO: review regexs
    Base64
  )

  val Default: Xtr = Xtr(DataFormatService.DEFAULT, Header.Empty())
}
