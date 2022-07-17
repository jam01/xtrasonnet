package com.datasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2020 the original author or authors.
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

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PeriodTest {
    @Test
    void testPeriod_between() {
        assertEquals("P1D", transform("tro.period.between('2020-10-21T16:08:07.131Z', '2020-10-22T10:20:07.131Z')"));
    }

    @Test
    void testPeriod_days() {
        assertEquals("P1D", transform("tro.period.days(1)"));
    }

    @Test
    void testPeriod_duration() {
        assertEquals("PT48H", transform("tro.period.duration({'days':2})"));
        assertEquals("PT2H", transform("tro.period.duration({'hours':2})"));
        assertEquals("PT2M", transform("tro.period.duration({'minutes':2})"));
        assertEquals("PT2S", transform("tro.period.duration({'seconds':2})"));
        assertEquals("PT26H3M4S", transform("tro.period.duration({'days':1,'hours':2,'minutes':3,'seconds':4})"));
    }

    @Test
    void testPeriod_hours() {
        assertEquals("PT1H", transform("tro.period.hours(1)"));
    }

    @Test
    void testPeriod_minutes() {
        assertEquals("PT1M", transform("tro.period.minutes(1)"));
    }

    @Test
    void testPeriod_months() {
        assertEquals("P1M", transform("tro.period.months(1)"));
    }

    @Test
    void testPeriod_period() {
        assertEquals("P1Y", transform("tro.period.period({'years':1})"));
        assertEquals("P1M", transform("tro.period.period({'months':1})"));
        assertEquals("P1D", transform("tro.period.period({'days':1})"));
        assertEquals("P1Y2M3D", transform("tro.period.period({'years':1,'months':2,'days':3})"));
    }

    @Test
    void testPeriod_seconds() {
        assertEquals("PT1S", transform("tro.period.seconds(1)"));
    }

    @Test
    void testPeriod_years() {
        assertEquals("P1Y", transform("tro.period.years(1)"));
    }
}
