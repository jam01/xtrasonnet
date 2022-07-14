package com.datasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZonedDateTimeTest {
    @Test
    void testOffset() {
        assertEquals("2020-07-23T21:00:00Z", transform("ds.datetime.plus('2019-07-22T21:00:00Z', 'P1Y1D')"));
    }

    @Test
    void testNow() throws Exception {
        ZonedDateTime before = ZonedDateTime.now();
        Thread.sleep(100);

        ZonedDateTime mapped = ZonedDateTime.parse(transform("ds.datetime.now()"));
        Thread.sleep(100);

        ZonedDateTime after = ZonedDateTime.now();
        assertTrue(before.compareTo(mapped) <= 0);
        assertTrue(after.compareTo(mapped) >= 0);
    }

    @Test
    void testFormat() {
        assertEquals("4 Jul 2019", transform("ds.datetime.format('2019-07-04T21:00:00Z', 'd MMM uuuu')"));
    }

    @Test
    void testCompare() {
        assertEquals("0", transform("ds.datetime.compare('2019-07-04T21:00:00Z', '2019-07-04T21:00:00Z')"));
    }

    @Test
    void testTimezone() {
        assertEquals("2019-07-04T19:00:00-07:00", transform("ds.datetime.changeTimeZone('2019-07-04T21:00:00-05:00', 'America/Los_Angeles')"));
    }

    @Test
    void testLocalDT() {
        assertEquals("2019-07-04", transform("ds.datetime.toLocalDate('2019-07-04T21:00:00-05:00')"));
        assertEquals("21:00:00", transform("ds.datetime.toLocalTime('2019-07-04T21:00:00-05:00')"));
    }

    @Test
    void testDateTime_atBeginningOfDay() {
        assertEquals("2020-10-21T00:00:00Z", transform("ds.datetime.atBeginningOfDay('2020-10-21T16:08:07.131Z')"));
    }

    @Test
    void testDateTime_atBeginningOfHour() {
        assertEquals("2020-10-21T16:00:00Z", transform("ds.datetime.atBeginningOfHour('2020-10-21T16:08:07.131Z')"));
    }

    @Test
    void testDateTime_atBeginningOfMonth() {
        assertEquals("2020-10-01T00:00:00Z", transform("ds.datetime.atBeginningOfMonth('2020-10-21T16:08:07.131Z')"));
        assertEquals("2020-10-01T00:00:00Z", transform("ds.datetime.atBeginningOfMonth('2020-10-01T16:08:07.131Z')"));
    }

    @Test
    void testDateTime_atBeginningOfWeek() {
        assertEquals("2020-10-18T00:00:00Z", transform("ds.datetime.atBeginningOfWeek('2020-10-21T16:08:07.131Z')"));
        assertEquals("2020-10-18T00:00:00Z", transform("ds.datetime.atBeginningOfWeek('2020-10-18T16:08:07.131Z')"));
    }

    @Test
    void testDateTime_atBeginningOfYear() {
        assertEquals("2020-01-01T00:00:00Z", transform("ds.datetime.atBeginningOfYear('2020-10-21T16:08:07.131Z')"));
    }

    @Test
    void testDateTime_date() {
        assertEquals("2020-01-01T00:00:00Z", transform("ds.datetime.date({'year':2020})"));
        assertEquals("0000-12-01T00:00:00Z", transform("ds.datetime.date({'month':12})"));
        assertEquals("0000-01-20T00:00:00Z", transform("ds.datetime.date({'day':20})"));
        assertEquals("0000-01-01T23:00:00Z", transform("ds.datetime.date({'hour':23})"));
        assertEquals("0000-01-01T00:23:00Z", transform("ds.datetime.date({'minute':23})"));
        assertEquals("0000-01-01T00:00:23Z", transform("ds.datetime.date({'second':23})"));
        /*
		assertEquals("0000-01-01T00:00:00.555Z", transform("ds.datetime.date({'nanosecond':1, 'second': 1})"));
		*/
        assertEquals("0000-01-01T00:00:00Z", transform("ds.datetime.date({'timezone':'UTC'})"));
    }

    @Test
    void testDateTime_parse() {
        assertEquals("2020-01-01T00:00:00Z", transform("ds.datetime.parse('1577836800', 'timestamp')"));
        assertEquals("2020-01-01T00:00:00Z", transform("ds.datetime.parse('1577836800', 'epoch')"));
        assertEquals("1990-12-31T10:10:10Z", transform("ds.datetime.parse('12/31/1990 10:10:10', 'MM/dd/yyyy HH:mm:ss')"));
    }

    @Test
    void test_daysBetween() {
        assertEquals("3", transform("ds.datetime.daysBetween('2020-07-04T00:00:00.000Z','2020-07-01T00:00:00.000Z')"));
        assertEquals("0", transform("ds.datetime.daysBetween('2020-07-04T23:59:59.000Z','2020-07-04T00:00:00.000Z')"));
    }

    @Test
    void test_isLeapYear() {
        assertEquals("true", transform("ds.datetime.isLeapYear('2020-07-04T21:00:00.000Z')"));
    }
}
