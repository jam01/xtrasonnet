package io.github.jam01.xtrasonnet

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
 * Changed:
 * - d74e8ff8838292274aa0c386d39fece6db16916d: Encapsulates Library logic
 *      Reimplemented DS under the new Library interface
 *
 * Adopted:
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
 * Changed:
 * - d19a57dfcf4382669d55ac4427916c8440c1bac3: fixes orderBy comparison
 * - 2662d96cdfbd613d766830420a0b2a6920d07b52: changed remove to rmKey and filterNotEq, and removeMatch to rmKeyIn and filterNotIn
 * - d37ba4c860723b42cecfe20e381c302eef75b49e - 2213fec224b8cbd1302f0b15542d1699308d3d08: removed null support from adopted functions
 * - bb160ee733a2770629935e8573c6c77574a9d8f7: rename orderBy to sortBy
 */

import io.github.jam01.xtrasonnet.document.{Document, MediaType}
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.modules.{Arrays, Base64, Crypto, Datetime, Duration, Math, Numbers, Objects, Strings, URL}
import io.github.jam01.xtrasonnet.spi.{Library, ValOrdering}
import io.github.jam01.xtrasonnet.spi.Library.{dummyPosition, emptyObj, keyFrom, memberOf}
import sjsonnet.ReadWriter.{ArrRead, ObjRead, ValRead}
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.{Error, EvalScope, Importer, Lazy, Materializer, Val}
import ujson.{Bool, Null, Num, Str}

import java.util
import java.util.{Collections, UUID}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

// re: performance of while-loops vs foreach see:
// https://www.theguardian.com/info/2021/oct/19/pondering-some-scala-performance-questions
// https://www.lihaoyi.com/post/MicrooptimizingyourScalacode.html#speed-through-while-loops
// re: performance of "unswitched loops" see:
// https://stackoverflow.com/a/2242246
// http://lampwww.epfl.ch/~hmiller/scala2013/resources/pdfs/paper9.pdf
// https://www.geeksforgeeks.org/loop-optimization-techniques-set-2/ item #7

// further optimizations possible:
// consider replacing memberOf(s) with lazy-invoke
// prefer new Val.Obj() than Val.Obj.mk
object Xtr extends Library {

  override def namespace() = "xtr"

  override def libsonnets(): java.util.Set[String] = Collections.emptySet();

  override def functions(dataFormats: DataFormatService,
                         header: Header, importer: Importer): java.util.Map[String, Val.Func] = Map(
    builtin("contains", "container", "value") {
      (_, ev, container: Val, value: Val) =>
        container match {
          // See: scala.collection.IterableOnceOps.exists
          case array: Val.Arr =>
            array.asLazyArray.exists(v => ev.equal(v.force, value))
          case str: Val.Str =>
            str.value.contains(str.value)
          case x => Error.fail("Expected Array or String, got: " + x.prettyName)
        }
    },

    builtin("entries", "obj") {
      (pos, ev, obj: Val.Obj) =>
        new Val.Arr(pos, obj.visibleKeyNames.collect({
          case key => Val.Obj.mk(pos,
            ("key", memberOf(Val.Str(pos, key))),
            ("value", memberOf(obj.value(key, pos)(ev)))
          )
        }))
    },

    builtin("filter", "array", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) => filter(arr.asLazyArray, func, ev)
    },

