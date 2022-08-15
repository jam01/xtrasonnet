package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DurationTest {
    @Test
    public void of() {
        assertEquals(transform("'P20Y3M1DT12H30M45S'"), transform("""
                local parts = {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                };

                xtr.duration.of(parts)"""));
    }

    @Disabled
    @Test
    public void toParts() {
        assertEquals(transform("""
                {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                }"""), transform("xtr.duration.toParts('P20Y3M1DT12H30M45S')"));
    }
//
//    @Test
//    public void nother1() {
//        OffsetDateTime date1 = OffsetDateTime.parse("2039-12-22T07:24:26Z");
//        OffsetDateTime date2 = OffsetDateTime.parse("2019-09-20T18:53:41Z");
//        var dur = Duration.between(date1.toLocalTime(), date2.toLocalTime());
//        date2 = date2.plus(dur);
//
//        var per = Period.between(date1.toLocalDate(), date2.toLocalDate());
//        var isNeg = per.isNegative();
//        per = isNeg ? per.negated() : per;
//
//        System.out.println(per);
//
////        var revDur = Duration.between(date2.toLocalTime(), date1.toLocalTime());
////        System.out.println(per + dur.abs().toString().substring(1));
//    }
//
//    @Test
//    public void nother() {
//        OffsetDateTime date1 = OffsetDateTime.parse("2039-12-22T07:24:26.425Z");
//        OffsetDateTime date2 = OffsetDateTime.parse("2019-09-20T18:53:41.425Z");
//        var dur = Duration.between(date1, date2);
//        var isNeg = dur.isNegative();
//        var durStr = dur.abs().toString();
//        System.out.println(durStr);
//        var hoursIdx = durStr.indexOf('H');
//        if (hoursIdx != -1) {
//            var hours = Long.valueOf(durStr.substring(2, hoursIdx));
//            System.out.println(hours);
//            var days = (int) (hours / 24);
//            System.out.println(days);
//            hours = hours % 24;
//            System.out.println(hours);
//
//            var per = Period.between(date1.toLocalDate(), date1.toLocalDate().plusDays(days)).toString();
//            System.out.println(per);
//            System.out.println((isNeg ? "-" : "") + per + "T" + hours + durStr.substring(hoursIdx));
//        }
//    }
//
//    @Test
//    public void nother2() {
//        var dur = Duration.between(OffsetDateTime.parse("2039-12-22T07:24:26.425Z"), OffsetDateTime.parse("2019-09-20T18:53:41.425Z"));
//        var isNeg = dur.isNegative();
//        var durStr = dur.abs().toString();
//        System.out.println(durStr);
//        var hoursIdx = durStr.indexOf('H');
//        if (hoursIdx != -1) {
//            var hours = Long.valueOf(durStr.substring(2, hoursIdx));
//            System.out.println(hours);
//            var days = (int) (hours / 24);
//            System.out.println(days);
//            hours = hours % 24;
//            System.out.println(hours);
//
//            var per = Period.ofDays(days).toString();
//            System.out.println(per);
//            System.out.println((isNeg ? "-" : "") + per + "T" + hours + durStr.substring(hoursIdx));
//        }
//    }
//
//    @Test
//    public void main() {
//        System.out.println(
//                transform("""
//                        xtr.duration.of({
//                            years: 20, months: 3, days: 1,
//                            hours: 12, minutes: 30, seconds: 45
//                        })""")
//        );
//
//        System.out.println(OffsetDateTime.parse("2019-09-20T18:53:41.425Z").plus(Period.parse("P20Y3M1D")).plus(Duration.parse("PT12H30M45S")));
//
////        System.out.println(
////                transform("xtr.duration.toParts('P20Y3M1DT12H30M45S')")
////        );
//
//        System.out.println(
//                transform("xtr.duration.between('2039-12-22T07:24:26.425Z', '2019-09-20T18:53:41.425Z')")
//        );
//    }
}
