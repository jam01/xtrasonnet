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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathTest {
    @Test
    void testMath_mantissa() {
        assertEquals("0.5", transform("ds.math.mantissa(2)"));
    }

    @Test
    void testMath_exponent() {
        assertEquals("3", transform("ds.math.exponent(5)"));
    }

    @Test
    void test_abs() {
        assertEquals("1", transform("ds.math.abs(-1)"));
    }

    @Test
    void test_avg() {
        assertEquals("3", transform("ds.math.avg([1,2,3,4,5])"));
    }

    @Test
    void test_ceil() {
        assertEquals("2", transform("ds.math.ceil(1.5)"));
    }

    @Test
    void test_sqrt() {
        assertEquals("2", transform("ds.math.sqrt(4)"));
    }

    @Test
    void test_mod() {
        assertEquals("1", transform("ds.math.mod(3,2)"));
    }

    @Test
    void test_pow() {
        assertEquals("8", transform("ds.math.pow(2,3)"));
    }

    @Test
    void test_sum() {
        assertEquals("15", transform("ds.math.sum([1,2,3,4,5])"));
    }

    @Test
    void test_randomInt() {
        double dblVal = Double.parseDouble(transform("ds.math.randomInt(10)"));
        assertTrue(dblVal >= 0 && dblVal <= 10);
    }

    @Test
    void test_random() {
        double dblVal = Double.parseDouble(transform("ds.math.random()"));
        assertTrue(dblVal >= 0 && dblVal <= 1);
    }

    @Test
    void test_floor() {
        assertEquals("1", transform("ds.math.floor(1.9)"));
    }

    @Test
    void test_round() {
        assertEquals("2", transform("ds.math.round(1.5)"));
    }
}