    builtin("filterObject", "array", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) => filterObject(obj, func, ev).asInstanceOf[Val]
    },

    builtin("indicesOf", "container", "value") {
      (pos, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str =>
            val sub = value.cast[Val.Str].value
            new Val.Arr(pos, sub.r.findAllMatchIn(str.value).map(_.start).map(item => Val.Num(pos, item)).toArray)
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
            new Val.Arr(pos, out.toArray)
          case x => Error.fail("Expected Array or String, got: " + x.prettyName)
        }
    },

    builtin("flatMap", "array", "func") {
      (pos, ev, value: Val.Arr, func: Val.Func) => flatMap(value.asLazyArray, func, ev)
    },

    builtin("flatMapObject", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) => flatMapObject(obj, func, ev)
    },

    builtin("flatten", "array") {
      (pos, _, value: Val.Arr) =>
        val out = new ArrayBuffer[Lazy]
        var i = 0
        while (i < value.length) {
          out.appendAll(value.asLazyArray(i).force.asArr.asLazyArray) // should we report we expected arr[arr]?
          i = i + 1
        }
        new Val.Arr(pos, out.toArray)
    },

    builtin("endsWith", "main", "sub") {
      (pos, ev, main: String, sub: String) =>
        main.toLowerCase().endsWith(sub.toLowerCase());
    },

    builtin("groupBy", "container", "func") {
      (pos, ev, container: Val, func: Val.Func) =>
        container match {
          case array: Val.Arr => groupBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => groupBy(obj, func, ev)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("isBlank", "value") {
      (pos, ev, str: String) => !Utils.hasText(str)
    },

    builtin("isDecimal", "value") {
      (pos, ev, value: Double) =>
        (java.lang.Math.ceil(value) != java.lang.Math.floor(value)).booleanValue()
    },

    builtin("isEmpty", "container") {
      (pos, ev, container: Val) =>
        container match {
          case s: Val.Str => s.value.isEmpty.booleanValue()
          case array: Val.Arr => array.asLazyArray.isEmpty.booleanValue()
          case s: Val.Obj => !s.hasKeys.booleanValue()
          case x => Error.fail("Expected String, Array, or Object, got: " + x.prettyName)
        }
    },

    builtin("isInteger", "value") {
      (pos, ev, value: Double) =>
        (java.lang.Math.ceil(value) == java.lang.Math.floor(value)).booleanValue()
    },

    builtin("join", "array", "sep") {
      (pos, ev, array: Val.Arr, sep: String) =>
        array.asLazyArray.map({
          _.force match {
            case str: Val.Str => str.value
            case num: Val.Num => if (!num.value.isWhole) num.value.toString else num.value.intValue().toString
            case x => Error.fail("Expected String, Number, or Boolean, got: " + x.prettyName)
          }
        }).mkString(sep)
    },

    builtin("keys", "obj") {
      (pos, _, obj: Val.Obj) =>
        new Val.Arr(pos, obj.visibleKeyNames.map(item => Val.Str(pos, item)))
    },

    builtin("toLowerCase", "str") {
      (pos, ev, str: String) =>
        str.toLowerCase();
    },

    builtin("map", "value", "func") {
      (pos, ev, arr: Val.Arr, func: Val.Func) => map(arr.asLazyArray, func, ev)
    },

    builtin("mapObject", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) => mapObject(obj, func, ev)
    },

    builtin("mapEntries", "value", "func") {
      (pos, ev, obj: Val.Obj, func: Val.Func) => mapEntries(obj, func, ev)
    },

    // TODO: optimize with while-loop
    builtin("max", "array") {
      (pos, ev, array: Val.Arr) =>
        var value = array.asLazyArray.head
        for (x <- array.asLazyArray) {
          // TODO: avoid string comparison
          value.force.prettyName match {
            case "string" =>
              if (value.force.cast[Val.Str].value < x.force.cast[Val.Str].value) {
                value = x
              }
            case "boolean" => if (x.isInstanceOf[Val.True]) {
              value = x
            }
            case "number" =>
              if (value.force.cast[Val.Num].value < x.force.cast[Val.Num].value) {
                value = x
              }
            case x => Error.fail("Expected Array of type String, Boolean, or Number, got: Array of type " + x)
          }
        }
        value.force
    },

    builtin("maxBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        val lazyArr = array.asLazyArray
        func.apply1(lazyArr.head, pos.noOffset)(ev) match {
          case _: Val.Str => lazyArr.maxBy(item => func.apply1(item, pos.noOffset)(ev).asString).force
          case _: Val.Bool => lazyArr.find(it => func.apply1(it, pos.noOffset)(ev).asBoolean).getOrElse(lazyArr.head).force
          case _: Val.Num => lazyArr.maxBy(it => func.apply1(it, pos.noOffset)(ev).asDouble).force
          case x => Error.fail("Expected Array of type String, Boolean, or Number, got: Array of type " + x)
        }
    },

    // TODO: optimize with while-loop
    builtin("min", "array") {
      (pos, ev, array: Val.Arr) =>
        var value = array.asLazyArray.head
        for (x <- array.asLazyArray) {
          // TODO: avoid string comparison
          value.force.prettyName match {
            case "string" =>
              if (value.force.cast[Val.Str].value > x.force.cast[Val.Str].value) {
                value = x
              }
            case "boolean" =>
              if (x.force.isInstanceOf[Val.False]) {
                value = x
              }
            case "number" =>
              if (value.force.cast[Val.Num].value > x.force.cast[Val.Num].value) {
                value = x
              }
            case x => Error.fail("Expected Array of type String, Boolean, or Number, got: Array of type " + x)
          }
        }
        value.force
    },

    builtin("minBy", "array", "func") {
      (pos, ev, array: Val.Arr, func: Val.Func) =>
        val lazyArr = array.asLazyArray
        func.apply1(lazyArr.head, pos.noOffset)(ev) match {
          case _: Val.Str => lazyArr.minBy(item => func.apply1(item, pos.noOffset)(ev).asString).force
          case _: Val.Bool => lazyArr.find(it => !func.apply1(it, pos.noOffset)(ev).asBoolean).getOrElse(lazyArr.head).force
          case _: Val.Num => lazyArr.minBy(item => func.apply1(item, pos.noOffset)(ev).asDouble).force
          case x => Error.fail("Expected Array of type String, Boolean, or Number, got: Array of type " + x)
        }
    },

    builtin("sortBy", "value", "func") {
      (pos, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => sortBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => sortBy(obj, func, ev)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    // TODO: add step param
    builtin("range", "begin", "end") {
      (pos, _, begin: Int, end: Int) =>
        new Val.Arr(pos, (begin to end).map(i => Val.Num(pos, i)).toArray)
    },

    builtin("replace", "str1", "str2", "replacement") {
      (pos, ev, str: String, str2: String, replacement: String) =>
        str.replace(str2, replacement)
    },

    builtinWithDefaults("read",
      "data" -> null,
      "mimeType" -> null,
      "params" -> Val.False(dummyPosition)) {
      (args, pos, ev) =>
        val data = args(0).cast[Val.Str].value
        val mimeType = args(1).cast[Val.Str].value
        val params = if (args(2).isInstanceOf[Val.False]) {
          emptyObj
        } else {
          args(2).cast[Val.Obj]
        }
        read(dataFormats, data, mimeType, params, ev)
    },

    builtinWithDefaults("readUrl",
      "url" -> null,
      "mimeType" -> null,
      "params" -> Val.False(dummyPosition)) {
      (args, pos, ev) =>
        val url = args(0).cast[Val.Str].value
        val mimeType = args(1).cast[Val.Str].value
        val params = if (args(2).isInstanceOf[Val.False]) {
          emptyObj
        } else {
          args(2).cast[Val.Obj]
        }
        val data = ResourceResolver.asString(url, null)
        read(dataFormats, data, mimeType, params, ev)
    },

    builtin("length", "value") {
      (pos, ev, value: Val) =>
        value match {
          case s: Val.Str => s.value.length()
          case s: Val.Obj => s.visibleKeyNames.length
          case array: Val.Arr => array.asLazyArray.length
          case s: Val.Func => s.params.names.length
          case x => Error.fail("Expected Array, String, or Object, got: " + x.prettyName)
        }
    },

    builtin("split", "str", "regex") {
      (pos, _, str: String, str2: String) =>
        new Val.Arr(pos, str.split(str2.charAt(0)).toIndexedSeq.map(item => Val.Str(pos, item)).toArray)
    },

    builtin("startsWith", "str1", "str2") {
      (pos, ev, str1: String, str2: String) => str1.toUpperCase().startsWith(str2.toUpperCase());
    },

    builtin("toString", "value") {
      (pos, ev, value: Val) => Materializer.stringify(value)(ev)
    },

    builtin("trim", "str") {
      (pos, ev, str: String) => str.trim()
    },

    builtin("type", "value") {
      (pos, ev, value: Val) =>
        value match {
          case _: Val.Bool => "boolean"
          case _: Val.Null => "null"
          case _: Val.Obj => "object"
          case _: Val.Arr => "array"
          case _: Val.Func => "function"
          case _: Val.Num => "number"
          case _: Val.Str => "string"
        }
    },

    builtin("toUpperCase", "str") {
      (pos, ev, str: String) =>
        str.toUpperCase()
    },

    builtin0("uuid") {
      (pos, _) =>
        UUID.randomUUID().toString
    },

    builtin("values", "obj") {
      (pos, ev, obj: Val.Obj) =>
        new Val.Arr(pos, obj.visibleKeyNames.map(key => obj.value(key, pos)(ev)))
    },

    builtinWithDefaults("write",
      "data" -> Val.Null(dummyPosition),
      "mimeType" -> Val.Null(dummyPosition),
      "params" -> emptyObj) { (args, pos, ev) =>
      val data = args(0)
      val mimeType = args(1).cast[Val.Str].value
      val params = args(2).cast[Val.Obj]
      write(dataFormats, data, mimeType, params, ev)
    },

    // funcs below taken from Std
    builtin("isString", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Str]
    },

    builtin("isBoolean", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.True] || v.isInstanceOf[Val.False]
    },

    builtin("isNumber", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Num]
    },

    builtin("isObject", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Obj]
    },

    builtin("isArray", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Arr]
    },

    builtin("isFunction", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Func]
    },

    builtin("foldLeft", "arr", "init", "func") {
      (pos, ev, arr: Val.Arr, init: Val, func: Val.Func) =>
        var current = init
        var i = 0
        while (i < arr.asLazyArray.length) {
          val c = current
          current = func.apply2(arr.asLazy(i), c, pos.noOffset)(ev)
          i = i + 1
        }
        current
    },

    builtin("foldRight", "arr", "init", "func") {
      (pos, ev, arr: Val.Arr, init: Val, func: Val.Func) =>
        var current = init
        var i = arr.asLazyArray.length - 1
        while (i >= 0) {
          val c = current
          current = func.apply2(arr.asLazy(i), c, pos.noOffset)(ev)
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
        new Val.Arr(pos, arr.asLazyArray.filter(x => !ev.equal(x.force, value)))
    },

    builtin("filterNotIn", "first", "second") {
      (pos, ev, first: Val, second: Val) =>
        first match {
          case arr: Val.Arr =>
            second match {
              case arr2: Val.Arr =>
                // unfortunately cannot use diff here because of lazy values
                new Val.Arr(pos, arr.asLazyArray
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
          case arr: Val.Arr => new Val.Arr(pos, arr.asLazyArray.reverse).asInstanceOf[Val]
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
      (pos, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str => str.value.indexOf(value.cast[Val.Str].value)
          case array: Val.Arr => array.asLazyArray.indexWhere(lzy => ev.equal(lzy.force, value))
          case x => Error.fail("Expected String or Array, got: " + x.prettyName)
        }
    },

    builtin("lastIndexOf", "container", "value") {
      (pos, ev, container: Val, value: Val) =>
        container match {
          case str: Val.Str => str.value.lastIndexOf(value.cast[Val.Str].value)
          case array: Val.Arr => array.asLazyArray.lastIndexWhere(lzy => ev.equal(lzy.force, value))
          case x => Error.fail("Expected String or Array, got: " + x.prettyName)
        }
    },

    builtin("parseNum", "str") { (pos, ev, str: String) =>
      str.toDouble
    }
  ).asJava

  override def modules(dataFormats: DataFormatService,
                       header: Header, importer: Importer): java.util.Map[String, Val.Obj] = Map(
    "datetime" -> moduleFrom(Datetime.functions: _*),

    "duration" -> moduleFrom(Duration.functions: _*),

    "crypto" -> moduleFrom(Crypto.functions: _*),

    "url" -> moduleFrom(URL.functions: _*),

    "math" -> moduleFrom(Math.functions: _*),

    "arrays" -> moduleFrom(Arrays.functions: _*),

    "objects" -> moduleFrom(Objects.functions: _*),

    "numbers" -> moduleFrom(Numbers.functions: _*),

    // TODO: review regexs
    "strings" -> moduleFrom(Strings.functions: _*),

    "base64" -> moduleFrom(Base64.functions: _*)
  ).asJava

  def read(dataFormats: DataFormatService, data: Object, mimeType: String, params: Val.Obj, ev: EvalScope): Val = {
    val paramsAsJava = ujson.read(Materializer.apply(params)(ev)).obj.map(keyVal => {
      (keyVal._1, keyVal._2 match {
        case Str(value) => value
        case Num(value) => String.valueOf(value)
        case bool: Bool => String.valueOf(bool)
        case Null => "null"
      })
    }).asJava

    val doc = Document.of(data, MediaType.parseMediaType(mimeType).withParameters(paramsAsJava))
    Materializer.reverse(dummyPosition, dataFormats.mandatoryRead(doc))
  }

  def write(dataFormats: DataFormatService, json: Val, mimeType: String, params: Val.Obj, ev: EvalScope): String = {
    val paramsAsJava = ujson.read(Materializer.apply(params)(ev)).obj.map(keyVal => {
      (keyVal._1, keyVal._2 match {
        case Str(value) => value
        case Num(value) => String.valueOf(value)
        case bool: Bool => String.valueOf(bool)
        case Null => "null"
      })
    }).asJava
    val mediaType = MediaType.parseMediaType(mimeType).withParameters(paramsAsJava)
    val plugin = dataFormats.thatCanWrite(mediaType, classOf[String])
      .orElseThrow(() => Error.fail("No suitable plugin found for mime type: " + mimeType))

    plugin.write(Materializer.apply(json)(ev), mediaType, classOf[String]).getContent
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
        if (func.apply2(array(i), Val.Num(pos, i), pos.noOffset)(ev).isInstanceOf[Val.True]) {
          out.append(item)
        }
        i = i + 1
      }
    } else if (args == 1) {
      while (i < array.length) {
        val item = array(i)
        if (func.apply1(array(i), pos.noOffset)(ev).isInstanceOf[Val.True]) {
          out.append(item)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    new Val.Arr(pos, out.toArray)
  }

  private def map(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val.Arr = {
    val pos = func.pos
    val args = func.params.names.length
    val out = new Array[Lazy](array.length)

    var i = 0
    if (args == 2) { //2 args
      while (i < array.length) {
        out(i) = func.apply2(array(i), Val.Num(pos, i), pos.noOffset)(ev)
        i = i + 1
      }
    } else if (args == 1) { // 1 arg
      while (i < array.length) {
        out(i) = func.apply1(array(i), pos.noOffset)(ev)
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    new Val.Arr(pos, out)
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
        if (func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev).isInstanceOf[Val.True]) {
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else if (args == 2) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        if (func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev).isInstanceOf[Val.True]) {
          m.append((k, memberOf(v)))
        }
        i = i + 1
      }
    } else if (args == 1) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos.noOffset)(ev)
        if (func.apply1(v, pos.noOffset)(ev).isInstanceOf[Val.True]) {
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
        val inner = func.apply2(array(i).force, Val.Num(pos, i), pos.noOffset)(ev).asArr
        var j = 0
        while (j < inner.length) {
          out.append(inner.asLazyArray(j))
          j = j + 1
        }
        i = i + 1
      }
    } else if (args == 1) { //  1 arg
      while (i < array.length) {
        val inner = func.apply1(array(i).force, pos.noOffset)(ev).asArr
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

    new Val.Arr(pos, out.toArray)
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
        val k = keyFrom(func.apply2(v, Val.Num(pos, i), pos.noOffset)(ev))
        mScala.getOrElseUpdate(k, mutable.ArrayBuffer[Lazy]()).addOne(v)
        i = i + 1
      }
    } else if (args == 1) {
      while (i < array.length) {
        val v = array(i)
        val k = keyFrom(func.apply1(v, pos.noOffset)(ev))
        mScala.getOrElseUpdate(k, mutable.ArrayBuffer[Lazy]()).addOne(v)
        i = i + 1
      }
    }
    else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.asScala.toSeq.map { case (k, buff) => (k, memberOf(new Val.Arr(pos, buff.toArray))) }.toArray: _*)
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
        val funcKey = keyFrom(func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev))
        mScala.getOrElseUpdate(funcKey, new util.LinkedHashMap[String, Val.Obj.Member]()).put(k, memberOf(v))
        i = i + 1
      }
    } else if (args == 1) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos)(ev)
        val funcKey = keyFrom(func.apply1(v, pos.noOffset)(ev))
        mScala.getOrElseUpdate(funcKey, new util.LinkedHashMap[String, Val.Obj.Member]()).put(k, memberOf(v))
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    Val.Obj.mk(pos, m.asScala.toSeq.map { case (k, map) => (k, memberOf(new Val.Obj(pos, map, false, null, null))) }.toArray: _*)
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
        out.append(func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev))
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        out.append(func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev))
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        out.append(func.apply1(v, pos.noOffset)(ev))
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }

    new Val.Arr(pos, out.toArray)
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
        func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, dummyPosition)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, dummyPosition)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply1(v, pos.noOffset)(ev) match {
          case s: Val.Obj =>
            if (s.visibleKeyNames.length > 1) Error.fail("Function must return a single key-value pair, otherwise consider flatMapObject.")
            out.put(s.visibleKeyNames.head, memberOf(s.value(s.visibleKeyNames.head, dummyPosition)(ev)))
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }
    new Val.Obj(pos, out, false, null, null)
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
        func.apply3(v, Val.Str(pos, k), Val.Num(pos, i), pos.noOffset)(ev) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, dummyPosition)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(2)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, dummyPosition)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args.equals(1)) {
      while (i < obj.visibleKeyNames.length) {
        k = obj.visibleKeyNames(i)
        v = obj.value(k, pos)(ev)
        func.apply1(v, pos.noOffset)(ev) match {
          case s: Val.Obj => s.visibleKeyNames.foreach(
            sKey => out.put(sKey, memberOf(s.value(sKey, dummyPosition)(ev)))
          )
          case x => Error.fail("function must return an Object, got: " + x.prettyName)
        }
        i = i + 1
      }
    } else {
      Error.fail("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }
    new Val.Obj(pos, out, false, null, null)
  }

  // TODO: optimize with while-loop
  private def sortBy(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
    val pos = func.pos
    val args = func.params.names.length

    if (args == 2) {
      new Val.Arr(pos,
        array.zipWithIndex.sortBy(
          it => func.apply2(it._1, Val.Num(pos, it._2), pos.noOffset)(ev)
        )(ord = ValOrdering).map(_._1))
    } else if (args == 1) {
      new Val.Arr(pos, array.sortBy(it => func.apply1(it, pos.noOffset)(ev))(ord = ValOrdering))
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
        m.toSeq.sortBy { case (k1, v1) => func.apply2(obj.value(k1, pos)(ev), Val.Str(pos, k1), pos.noOffset)(ev) }(ord = ValOrdering): _*
      )
    } else if (args == 1) {
      Val.Obj.mk(pos,
        m.toSeq.sortBy { case (k1, v1) => func.apply1(obj.value(k1, pos)(ev), pos.noOffset)(ev) }(ord = ValOrdering): _*
      )
    } else {
      Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
  }
}
