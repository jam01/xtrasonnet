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
 * - 5bb242721f728c00432234cd24f7256e21c4caac: Added some expanded functionality
 *      Functions: datetime.atBeginningOf*
 * - 78acf4ebf5545b88df4cf9f77434335fc857eaa1: Added date function and period module
 * - c20475cacff9b6790e85afaf7ae730d4aa9c4470: Merge pull request #86 from datasonnet/unix-timestamp
 *      Functions: datetime.parse
 */

import io.github.jam01.xtrasonnet.spi.Library.{builtinx, memberOf}
import io.github.jam01.xtrasonnet.spi.Library.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val
import sjsonnet.Error

import java.time.{Duration, Instant, OffsetDateTime, Period, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.collection.mutable

object Datetime {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtinx("now") { (_, _) => OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },

    builtin("format", "datetime", "outputFormat") { (_, _, datetime: String, outputFormat: String) =>
      val datetimeObj = OffsetDateTime.parse(datetime)
      datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
    },

    builtin("compare", "datetime", "datetwo") { (_, _, datetimeone: String, datetimetwo: String) =>
      val datetimeObj1 = OffsetDateTime.parse(datetimeone)
      val datetimeObj2 = OffsetDateTime.parse(datetimetwo)

      java.lang.Math.max(-1, java.lang.Math.min(datetimeObj1.compareTo(datetimeObj2), 1))
    },

    builtin("plus", "datetime", "duration") { (_, _, date: String, duration: String) =>
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

    builtin("minus", "datetime", "duration") { (_, _, date: String, duration: String) =>
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
      (_, _, datetime: String, offset: String) =>
        val datetimeObj = OffsetDateTime.parse(datetime)
        val zoneId = ZoneOffset.of(offset)
        val newDateTimeObj = datetimeObj.withOffsetSameInstant(zoneId)
        newDateTimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    builtin("toLocalDate", "datetime") { (_, _, datetime: String) =>
      val datetimeObj = OffsetDateTime.parse(datetime)
      datetimeObj.toLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    },

    builtin("toLocalTime", "datetime") { (_, _, datetime: String) =>
      val datetimeObj = OffsetDateTime.parse(datetime)
      datetimeObj.toLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
    },

    builtin("toLocalDateTime", "datetime") { (_, _, datetime: String) =>
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
      (_, _, datetime: String) =>
        OffsetDateTime
          .parse(datetime)
          .toLocalDate.isLeapYear;
    },

    builtinx("today") {
      (_, _) =>
        val date = OffsetDateTime.now()
        date.minusHours(date.getHour)
          .minusMinutes(date.getMinute)
          .minusSeconds(date.getSecond)
          .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    builtinx("tomorrow") {
      (_, _) =>
        val date = OffsetDateTime.now()
        date.plusDays(1)
          .minusHours(date.getHour)
          .minusMinutes(date.getMinute)
          .minusSeconds(date.getSecond)
          .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    builtinx("yesterday") {
      (_, _) =>
        val date = OffsetDateTime.now()
        date.minusDays(1)
          .minusHours(date.getHour)
          .minusMinutes(date.getMinute)
          .minusSeconds(date.getSecond)
          .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    builtin("toParts", "datetime") {
      (pos, _, datetime: String) =>
        val date = OffsetDateTime.parse(datetime)
        val out = new java.util.LinkedHashMap[String, Val.Obj.Member]
        out.put("year", memberOf(Val.Num(pos, date.getYear)))
        out.put("month", memberOf(Val.Num(pos, date.getMonthValue)))
        out.put("day", memberOf(Val.Num(pos, date.getDayOfWeek.getValue)))
        out.put("hour", memberOf(Val.Num(pos, date.getHour)))
        out.put("minute", memberOf(Val.Num(pos, date.getMinute)))
        out.put("second", memberOf(Val.Num(pos, date.getSecond)))
        out.put("nanosecond", memberOf(Val.Num(pos, date.getNano)))
        out.put("offset", memberOf(Val.Str(pos, date.getOffset.getId)))

        new Val.Obj(pos, out, false, null, null)
    },

    /*
     * datasonnet-mapper: start
     *
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     */
    builtin("atBeginningOfDay", "datetime") {
      (_, _, datetime: String) =>
        val date = OffsetDateTime.parse(datetime)
        date.minusHours(date.getHour)
          .minusMinutes(date.getMinute)
          .minusSeconds(date.getSecond)
          .minusNanos(date.getNano)
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    /*
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     */
    builtin("atBeginningOfHour", "datetime") {
      (_, _, datetime: String) =>
        val date = OffsetDateTime.parse(datetime)
        date.minusMinutes(date.getMinute)
          .minusSeconds(date.getSecond)
          .minusNanos(date.getNano)
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    /*
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     */
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

    /*
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     */
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

    /*
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     */
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

    /*
     * Changes made:
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     * - a192423d89a9bdb898a3d3314db306a4a9d10773: rename epoch format to unix
     */
    builtin("parse", "datetime", "inputFormat") { (_, _, datetime: Val, inputFormat: String) =>
      var datetimeObj: OffsetDateTime = null
      inputFormat.toLowerCase match {
        case "unix" =>
          var inst: Instant = null
          datetime match {
            case str: Val.Str => inst = Instant.ofEpochSecond(str.value.toLong)
            case num: Val.Num => inst = Instant.ofEpochSecond(num.value.toLong)
            case _ => Error.fail("Expected datetime to be a string or number, got: " + datetime.prettyName)
          }
          datetimeObj = OffsetDateTime.ofInstant(inst, ZoneOffset.UTC)
        case _ => datetimeObj =
          OffsetDateTime.parse(datetime.asString, DateTimeFormatter.ofPattern(inputFormat).withZone(ZoneOffset.UTC)) // defaulting to UTC
      }
      datetimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    /*
     * Changes made:
     * - 807cd78abf40551644067455338e5b2a683e86bd: upgraded sjsonnet
     * - fc930f88524269cb6ddfa26b24f8c34df5502756: refactor datetime and period modules
     * - a192423d89a9bdb898a3d3314db306a4a9d10773: rename epoch format to unix
     */
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
    }
    /*
     * datasonnet-mapper: end
     */
  )
}
