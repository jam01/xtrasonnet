package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import io.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public class DatetimeTest {

    @Test
    public void atBeginningOf() {
        Assertions.assertEquals(TestUtils.transform("'2020-12-31T00:00:00Z'"), TestUtils.transform("xtr.datetime.atBeginningOfDay('2020-12-31T23:19:35Z')"));
        Assertions.assertEquals(TestUtils.transform("'2020-12-31T23:00:00Z'"), TestUtils.transform("xtr.datetime.atBeginningOfHour('2020-12-31T23:19:35Z')"));
        Assertions.assertEquals(TestUtils.transform("'2020-12-01T00:00:00Z'"), TestUtils.transform("xtr.datetime.atBeginningOfMonth('2020-12-31T23:19:35Z')"));
        Assertions.assertEquals(TestUtils.transform("'2020-12-27T00:00:00Z'"), TestUtils.transform("xtr.datetime.atBeginningOfWeek('2020-12-31T23:19:35Z')"));
        Assertions.assertEquals(TestUtils.transform("'2020-01-01T00:00:00Z'"), TestUtils.transform("xtr.datetime.atBeginningOfYear('2020-12-31T23:19:35Z')"));
    }

    @Test
    public void between() {
        Assertions.assertEquals(TestUtils.transform("'-P6D'"), TestUtils.transform("""
                local date1 = '2019-09-20T18:53:41.425Z';
                local date2 = '2019-09-14T18:53:41.425Z';

                xtr.datetime.between(date1, date2)"""));
        Assertions.assertEquals(TestUtils.transform("'-P6DT30M'"), TestUtils.transform("""
                local date1 = '2019-09-20T18:53:41.425Z';
                local date2 = '2019-09-14T18:23:41.425Z';

                xtr.datetime.between(date1, date2)"""));
    }

    @Test
    public void compare() {
        Assertions.assertEquals(TestUtils.transform("1"), TestUtils.transform("xtr.datetime.compare('2020-12-31T23:19:35Z','2020-01-01T00:00:00Z')"));
    }

    @Test
    public void current() {
        // clock dependent, so assert shape and relationships rather than a fixed instant -- the
        // previous version compared against a hardcoded 2021 timestamp and could only ever be
        // @Disabled, leaving now/today/tomorrow with no coverage at all
        // note the parens: today and tomorrow are functions, and referencing one without calling it
        // fails inside sjsonnet's own error reporting, since builtins carry a null position
        var now = OffsetDateTime.parse(unquote(TestUtils.transform("xtr.datetime.now()")));
        var today = OffsetDateTime.parse(unquote(TestUtils.transform("xtr.datetime.today()")));
        var tomorrow = OffsetDateTime.parse(unquote(TestUtils.transform("xtr.datetime.tomorrow()")));

        Assertions.assertEquals(LocalTime.MIDNIGHT, today.toLocalTime(), "today should be the start of the day");
        Assertions.assertEquals(today.plusDays(1), tomorrow);
        Assertions.assertFalse(now.isBefore(today));
    }

    private static String unquote(String json) {
        return json.substring(1, json.length() - 1);
    }

    @Test
    public void format() {
        Assertions.assertEquals(TestUtils.transform("'2019/09/20'"), TestUtils.transform("xtr.datetime.format('2019-09-20T18:53:41.425Z', 'yyyy/MM/dd')"));
    }

    @Test
    public void inOffset() {
        Assertions.assertEquals(TestUtils.transform("'2020-12-31T15:19:35-08:00'"), TestUtils.transform("xtr.datetime.inOffset('2020-12-31T23:19:35Z', '-08:00')"));
    }

    @Test
    public void is() {
        Assertions.assertEquals(TestUtils.transform("false"), TestUtils.transform("xtr.datetime.isLeapYear('2019-09-14T18:53:41.425Z')"));
    }

    @Test
    public void of() {
        Assertions.assertEquals(TestUtils.transform("'2021-01-01T00:00:00-08:00'"), TestUtils.transform("""
                local parts = {
                    'year': 2021,
                    'offset': '-08:00'
                };
                xtr.datetime.of(parts)"""));
    }

    @Test
    public void parse() {
        Assertions.assertEquals(TestUtils.transform("'1990-12-31T10:10:10Z'"), TestUtils.transform("xtr.datetime.parse('12/31/1990 10:10:10', 'MM/dd/yyyy HH:mm:ss')"));
        Assertions.assertEquals(TestUtils.transform("'1990-12-31T10:10:10-06:00'"), TestUtils.transform("xtr.datetime.parse('12/31/1990 10:10:10 -06:00', 'MM/dd/yyyy HH:mm:ss XXX')"));
    }

    @Test
    public void plusMinus() {
        Assertions.assertEquals(TestUtils.transform("'2019-09-18T18:53:41Z'"), TestUtils.transform("xtr.datetime.minus('2019-09-20T18:53:41Z', 'P2D')"));
        Assertions.assertEquals(TestUtils.transform("'2019-09-18T16:53:41Z'"), TestUtils.transform("xtr.datetime.minus('2019-09-20T18:53:41Z', 'P2DT2H')"));
        Assertions.assertEquals(TestUtils.transform("'2019-09-20T18:53:41Z'"), TestUtils.transform("xtr.datetime.plus('2019-09-18T18:53:41Z', 'P2D')"));
        Assertions.assertEquals(TestUtils.transform("'2019-09-20T20:53:41Z'"), TestUtils.transform("xtr.datetime.plus('2019-09-18T18:53:41Z', 'P2DT2H')"));
    }

    @Test
    public void plusMinus_timeOnlyDurations() {
        Assertions.assertEquals(TestUtils.transform("'2019-09-18T20:53:41Z'"), TestUtils.transform("xtr.datetime.plus('2019-09-18T18:53:41Z', 'PT2H')"));
        Assertions.assertEquals(TestUtils.transform("'2019-09-18T16:53:41Z'"), TestUtils.transform("xtr.datetime.minus('2019-09-18T18:53:41Z', 'PT2H')"));

        // composes with duration.of, whose canonical output is time-only for time-only parts
        Assertions.assertEquals(TestUtils.transform("'2019-09-18T20:53:41Z'"),
                TestUtils.transform("xtr.datetime.plus('2019-09-18T18:53:41Z', xtr.duration.of({hours: 2}))"));
    }

    @Test
    public void plusMinus_negativeDurationAppliesToAllParts() {
        // -P1DT2H is minus(1 day and 2 hours); subtracting it adds both parts
        Assertions.assertEquals(TestUtils.transform("'2019-09-21T20:53:41Z'"), TestUtils.transform("xtr.datetime.minus('2019-09-20T18:53:41Z', '-P1DT2H')"));
        Assertions.assertEquals(TestUtils.transform("'2019-09-19T16:53:41Z'"), TestUtils.transform("xtr.datetime.plus('2019-09-20T18:53:41Z', '-P1DT2H')"));
    }

    @Test
    public void plusMinus_invalidDuration() {
        var ex = Assertions.assertThrows(RuntimeException.class,
                () -> TestUtils.transform("xtr.datetime.plus('2019-09-18T18:53:41Z', '2 days')"));
        Assertions.assertTrue(ex.getMessage().contains("Invalid ISO-8601 duration: 2 days"), ex.getMessage());
    }

    @Test
    public void toLocal() {
        Assertions.assertEquals(TestUtils.transform("'2019-07-04'"), TestUtils.transform("xtr.datetime.toLocalDate('2019-07-04T18:53:41Z')"));
        Assertions.assertEquals(TestUtils.transform("'2019-07-04T21:00:00'"), TestUtils.transform("xtr.datetime.toLocalDateTime('2019-07-04T21:00:00Z')"));
        Assertions.assertEquals(TestUtils.transform("'21:00:00'"), TestUtils.transform("xtr.datetime.toLocalTime('2019-07-04T21:00:00Z')"));
    }

    @Test
    public void toParts() {
        // 2019-07-10 is a Wednesday, so day-of-month (10) and day-of-week (3) differ. The previous
        // fixture used 2019-07-04, a Thursday, where both are 4 -- so it passed while `day` was
        // being populated with the day-of-week.
        Assertions.assertEquals(TestUtils.transform("""
                {
                    year: 2019, month: 7, day: 10, dayOfWeek: 3,
                    hour: 21, minute: 0, second: 0, nanosecond: 0,
                    offset: 'Z'
                }"""), TestUtils.transform("xtr.datetime.toParts('2019-07-10T21:00:00Z')"));
    }

    @Test
    public void ofToPartsRoundTrips() {
        Assertions.assertEquals(TestUtils.transform("'2019-07-10T21:34:56Z'"),
                TestUtils.transform("xtr.datetime.of(xtr.datetime.toParts('2019-07-10T21:34:56Z'))"));
    }

    @Test
    public void ofRejectsUnknownParts() {
        var ex = Assertions.assertThrows(RuntimeException.class,
                () -> TestUtils.transform("xtr.datetime.of({years: 2021})"));
        Assertions.assertTrue(ex.getMessage().contains("Unexpected datetime part: years"), ex.getMessage());
    }
}
