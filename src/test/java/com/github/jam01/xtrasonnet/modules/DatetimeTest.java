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

public class DatetimeTest {

    @Test
    public void atBeginningOf() {
        assertEquals(transform("'2020-12-31T00:00:00Z'"), transform("xtr.datetime.atBeginningOfDay('2020-12-31T23:19:35Z')"));
        assertEquals(transform("'2020-12-31T23:00:00Z'"), transform("xtr.datetime.atBeginningOfHour('2020-12-31T23:19:35Z')"));
        assertEquals(transform("'2020-12-01T00:00:00Z'"), transform("xtr.datetime.atBeginningOfMonth('2020-12-31T23:19:35Z')"));
        assertEquals(transform("'2020-12-27T00:00:00Z'"), transform("xtr.datetime.atBeginningOfWeek('2020-12-31T23:19:35Z')"));
        assertEquals(transform("'2020-01-01T00:00:00Z'"), transform("xtr.datetime.atBeginningOfYear('2020-12-31T23:19:35Z')"));
    }

    @Test
    public void between() {
        assertEquals(transform("'-P6D'"), transform("""
                local date1 = '2019-09-20T18:53:41.425Z';
                local date2 = '2019-09-14T18:53:41.425Z';

                xtr.datetime.between(date1, date2)"""));
        assertEquals(transform("'-P6DT30M'"), transform("""
                local date1 = '2019-09-20T18:53:41.425Z';
                local date2 = '2019-09-14T18:23:41.425Z';

                xtr.datetime.between(date1, date2)"""));
    }

    @Test
    public void compare() {
        assertEquals(transform("1"), transform("xtr.datetime.compare('2020-12-31T23:19:35Z','2020-01-01T00:00:00Z')"));
    }

    @Disabled
    @Test
    public void current() {
        assertEquals(transform("'2021-01-05T13:09:45.476375-05:00'"), transform("xtr.datetime.now()"));
        assertEquals(transform("'2021-01-05T00:00:00-05:00'"), transform("xtr.datetime.today"));
        assertEquals(transform("'2021-01-06T00:00:00-05:00'"), transform("xtr.datetime.tomorrow"));
    }

    @Test
    public void format() {
        assertEquals(transform("'2019/09/20'"), transform("xtr.datetime.format('2019-09-20T18:53:41.425Z', 'yyyy/MM/dd')"));
    }

    @Test
    public void inOffset() {
        assertEquals(transform("'2020-12-31T15:19:35-08:00'"), transform("xtr.datetime.inOffset('2020-12-31T23:19:35Z', '-08:00')"));
    }

    @Test
    public void is() {
        assertEquals(transform("false"), transform("xtr.datetime.isLeapYear('2019-09-14T18:53:41.425Z')"));
    }

    @Test
    public void of() {
        assertEquals(transform("'2021-01-01T00:00:00-08:00'"), transform("""
                local parts = {
                    'year': 2021,
                    'offset': '-08:00'
                };
                xtr.datetime.of(parts)"""));
    }

    @Test
    public void parse() {
        assertEquals(transform("'1990-12-31T10:10:10Z'"), transform("xtr.datetime.parse('12/31/1990 10:10:10', 'MM/dd/yyyy HH:mm:ss')"));
    }

    @Test
    public void plusMinus() {
        assertEquals(transform("'2019-09-18T18:53:41Z'"), transform("xtr.datetime.minus('2019-09-20T18:53:41Z', 'P2D')"));
        assertEquals(transform("'2019-09-18T16:53:41Z'"), transform("xtr.datetime.minus('2019-09-20T18:53:41Z', 'P2DT2H')"));
        assertEquals(transform("'2019-09-20T18:53:41Z'"), transform("xtr.datetime.plus('2019-09-18T18:53:41Z', 'P2D')"));
        assertEquals(transform("'2019-09-20T20:53:41Z'"), transform("xtr.datetime.plus('2019-09-18T18:53:41Z', 'P2DT2H')"));
    }

    @Test
    public void toLocal() {
        assertEquals(transform("'2019-07-04'"), transform("xtr.datetime.toLocalDate('2019-07-04T18:53:41Z')"));
        assertEquals(transform("'2019-07-04T21:00:00'"), transform("xtr.datetime.toLocalDateTime('2019-07-04T21:00:00Z')"));
        assertEquals(transform("'21:00:00'"), transform("xtr.datetime.toLocalTime('2019-07-04T21:00:00Z')"));
    }

    @Test
    public void toParts() {
        assertEquals(transform("""
                {
                    year: 2019, month: 7, day: 4,
                    hour: 21, minute: 0, second: 0, nanosecond: 0,
                    offset: 'Z'
                }"""), transform("xtr.datetime.toParts('2019-07-04T21:00:00Z')"));
    }
}
