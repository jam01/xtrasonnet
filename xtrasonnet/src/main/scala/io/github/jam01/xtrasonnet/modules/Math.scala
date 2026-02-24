package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, NumberMath, Val}

import java.math.{MathContext, RoundingMode}
import scala.util.Random

object Math extends AbstractFunctionModule {
  override def name: String = "math"
  
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("abs", "num") {
      (pos, _, num: Val.Num) =>
        num match {
          case Val.Int64(pos, value) => Val.Num(pos, value.abs)
          case Val.Float64(pos, value) => Val.Num(pos, value.abs)
          case Val.Dec128(pos, value) => Val.Num(pos, value.abs)
        }
    },

    // See: https://damieng.com/blog/2014/12/11/sequence-averages-in-scala
    // See: https://gist.github.com/gclaramunt/5710280
    builtin("avg", "array") {
      (pos, ev, array: Val.Arr) =>
        val (sum, length) = array.asLazyArray.foldLeft((Val.Num(position, 0), 0))({
          case ((sum, length), num) =>
            (num.force match {
              case num: Val.Num => NumberMath.add(position, sum, num)(ev)
              case x => Error.fail("Expected Array pf Numbers, got: Array of " + x.prettyName)
            }, 1 + length)
        })
        NumberMath.divide(pos, sum, Val.Num(position, length))(ev).asInstanceOf[Val]
    },

    builtin("ceil", "num") {
      (_, _, num: Val.Num) =>
        num match {
          case Val.Int64(pos, value) => num
          case Val.Float64(pos, value) => Val.Num(pos, value.ceil)
          case Val.Dec128(pos, value) => Val.Num(pos, value.setScale(0, BigDecimal.RoundingMode.CEILING))
        }
    },

    builtin("floor", "num") {
      (_, _, num: Val.Num) =>
        num match {
          case Val.Int64(pos, value) => num
          case Val.Float64(pos, value) => Val.Num(pos, value.floor)
          case Val.Dec128(pos, value) => Val.Num(pos, value.setScale(0, BigDecimal.RoundingMode.FLOOR))
        }
    },

    builtin("pow", "num1", "num2") {
      (pos, _, num1: Val.Num, num2: Int) =>
        if (num2 <= 0 || num2 >= Int.MaxValue) {
          throw new IllegalArgumentException("Exponent must be valid Int32 integer")
        }

        num1 match {
          case Val.Int64(_, v1) => Val.Num(pos, BigDecimal.decimal(v1).pow(num2))
          case Val.Float64(_, v1) => Val.Num(pos, BigDecimal.decimal(v1).pow(num2))
          case Val.Dec128(_, v1) => Val.Num(pos, v1.pow(num2))
        }
    },

    builtin("random") {
      (_, _) =>
        (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
    },

    builtin("randomInt", "num") {
      (_, _, num: Int) =>
        (Random.nextInt(num - 0) + 0).intValue()
    },

    builtinWithDefaults("round",
      "num" -> Val.Null(position),
      "mode" -> Val.Str(position, "half-up"),
      "precision" -> Val.Num(position, 0)) { (args, pos, ev) =>
      val num = args(0).cast[Val.Num]
      val mode = args(1).asString
      val prec = args(2).asInt

      val res = (num match {
          case Val.Int64(pos, value) => java.math.BigDecimal(value, MathContext.DECIMAL128)
          case Val.Float64(pos, value) => java.math.BigDecimal(value, MathContext.DECIMAL128)
          case Val.Dec128(pos, value) => value.underlying()
        }).setScale(prec, RoundingMode.valueOf(mode.toUpperCase().replace('-', '_')))
      Val.Num(pos, res).asInstanceOf[Val]
    },

    builtin("sqrt", "num") {
      (pos, _, num: Val.Num) =>
        num match {
          case Val.Int64(_, value) => Val.Num(pos, new java.math.BigDecimal(value, MathContext.DECIMAL128).sqrt(MathContext.DECIMAL128))
          case Val.Float64(_, value) => Val.Num(pos, scala.math.sqrt(value))
          case Val.Dec128(_, value) => Val.Num(pos, value.underlying().sqrt(MathContext.DECIMAL128))
        }
    },

    builtin("sum", "array") {
      (pos, ev, array: Val.Arr) =>
        array.asLazyArray.foldLeft(Val.Num(position, 0))((sum, value) =>
          value.force match {
            case num: Val.Num => NumberMath.add(pos, sum, num)(ev)
            case x => Error.fail("Expected Array of Numbers, got: Array of " + x.prettyName)
          }
        ).asInstanceOf[Val]
    },

    // funcs below taken from Std but using Java's Math
    builtin("clamp", "x", "minVal", "maxVal") { (pos, ev, x: Val.Num, minVal: Val.Num, maxVal: Val.Num) =>
      val minClamped = if (NumberMath.compareTo(x, minVal) < 0) minVal else x
      if (NumberMath.compareTo(minClamped, maxVal) > 0) maxVal else minClamped
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
