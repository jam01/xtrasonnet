package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.{Error, Val}
import sjsonnet.functions.AbstractFunctionModule

import java.time.*
import java.time.format.DateTimeParseException
import scala.collection.mutable

object Duration extends AbstractFunctionModule {
  override def name: String = "duration"

  private val partNames = Array("years", "months", "days", "hours", "minutes", "seconds")

  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("of", "obj") {
      (pos, ev, obj: Val.Obj) =>
        val out = mutable.Map[String, Val]()
        obj.visibleKeyNames.foreach { key =>
          if (!partNames.contains(key)) Error.fail("Unexpected duration part: " + key)
          out.addOne(key, obj.value(key, pos)(ev))
        }
        val period = Period.ZERO
          .plusYears(out.getOrElse("years", Val.Num(pos, 0)).asInt)
          .plusMonths(out.getOrElse("months", Val.Num(pos, 0)).asInt)
          .plusDays(out.getOrElse("days", Val.Num(pos, 0)).asInt)
        val dduration = java.time.Duration.ZERO
          .plusHours(out.getOrElse("hours", Val.Num(pos, 0)).asLong)
          .plusMinutes(out.getOrElse("minutes", Val.Num(pos, 0)).asLong)
          .plusSeconds(out.getOrElse("seconds", Val.Num(pos, 0)).asLong)

        if (period.isZero) dduration.toString // covers the all-zero case: "PT0S"
        else if (dduration.isZero) period.toString
        else period.toString + dduration.toString.substring(1)
    },

    builtin("toParts", "str") {
      (pos, _, duration: String) =>
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
        } catch {
          case _: DateTimeParseException => Error.fail("Invalid ISO-8601 duration: " + duration)
        }

        if (negative) {
          period = period.negated()
          dduration = dduration.negated()
        }

        val out = new java.util.LinkedHashMap[String, Val.Obj.Member]
        out.put("years", memberOf(Val.Num(pos, period.getYears)))
        out.put("months", memberOf(Val.Num(pos, period.getMonths)))
        out.put("days", memberOf(Val.Num(pos, period.getDays)))
        out.put("hours", memberOf(Val.Num(pos, dduration.toHours)))
        out.put("minutes", memberOf(Val.Num(pos, dduration.toMinutesPart)))
        out.put("seconds", memberOf(Val.Num(pos, dduration.toSecondsPart)))

        new Val.Obj(pos, out, false, null, null)
    }
  )
}
