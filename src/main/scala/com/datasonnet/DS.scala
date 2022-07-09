package com.datasonnet

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright retention, per Apache 2.0-4.c */
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


import com.datasonnet.document.{DefaultDocument, MediaType}
import com.datasonnet.header.Header
import com.datasonnet.modules.{Crypto, JsonPath, Regex}
import com.datasonnet.spi.Library.{emptyObj, memberOf}
import com.datasonnet.spi.{DataFormatService, Library, ujsonUtils}
import sjsonnet.ReadWriter.ArrRead
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val.Builtin
import sjsonnet.{Error, EvalScope, Expr, Importer, Lazy, Materializer, Position, ReadWriter, Val}
import ujson.Value

import java.math.{BigDecimal, RoundingMode}
import java.net.URL
import java.security.SecureRandom
import java.text.DecimalFormat
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util
import java.util.{Base64, Scanner, UUID}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Random, Using}

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
object DS extends Library {

  override def namespace() = "ds"

  override def libsonnets(): java.util.Set[String] = Set("util").asJava

  private val dummyPosition = new Position(null, 0)

  override def functions(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Func] = Map(
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
          case key =>
            Val.Obj.mk(pos,
              ("key", memberOf(Val.Str(pos, key))),
              ("value", memberOf(obj.value(key, pos)(ev)))
            )
        }))
    },

    builtin("filter", "array", "func") {
      (pos, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => filter(array.asLazyArray, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Array, got: " + x.prettyName)
        }
    },

    builtin("filterObject", "obj", "func") {
      (_, ev, value: Val, func: Val.Func) =>
        value match {
          case obj: Val.Obj => filterObject(obj, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Object, got: " + x.prettyName)
        }
    },

    builtin("find", "container", "value") {
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
      (pos, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => flatMap(array.asLazyArray, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Array, got: " + x.prettyName)
        }
    },

    builtin("flatten", "array") {
      (pos, _, value: Val) =>
        value match {
          case array: Val.Arr =>
            val out = new ArrayBuffer[Lazy]

            var i = 0
            while (i < array.length) {
              array.asLazyArray(i).force match {
                case n: Val.Null => out.append(n)
                case v: Val.Arr => out.appendAll(v.asLazyArray)
                case x => Error.fail("Expected Array, got: " + x.prettyName)
              }
              i = i + 1
            }
            new Val.Arr(pos, out.toArray)
          case Val.Null(_) => value
          case x => Error.fail("Expected Array, got: " + x.prettyName)
        }
    },

    builtin("distinctBy", "container", "func") {
      (_, ev, container: Val, func: Val.Func) =>
        container match {
          case arr: Val.Arr => distinctBy(arr.asLazyArray, func, ev)
          case obj: Val.Obj => distinctBy(obj, func, ev)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
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
          case Val.Null(_) => container
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("isBlank", "value") {
      (pos, ev, value: Val) =>
        value match {
          case s: Val.Str => s.value.trim().isEmpty
          case Val.Null(_) => true
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isDecimal", "value") {
      (pos, ev, value: Double) =>
        (Math.ceil(value) != Math.floor(value)).booleanValue()
    },

    builtin("isEmpty", "container") {
      (pos, ev, container: Val) =>
        container match {
          case Val.Null(_) => true
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

    builtin("joinBy", "array", "sep") {
      (pos, ev, array: Val.Arr, sep: String) =>
        array.asLazyArray.map({
          _.force match {
            case str: Val.Str => str.value
            case _: Val.True => "true"
            case _: Val.False => "false"
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

    builtin("map", "array", "func") {
      (pos, ev, array: Val, func: Val.Func) =>
        array match {
          case seq: Val.Arr => map(seq.asLazyArray, func, ev)
          case Val.Null(_) => array
          case x => Error.fail("Expected Array, got: " + x.prettyName)
        }
    },

    builtin("mapEntries", "value", "func") {
      (_, ev, value: Val, func: Val.Func) =>
        value match {
          case obj: Val.Obj => mapEntries(obj, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Object, got: " + x.prettyName)
        }
    },

    builtin("mapObject", "value", "func") {
      (_, ev, value: Val, func: Val.Func) =>
        value match {
          case obj: Val.Obj => mapObject(obj, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Object, got: " + x.prettyName)
        }
    },

    builtin("match", "string", "regex") {
      (pos, _, string: String, regex: String) =>
        val out = new ArrayBuffer[Lazy]
        regex.r.findAllMatchIn(string).foreach(
          word => (0 to word.groupCount).foreach(index => out += Val.Str(pos, word.group(index)))
        )
        new Val.Arr(pos, out.toArray)
    },

    builtin("matches", "string", "regex") {
      (pos, ev, string: String, regex: String) =>
        regex.r.matches(string);
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
          case _: Val.Bool =>
            if (lazyArr.exists(it => it.force.asBoolean)) Val.True(pos) else Val.False(pos)
          case _: Val.Num => lazyArr.maxBy(item => func.apply1(item, pos.noOffset)(ev).asDouble).force
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
          case _: Val.Bool =>
            if (lazyArr.exists(it => it.force.asBoolean)) Val.False(pos) else Val.True(pos)
          case _: Val.Num => lazyArr.minBy(item => func.apply1(item, pos.noOffset)(ev).asDouble).force
          case x => Error.fail("Expected Array of type String, Boolean, or Number, got: Array of type " + x)
        }
    },

    builtin("orderBy", "value", "func") {
      (pos, ev, value: Val, func: Val.Func) =>
        value match {
          case array: Val.Arr => orderBy(array.asLazyArray, func, ev)
          case obj: Val.Obj => orderBy(obj, func, ev)
          case Val.Null(_) => value
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    // TODO: add step param
    builtin("range", "begin", "end") {
      (pos, _, begin: Int, end: Int) =>
        new Val.Arr(pos, (begin to end).map(i => Val.Num(pos, i)).toArray)
    },

    builtin("replace", "string", "regex", "replacement") {
      (pos, ev, str: String, reg: String, replacement: String) =>
        reg.r.replaceAllIn(str, replacement)
    },

    builtinWithDefaults("read",
      "data" -> Val.Null(dummyPosition),
      "mimeType" -> Val.Null(dummyPosition),
      "params" -> null) { (args, pos, ev) =>
      val data = args(0).cast[Val.Str].value
      val mimeType = args(1).cast[Val.Str].value
      val params = if (args(2).force.isInstanceOf[Val.Null]) {
        emptyObj
      } else {
        args(2).cast[Val.Obj]
      }
      read(dataFormats, data, mimeType, params, ev)
    },

    //    //TODO add read mediatype
    builtin("readUrl", "url") {
      (pos, ev, url: String) =>
        url match {
          case str if str.startsWith("classpath://") => importer.read(DataSonnetPath(str.substring(12))) match {
            case Some(value) => Materializer.reverse(pos, ujsonUtils.parse(value))
            case None => Val.Null(pos)
          }
          case _ =>
            val out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
            Materializer.reverse(pos, ujsonUtils.parse(out));
        }
    },

    builtin("scan", "str", "regex") {
      (pos, ev, str: String, regex: String) =>
        new Val.Arr(pos, regex.r.findAllMatchIn(str).map(item => {
          new Val.Arr(pos, (0 to item.groupCount).map(i => Val.Str(pos, item.group(i))).toArray)
        }).toArray
        )
    },

    builtin("sizeOf", "value") {
      (pos, ev, value: Val) =>
        value match {
          case s: Val.Str => s.value.length()
          case s: Val.Obj => s.visibleKeyNames.length
          case array: Val.Arr => array.asLazyArray.length
          case s: Val.Func => s.params.names.length
          case Val.Null(_) => 0
          case x => Error.fail("Expected Array, String, or Object, got: " + x.prettyName)
        }
    },

    builtin("splitBy", "str", "regex") {
      (pos, _, str: String, regex: String) =>
        new Val.Arr(pos, regex.r.split(str).toIndexedSeq.map(item => Val.Str(pos, item)).toArray)
    },

    builtin("startsWith", "str1", "str2") {
      (pos, ev, str1: String, str2: String) =>
        str1.toUpperCase().startsWith(str2.toUpperCase());
    },

    builtin("toString", "value") {
      (pos, ev, value: Val) =>
        convertToString(value)
    },

    builtin("trim", "str") {
      (pos, ev, str: String) =>
        str.trim()
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
        Val.Str(pos, UUID.randomUUID().toString).asInstanceOf[Val]
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

    builtin("isfunction", "v") { (pos, ev, v: Val) =>
      v.isInstanceOf[Val.Func]
    },

    // moved array to first position
    builtin("foldLeft", "arr", "init", "func") { (pos, ev, arr: Val.Arr, init: Val, func: Val.Func) =>
      var current = init
      for (item <- arr.asLazyArray) {
        val c = current
        current = func.apply2(c, item, pos.noOffset)(ev)
      }
      current
    },

    // TODO: add test
    // TODO: can we do this without reverse? has to traverse the collection twice
    builtin("foldRight", "arr", "init", "func") { (pos, ev, arr: Val.Arr, init: Val, func: Val.Func) =>
      var current = init
      for (item <- arr.asLazyArray.reverse) {
        val c = current
        current = func.apply2(item, c, pos.noOffset)(ev)
      }
      current
    },

    builtin("parseInt", "str") { (pos, ev, str: String) =>
      str.toInt
    },

    builtin("parseOctal", "str") { (pos, ev, str: String) =>
      Integer.parseInt(str, 8)
    },

    builtin("parseHex", "str") { (pos, ev, str: String) =>
      Integer.parseInt(str, 16)
    },

    builtin("parseDouble", "str") { (pos, ev, str: String) =>
      str.toDouble
    },

    builtin("combine", "first", "second") {
      (pos, ev, first: Val, second: Val) =>
        first match {
          case str: Val.Str =>
            second match {
              case Val.Str(_, str2) => Val.Str(pos, str.value.concat(str2)).asInstanceOf[Val]
              case Val.Num(_, num) =>
                Val.Str(pos, str.value.concat(
                  if (Math.ceil(num) == Math.floor(num)) {
                    num.toInt.toString
                  } else {
                    num.toString
                  }
                ))
              case x => Error.fail("Expected String or Number, got: " + x.prettyName)
            }
          case Val.Num(_, num) =>
            val stringNum = if (Math.ceil(num) == Math.floor(num)) {
              num.toInt.toString
            } else {
              num.toString
            }
            second match {
              case str: Val.Str => Val.Str(pos, stringNum.concat(str.value))
              case Val.Num(_, num2) =>
                Val.Str(pos, stringNum.concat(
                  if (Math.ceil(num2) == Math.floor(num2)) {
                    num2.toInt.toString
                  } else {
                    num2.toString
                  }
                ))
              case x => Error.fail("Expected String or Number, got: " + x.prettyName)
            }
          case arr: Val.Arr =>
            second match {
              case arr2: Val.Arr => new Val.Arr(pos, arr.asLazyArray.concat(arr2.asLazyArray))
              case x => Error.fail("Expected Array, got: " + x.prettyName)
            }
          case obj: Val.Obj =>
            val out = new util.LinkedHashMap[String, Val.Obj.Member]()
            second match {
              case obj2: Val.Obj =>
                obj.visibleKeyNames.map(sKey => out.put(sKey, memberOf(obj.value(sKey, pos)(ev))))
                obj2.visibleKeyNames.map(sKey => out.put(sKey, memberOf(obj2.value(sKey, pos)(ev))))
                new Val.Obj(pos, out, false, null, null)
              case x => Error.fail("Expected Object, got: " + x.prettyName)
            }
          case x => Error.fail("Expected Array, Object, Number, or String, got: " + x.prettyName)
        }
    },

    builtin("remove", "collection", "value") {
      (pos, ev, collection: Val, value: Val) =>
        collection match {
          case arr: Val.Arr =>
            new Val.Arr(pos, arr.asLazyArray.collect({
              case x if !ev.equal(x.force, value) => x
            }))
          case obj: Val.Obj =>
            Val.Obj.mk(pos,
              (value match {
                case str: Val.Str =>
                  obj.visibleKeyNames.toSeq.collect({
                    case key if !key.equals(str.value) =>
                      key -> memberOf(obj.value(key, pos)(ev))
                  })
                case arr: Val.Arr =>
                  obj.visibleKeyNames.toSeq.collect({
                    case key if !arr.asLazyArray.exists(item => item.force.asString.equals(key)) =>
                      key -> memberOf(obj.value(key, pos)(ev))
                  })
                case x => Error.fail("Expected String or Array, got: " + x.prettyName)
              }): _*).asInstanceOf[Val]
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("removeMatch", "first", "second") {
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
            second match {
              case obj2: Val.Obj =>
                Val.Obj.mk(pos, obj.visibleKeyNames.toSeq.collect({
                  case key if !(obj2.containsKey(key) && ev.equal(obj.value(key, pos)(ev), obj2.value(key, pos)(ev))) =>
                    key -> memberOf(obj.value(key, pos)(ev))
                }): _*)
              case x => Error.fail("Expected Object, got: " + x.prettyName)
            }
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    },

    builtin("append", "first", "second") {
      (pos, ev, arr: Val.Arr, second: Val) =>
        val out = new ArrayBuffer[Lazy]
        new Val.Arr(pos, out.appendAll(arr.asLazyArray).append(second).toArray)
    },

    builtin("prepend", "first", "second") {
      (pos, ev, arr: Val.Arr, second: Val) =>
        val out = new ArrayBuffer[Lazy]
        new Val.Arr(pos, out.append(second).appendAll(arr.asLazyArray).toArray)
    },

    builtin("reverse", "collection") {
      (pos, ev, collection: Val) =>
        collection match {
          case str: Val.Str => Val.Str(pos, str.value.reverse)
          case arr: Val.Arr => new Val.Arr(pos, arr.asLazyArray.reverse).asInstanceOf[Val]
          case obj: Val.Obj =>
            var result: Seq[(String, Val.Obj.Member)] = Seq()
            obj.visibleKeyNames.foreach(key => result = result.prepended(
              key -> memberOf(obj.value(key, pos)(ev))
            ))
            Val.Obj.mk(pos, result: _*)
          case x => Error.fail("Expected Array or Object, got: " + x.prettyName)
        }
    }
  ).asJava

  override def modules(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Obj] = Map(
    "xml" -> moduleFrom(
      builtinWithDefaults("flattenContents",
        "element" -> Val.Null(dummyPosition),
        "namespaces" -> emptyObj,
        "params" -> emptyObj) {
        (args, pos, ev) =>
          val element = args(0).asObj
          val namespaces = args(1).asObj
          val params = args(2).asObj

          val wrapperName = "a"
          val wrapperStop = s"</$wrapperName>"

          val wrapperProperties = new util.LinkedHashMap[String, Val.Obj.Member]()
          wrapperProperties.put("a", memberOf(element))
          val wrapped = new Val.Obj(pos, wrapperProperties, false, null, null)

          val xmlProperties = new util.LinkedHashMap[String, Val.Obj.Member]()
          xmlProperties.put("OmitXmlDeclaration", memberOf(Val.Str(pos, "true")))
          namespaces.visibleKeyNames.foreach(key => {
            xmlProperties.put("NamespaceDeclarations." + key, memberOf(namespaces.value(key, pos)(ev)))
          })

          val properties = Val.Obj.mk(pos, params.visibleKeyNames.map(k => (k, memberOf(params.value(k, pos)(ev)))) ++ xmlProperties.asScala.toArray: _*)
          val written = write(dataFormats, wrapped, "application/xml", properties, ev)

          written.substring(written.indexOf(">") + 1, written.length - wrapperStop.length)
      },
    ),

    "datetime" -> moduleFrom(
      builtin0("now") { (pos, ev) => ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },

      builtin("parse", "datetime", "inputFormat") { (pos, ev, datetime: Val, inputFormat: String) =>
        var datetimeObj: ZonedDateTime = null
        inputFormat.toLowerCase match {
          case "timestamp" | "epoch" =>
            var inst: Instant = null
            datetime match {
              case str: Val.Str => inst = Instant.ofEpochSecond(str.value.toInt.toLong)
              case num: Val.Num => inst = Instant.ofEpochSecond(num.value.toLong)
              case _ => Error.fail("Expected datetime to be a string or number, got: " + datetime.prettyName)
            }
            datetimeObj = java.time.ZonedDateTime.ofInstant(inst, ZoneOffset.UTC)
          case _ =>
            datetimeObj = try { //will catch any errors if zone data is missing and default to Z
              java.time.ZonedDateTime.parse(datetime.cast[Val.Str].value, DateTimeFormatter.ofPattern(inputFormat))
            } catch {
              case e: DateTimeException =>
                LocalDateTime.parse(datetime.cast[Val.Str].value, DateTimeFormatter.ofPattern(inputFormat)).atZone(ZoneId.of("Z"))
            }
        }
        datetimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("format", "datetime", "outputFormat") { (pos, ev, datetime: String, outputFormat: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
      },

      builtin("compare", "datetime", "datetwo") { (pos, ev, datetimeone: String, datetimetwo: String) =>
        val datetimeObj1 = java.time.ZonedDateTime
          .parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val datetimeObj2 = java.time.ZonedDateTime
          .parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        datetimeObj1.compareTo(datetimeObj2)
      },

      builtin("plus", "datetime", "period") { (pos, ev, date: String, period: String) =>
        val datetime = java.time.ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        if (period.contains("T")) {
          datetime.plus(Duration.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
          datetime.plus(Period.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
      },

      builtin("minus", "datetime", "period") { (pos, ev, date: String, period: String) =>
        val datetime = java.time.ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        if (period.contains("T")) {
          datetime.minus(Duration.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
          datetime.minus(Period.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
      },

      builtin("changeTimeZone", "datetime", "timezone") {
        (pos, ev, datetime: String, timezone: String) =>
          val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val zoneId = ZoneId.of(timezone)
          val newDateTimeObj = datetimeObj.withZoneSameInstant(zoneId)
          newDateTimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("toLocalDate", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },

      builtin("toLocalTime", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
      },

      builtin("toLocalDateTime", "datetime") { (pos, ev, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },

      builtin("daysBetween", "datetime", "datetwo") {
        (pos, ev, datetimeone: String, datetimetwo: String) =>
          val dateone = java.time.ZonedDateTime
            .parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val datetwo = java.time.ZonedDateTime
            .parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          ChronoUnit.DAYS.between(dateone, datetwo).abs.toDouble;
      },

      builtin("isLeapYear", "datetime") {
        (pos, ev, datetime: String) =>
          java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toLocalDate.isLeapYear;
      },

      builtin("atBeginningOfDay", "datetime") {
        (_, _, datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfHour", "datetime") {
        (_, _, datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfMonth", "datetime") {
        (_, _, datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusDays(date.getDayOfMonth - 1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfWeek", "datetime") {
        (_, _, datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

          date.minusDays(if (date.getDayOfWeek.getValue == 7) 0 else date.getDayOfWeek.getValue)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfYear", "datetime") {
        (_, _, datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusMonths(date.getMonthValue - 1)
            .minusDays(date.getDayOfMonth - 1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("date", "obj") {
        (pos, ev, obj: Val.Obj) =>
          //year, month, dayOfMonth, hour, minute, second, nanoSecond, zoneId
          val out = mutable.Map[String, Val]()
          obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
          java.time.ZonedDateTime.of(
            out.getOrElse("year", Val.Num(pos, 0)).cast[Val.Num].value.toInt,
            out.getOrElse("month", Val.Num(pos, 1)).cast[Val.Num].value.toInt,
            out.getOrElse("day", Val.Num(pos, 1)).cast[Val.Num].value.toInt,
            out.getOrElse("hour", Val.Num(pos, 0)).cast[Val.Num].value.toInt,
            out.getOrElse("minute", Val.Num(pos, 0)).cast[Val.Num].value.toInt,
            out.getOrElse("second", Val.Num(pos, 0)).cast[Val.Num].value.toInt,
            0, //out.getOrElse("nanosecond", Val.Num(0)).cast[Val.Num].value.toInt TODO?
            ZoneId.of(out.getOrElse("timezone", Val.Str(pos, "Z")).cast[Val.Str].value)
          ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("today") {
        (_, _) =>
          val date = java.time.ZonedDateTime.now()
          date.minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("tomorrow") {
        (_, _) =>
          val date = java.time.ZonedDateTime.now()
          date.plusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("yesterday") {
        (_, _) =>
          val date = java.time.ZonedDateTime.now()
          date.minusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },
    ),

    "period" -> moduleFrom(
      builtin("between", "datetimeone", "datetimetwo") {
        (_, _, datetimeone: String, datetimetwo: String) =>
          Period.between(
            java.time.ZonedDateTime.parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate,
            java.time.ZonedDateTime.parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate
          ).toString
      },

      builtin("days", "num") {
        (_, _, num: Int) =>
          Period.ofDays(num).toString
      },

      builtin("duration", "obj") {
        (pos, ev, obj: Val.Obj) =>
          val out = mutable.Map[String, Val]()
          obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
          Duration.ZERO
            .plusDays(out.getOrElse("days", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusHours(out.getOrElse("hours", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusMinutes(out.getOrElse("minutes", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusSeconds(out.getOrElse("seconds", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .toString
      },

      builtin("hours", "num") {
        (_, _, num: Int) =>
          Duration.ofHours(num).toString
      },

      builtin("minutes", "num") {
        (_, _, num: Int) =>
          Duration.ofMinutes(num).toString
      },

      builtin("months", "num") {
        (_, _, num: Int) =>
          Period.ofMonths(num).toString
      },

      builtin("period", "obj") {
        (pos, ev, obj: Val.Obj) =>
          val out = mutable.Map[String, Val]()
          obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
          Period.ZERO
            .plusYears(out.getOrElse("years", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusMonths(out.getOrElse("months", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .plusDays(out.getOrElse("days", Val.Num(pos, 0)).cast[Val.Num].value.toLong)
            .toString
      },

      builtin("seconds", "num") {
        (_, _, num: Int) =>
          Duration.ofSeconds(num).toString
      },

      builtin("years", "num") {
        (_, _, num: Int) =>
          Period.ofYears(num).toString
      },
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

      /**
       * Encrypts the value with specified JDK Cipher Transformation and the provided secret. Converts the encryption
       * to a readable format with Base64
       *
       * @builtinParam value The message to be encrypted.
       * @types [String]
       * @builtinParam secret The secret used to encrypt the original messsage.
       * @types [String]
       * @builtinParam transformation The string that describes the operation (or set of operations) to be performed on
       *               the given input, to produce some output. A transformation always includes the name of a cryptographic algorithm
       *               (e.g., AES), and may be followed by a feedback mode and padding scheme. A transformation is of the form:
       *               "algorithm/mode/padding" or "algorithm"
       * @types [String]
       * @builtinReturn Base64 String value of the encrypted message
       * @types [String]
       * @changed 2.0.3
       */
      builtin("encrypt", "value", "secret", "algorithm") {
        (pos, ev, value: String, secret: String, transformation: String) =>
          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            Val.Str(pos, Base64.getEncoder.encodeToString(cipher.doFinal(value.getBytes)))
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

            Val.Str(pos, Base64.getEncoder.encodeToString(combinedPayload)).asInstanceOf[Val]
          }
      },

      /**
       * Decrypts the Base64 value with specified JDK Cipher Transformation and the provided secret.
       *
       * @builtinParam value The encrypted message to be decrypted.
       * @types [String]
       * @builtinParam secret The secret used to encrypt the original messsage.
       * @types [String]
       * @builtinParam algorithm The algorithm used for the encryption.
       * @types [String]
       * @builtinParam mode The encryption mode to be used.
       * @types [String]
       * @builtinParam padding The encryption secret padding to be used
       * @types [String]
       * @builtinReturn Base64 String value of the encrypted message
       * @types [String]
       * @changed 2.0.3
       */
      builtin("decrypt", "value", "secret", "algorithm") {
        (pos, ev, value: String, secret: String, transformation: String) =>
          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            Val.Str(pos, new String(cipher.doFinal(Base64.getDecoder.decode(value))))
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

            Val.Str(pos, new String(cipher.doFinal(encryptedBytes))).asInstanceOf[Val]
          }
      }
    ),

    "jsonpath" -> moduleFrom(
      builtin("select", "json", "path") {
        (pos, ev, json: Val, path: String) =>
          Materializer.reverse(pos, ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      }
    ),

    "regex" -> moduleFrom(
      builtin("regexFullMatch", "expr", "str") {
        (pos, ev, expr: String, str: String) =>
          Materializer.reverse(pos, Regex.regexFullMatch(expr, str))
      },

      builtin("regexPartialMatch", "expr", "str") {
        (pos, ev, expr: String, str: String) =>
          Materializer.reverse(pos, Regex.regexPartialMatch(expr, str))
      },

      builtin("regexScan", "expr", "str") {
        (pos, ev, expr: String, str: String) =>
          Materializer.reverse(pos, Regex.regexScan(expr, str))
      },

      builtin("regexQuoteMeta", "str") {
        (pos, ev, str: String) =>
          Regex.regexQuoteMeta(str)
      },

      builtin("regexReplace", "str", "pattern", "replace") {
        (pos, ev, str: String, pattern: String, replace: String) =>
          Regex.regexReplace(str, pattern, replace)
      },

      builtinWithDefaults("regexGlobalReplace", "str" -> Val.Null(dummyPosition),
        "pattern" -> Val.Null(dummyPosition),
        "replace" -> Val.Null(dummyPosition)) { (args, pos, ev) =>
        val str = args(0).asInstanceOf[Val.Str].value
        val pattern = args(1).asInstanceOf[Val.Str].value
        val replace = args(2)

        replace match {
          case replaceStr: Val.Str => Regex.regexGlobalReplace(str, pattern, replaceStr.value)
          case replaceF: Val.Func =>
            val func = new util.function.Function[Value, String] {
              override def apply(t: Value): String = {
                val v = Materializer.reverse(pos, t)
                replaceF.apply1(v, pos.noOffset)(ev) match {
                  case resultStr: Val.Str => resultStr.value
                  case _ => Error.fail("The result of the replacement function must be a String")
                }
              }
            }
            Regex.regexGlobalReplace(str, pattern, func)
          case _ => Error.fail("'replace' parameter must be either String or function")
        }
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

      builtin("mod", "num1", "num2") {
        (pos, ev, num1: Double, num2: Double) =>
          num1 % num2;
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
          (Random.nextInt((num - 0) + 1) + 0).intValue()
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

      builtin("divideBy", "array", "size") {
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
          val out = mutable.Buffer.empty[Lazy]
          array.asLazyArray.collect({
            case item if array.asLazyArray.count(lzy => ev.equal(lzy.force, item.force)) >= 2 &&
              !out.exists(lzy => ev.equal(lzy.force, item.force)) => out.append(item)
          })
          new Val.Arr(pos, out.toArray)
      },

      builtin("every", "value", "func") {
        (pos, ev, value: Val, func: Val.Func) =>
          value match {
            case arr: Val.Arr => Val.bool(pos, arr.forall(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True]))
            case Val.Null(_) => Val.True(pos).asInstanceOf[Val]
            case x => Error.fail("Expected Array, got: " + x.prettyName)
          }
      },

      builtin("firstWith", "arr", "func") {
        (pos, ev, arr: Val.Arr, func: Val.Func) =>
          val pos = func.pos
          val args = func.params.names.length

          if (args == 2)
            arr.asLazyArray.zipWithIndex
              .find(item => func.apply2(item._1, Val.Num(pos, item._2), pos.noOffset)(ev).isInstanceOf[Val.True])
              .map(_._1)
              .getOrElse(Val.Null)
              .asInstanceOf[Val]
          else if (args == 1)
            arr.asLazyArray.find(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
              .getOrElse(Val.Null(pos))
              .asInstanceOf[Val]
          else {
            Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
          }
      },

      builtin("deepFlatten", "arr") {
        (pos, ev, arr: Val.Arr) =>
          new Val.Arr(pos, deepFlatten(arr.asLazyArray))
      },

      builtin("indexOf", "container", "value") {
        (pos, ev, container: Val, value: Val) =>
          container match {
            case str: Val.Str => Val.Num(pos, str.value.indexOf(value.cast[Val.Str].value)).asInstanceOf[Val]
            case array: Val.Arr => Val.Num(pos, array.asLazyArray.indexWhere(lzy => ev.equal(lzy.force, value)))
            case Val.Null(_) => Val.Num(pos, -1)
            case x => Error.fail("Expected String or Array, got: " + x.prettyName)
          }
      },

      builtin("indexWhere", "arr", "func") {
        (pos, ev, array: Val.Arr, func: Val.Func) =>
          array.asLazyArray.indexWhere(func.apply1(_, pos.noOffset)(ev).isInstanceOf[Val.True])
      },

      builtin4("join", "arrL", "arryR", "funcL", "funcR") {
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

      builtin("lastIndexOf", "container", "value") {
        (pos, ev, container: Val, value: Val) =>
          container match {
            case str: Val.Str => Val.Num(pos, str.value.lastIndexOf(value.cast[Val.Str].value)).asInstanceOf[Val]
            case array: Val.Arr => Val.Num(pos, array.asLazyArray.lastIndexWhere(lzy => ev.equal(lzy.force, value)))
            case Val.Null(_) => Val.Num(pos, -1)
            case x => Error.fail("Expected String or Array, got: " + x.prettyName)
          }
      },

      builtin4("leftJoin", "arrL", "arryR", "funcL", "funcR") {
        (pos, ev, arrL: Val.Arr, arrR: Val.Arr, funcL: Val.Func, funcR: Val.Func) =>
          //make backup array for leftovers
          var leftoversL = arrL.asLazyArray
          val out = new ArrayBuffer[Lazy]

          arrL.asLazyArray.foreach({
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

      builtin("occurrences", "arr", "func") {
        (pos, ev, array: Val.Arr, func: Val.Func) =>
          // no idea why, but this sorts the result in the correct order
          val ordered = mutable.Map.from(
            array.asLazyArray
              .groupBy(item => convertToString(func.apply1(item, pos.noOffset)(ev)))
              .map(item => item._1 -> memberOf(Val.Num(pos, item._2.length)))
          )

          Val.Obj.mk(pos, ordered.toSeq: _*)
      },

      builtin4("outerJoin", "arrL", "arrR", "funcL", "funcR") {
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

      builtin("slice", "arr", "start", "end") {
        (pos, ev, array: Val.Arr, start: Int, end: Int) =>
          //version commented below is slightly slower
          //new Val.Arr(pos, array.asLazyArray.splitAt(start)._2.splitAt(end-1)._1)
          new Val.Arr(pos, array.asLazyArray.zipWithIndex.filter({
            case (_, index) => (index >= start) && (index < end)
          }).map(_._1)
          )
      },

      builtin("some", "value", "func") {
        (pos, ev, value: Val, func: Val.Func) =>
          value match {
            case array: Val.Arr =>
              Val.bool(pos, array.asLazyArray.exists(item => func.apply1(item, pos.noOffset)(ev).isInstanceOf[Val.True]))
            case Val.Null(_) => value
            case x => Error.fail("Expected Array, got: " + x.prettyName)
          }
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
      }
    ),

    "binaries" -> moduleFrom(
      builtin("fromBase64", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num => Val.Str(pos, new String(Base64.getDecoder.decode(x.value.toString)))
            case x: Val.Str => Val.Str(pos, new String(Base64.getDecoder.decode(x.value))).asInstanceOf[Val]
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("fromHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Str => Val.Str(pos, x.value.toSeq.sliding(2, 2).map(byte => Integer.parseInt(byte.unwrap, 16).toChar).mkString).asInstanceOf[Val]
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("readLinesWith", "value", "encoding") {
        (pos, ev, value: String, enc: String) =>
          new Val.Arr(pos, new String(value.getBytes(), enc).split('\n').toIndexedSeq.collect({
            case str => Val.Str(pos, str)
          }).toArray
          )
      },

      builtin("toBase64", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value % 1 == 0) Val.Str(pos, new String(Base64.getEncoder.encode(x.value.toInt.toString.getBytes())))
              else Val.Str(pos, new String(Base64.getEncoder.encode(x.value.toString.getBytes())))
            case x: Val.Str => Val.Str(pos, new String(Base64.getEncoder.encode(x.value.getBytes()))).asInstanceOf[Val]
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("toHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num => Val.Str(pos, Integer.toString(x.value.toInt, 16).toUpperCase())
            case x: Val.Str => Val.Str(pos, x.value.getBytes().map(_.toHexString).mkString.toUpperCase()).asInstanceOf[Val]
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("writeLinesWith", "value", "encoding") {
        (pos, ev, value: Val.Arr, enc: String) =>
          val str = value.asLazyArray.map(item => item.force.asInstanceOf[Val.Str].value).mkString("\n") + "\n"
          Val.Str(pos, new String(str.getBytes, enc)).asInstanceOf[Val]
      }
    ),

    "objects" -> moduleFrom(
      builtin("divideBy", "obj", "num") {
        (pos, ev, obj: Val.Obj, num: Int) =>
          val out = new ArrayBuffer[Lazy]

          obj.visibleKeyNames.sliding(num, num).foreach({
            map =>
              val currentObject = new util.LinkedHashMap[String, Val.Obj.Member]()
              map.foreach(key => currentObject.put(key, memberOf(obj.value(key, pos)(ev))))
              out.append(new Val.Obj(pos, currentObject, false, null, null))
          })
          new Val.Arr(pos, out.toArray)
      },

      builtin("everyEntry", "value", "func") {
        (pos, ev, value: Val, func: Val.Func) =>
          val pos = func.pos
          val args = func.params.names.length

          value match {
            case obj: Val.Obj =>
              if (args == 2)
                Val.bool(pos, obj.visibleKeyNames.toSeq.forall(key => func.apply2(obj.value(key, pos)(ev), Val.Str(pos, key), pos.noOffset)(ev).isInstanceOf[Val.True]))
              else if (args == 1)
                Val.bool(pos, obj.visibleKeyNames.toSeq.forall(key => func.apply1(obj.value(key, pos)(ev), pos.noOffset)(ev).isInstanceOf[Val.True]))
              else {
                Error.fail("Expected embedded function to have 1 or 2 parameters, received: " + args)
              }
            case Val.Null(_) => Val.True(pos).asInstanceOf[Val]
            case x => Error.fail("Expected Array, got: " + x.prettyName)
          }
      },

      builtin("mergeWith", "valueOne", "valueTwo") {
        (pos, ev, valueOne: Val, valueTwo: Val) =>
          val out = new util.LinkedHashMap[String, Val.Obj.Member]()
          valueOne match {
            case obj: Val.Obj =>
              valueTwo match {
                case obj2: Val.Obj =>
                  obj.visibleKeyNames.foreach(
                    key => out.put(key, memberOf(obj.value(key, pos)(ev)))
                  )
                  obj2.visibleKeyNames.foreach(
                    key => out.put(key, memberOf(obj2.value(key, pos)(ev)))
                  )
                  new Val.Obj(pos, out, false, null, null)
                case Val.Null(_) => valueOne
                case x => Error.fail("Expected Object, got: " + x.prettyName)
              }
            case Val.Null(_) =>
              valueTwo match {
                case _: Val.Obj => valueTwo
                case x => Error.fail("Expected Object, got: " + x.prettyName)
              }
            case x => Error.fail("Expected Object, got: " + x.prettyName)
          }
      },

      builtin("someEntry", "value", "func") {
        (pos, ev, value: Val, func: Val.Func) =>
          value match {
            case obj: Val.Obj =>
              Val.bool(pos, obj.visibleKeyNames.exists(
                item => func.apply2(obj.value(item, pos)(ev), Val.Str(pos, item), pos.noOffset)(ev).isInstanceOf[Val.True]
              ))
            case Val.Null(_) => Val.False(pos).asInstanceOf[Val]
            case x => Error.fail("Expected Object, got: " + x.prettyName)
          }
      },

      builtin("takeWhile", "obj", "func") {
        (pos, ev, obj: Val.Obj, func: Val.Func) =>
          val out = new util.LinkedHashMap[String, Val.Obj.Member]()
          obj.visibleKeyNames.takeWhile(
            item => func.apply2(obj.value(item, pos)(ev), Val.Str(pos, item), pos.noOffset)(ev).isInstanceOf[Val.True]
          ).foreach(key => out.put(key, memberOf(obj.value(key, pos)(ev))))

          new Val.Obj(pos, out, false, null, null)
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
              else Val.Num(pos, BigInt.apply(x.value.toLong.toString, 2).bigInteger.longValue())
            case x: Val.Str => Val.Num(pos, BigInt.apply(x.value, 2).bigInteger.longValue())
            case Val.Null(_) => value
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("fromHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if ("[^0-9a-f]".r.matches(x.value.toString.toLowerCase())) {
                Error.fail("Expected Binary, got: Number")
              }
              else Val.Num(pos, BigInt.apply(x.value.toLong.toString, 16).bigInteger.longValue());
            case x: Val.Str => Val.Num(pos, BigInt.apply(x.asString, 16).bigInteger.longValue());
            case Val.Null(_) => value
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("fromRadixNumber", "value", "num") {
        (pos, ev, value: Val, num: Int) =>
          value match {
            case x: Val.Num => Val.Num(pos, BigInt.apply(x.value.toLong.toString, num).bigInteger.longValue())
            case x: Val.Str => Val.Num(pos, BigInt.apply(x.value, num).bigInteger.longValue()).asInstanceOf[Val]
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
            //null not supported in DW function
          }
      },

      builtin("toBinary", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) Val.Str(pos, "-" + x.value.toLong.abs.toBinaryString)
              else Val.Str(pos, x.value.toLong.toBinaryString)
            case x: Val.Str =>
              if (x.value.startsWith("-")) Val.Str(pos, x.value.toLong.abs.toBinaryString)
              else Val.Str(pos, x.value.toLong.toBinaryString)
            case Val.Null(_) => value
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("toHex", "value") {
        (pos, ev, value: Val) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) Val.Str(pos, "-" + x.value.toLong.abs.toHexString)
              else Val.Str(pos, x.value.toLong.toHexString)
            case x: Val.Str =>
              if (x.value.startsWith("-")) Val.Str(pos, x.value.toLong.abs.toHexString)
              else Val.Str(pos, x.value.toLong.toHexString)
            case Val.Null(_) => value
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
          }
      },

      builtin("toRadixNumber", "value", "num") {
        (pos, ev, value: Val, num: Int) =>
          value match {
            case x: Val.Num =>
              if (x.value < 0) Val.Str(pos, "-" + BigInt.apply(x.value.toLong).toString(num))
              else Val.Str(pos, BigInt.apply(x.value.toLong).toString(num)).asInstanceOf[Val]
            // Val.Str(Integer.toString(x.toInt, num))
            case x: Val.Str =>
              if (x.value.startsWith("-")) Val.Str(pos, "-" + BigInt.apply(x.value.toLong).toString(num))
              else Val.Str(pos, BigInt.apply(x.value.toLong).toString(num)).asInstanceOf[Val]
            case x => Error.fail("Expected Binary, got: " + x.prettyName)
            //DW functions does not support null
          }
      }
    ),

    "strings" -> moduleFrom(
      builtin("appendIfMissing", "str1", "str2") {
        (pos, ev, value: Val, append: String) =>
          value match {
            case str: Val.Str =>
              var ret = str.value
              if (!ret.endsWith(append)) {
                ret = ret + append
              }
              Val.Str(pos, ret)
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("camelize", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              //regex fo _CHAR
              val regex = "(_+)([0-9A-Za-z])".r("underscore", "letter")

              //Start string at first non underscore, lower case xt
              var temp = value.value.substring("[^_]".r.findFirstMatchIn(value.value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"${(m group "letter").toUpperCase()}")
              Val.Str(pos, temp).asInstanceOf[Val]
            case n: Val.Null => n
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("capitalize", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case xt
              var temp = value.value.substring("[0-9A-Za-z]".r.findFirstMatchIn(value.value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toUpper.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s" ${(m group "two").toUpperCase() + (m group "three").toLowerCase()}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"} ${(m group "start").toUpperCase()}")

              Val.Str(pos, temp).asInstanceOf[Val]
            case n: Val.Null => n
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
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
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case xt
              var temp = value.value

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"-${(m group "two") + (m group "three").toLowerCase()}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}-${m group "start"}")

              Val.Str(pos, temp.toLowerCase());
            case Val.Null(_) => str
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("fromCharCode", "num") {
        (pos, ev, num: Int) =>
          String.valueOf(num.asInstanceOf[Char])
      },

      builtin("isAlpha", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              if ("^[A-Za-z]+$".r.matches(value.value)) {
                true
              }
              else {
                false
              }
            case Val.Null(_) => false
            case _: Val.Num => false
            case _: Val.Bool => true
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isAlphanumeric", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              if ("^[A-Za-z0-9]+$".r.matches(value.value)) {
                true
              }
              else {
                false
              }
            case Val.Null(_) => false
            case _: Val.Num => true
            case _: Val.Bool => true
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isLowerCase", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              if ("^[a-z]+$".r.matches(value.value)) {
                true
              }
              else {
                false
              }
            case Val.Null(_) => false
            case _: Val.Num => false
            case _: Val.Bool => true
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isNumeric", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              if ("^[0-9]+$".r.matches(value.value)) {
                true
              }
              else {
                false
              }
            case _: Val.Num => true
            case _: Val.Bool | Val.Null(_) => false
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isUpperCase", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              if ("^[A-Z]+$".r.matches(value.value)) {
                true
              }
              else {
                false
              }
            case _: Val.Num => false
            case _: Val.Bool | Val.Null(_) => false
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("isWhitespace", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str => value.value.trim().isEmpty
            case _: Val.Num => false
            case _: Val.Bool | Val.Null(_) => false
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("leftPad", "str", "offset") {
        (pos, ev, str: Val, offset: Int) =>
          str match {
            case str: Val.Str =>
              Val.Str(pos, ("%" + offset + "s").format(str.value))
            case _: Val.True =>
              Val.Str(pos, ("%" + offset + "s").format("true"))
            case _: Val.False =>
              Val.Str(pos, ("%" + offset + "s").format("false"))
            case x: Val.Num =>
              //TODO change to use sjsonnet's Format and DecimalFormat
              Val.Str(pos, ("%" + offset + "s").format(new DecimalFormat("0.#").format(x.value))).asInstanceOf[Val]
            case Val.Null(_) => str
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("ordinalize", "num") {
        (pos, ev, num: Val) =>
          (num match { //convert number value to string
            case Val.Null(_) => "null"
            case value: Val.Str =>
              if ("^[0-9]+$".r.matches(value.value)) {
                value.value
              }
              else {
                "X"
              }
            case value: Val.Num => value.value.toInt.toString
            case _ => Error.fail("Expected Number, got: " + num.prettyName)
          }) match { //convert string number to ordinalized string number
            case "null" => Val.Null(pos)
            case "X" => Error.fail("Expected Number, got: " + num.prettyName)
            case str =>
              if (str.endsWith("11") || str.endsWith("12") || str.endsWith("13")) {
                Val.Str(pos, str + "th")
              }
              else {
                if (str.endsWith("1")) {
                  Val.Str(pos, str + "st")
                }
                else if (str.endsWith("2")) {
                  Val.Str(pos, str + "nd")
                }
                else if (str.endsWith("3")) {
                  Val.Str(pos, str + "rd")
                }
                else {
                  Val.Str(pos, str + "th").asInstanceOf[Val]
                }
              }
          }
      },

      builtin("pluralize", "value") {
        (pos, ev, value: Val) =>
          value match {
            case str: Val.Str =>
              val comparator = str.value.toLowerCase()
              val specialSList = List("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
              if (specialSList.contains(comparator)) {
                Val.Str(pos, str.value + "s")
              }
              else if (comparator.isEmpty) Val.Str(pos, "")
              else {
                if (comparator.endsWith("y")) {
                  Val.Str(pos, str.value.substring(0, str.value.length - 1) + "ies")
                }
                else if (comparator.endsWith("x")) {
                  Val.Str(pos, str.value + "es")
                }
                else {
                  Val.Str(pos, str.value + "s")
                }
              }
            case Val.Null(_) => value
            case x => Error.fail("Expected Number, got: " + x.prettyName)
          }
      },

      builtin("prependIfMissing", "str1", "str2") {
        (pos, ev, value: Val, append: String) =>
          value match {
            case str: Val.Str =>
              var ret = str.value
              if (!ret.startsWith(append)) {
                ret = append + ret
              }
              Val.Str(pos, ret)
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("repeat", "str", "num") {
        (pos, ev, str: String, num: Int) =>
          var ret = ""
          for (_ <- 0 until num) {
            ret += str
          }
          Val.Str(pos, ret).asInstanceOf[Val]
      },

      builtin("rightPad", "str", "offset") {
        (pos, ev, value: Val, offset: Int) =>
          value match {
            case str: Val.Str =>
              Val.Str(pos, str.value.padTo(offset, ' '))
            case x: Val.Num =>
              //TODO change to use sjsonnet's Format and DecimalFormat
              Val.Str(pos, new DecimalFormat("0.#").format(x.value).padTo(offset, ' '))
            case _: Val.True =>
              Val.Str(pos, "true".padTo(offset, ' '))
            case _: Val.False =>
              Val.Str(pos, "false".padTo(offset, ' '))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("singularize", "value") {
        (pos, ev, value: Val) =>
          value match {
            case s: Val.Str =>
              if (s.value.endsWith("ies"))
                Val.Str(pos, s.value.substring(0, s.value.length - 3) + "y")
              else if (s.value.endsWith("es"))
                Val.Str(pos, s.value.substring(0, s.value.length - 2))
              else
                Val.Str(pos, s.value.substring(0, s.value.length - 1))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("substringAfter", "value", "sep") {
        (pos, ev, value: Val, sep: String) =>
          value match {
            case s: Val.Str =>
              Val.Str(pos, s.value.substring(
                s.value.indexOf(sep) match {
                  case -1 => s.value.length
                  case i => if (sep.equals("")) i else i + 1
                }
              ))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("substringAfterLast", "value", "sep") {
        (pos, ev, value: Val, sep: String) =>
          value match {
            case s: Val.Str =>
              val split = s.value.split(sep)
              if (sep.equals("")) Val.Str(pos, "")
              else if (split.length == 1) Val.Str(pos, "")
              else Val.Str(pos, split(split.length - 1))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("substringBefore", "value", "sep") {
        (pos, ev, value: Val, sep: String) =>
          value match {
            case s: Val.Str =>
              Val.Str(pos, s.value.substring(0,
                s.value.indexOf(sep) match {
                  case -1 => 0
                  case x => x
                }
              ))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("substringBeforeLast", "value", "sep") {
        (pos, ev, value: Val, sep: String) =>
          value match {
            case s: Val.Str =>
              Val.Str(pos, s.value.substring(0,
                s.value.lastIndexOf(sep) match {
                  case -1 => 0
                  case x => x
                }
              ))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("underscore", "str") {
        (pos, ev, str: Val) =>
          str match {
            case value: Val.Str =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case xt
              var temp = value.value.substring("[0-9A-Za-z]".r.findFirstMatchIn(value.value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"_${(m group "two") + (m group "three")}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}_${m group "start"}")

              Val.Str(pos, temp.toLowerCase);

            case Val.Null(_) => str
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("unwrap", "value", "wrapper") {
        (pos, ev, value: Val, wrapper: String) =>
          value match {
            case str: Val.Str =>
              val starts = str.value.startsWith(wrapper)
              val ends = str.value.endsWith(wrapper)
              if (starts && ends) Val.Str(pos, str.value.substring(0 + wrapper.length, str.value.length - wrapper.length))
              else if (starts) Val.Str(pos, str.value.substring(0 + wrapper.length, str.value.length) + wrapper)
              else if (ends) Val.Str(pos, wrapper + str.value.substring(0, str.value.length - wrapper.length))
              else str
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("withMaxSize", "value", "num") {
        (pos, ev, value: Val, num: Int) =>
          value match {
            case str: Val.Str =>
              if (str.value.length <= num) str
              else Val.Str(pos, str.value.substring(0, num))
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("wrapIfMissing", "value", "wrapper") {
        (pos, ev, value: Val, wrapper: String) =>
          value match {
            case str: Val.Str =>
              val ret = new StringBuilder(str.value)
              if (!str.value.startsWith(wrapper)) ret.insert(0, wrapper)
              if (!str.value.endsWith(wrapper)) ret.append(wrapper)
              Val.Str(pos, ret.toString())
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      },

      builtin("wrapWith", "value", "wrapper") {
        (pos, ev, value: Val, wrapper: String) =>
          value match {
            case str: Val.Str => Val.Str(pos, wrapper + str.value + wrapper)
            case Val.Null(_) => value
            case x => Error.fail("Expected String, got: " + x.prettyName)
          }
      }
    )
  ).asJava

  private def convertToString(value: Val): String = {
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
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val doc = new DefaultDocument(data, new MediaType(supert, subt, paramsAsJava))

    val plugin = dataFormats.thatCanRead(doc)
      .orElseThrow(() => Error.fail("No suitable plugin found for mime type: " + mimeType))

    Materializer.reverse(dummyPosition, plugin.read(doc))
  }

  def write(dataFormats: DataFormatService, json: Val, mimeType: String, params: Val.Obj, ev: EvalScope): String = {
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val mediaType = new MediaType(supert, subt, paramsAsJava)

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
        array(i).force match {
          case inner: Val.Arr =>
            var j = 0
            while (j < inner.length) {
              out.append(func.apply2(inner.asLazyArray(j).force, Val.Num(pos, j), pos.noOffset)(ev))
              j = j + 1
            }
          case x => Error.fail("Expected Array of Arrays, got: Array of " + x.prettyName)
        }
        i = i + 1
      }
    } else if (args == 1) { //  1 arg
      while (i < array.length) {
        array(i).force match {
          case inner: Val.Arr =>
            var j = 0
            while (j < inner.length) {
              out.append(func.apply1(inner.asLazyArray(j).force, pos.noOffset)(ev))
              j = j + 1
            }
          case x => Error.fail("Expected Array of Arrays, got: Array of " + x.prettyName)
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
        val k = convertToString(func.apply2(v, Val.Num(pos, i), pos.noOffset)(ev))
        mScala.getOrElseUpdate(k, mutable.ArrayBuffer[Lazy]()).addOne(v)
        i = i + 1
      }
    } else if (args == 1) {
      while (i < array.length) {
        val v = array(i)
        val k = convertToString(func.apply1(v, pos.noOffset)(ev))
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
        val funcKey = convertToString(func.apply2(v, Val.Str(pos, k), pos.noOffset)(ev))
        mScala.getOrElseUpdate(funcKey, new util.LinkedHashMap[String, Val.Obj.Member]()).put(k, memberOf(v))
        i = i + 1
      }
    } else if (args == 1) {
      while (i < obj.visibleKeyNames.length) {
        val k = obj.visibleKeyNames(i)
        val v = obj.value(k, pos)(ev)
        val funcKey = convertToString(func.apply1(v, pos.noOffset)(ev))
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
      case value: Val.Num => Ordering.Double.TotalOrdering.compare(value.value, y.asInstanceOf[Val.Num].value)
      case value: Val.Str => Ordering.String.compare(value.value, y.asInstanceOf[Val.Str].value)
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
