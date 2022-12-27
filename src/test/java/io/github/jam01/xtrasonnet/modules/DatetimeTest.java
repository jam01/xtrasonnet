package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

    @Disabled
    @Test
    public void current() {
        Assertions.assertEquals(TestUtils.transform("'2021-01-05T13:09:45.476375-05:00'"), TestUtils.transform("xtr.datetime.now()"));
        Assertions.assertEquals(TestUtils.transform("'2021-01-05T00:00:00-05:00'"), TestUtils.transform("xtr.datetime.today"));
        Assertions.assertEquals(TestUtils.transform("'2021-01-06T00:00:00-05:00'"), TestUtils.transform("xtr.datetime.tomorrow"));
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
    public void toLocal() {
        Assertions.assertEquals(TestUtils.transform("'2019-07-04'"), TestUtils.transform("xtr.datetime.toLocalDate('2019-07-04T18:53:41Z')"));
        Assertions.assertEquals(TestUtils.transform("'2019-07-04T21:00:00'"), TestUtils.transform("xtr.datetime.toLocalDateTime('2019-07-04T21:00:00Z')"));
        Assertions.assertEquals(TestUtils.transform("'21:00:00'"), TestUtils.transform("xtr.datetime.toLocalTime('2019-07-04T21:00:00Z')"));
    }

    @Test
    public void toParts() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                    year: 2019, month: 7, day: 4,
                    hour: 21, minute: 0, second: 0, nanosecond: 0,
                    offset: 'Z'
                }"""), TestUtils.transform("xtr.datetime.toParts('2019-07-04T21:00:00Z')"));
    }
}
