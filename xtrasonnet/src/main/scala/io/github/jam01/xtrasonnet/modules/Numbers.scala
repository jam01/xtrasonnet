package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, RenderUtils, Val}

object Numbers extends AbstractFunctionModule {
  override def name: String = "numbers"

  /**
   * Parses digits in the given radix.
   *
   * BigInt.apply throws a bare NumberFormatException for both a bad radix and bad digits, without
   * saying which argument was at fault.
   */
  private def ofRadix(digits: String, radix: Int): BigDecimal = {
    checkRadix(radix)
    try BigDecimal(BigInt.apply(digits, radix))
    catch {
      case _: NumberFormatException =>
        Error.fail("Expected a String of radix " + radix + " digits, got: " + digits)
    }
  }

  /**
   * java.math.BigInteger.toString(radix) silently falls back to radix 10 for a radix outside
   * [2, 36], so an unchecked radix would answer in the wrong base rather than fail.
   */
  private def checkRadix(radix: Int): Unit = {
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      Error.fail("Expected a radix within [" + Character.MIN_RADIX + ", " + Character.MAX_RADIX + "], got: " + radix)
    }
  }

  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("ofBinary", "value") {
      (pos, _, value: Val) =>
        value match {
          case x: Val.Num => Val.Num(pos, ofRadix(RenderUtils.renderNum(x), 2)).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, ofRadix(x.str, 2)).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofHex", "value") {
      (pos, _, value: Val) =>
        value match {
          case x: Val.Num => Val.Num(pos, ofRadix(RenderUtils.renderNum(x), 16)).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, ofRadix(x.str, 16)).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofRadix", "value", "num") {
      (pos, _, value: Val, num: Int) =>
        value match {
          case x: Val.Num => Val.Num(pos, ofRadix(RenderUtils.renderNum(x), num)).asInstanceOf[Val]
          case x: Val.Str => Val.Num(pos, ofRadix(x.str, num)).asInstanceOf[Val]
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toBinary", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(RenderUtils.renderNum(x)).toBigInt.toString(2)
          case x: Val.Str => BigDecimal(x.str).toBigInt.toString(2)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toHex", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(RenderUtils.renderNum(x)).toBigInt.toString(16)
          case x: Val.Str => BigDecimal(x.str).toBigInt.toString(16)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("toRadix", "value", "num") {
      (_, _, value: Val, num: Int) =>
        checkRadix(num)
        value match {
          case x: Val.Num => BigDecimal(RenderUtils.renderNum(x)).toBigInt.toString(num)
          case x: Val.Str => BigDecimal(x.str).toBigInt.toString(num)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    },

    builtin("ofOctal", "str") { (pos, _, num: Val) =>
      num match {
        case x: Val.Num => Val.Num(pos, ofRadix(RenderUtils.renderNum(x), 8)).asInstanceOf[Val]
        case x: Val.Str => Val.Num(pos, ofRadix(x.str, 8)).asInstanceOf[Val]
        case x => Error.fail("Expected Number or String, got: " + x.prettyName)
      }
    },

    builtin("toOctal", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => BigDecimal(RenderUtils.renderNum(x)).toBigInt.toString(8)
          case x: Val.Str => BigDecimal(x.str).toBigInt.toString(8)
          case x => Error.fail("Expected Number or String, got: " + x.prettyName)
        }
    }
  )
}
