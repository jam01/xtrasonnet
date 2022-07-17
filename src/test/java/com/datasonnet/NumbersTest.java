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

public class NumbersTest {
    @Test
    void testNumbers_fromBinary() {
        assertEquals("-2", transform("tro.numbers.fromBinary(\"-10\")"));
        assertEquals("2147483647", transform("tro.numbers.fromBinary(\"1111111111111111111111111111111\")"));
        assertEquals("3", transform("tro.numbers.fromBinary(11)"));
        assertEquals("4", transform("tro.numbers.fromBinary(\"100\")"));
        assertEquals("2305843009213693952", transform("tro.numbers.fromBinary(\"1111111111111111111111111111111111111111111111111111111111111\")"));
    }

    @Test
    void testNumbers_fromHex() {
        assertEquals("-1", transform("tro.numbers.fromHex(\"-1\")"));
        assertEquals("4078315", transform("tro.numbers.fromHex(\"3e3aeb\")"));
        assertEquals("0", transform("tro.numbers.fromHex(0)"));
        assertEquals("15", transform("tro.numbers.fromHex(\"f\")"));
        assertEquals("68719476735", transform("tro.numbers.fromHex(\"FFFFFFFFF\")"));
    }

    @Test
    void testNumbers_fromRadixNumber() {
        assertEquals("2", transform("tro.numbers.fromRadixNumber(10, 2)"));
        assertEquals("255", transform("tro.numbers.fromRadixNumber(\"ff\", 16)"));
        assertEquals("68719476735", transform("tro.numbers.fromRadixNumber(\"FFFFFFFFF\", 16)"));
    }

    @Test
    void testNumbers_toBinary() {
        assertEquals("-10", transform("tro.numbers.toBinary(-2)"));
        assertEquals("0", transform("tro.numbers.toBinary(0)"));
        assertEquals("10", transform("tro.numbers.toBinary(2)"));
        assertEquals("100111011100110101100100111111111", transform("tro.numbers.toBinary(5294967295)"));
    }

    @Test
    void testNumbers_toHex() {
        assertEquals("-1", transform("tro.numbers.toHex(-1)"));
        assertEquals("0", transform("tro.numbers.toHex(0)"));
        assertEquals("f", transform("tro.numbers.toHex(15)"));
        assertEquals("FFFFFFFFF".toLowerCase(), transform("tro.numbers.toHex(68719476735)"));
    }

    @Test
    void testNumbers_toRadixNumber() {
        assertEquals("10", transform("tro.numbers.toRadixNumber(2, 2)"));
        assertEquals("ff", transform("tro.numbers.toRadixNumber(255, 16)"));
        assertEquals("fffffffff", transform("tro.numbers.toRadixNumber(68719476735, 16)"));
    }

}