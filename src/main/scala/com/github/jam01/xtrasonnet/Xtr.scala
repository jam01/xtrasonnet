package com.github.jam01.xtrasonnet

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
 *      Reimplemented DS under the new Library interface, while keeping some function under ZonedDateTime,
 *      Formats, Crypto, JsonPath, and URL
 *
 * Adopted:
 * - 6b92da38753b0f8d00f12dc5859c644027d92cd1: Added operations for concatentation and removal
 *      Functions: combine, remove, removeMatch
 * - 5666b472de694383231b043c8c7861833831db96: Fixed numbers module to allow long values
 * - 386223447f864492ca4703a4d9eaa49eea9b64a3: Converted util functions to scala
 *      Functions: duplicates, deepFlatten, occurrences
 * - 482b67a18b29a331cbb6366c81885ee35d9c9075: Fixed orderBy Functionality
 *      Functions: orderBy, toString
 * - 5bb242721f728c00432234cd24f7256e21c4caac: Added some expanded functionality
 *      Functions: indexOf, lastIndexOf, datetime.atBeginningOf*
 * - 78acf4ebf5545b88df4cf9f77434335fc857eaa1: Added date function and period module
 * - 5f7619dea8ac4e04e0d7e527999095d6bbac6029: Added String option to reverse function
 *      Functions: reverse
 * - c20475cacff9b6790e85afaf7ae730d4aa9c4470: Merge pull request #86 from datasonnet/unix-timestamp
 *      Functions: datetime.parse
 *
 * Changed:
 * - d19a57dfcf4382669d55ac4427916c8440c1bac3: fixes orderBy comparison
 * - changed remove to rm and rmAll to rmKey and rmKeyIn
 * - removed null support from most functions, including those adopted
 * - refactored datetime to use OffsetDateTime and changed Period functionality for ISO8601 Duration
 */

import com.github.jam01.xtrasonnet.document.Document.BasicDocument
import com.github.jam01.xtrasonnet.document.MediaType
import com.github.jam01.xtrasonnet.header.Header
import com.github.jam01.xtrasonnet.modules.{Crypto, JsonPath}
import com.github.jam01.xtrasonnet.spi.Library
import com.github.jam01.xtrasonnet.spi.Library.{emptyObj, memberOf}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.ReadWriter.{ArrRead, ObjRead, ValRead}
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val.{Builtin, Obj}
import sjsonnet.{Error, EvalScope, Expr, FileScope, Importer, Lazy, Materializer, Position, ReadWriter, Val}
import ujson.{Bool, Null, Num, Str}

