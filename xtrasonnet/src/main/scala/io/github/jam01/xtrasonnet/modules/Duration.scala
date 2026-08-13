package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.{Error, Val}
import sjsonnet.functions.AbstractFunctionModule

import java.math.MathContext
import java.time.*
import java.time.format.DateTimeParseException
import scala.collection.mutable

object Duration extends AbstractFunctionModule {
  override def name: String = "duration"

  private val partNames = Array("years", "months", "days", "hours", "minutes", "seconds")
  private val NanosPerSecond = BigDecimal(1000000000)

  private def bigDecOf(num: Val.Num): BigDecimal = num match {
    case i: Val.Int64 => BigDecimal(i.value)
    case f: Val.Float64 => BigDecimal(f.value)
    case d: Val.Dec128 => d.value
  }

  /** The given part as a whole number; every part but seconds rejects fractions rather than truncate. */
  private def wholePart(parts: mutable.Map[String, Val], name: String): Long = parts.get(name) match {
    case None => 0L
    case Some(n: Val.Num) =>
      val bd = bigDecOf(n)
      if (!bd.isWhole) Error.fail("Expected a whole number for duration part " + name + ", got: " + n)
      if (!bd.isValidLong) Error.fail("Duration part out of range: " + name)
      bd.toLong
    case Some(x) => Error.fail("Expected number for duration part " + name + ", got: " + x.prettyName)
  }

  /** The seconds part in nanoseconds, honoring fractional values. */
  private def secondsNanos(parts: mutable.Map[String, Val]): Long = parts.get("seconds") match {
    case None => 0L
    case Some(n: Val.Num) =>
      val nanos = (bigDecOf(n) * NanosPerSecond).setScale(0, BigDecimal.RoundingMode.HALF_UP)
      if (!nanos.isValidLong) Error.fail("Duration part out of range: seconds")
      nanos.toLong
    case Some(x) => Error.fail("Expected number for duration part seconds, got: " + x.prettyName)
  }

  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("of", "obj") {
      (pos, ev, obj: Val.Obj) =>
        val out = mutable.Map[String, Val]()
        obj.visibleKeyNames.foreach { key =>
          if (!partNames.contains(key)) Error.fail("Unexpected duration part: " + key)
          out.addOne(key, obj.value(key, pos)(ev))
        }

        try {
          val period = Period.ZERO
            .plusYears(wholePart(out, "years"))
            .plusMonths(wholePart(out, "months"))
            .plusDays(wholePart(out, "days"))
          val dduration = java.time.Duration.ZERO
            .plusHours(wholePart(out, "hours"))
            .plusMinutes(wholePart(out, "minutes"))
            .plusNanos(secondsNanos(out))

          if (period.isZero) dduration.toString // covers the all-zero case: "PT0S"
          else if (dduration.isZero) period.toString
          else period.toString + dduration.toString.substring(1)
        } catch {
          case _: ArithmeticException | _: DateTimeException => Error.fail("Duration parts out of range")
        }
    },

    builtin("toParts", "str") {
      (pos, _, duration: String) =>
        val (period, dduration) = parseParts(duration)

        val out = new java.util.LinkedHashMap[String, Val.Obj.Member]
        val hours = dduration.toHours
        val minutes = dduration.toMinutesPart
        // exact seconds, so fractions like PT1.5S survive; getNano is a non-negative adjustment on getSeconds
        val seconds = BigDecimal(dduration.getSeconds - hours * 3600 - minutes * 60) +
          BigDecimal(dduration.getNano) / NanosPerSecond

        out.put("years", memberOf(Val.Num(pos, period.getYears)))
        out.put("months", memberOf(Val.Num(pos, period.getMonths)))
        out.put("days", memberOf(Val.Num(pos, period.getDays)))
        out.put("hours", memberOf(Val.Num(pos, hours)))
        out.put("minutes", memberOf(Val.Num(pos, minutes)))
        out.put("seconds", memberOf(
          if (seconds.isWhole) Val.Num(pos, seconds.toLong)
          else Val.Num(pos, new BigDecimal(seconds.bigDecimal, MathContext.DECIMAL128))))

        new Val.Obj(pos, out, false, null, null)
    }
  )

  /**
   * Parses an ISO-8601 duration into its date and time components; a leading sign applies to both.
   *
   * Handles the shapes java.time cannot parse in one call: combined date-and-time strings, time-only
   * strings ("PT4H"), and a leading sign, failing with an evaluation error for anything malformed.
   */
  def parseParts(duration: String): (Period, java.time.Duration) = {
    val negative = duration.startsWith("-")
    val abs = if (negative || duration.startsWith("+")) duration.substring(1) else duration
    val timeIdx = abs.indexOf('T')
    var period = Period.ZERO
    var dduration = java.time.Duration.ZERO

    try {
      if (timeIdx != -1) {
        dduration = java.time.Duration.parse("P" + abs.substring(timeIdx))
        val datePart = abs.substring(0, timeIdx)
        if (datePart != "P") period = Period.parse(datePart) // "P" alone means no date components
      } else {
        period = Period.parse(abs)
      }

      if (negative) {
        period = period.negated()
        dduration = dduration.negated()
      }
    } catch {
      // ArithmeticException covers negation overflow of a component at the numeric range's edge
      case _: DateTimeParseException | _: ArithmeticException =>
        Error.fail("Invalid ISO-8601 duration: " + duration)
    }

    (period, dduration)
  }
}
