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

public class BinariesTest {
    @Test
    void testBinaries_fromBase64() {
        assertEquals("Hello World", transform("tro.binaries.fromBase64('SGVsbG8gV29ybGQ=')"));
        assertEquals("45", transform("tro.binaries.fromBase64('NDU=')"));
    }

    @Test
    void testBinaries_fromHex() {
        assertEquals("Hello World", transform("tro.binaries.fromHex('48656C6C6F20576F726C64')"));
        assertEquals("45", transform("tro.binaries.fromHex('3435')"));
        assertEquals("-", transform("tro.binaries.fromHex('2D')"));
    }

    // TODO: 8/19/20 additional testing
    @Test
    void testBinaries_readLinesWith() {
        assertEquals("[Line 1,Line 2,Line 3,Line 4,Line 5]", transform("tro.binaries.readLinesWith('Line 1\\nLine 2\\nLine 3\\nLine 4\\nLine 5\\n', 'UTF-8')"));
    }

    @Test
    void testBinaries_toBase64() {
        assertEquals("SGVsbG8gV29ybGQ=", transform("tro.binaries.toBase64('Hello World')"));
        assertEquals("NDU=", transform("tro.binaries.toBase64(45)"));
        assertEquals("NDU=", transform("tro.binaries.toBase64(45.0)"));
        assertEquals("NDUuMQ==", transform("tro.binaries.toBase64(45.1)"));
    }

    @Test
    void testBinaries_toHex() {
        assertEquals("48656C6C6F20576F726C64", transform("tro.binaries.toHex('Hello World')"));
        assertEquals("2D", transform("tro.binaries.toHex(45)"));
        assertEquals("2D", transform("tro.binaries.toHex(45.0)"));
        assertEquals("2D", transform("tro.binaries.toHex(45.1)"));
    }

    //TODO additional testing
    @Test
    void testBinaries_writeLinesWith() {
        assertEquals("Line 1\\nLine 2\\nLine 3\\nLine 4\\nLine 5\\n", transform("tro.binaries.writeLinesWith(['Line 1','Line 2','Line 3','Line 4','Line 5'], 'UTF-8')"));
    }
}
