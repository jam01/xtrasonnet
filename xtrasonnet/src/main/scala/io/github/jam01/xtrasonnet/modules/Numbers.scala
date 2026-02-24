package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, Val}

object Numbers extends AbstractFunctionModule {
  override def name: String = "numbers"

  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("ofBinary", "value") {
      (pos, _, value: Val) =>
        value match {
          case x: Val.Num => Val.Num(pos, BigDecimal(BigInt.apply(x.toString, 2))).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, BigDecimal(BigInt.apply(x.value, 2))).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofHex", "value") {
      (pos, _, value: Val) =>
        value match {
          case x: Val.Num => Val.Num(pos, BigDecimal(BigInt.apply(x.toString, 16))).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, BigDecimal(BigInt.apply(x.value, 16))).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofRadix", "value", "num") {
      (pos, _, value: Val, num: Int) =>
        value match {
          case x: Val.Num => Val.Num(pos, BigDecimal(BigInt.apply(x.toString, num))).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, BigDecimal(BigInt.apply(x.value, num))).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toBinary", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(x.toString).toBigInt.toString(2)
          case x: Val.Str => BigDecimal(x.value).toBigInt.toString(2)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toHex", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(x.toString).toBigInt.toString(16)
          case x: Val.Str => BigDecimal(x.value).toBigInt.toString(16)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toRadix", "value", "num") {
      (_, _, value: Val, num: Int) =>
        value match {
          case x: Val.Num => BigDecimal(x.toString).toBigInt.toString(num)
          case x: Val.Str => BigDecimal(x.value).toBigInt.toString(num)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofOctal", "str") { (pos, _, num: Val) =>
      num match {
        case x: Val.Num => Val.Num(pos, BigDecimal(BigInt.apply(x.toString, 8))).asInstanceOf[Val]
        case x: Val.Str => Val.Num(pos, BigDecimal(BigInt.apply(x.value, 8))).asInstanceOf[Val]
        case x => Error.fail("Expected Number or String, got: " + x.prettyName)
      }
    },

    builtin("toOctal", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(x.toString).toBigInt.toString(8)
          case x: Val.Str => BigDecimal(x.value).toBigInt.toString(8)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    }
  )
}
