package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.spi.Library.{builtinx, dummyPosition}
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val
import sjsonnet.Error

import java.math.{BigDecimal, RoundingMode}
import scala.util.Random

object Math {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("abs", "num") {
      (_, _, num: Double) =>
        java.lang.Math.abs(num);
    },

    // See: https://damieng.com/blog/2014/12/11/sequence-averages-in-scala
    // See: https://gist.github.com/gclaramunt/5710280
    builtin("avg", "array") {
      (_, _, array: Val.Arr) =>
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
      (_, _, num: Double) =>
        java.lang.Math.ceil(num);
    },

    builtin("floor", "num") {
      (_, _, num: Double) =>
        java.lang.Math.floor(num);
    },

    builtin("pow", "num1", "num2") {
      (_, _, num1: Double, num2: Double) =>
        java.lang.Math.pow(num1, num2)
    },

    builtinx("random") {
      (_, _) =>
        (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
    },

    builtin("randomInt", "num") {
      (_, _, num: Int) =>
        (Random.nextInt(num - 0) + 0).intValue()
    },

    builtinWithDefaults("round",
      "num" -> Val.Null(dummyPosition),
      "mode" -> Val.Str(dummyPosition, "half-up"),
      "precision" -> Val.Num(dummyPosition, 0)) { (args, _, _) =>
      val num = args(0).cast[Val.Num].value
      val mode = args(1).asString
      val prec = args(2).asInt

      val res = BigDecimal.valueOf(num).setScale(prec, RoundingMode.valueOf(mode.toUpperCase().replace('-', '_'))).doubleValue()
      if (prec == 0) res.intValue() else res
    },

    builtin("sqrt", "num") {
      (_, _, num: Double) =>
        java.lang.Math.sqrt(num)
    },

    builtin("sum", "array") {
      (_, _, array: Val.Arr) =>
        array.asLazyArray.foldLeft(0.0)((sum, value) =>
          value.force match {
            case num: Val.Num => sum + num.value
            case x => Error.fail("Expected Array of Numbers, got: Array of " + x.prettyName)
          }
        )
    },

    // funcs below taken from Std but using Java's Math
    builtin("clamp", "x", "minVal", "maxVal") { (_, _, x: Double, minVal: Double, maxVal: Double) =>
      java.lang.Math.max(minVal, java.lang.Math.min(x, maxVal))
    },

    builtin("pow", "x", "n") { (_, _, x: Double, n: Double) =>
      java.lang.Math.pow(x, n)
    },

    builtin("sin", "x") { (_, _, x: Double) =>
      java.lang.Math.sin(x)
    },

    builtin("cos", "x") { (_, _, x: Double) =>
      java.lang.Math.cos(x)
    },

    builtin("tan", "x") { (_, _, x: Double) =>
      java.lang.Math.tan(x)
    },

    builtin("asin", "x") { (_, _, x: Double) =>
      java.lang.Math.asin(x)
    },

    builtin("acos", "x") { (_, _, x: Double) =>
      java.lang.Math.acos(x)
    },

    builtin("atan", "x") { (_, _, x: Double) =>
      java.lang.Math.atan(x)
    },

    builtin("log", "x") { (_, _, x: Double) =>
      java.lang.Math.log(x)
    },

    builtin("exp", "x") { (_, _, x: Double) =>
      java.lang.Math.exp(x)
    },

    builtin("mantissa", "x") { (_, _, x: Double) =>
      x * java.lang.Math.pow(2.0, -((java.lang.Math.log(x) / java.lang.Math.log(2)).toInt + 1))
    },

    builtin("exponent", "x") { (_, _, x: Double) =>
      (java.lang.Math.log(x) / java.lang.Math.log(2)).toInt + 1
    }

  )
}