import java.math.{BigDecimal, RoundingMode}
import java.net.URL
import java.security.SecureRandom
import java.text.DecimalFormat
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util
import java.util.{Base64, Collections, Scanner, UUID}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.util.Random

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

  private val dummyPosition = new Position(null, 0)

  override def functions(dataFormats: DataFormatService,
                         header: Header, importer: Importer): java.util.Map[String, Val.Func] = Map(
    builtin("contains", "container", "value") {
      (_, ev, container: Val, value: Val) =>
        container match {
          // See: scala.collection.IterableOnceOps.exists
          case array: Val.Arr =>
            array.asLazyArray.exists(v => ev.equal(v.force, value))
          case obj: Val.Obj =>
            obj.containsVisibleKey(value.asString)
          case str: Val.Str =>
            str.value.r.findAllMatchIn(str.value).nonEmpty;
          case x => Error.fail("Expected Array or String, got: " + x.prettyName)
        }
    },

    builtin("entriesOf", "obj") {
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
        (Math.ceil(value) != Math.floor(value)).booleanValue()
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

    builtin("isEven", "num") {
      (pos, ev, num: Double) =>
        (num % 2) == 0
    },

    builtin("isInteger", "value") {
      (pos, ev, value: Double) =>
        (Math.ceil(value) == Math.floor(value)).booleanValue()
    },

    builtin("isOdd", "num") {
      (pos, ev, num: Double) =>
        (num % 2) != 0
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

    builtin("keysOf", "obj") {
      (pos, _, obj: Val.Obj) =>
        new Val.Arr(pos, obj.visibleKeyNames.map(item => Val.Str(pos, item)))
    },

    builtin("lower", "str") {
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

    builtin("orderBy", "value", "func") {
      (pos, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => orderBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => orderBy(obj, func, ev)
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
        url match {
          case str if str.startsWith("classpath://") => importer.read(ClasspathPath(str.substring(12))) match {
            case Some(value) => read(dataFormats, value, mimeType, params, ev)
            case None => Val.Null(pos)
          }
          case _ =>
            val out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
            read(dataFormats, out, mimeType, params, ev)
        }
    },

    builtin("sizeOf", "value") {
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

    builtin("typeOf", "value") {
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
            // TODO: append null if current smaller than max
            current.append(lazyArr(j).force.asArr.asLazyArray(i))
            j = j + 1
          }
          out.append(new Val.Arr(pos, current.toArray))
          i = i + 1
        }

        new Val.Arr(pos, out.toArray)
    },

    builtin("upper", "str") {
      (pos, ev, str: String) =>
        str.toUpperCase()
    },

    builtin0("uuid") {
      (pos, _) =>
        UUID.randomUUID().toString
    },

    builtin("valuesOf", "obj") {
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

    builtin("zip", "array1", "array2") {
      (pos, ev, array1: Val.Arr, array2: Val.Arr) =>

        val smallArray = if (array1.asLazyArray.length <= array2.asLazyArray.length) array1 else array2
        val bigArray = (if (smallArray == array1) array2 else array1).asLazyArray
        val out = new ArrayBuffer[Lazy]
        for ((v, i) <- smallArray.asLazyArray.zipWithIndex) {
          val current = new ArrayBuffer[Lazy]
          if (smallArray == array1) {
            current.append(v)
            current.append(bigArray(i))
          }
          else {
            current.append(bigArray(i))
            current.append(v)
          }
          out.append(new Val.Arr(pos, current.toArray))
        }
        new Val.Arr(pos, out.toArray)
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

    builtinWithDefaults("objectFrom",
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

      val m = new util.LinkedHashMap[String, Val.Obj.Member](lzyArr.length)
      var i = 0
      while (i < lzyArr.length) {
        val k = kFunc.apply1(lzyArr(i), pos.noOffset)(ev)
        if (!k.isInstanceOf[Val.Str]) Error.fail("Key Function should return a String, got: " + k.prettyName)
        val j = i.intValue // ints are objects in Scala, so we must set a 'final' reference

        m.put(k.asString,
          if (vFunc.isInstanceOf[Val.False]) new Obj.Member(false, Visibility.Normal) {
            override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = lzyArr(j).force
          } else new Obj.Member(false, Visibility.Normal) {
            override def invoke(self: Obj, sup: Obj, fs: FileScope, ev: EvalScope): Val = vFunc.asFunc.apply1(lzyArr(j), pos.noOffset)(ev)
          })
        i = i + 1
      }

      new Val.Obj(pos, m, false, null, null).asInstanceOf[Val]
    },

    builtin("parseNum", "str") { (pos, ev, str: String) =>
      str.toDouble
    }
  ).asJava

  override def modules(dataFormats: DataFormatService,
                       header: Header, importer: Importer): java.util.Map[String, Val.Obj] = Map(
    "datetime" -> moduleFrom(
      builtin0("now") { (pos, ev) => OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },

      builtin("parse", "datetime", "inputFormat") { (pos, ev, datetime: Val, inputFormat: String) =>
        var datetimeObj: OffsetDateTime = null
        inputFormat.toLowerCase match {
          case "epoch" =>
            var inst: Instant = null
            datetime match {
              case str: Val.Str => inst = Instant.ofEpochSecond(str.value.toInt.toLong)
              case num: Val.Num => inst = Instant.ofEpochSecond(num.value.toLong)
              case _ => Error.fail("Expected datetime to be a string or number, got: " + datetime.prettyName)
            }
            datetimeObj = OffsetDateTime.ofInstant(inst, ZoneOffset.UTC)
          case _ => datetimeObj = try {
            OffsetDateTime.parse(datetime.asString, DateTimeFormatter.ofPattern(inputFormat)) // parse will throw if there's no zone offset
          } catch {
            case _: DateTimeException =>
              LocalDateTime.parse(datetime.asString, DateTimeFormatter.ofPattern(inputFormat)).atOffset(ZoneOffset.UTC) // default to UTC
          }
        }
        datetimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("format", "datetime", "outputFormat") { (pos, ev, datetime: String, outputFormat: String) =>
        val datetimeObj = OffsetDateTime.parse(datetime)
        datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
      },

      builtin("compare", "datetime", "datetwo") { (pos, ev, datetimeone: String, datetimetwo: String) =>
        val datetimeObj1 = OffsetDateTime.parse(datetimeone)
        val datetimeObj2 = OffsetDateTime.parse(datetimetwo)

        Math.max(-1, Math.min(datetimeObj1.compareTo(datetimeObj2), 1))
      },

      builtin("plus", "datetime", "duration") { (pos, ev, date: String, duration: String) =>
        var datetime = OffsetDateTime.parse(date)
        val timeIdx = duration.indexOf('T')

        if (timeIdx != -1) {
          datetime = datetime
            .plus(Duration.parse('P' + duration.substring(timeIdx)))
            .plus(Period.parse(duration.substring(0, timeIdx)))
        } else {
          datetime = datetime.plus(Period.parse(duration))
        }

        datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("minus", "datetime", "duration") { (pos, ev, date: String, duration: String) =>
        var datetime = OffsetDateTime.parse(date)
        val timeIdx = duration.indexOf('T')

        if (timeIdx != -1) {
          datetime = datetime
            .minus(Duration.parse('P' + duration.substring(timeIdx)))
            .minus(Period.parse(duration.substring(0, timeIdx)))
        } else {
          datetime = datetime.minus(Period.parse(duration))
        }

        datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("inOffset", "datetime", "offset") {
        (pos, ev, datetime: String, offset: String) =>
          val datetimeObj = OffsetDateTime.parse(datetime)
          val zoneId = ZoneOffset.of(offset)
          val newDateTimeObj = datetimeObj.withOffsetSameInstant(zoneId)
          newDateTimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("toLocalDate", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = OffsetDateTime.parse(datetime)
        datetimeObj.toLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },

      builtin("toLocalTime", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = OffsetDateTime.parse(datetime)
        datetimeObj.toLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
      },

      builtin("toLocalDateTime", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = OffsetDateTime.parse(datetime)
        datetimeObj.toLocalDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },

      builtin("between", "datetimeone", "datetimetwo") {
        (_, _, datetimeone: String, datetimetwo: String) =>
          val d1 = OffsetDateTime.parse(datetimeone)
          val d2 = OffsetDateTime.parse(datetimetwo)

          val dur = Duration.between(d1, d2)
          val isNeg = dur.isNegative
          val durStr = dur.abs.toString
          val hoursIdx = durStr.indexOf('H')

          if (hoursIdx == -1) {
            (if (isNeg) "-" else "") + durStr
          } else {
            var hours = durStr.substring(2, hoursIdx).toLong
            if (hours < 24) {
              (if (isNeg) "-" else "") + durStr
            } else {
              val days = (hours / 24).toInt
              hours = hours % 24
              val per = Period.between(d1.toLocalDate, d1.toLocalDate.plusDays(days)).toString
              (if (isNeg) "-" else "") + per + (
                if (hours == 0 && durStr.endsWith("H")) "" // only had hours and now 0, remove
                else "T" +
                  (if (hours == 0) durStr.substring(hoursIdx + 1) // hours are now 0, remove hours
                  else hours + durStr.substring(hoursIdx)) // some hours remaining
                )
            }
          }
      },

      builtin("isLeapYear", "datetime") {
        (pos, ev, datetime: String) =>
          OffsetDateTime
            .parse(datetime)
            .toLocalDate.isLeapYear;
      },

      builtin("atBeginningOfDay", "datetime") {
        (_, _, datetime: String) =>
          val date = OffsetDateTime.parse(datetime)
          date.minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfHour", "datetime") {
        (_, _, datetime: String) =>
          val date = OffsetDateTime.parse(datetime)
          date.minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfMonth", "datetime") {
        (_, _, datetime: String) =>
          val date = OffsetDateTime
            .parse(datetime)
          date.minusDays(date.getDayOfMonth - 1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfWeek", "datetime") {
        (_, _, datetime: String) =>
          val date = OffsetDateTime
            .parse(datetime)

          date.minusDays(if (date.getDayOfWeek.getValue == 7) 0 else date.getDayOfWeek.getValue)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfYear", "datetime") {
        (_, _, datetime: String) =>
          val date = OffsetDateTime
            .parse(datetime)
          date.minusMonths(date.getMonthValue - 1)
            .minusDays(date.getDayOfMonth - 1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("of", "obj") {
        (pos, ev, obj: Val.Obj) =>
          //year, month, dayOfMonth, hour, minute, second, nanoSecond, zoneId
          val out = mutable.Map[String, Val]()
          obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
          OffsetDateTime.of(
            out.getOrElse("year", Val.Num(pos, 0)).asInt,
            out.getOrElse("month", Val.Num(pos, 1)).asInt,
            out.getOrElse("day", Val.Num(pos, 1)).asInt,
            out.getOrElse("hour", Val.Num(pos, 0)).asInt,
            out.getOrElse("minute", Val.Num(pos, 0)).asInt,
            out.getOrElse("second", Val.Num(pos, 0)).asInt,
            out.getOrElse("nanosecond", Val.Num(pos, 0)).asInt,
            ZoneOffset.of(out.getOrElse("offset", Val.Str(pos, "Z")).cast[Val.Str].value)
          ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("today") {
        (_, _) =>
          val date = OffsetDateTime.now()
          date.minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("tomorrow") {
        (_, _) =>
          val date = OffsetDateTime.now()
          date.plusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("yesterday") {
        (_, _) =>
          val date = OffsetDateTime.now()
          date.minusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("toParts", "datetime") {
        (pos, ev, datetime: String) =>
          val date = OffsetDateTime.parse(datetime)
          val out = new util.LinkedHashMap[String, Val.Obj.Member]
          out.put("year", memberOf(Val.Num(pos, date.getYear)))
          out.put("month", memberOf(Val.Num(pos, date.getMonthValue)))
          out.put("day", memberOf(Val.Num(pos, date.getDayOfWeek.getValue)))
          out.put("hour", memberOf(Val.Num(pos, date.getHour)))
          out.put("minute", memberOf(Val.Num(pos, date.getMinute)))
          out.put("second", memberOf(Val.Num(pos, date.getSecond)))
          out.put("nanosecond", memberOf(Val.Num(pos, date.getNano)))
          out.put("offset", memberOf(Val.Str(pos, date.getOffset.getId)))

          new Val.Obj(pos, out, false, null, null)
      }
    ),

    "duration" -> moduleFrom(
      builtin("of", "obj") {
        (pos, ev, obj: Val.Obj) =>
          val out = mutable.Map[String, Val]()
          obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
          Period.ZERO
            .plusYears(out.getOrElse("years", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusMonths(out.getOrElse("months", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusDays(out.getOrElse("days", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .toString +
            Duration.ZERO
              .plusHours(out.getOrElse("hours", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
              .plusMinutes(out.getOrElse("minutes", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
              .plusSeconds(out.getOrElse("seconds", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
              .toString.substring(1)
      },

      builtin("toParts", "str") {
        (pos, ev, duration: String) =>
          val out = new util.LinkedHashMap[String, Val.Obj.Member]
          val timeIdx = duration.indexOf('T')
          var period: Period = null
          var dduration: Duration = null

          if (timeIdx != -1) {
            dduration = Duration.parse('P' + duration.substring(timeIdx))
            period = Period.parse(duration.substring(0, timeIdx))
          } else {
            period = Period.parse(duration.substring(0, timeIdx))
          }

          out.put("years", memberOf(Val.Num(pos, period.getYears)))
          out.put("months", memberOf(Val.Num(pos, period.getMonths)))
          out.put("days", memberOf(Val.Num(pos, period.getDays)))
          if (dduration != null) { // TODO: probably super inefficient
            val hours = dduration.toHours
            val minutes = dduration.minusHours(hours).toMinutes
            val seconds = dduration.minusHours(hours).minusMinutes(minutes).toSeconds
            out.put("hours", memberOf(Val.Num(pos, hours)))
            out.put("minutes", memberOf(Val.Num(pos, minutes)))
            out.put("seconds", memberOf(Val.Num(pos, seconds)))
          }

          new Val.Obj(pos, out, false, null, null)
      }
    ),

    "crypto" -> moduleFrom(
      builtin("hash", "value", "algorithm") {
        (pos, ev, value: String, algorithm: String) =>
          Crypto.hash(value, algorithm)
      },

      builtin("hmac", "value", "secret", "algorithm") {
        (pos, ev, value: String, secret: String, algorithm: String) =>
          Crypto.hmac(value, secret, algorithm)
      },

      builtin("encrypt", "value", "secret", "algorithm") {
        (pos, ev, value: String, secret: String, transformation: String) =>
          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            Base64.getEncoder.encodeToString(cipher.doFinal(value.getBytes))
          } else {
            // https://stackoverflow.com/a/52571774/4814697
            val rand: SecureRandom = new SecureRandom()
            val iv = new Array[Byte](cipher.getBlockSize)
            rand.nextBytes(iv)

            cipher.init(Cipher.ENCRYPT_MODE,
              new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
              new IvParameterSpec(iv),
              rand)

            // encrypted data:
            val encryptedBytes = cipher.doFinal(value.getBytes)

            // append Initiation Vector as a prefix to use it during decryption:
            val combinedPayload = new Array[Byte](iv.length + encryptedBytes.length)

            // populate payload with prefix IV and encrypted data
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length)
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length)

            Base64.getEncoder.encodeToString(combinedPayload)
          }
      },

      builtin("decrypt", "value", "secret", "algorithm") {
        (pos, ev, value: String, secret: String, transformation: String) =>
          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            new String(cipher.doFinal(Base64.getDecoder.decode(value)))
          } else {
            // https://stackoverflow.com/a/52571774/4814697
            // separate prefix with IV from the rest of encrypted data//separate prefix with IV from the rest of encrypted data
            val encryptedPayload = Base64.getDecoder.decode(value)
            val iv = new Array[Byte](cipher.getBlockSize)
            val encryptedBytes = new Array[Byte](encryptedPayload.length - iv.length)
            val rand: SecureRandom = new SecureRandom()

            // populate iv with bytes:
            System.arraycopy(encryptedPayload, 0, iv, 0, iv.length)

            // populate encryptedBytes with bytes:
            System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length)

            cipher.init(Cipher.DECRYPT_MODE,
              new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
              new IvParameterSpec(iv),
              rand)

            new String(cipher.doFinal(encryptedBytes))
          }
      }
    ),

    "jsonpath" -> moduleFrom(
      builtin("eval", "json", "path") {
        (pos, ev, json: Val, path: String) =>
          Materializer.reverse(pos, ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      }
    ),

    "url" -> moduleFrom(
      builtinWithDefaults("encode",
        "data" -> Val.Null(dummyPosition),
        "encoding" -> Val.Str(dummyPosition, "UTF-8")) { (args, pos, ev) =>
        val data = args(0).cast[Val.Str].value
        val encoding = args(1).cast[Val.Str].value

        java.net.URLEncoder.encode(data, encoding)
      },

      builtinWithDefaults("decode",
        "data" -> Val.Null(dummyPosition),
        "encoding" -> Val.Str(dummyPosition, "UTF-8")) { (args, pos, ev) =>
        val data = args(0).cast[Val.Str].value
        val encoding = args(1).cast[Val.Str].value

        java.net.URLDecoder.decode(data, encoding)
      }
    ),

    "math" -> moduleFrom(
      builtin("abs", "num") {
        (pos, ev, num: Double) =>
          Math.abs(num);
      },

      // See: https://damieng.com/blog/2014/12/11/sequence-averages-in-scala
      // See: https://gist.github.com/gclaramunt/5710280
      builtin("avg", "array") {
        (pos, ev, array: Val.Arr) =>
          val (sum, length) = array.asLazyArray.foldLeft((0.0, 0))({
            case ((sum, length), num) =>
              (num.force match {
                case num: Val.Num => sum + num.value
                case x => Error.fail("Expected Array pf Numbers, got: Array of " + x.prettyName)
              }, 1 + length)
          })
          sum / length
      },

      builtin("ceil", "num") {
        (pos, ev, num: Double) =>
          Math.ceil(num);
      },

      builtin("floor", "num") {
        (pos, ev, num: Double) =>
          Math.floor(num);
      },

      builtin("pow", "num1", "num2") {
        (pos, ev, num1: Double, num2: Double) =>
          Math.pow(num1, num2)
      },

      builtin0("random") {
        (pos, ev) =>
          (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
      },

      builtin("randomInt", "num") {
        (pos, ev, num: Int) =>
          (Random.nextInt(num - 0) + 0).intValue()
      },

      builtinWithDefaults("round",
        "num" -> Val.Null(dummyPosition),
        "precision" -> Val.Num(dummyPosition, 0)) { (args, pos, ev) =>
        val num = args(0).cast[Val.Num].value
        val prec = args(1).cast[Val.Num].value.toInt

        if (prec == 0) {
          Math.round(num).intValue()
        } else {
          BigDecimal.valueOf(num).setScale(prec, RoundingMode.HALF_UP).doubleValue()
        }
      },

      builtin("sqrt", "num") {
        (pos, ev, num: Double) =>
          Math.sqrt(num)
      },

      builtin("sum", "array") {
        (pos, ev, array: Val.Arr) =>
          array.asLazyArray.foldLeft(0.0)((sum, value) =>
            value.force match {
              case num: Val.Num => sum + num.value
              case x => Error.fail("Expected Array of Numbers, got: Array of " + x.prettyName)
            }
          )
      },

      // funcs below taken from Std but using Java's Math
      builtin("clamp", "x", "minVal", "maxVal") { (pos, ev, x: Double, minVal: Double, maxVal: Double) =>
        Math.max(minVal, Math.min(x, maxVal))
      },

      builtin("pow", "x", "n") { (pos, ev, x: Double, n: Double) =>
        Math.pow(x, n)
      },

      builtin("sin", "x") { (pos, ev, x: Double) =>
        Math.sin(x)
      },

      builtin("cos", "x") { (pos, ev, x: Double) =>
        Math.cos(x)
      },

      builtin("tan", "x") { (pos, ev, x: Double) =>
        Math.tan(x)
      },

      builtin("asin", "x") { (pos, ev, x: Double) =>
        Math.asin(x)
      },

      builtin("acos", "x") { (pos, ev, x: Double) =>
        Math.acos(x)
      },

      builtin("atan", "x") { (pos, ev, x: Double) =>
        Math.atan(x)
      },

      builtin("log", "x") { (pos, ev, x: Double) =>
        Math.log(x)
      },

      builtin("exp", "x") { (pos, ev, x: Double) =>
        Math.exp(x)
      },

      builtin("mantissa", "x") { (pos, ev, x: Double) =>
        x * Math.pow(2.0, -((Math.log(x) / Math.log(2)).toInt + 1))
      },

      builtin("exponent", "x") { (pos, ev, x: Double) =>
        (Math.log(x) / Math.log(2)).toInt + 1
      }
    ),

    "arrays" -> moduleFrom(
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

      builtin("splitEvery", "array", "size") { // TODO: better name?
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

      builtin("duplicates", "array") {
        (pos, ev, array: Val.Arr) =>
          val out = mutable.ArrayBuffer[Lazy]()
          array.asLazyArray.collect({
            case item if array.asLazyArray.count(lzy => ev.equal(lzy.force, item.force)) >= 2 &&
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

      builtin("deepFlatten", "arr") {
        (pos, ev, arr: Val.Arr) =>
          new Val.Arr(pos, deepFlatten(arr.asLazyArray))
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

      builtin4("innerJoin", "arrL", "arryR", "funcL", "funcR") {
        (pos, ev, arrL: Val.Arr, arrR: Val.Arr, funcL: Val.Func, funcR: Val.Func) =>
          val out = new ArrayBuffer[Lazy]

          arrL.asLazyArray.foreach({
            valueL =>
              val compareL = funcL.apply1(valueL, pos.noOffset)(ev)
              //append all that match the condition
              out.appendAll(arrR.asLazyArray.collect({
                case valueR if ev.equal(compareL, funcR.apply1(valueR, pos.noOffset)(ev)) =>
                  val temp = new util.LinkedHashMap[String, Val.Obj.Member]()
                  temp.put("l", memberOf(valueL.force))
                  temp.put("r", memberOf(valueR.force))
                  new Val.Obj(pos, temp, false, null, null)
              }))
          })
          new Val.Arr(pos, out.toArray)
      },

      builtin4("leftJoin", "arrL", "arryR", "funcL", "funcR") {
        (pos, ev, arrL: Val.Arr, arrR: Val.Arr, funcL: Val.Func, funcR: Val.Func) =>
          //make backup array for leftovers
          var leftoversL = arrL.asLazyArray
          val out = new ArrayBuffer[Lazy]

          arrL.foreach({
            valueL =>
              val compareL = funcL.apply1(valueL, pos.noOffset)(ev)
              //append all that match the condition
              out.appendAll(arrR.asLazyArray.collect({
                case valueR if ev.equal(compareL, funcR.apply1(valueR, pos.noOffset)(ev)) =>
                  val temp = new util.LinkedHashMap[String, Val.Obj.Member]()
                  //remove matching values from the leftOvers arrays
                  leftoversL = leftoversL.filter(item => !ev.equal(item.force, valueL.force))

                  temp.put("l", memberOf(valueL.force))
                  temp.put("r", memberOf(valueR.force))
                  new Val.Obj(pos, temp, false, null, null)
              }))
          })

          out.appendAll(leftoversL.map(
            leftOver =>
              Val.Obj.mk(pos, ("l" -> memberOf(leftOver.force)))
          ))
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

      builtin4("rightJoin", "arrL", "arrR", "funcL", "funcR") {
        (pos, ev, arrL: Val.Arr, arrR: Val.Arr, funcL: Val.Func, funcR: Val.Func) =>
          //make backup array for leftovers
          var lzArrL = arrL.asLazyArray
          var lzArrR = arrR.asLazyArray

          val out = new ArrayBuffer[Lazy]

          lzArrL.foreach({
            valueL =>
              val compareL = funcL.apply1(valueL, pos.noOffset)(ev)
              // append all that match the condition
              out.appendAll(arrR.asLazyArray.collect({
                case valueR if ev.equal(compareL, funcR.apply1(valueR, pos.noOffset)(ev)) =>
                  val temp = new util.LinkedHashMap[String, Val.Obj.Member]()
                  // remove matching values from the leftOvers arrays
                  lzArrL = lzArrL.filter(item => !ev.equal(item.force, valueL.force))
                  lzArrR = lzArrR.filter(item => !ev.equal(item.force, valueR.force))

                  temp.put("l", memberOf(valueL.force))
                  temp.put("r", memberOf(valueR.force))
                  new Val.Obj(pos, temp, false, null, null)
              }))
          })

          out.appendAll(lzArrL.map(
            leftOver =>
              Val.Obj.mk(pos, "l" -> memberOf(leftOver.force)
              ))).appendAll(lzArrR.map(
            leftOver =>
              Val.Obj.mk(pos, "r" -> memberOf(leftOver.force)
              )))
          new Val.Arr(pos, out.toArray)
      },

      builtin("partition", "arr", "func") {
        (pos, ev, array: Val.Arr, func: Val.Func) =>
          val out = new util.LinkedHashMap[String, Val.Obj.Member]()
          val part = array.asLazyArray.partition(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
          out.put("success", memberOf(new Val.Arr(pos, part._1)))
          out.put("failure", memberOf(new Val.Arr(pos, part._2)))
          new Val.Obj(pos, out, false, null, null)
      },

      builtin("any", "value", "func") {
        (pos, ev, array: Val.Arr, func: Val.Func) => array.asLazyArray.exists(item => func.apply1(item, pos.noOffset)(ev).isInstanceOf[Val.True])
      },

      builtin("splitAt", "array", "index") {
        (pos, ev, array: Val.Arr, index: Int) =>
          val split = array.asLazyArray.splitAt(index)
          val out = new util.LinkedHashMap[String, Val.Obj.Member]()

          out.put("l", memberOf(new Val.Arr(pos, split._1)))
          out.put("r", memberOf(new Val.Arr(pos, split._2)))
          new Val.Obj(pos, out, false, null, null)
      },

      builtin("splitWhere", "arr", "func") {
        (pos, ev, arr: Val.Arr, func: Val.Func) =>
          val split = arr.asLazyArray.splitAt(arr.asLazyArray.indexWhere(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True]))
          val out = new util.LinkedHashMap[String, Val.Obj.Member]()

          out.put("l", memberOf(new Val.Arr(pos, split._1)))
          out.put("r", memberOf(new Val.Arr(pos, split._2)))
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
      }
    ),

    "objects" -> moduleFrom(
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
      }
    ),

    "numbers" -> moduleFrom(
      builtin("fromBinary", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if ("[^2-9]".r.matches(x.toString)) {
                Error.fail("Expected Binary, got: Number")
              }
              else BigInt.apply(x.value.toLong.toString, 2).bigInteger.doubleValue
            case x: Val.Str => BigInt.apply(x.value, 2).bigInteger.doubleValue
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("fromHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if ("[^0-9a-f]".r.matches(x.value.toString.toLowerCase())) {
                Error.fail("Expected Hexadecimal, got: Number")
              }
              else BigInt.apply(x.value.toLong.toString, 16).bigInteger.doubleValue
            case x: Val.Str => BigInt.apply(x.asString, 16).bigInteger.doubleValue
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("fromRadix", "value", "num") {
        (pos, ev, value: Val, num: Int) =>
          value match {
            case x: Val.Num => BigInt.apply(x.value.toLong.toString, num).bigInteger.doubleValue
            case x: Val.Str => BigInt.apply(x.value, num).bigInteger.doubleValue
            case x => Error.fail("Expected Base(num), got: " + x.prettyName)
          }
      },

      builtin("toBinary", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) "-" + x.value.toLong.abs.toBinaryString
              else x.value.toLong.toBinaryString
            case x: Val.Str =>
              if (x.value.startsWith("-")) x.value.toLong.abs.toBinaryString
              else x.value.toLong.toBinaryString
            case x => Error.fail("Expected Number or String, got: " + x.prettyName)
          }
      },

      builtin("toHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) "-" + x.value.toLong.abs.toHexString.toUpperCase
              else x.value.toLong.toHexString.toUpperCase
            case x: Val.Str =>
              if (x.value.startsWith("-")) x.value.toLong.abs.toHexString.toUpperCase
              else x.value.toLong.toHexString.toUpperCase
            case x => Error.fail("Expected Number or String, got: " + x.prettyName)
          }
      },

      builtin("toRadix", "value", "num") {
        (pos, ev, value: Val, num: Int) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) "-" + BigInt.apply(x.value.toLong).toString(num)
              else BigInt.apply(x.value.toLong).toString(num)
            // Val.Str(Integer.toString(x.toInt, num))
            case x: Val.Str =>
              if (x.value.startsWith("-")) "-" + BigInt.apply(x.value.toLong).toString(num)
              else BigInt.apply(x.value.toLong).toString(num)
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
            //DW functions does not support null
          }
      },

      builtin("fromOctal", "str") { (pos, ev, num: Val) =>
        num match {
          case str: Val.Str => Integer.parseInt(str.asString, 8)
          case n: Val.Num => Integer.parseInt(n.asInt.toString, 8)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
      }
    ),

    "strings" -> moduleFrom(
      builtin("appendIfMissing", "str1", "str2") {
        (pos, ev, str: String, append: String) =>
          var ret = str
          if (!ret.endsWith(append)) {
            ret = ret + append
          }
          ret
      },

      builtin("camelize", "str") {
        (pos, ev, str: String) =>
          //regex fo _CHAR
          "([A-Z])|[\\s-_]+(\\w)".r("head", "tail").replaceAllIn(str, found => {
            if (found.group(2) != null) found.group(2).toUpperCase
            else found.group(1).toLowerCase
          })
      },

      builtin("capitalize", "str") {
        (pos, ev, str: String) =>
          //regex fo _CHAR
          val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
          val middleRegex = "([a-z])([A-Z])".r("end", "start")

          //Start string at first non underscore, lower case xt
          var temp = str.substring("[0-9A-Za-z]".r.findFirstMatchIn(str).map(_.start).toList.head)
          temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toUpper.toString)

          //replace and uppercase
          temp = regex.replaceAllIn(temp, m => s" ${(m group "two").toUpperCase() + (m group "three").toLowerCase()}")
          temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"} ${(m group "start").toUpperCase()}")

          temp
      },

      builtin("charCode", "str") {
        (pos, ev, str: String) =>
          str.codePointAt(0)
      },

      builtin("charCodeAt", "str", "num") {
        (pos, ev, str: String, num: Int) =>
          str.codePointAt(num)
      },

      builtin("dasherize", "str") {
        (pos, ev, str: String) =>
          //regex fo _CHAR
          val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
          val middleRegex = "([a-z])([A-Z])".r("end", "start")

          //Start string at first non underscore, lower case xt
          var temp = str

          //replace and uppercase
          temp = regex.replaceAllIn(temp, m => s"-${(m group "two") + (m group "three").toLowerCase()}")
          temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}-${m group "start"}")

          temp.toLowerCase()
      },

      builtin("ofCharCode", "num") {
        (pos, ev, num: Int) =>
          String.valueOf(num.asInstanceOf[Char])
      },

      builtin("isAlpha", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str => "^[A-Za-z]+$".r.matches(value.value)
            case _: Val.Num => false
            case _: Val.Bool => true
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isAlphanumeric", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str => "^[A-Za-z0-9]+$".r.matches(value.value)
            case _: Val.Num => true
            case _: Val.Bool => true
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isLowerCase", "str") {
        (pos, ev, str: String) => "^[a-z]+$".r.matches(str)
      },

      builtin("isNumeric", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str => "^[0-9]+$".r.matches(value.value)
            case _: Val.Num => true
            case _: Val.Bool => false
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isUpperCase", "str") {
        (pos, ev, str: String) => "^[A-Z]+$".r.matches(str)
      },

      builtin("leftPad", "str", "offset", "pad") {
        (pos, ev, str: Val, size: Int, pad: String) =>
          str match {
            case str: Val.Str => ("%" + size + "s").format(str.value).replace(" ", pad.substring(0, 1))
            //TODO change to use sjsonnet's Format and DecimalFormat
            case x: Val.Num => ("%" + size + "s").format(new DecimalFormat("0.#").format(x.value)).replace(" ", pad.substring(0, 1))
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("numOrdinalOf", "num") {
        (pos, ev, num: Val) =>
          val str = num match { //convert number value to string
            case value: Val.Str =>
              if ("^[0-9]+$".r.matches(value.value)) value.value
              else Error.fail("Expected Number, got: " + value.value)
            case value: Val.Num => value.value.toInt.toString
            case _ => Error.fail("Expected Number, got: " + num.prettyName)
          }
          if (str.endsWith("11") || str.endsWith("12") || str.endsWith("13")) str + "th"
          else {
            if (str.endsWith("1")) str + "st"
            else if (str.endsWith("2")) str + "nd"
            else if (str.endsWith("3")) str + "rd"
            else str + "th"
          }
      },

      builtin("pluralize", "value") {
        (pos, ev, str: String) =>
          val comparator = str.toLowerCase()
          val specialSList = List("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
          if (specialSList.contains(comparator)) {
            str + "s"
          }
          else if (comparator.isEmpty) ""
          else {
            if (comparator.endsWith("y")) str.substring(0, str.length - 1) + "ies"
            else if (comparator.endsWith("x")) str + "es"
            else str + "s"
          }
      },

      builtin("prependIfMissing", "str1", "str2") {
        (pos, ev, str: String, append: String) =>
          var ret = str
          if (!ret.startsWith(append)) {
            ret = append + ret
          }
          ret
      },

      builtin("repeat", "str", "num") {
        (pos, ev, str: String, num: Int) =>
          var ret = ""
          for (_ <- 0 until num) {
            ret += str
          }
          ret
      },

      builtin("rightPad", "str", "offset", "pad") {
        (pos, ev, value: Val, offset: Int, pad: String) =>
          value match {
            case str: Val.Str => str.value.padTo(offset, pad.charAt(0))
            //TODO change to use sjsonnet's Format and DecimalFormat
            case x: Val.Num => new DecimalFormat("0.#").format(x.value).padTo(offset, pad.charAt(0))
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("singularize", "value") {
        (pos, ev, s: String) =>
          if (s.endsWith("ies"))
            s.substring(0, s.length - 3) + "y"
          else if (s.endsWith("es"))
            s.substring(0, s.length - 2)
          else
            s.substring(0, s.length - 1)
      },

      builtin("substringAfter", "value", "sep") {
        (pos, ev, s: String, sep: String) =>
          s.substring(
            s.indexOf(sep) match {
              case -1 => s.length
              case i => if (sep.equals("")) i else i + 1
            }
          )
      },

      builtin("substringAfterLast", "value", "sep") {
        (pos, ev, s: String, sep: String) =>
          val split = s.split(sep)
          if (sep.equals("")) ""
          else if (split.length == 1) ""
          else split(split.length - 1)
      },

      builtin("substringBefore", "value", "sep") {
        (pos, ev, s: String, sep: String) =>
          s.substring(0,
            s.indexOf(sep) match {
              case -1 => 0
              case x => x
            }
          )
      },

      builtin("substringBeforeLast", "value", "sep") {
        (pos, ev, s: String, sep: String) =>
          s.substring(0,
            s.lastIndexOf(sep) match {
              case -1 => 0
              case x => x
            }
          )
      },

      builtin("underscore", "str") {
        (pos, ev, str: String) =>
          //regex fo _CHAR
          val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
          val middleRegex = "([a-z])([A-Z])".r("end", "start")

          //Start string at first non underscore, lower case xt
          var temp = str.substring("[0-9A-Za-z]".r.findFirstMatchIn(str).map(_.start).toList.head)
          temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

          //replace and uppercase
          temp = regex.replaceAllIn(temp, m => s"_${(m group "two") + (m group "three")}")
          temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}_${m group "start"}")

          temp.toLowerCase
      },

      builtin("unwrap", "value", "wrapper") {
        (pos, ev, str: String, wrapper: String) =>
          val starts = str.startsWith(wrapper)
          val ends = str.endsWith(wrapper)
          if (starts && ends) str.substring(0 + wrapper.length, str.length - wrapper.length)
          else if (starts) str.substring(0 + wrapper.length, str.length) + wrapper
          else if (ends) wrapper + str.substring(0, str.length - wrapper.length)
          else str
      },

      builtin("truncate", "value", "num") {
        (pos, ev, str: String, num: Int) =>
          if (str.length <= num) str
          else str.substring(0, num)
      },

      builtin("wrapIfMissing", "value", "wrapper") {
        (pos, ev, str: String, wrapper: String) =>
          val ret = new mutable.StringBuilder(str)
          if (!str.startsWith(wrapper)) ret.insert(0, wrapper)
          if (!str.endsWith(wrapper)) ret.append(wrapper)
          ret.toString()
      },

      builtin("wrap", "value", "wrapper") {
        (pos, ev, str: String, wrapper: String) => wrapper + str + wrapper
      },

      // todo: and these?
//      builtin("scan", "str", "regex") {
//        (pos, ev, str: String, regex: String) =>
//          new Val.Arr(pos, regex.r.findAllMatchIn(str).map(item => {
//            new Val.Arr(pos, (0 to item.groupCount).map(i => Val.Str(pos, item.group(i))).toArray)
//          }).toArray
//          )
//      },
//
//      builtin("match", "string", "regex") {
//        (pos, _, string: String, regex: String) =>
//          val out = new ArrayBuffer[Lazy]
//          regex.r.findAllMatchIn(string).foreach(
//            word => (0 to word.groupCount).foreach(index => out += Val.Str(pos, word.group(index)))
//          )
//          new Val.Arr(pos, out.toArray)
//      },
//
//      builtin("matches", "string", "regex") {
//        (pos, ev, string: String, regex: String) =>
//          regex.r.matches(string);
//      }
    ),

    "base64" -> moduleFrom(
      builtin("decode", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num => new String(Base64.getDecoder.decode(x.value.toString))
            case x: Val.Str => new String(Base64.getDecoder.decode(x.value))
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("encode", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value % 1 == 0) new String(Base64.getEncoder.encode(x.value.toInt.toString.getBytes()))
              else new String(Base64.getEncoder.encode(x.value.toString.getBytes()))
            case x: Val.Str => new String(Base64.getEncoder.encode(x.value.getBytes()))
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      }
    )
  ).asJava

  private def keyFrom(value: Val): String = {
    value match {
      case x: Val.Num =>
        val tmp = x.value
        if (tmp.ceil == tmp.floor) tmp.longValue.toString
        else tmp.toString
      case x: Val.Str => x.value
      case Val.Null(_) => "null"
      case _: Val.True => "true"
      case _: Val.False => "false"
    }
  }

  def builtin0[R: ReadWriter](name: String)
                             (eval: (Position, EvalScope) => R): (String, Val.Func) = {
    (name, new Builtin0() {
      def evalRhs(ev: EvalScope, outerPos: Position): Val = {
        //println("--- calling builtin: "+name)
        implicitly[ReadWriter[R]].write(outerPos, eval(outerPos, ev))
      }
    })
  }

  def builtin4[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String)
                                                                                             (eval: (Position, EvalScope, T1, T2, T3, T4) => R): (String, Val.Func) = {
    (name, new Builtin4(p1, p2, p3, p4) {
      def evalRhs(arg1: Val, arg2: Val, arg3: Val, arg4: Val, ev: EvalScope, outerPos: Position): Val = {
        //println("--- calling builtin: "+name)
        val v1: T1 = implicitly[ReadWriter[T1]].apply(arg1)
        val v2: T2 = implicitly[ReadWriter[T2]].apply(arg2)
        val v3: T3 = implicitly[ReadWriter[T3]].apply(arg3)
        val v4: T4 = implicitly[ReadWriter[T4]].apply(arg4)
        implicitly[ReadWriter[R]].write(outerPos, eval(outerPos, ev, v1, v2, v3, v4))
      }
    })
  }

  def read(dataFormats: DataFormatService, data: String, mimeType: String, params: Val.Obj, ev: EvalScope): Val = {
    val paramsAsJava = ujson.read(Materializer.apply(params)(ev)).obj.map(keyVal => {
      (keyVal._1, keyVal._2 match {
        case Str(value) => value
        case Num(value) => String.valueOf(value)
        case bool: Bool => String.valueOf(bool)
        case Null => "null"
      })
    }).asJava
    val doc = new BasicDocument(data, MediaType.parseMediaType(mimeType).withParameters(paramsAsJava))

    val plugin = dataFormats.thatCanRead(doc)
      .orElseThrow(() => Error.fail("No suitable plugin found for mime type: " + mimeType))

    Materializer.reverse(dummyPosition, plugin.read(doc))
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
  private def orderBy(array: Array[Lazy], func: Val.Func, ev: EvalScope): Val = {
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
  private def orderBy(obj: Val.Obj, func: Val.Func, ev: EvalScope): Val = {
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

  private def deepFlatten(array: Array[Lazy]): Array[Lazy] = {
    array.foldLeft(new ArrayBuffer[Lazy])((agg, curr) => {
      curr.force match {
        case inner: Val.Arr => agg.appendAll(deepFlatten(inner.asLazyArray))
        case _ => agg.append(curr)
      }
    }).toArray
  }
}

// this assumes that we're comparing Vals of the same type
object ValOrdering extends Ordering[Val] {
  def compare(x: Val, y: Val): Int =
    x match {
      case value: Val.Num => Ordering.Double.TotalOrdering.compare(value.value, y.asDouble)
      case value: Val.Str => Ordering.String.compare(value.value, y.asString)
      case bool: Val.Bool => Ordering.Boolean.compare(bool.asBoolean, y.asBoolean)
      case unsupported: Val => Error.fail("Expected embedded function to return a String, Number, or Boolean, received: " + unsupported.prettyName)
    }
}

abstract class Builtin4(pn1: String, pn2: String, pn3: String, pn4: String, defs: Array[Expr] = null) extends Builtin(Array(pn1, pn2, pn3, pn4), defs) {
  final def evalRhs(args: Array[Val], ev: EvalScope, pos: Position): Val =
    evalRhs(args(0), args(1), args(2), args(3), ev, pos)

  def evalRhs(arg1: Val, arg2: Val, arg3: Val, arg4: Val, ev: EvalScope, pos: Position): Val

  override def apply(argVals: Array[_ <: Lazy], namedNames: Array[String], outerPos: Position)(implicit ev: EvalScope): Val =
    if (namedNames == null && argVals.length == 4)
      evalRhs(argVals(0).force, argVals(1).force, argVals(2).force, argVals(3).force, ev, outerPos)
    else super.apply(argVals, namedNames, outerPos)
}

abstract class Builtin0() extends Builtin(Array.empty, null) {
  final def evalRhs(args: Array[Val], ev: EvalScope, pos: Position): Val =
    evalRhs(ev, pos)

  def evalRhs(ev: EvalScope, pos: Position): Val

  override def apply(argVals: Array[_ <: Lazy], namedNames: Array[String], outerPos: Position)(implicit ev: EvalScope): Val =
    if (namedNames == null && argVals.length == 1) evalRhs(ev, outerPos)
    else super.apply(argVals, namedNames, outerPos)

  override def apply1(argVal: Lazy, outerPos: Position)(implicit ev: EvalScope): Val =
    if (params.names.length == 1) evalRhs(ev, outerPos)
    else super.apply(Array(argVal), null, outerPos)
}
