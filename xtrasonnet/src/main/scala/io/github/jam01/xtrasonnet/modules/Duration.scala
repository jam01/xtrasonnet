package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.Val
import sjsonnet.functions.AbstractFunctionModule

import java.time.*
import scala.collection.mutable

object Duration extends AbstractFunctionModule {
  override def name: String = "duration"
  
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("of", "obj") {
      (pos, ev, obj: Val.Obj) =>
        val out = mutable.Map[String, Val]()
        obj.visibleKeyNames.foreach(key => out.addOne(key, obj.value(key, pos)(ev)))
        Period.ZERO
          .plusYears(out.getOrElse("years", Val.Num(pos, 0)).asInt)
          .plusMonths(out.getOrElse("months", Val.Num(pos, 0)).asInt)
          .plusDays(out.getOrElse("days", Val.Num(pos, 0)).asInt)
          .toString +
          java.time.Duration.ZERO
            .plusHours(out.getOrElse("hours", Val.Num(pos, 0)).asLong)
            .plusMinutes(out.getOrElse("minutes", Val.Num(pos, 0)).asLong)
            .plusSeconds(out.getOrElse("seconds", Val.Num(pos, 0)).asLong)
            .toString.substring(1)
    },

    builtin("toParts", "str") {
      (pos, _, duration: String) =>
        val out = new java.util.LinkedHashMap[String, Val.Obj.Member]
        val timeIdx = duration.indexOf('T')
        var period: Period = null
        var dduration: Duration = null

        if (timeIdx != -1) {
          dduration = java.time.Duration.parse('P' + duration.substring(timeIdx))
          period = Period.parse(duration.substring(0, timeIdx))
        } else {
          period = Period.parse(duration)
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
  )
}
