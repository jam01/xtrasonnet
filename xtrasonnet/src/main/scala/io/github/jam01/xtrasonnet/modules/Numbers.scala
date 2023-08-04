package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2023 Jose Montoya.
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
 * - 5666b472de694383231b043c8c7861833831db96: Fixed numbers module to allow long values
 *    Functions: of/toBinary, of/toHex, of/toRadix
 * Changes made:
 * - 2662d96cdfbd613d766830420a0b2a6920d07b52: change ds funtions and behaviors
 */

import io.github.jam01.xtrasonnet.spi.Library.Std.{builtin, builtinWithDefaults}
import sjsonnet.{Val, Error}

object Numbers {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("ofBinary", "value") {
      (_, _, value: Val) =>
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

    builtin("ofHex", "value") {
      (_, _, value: Val) =>
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

    builtin("ofRadix", "value", "num") {
      (_, _, value: Val, num: Int) =>
        value match {
          case x: Val.Num => BigInt.apply(x.value.toLong.toString, num).bigInteger.doubleValue
          case x: Val.Str => BigInt.apply(x.value, num).bigInteger.doubleValue
          case x => Error.fail("Expected Base(num), got: " + x.prettyName)
        }
    },

    builtin("toBinary", "value") {
      (_, _, value: Val) =>
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
      (_, _, value: Val) =>
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
      (_, _, value: Val, num: Int) =>
        value match {
          case x: Val.Num =>
            if (x.value < 0) "-" + BigInt.apply(x.value.toLong).toString(num)
            else BigInt.apply(x.value.toLong).toString(num)
          // Val.Str(Integer.toString(x.toInt, num))
          case x: Val.Str =>
            if (x.value.startsWith("-")) "-" + BigInt.apply(x.value.toLong).toString(num)
            else BigInt.apply(x.value.toLong).toString(num)
          case x => Error.fail("Expected Binary, got: " + x.prettyName)
        }
    },

    builtin("ofOctal", "str") { (_, _, num: Val) =>
      num match {
        case str: Val.Str => Integer.parseInt(str.asString, 8)
        case n: Val.Num => Integer.parseInt(n.asInt.toString, 8)
        case x => Error.fail("Expected Number or String, got: " + x.prettyName)
      }
    }
  )
}
