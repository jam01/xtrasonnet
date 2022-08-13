package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumbersTest {

    @Test
    public void from() {
        assertEquals(transform("100"), transform("xtr.numbers.fromBinary(1100100)"));
        assertEquals(transform("15"), transform("xtr.numbers.fromHex('F')"));
        assertEquals(transform("36446"), transform("xtr.numbers.fromOctal(107136)"));
        assertEquals(transform("3"), transform("xtr.numbers.fromRadix('10', 3)"));
    }

    @Test
    public void to() {
        assertEquals(transform("'1100100'"), transform("xtr.numbers.toBinary(100)"));
        assertEquals(transform("'F'"), transform("xtr.numbers.toHex(15)"));
//        assertEquals(transform("'107136'"), transform("xtr.numbers.toOctal(36446)"));
        assertEquals(transform("'10'"), transform("xtr.numbers.toRadix('3', 3)"));
    }
}
