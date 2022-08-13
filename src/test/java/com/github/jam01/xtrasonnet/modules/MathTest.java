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

public class MathTest {

    @Test
    public void main() {
        assertEquals(transform("1"), transform("xtr.math.abs(-1)"));
        assertEquals(transform("0"), transform("xtr.math.acos(1)"));
        assertEquals(transform("1.5707963267948966"), transform("xtr.math.asin(1)"));
        assertEquals(transform("0.7853981633974483"), transform("xtr.math.atan(1)"));
        assertEquals(transform("2"), transform("xtr.math.avg([1,2,3])"));
        assertEquals(transform("2"), transform("xtr.math.ceil(1.01)"));
        assertEquals(transform("10"), transform("xtr.math.clamp(100, 0, 10)"));
        assertEquals(transform("1"), transform("xtr.math.cos(0)"));
        assertEquals(transform("7.38905609893065"), transform("xtr.math.exp(2)"));
        assertEquals(transform("2"), transform("xtr.math.exponent(2)"));
        assertEquals(transform("4"), transform("xtr.math.floor(4.99)"));
        assertEquals(transform("0.6931471805599453"), transform("xtr.math.log(2)"));
        assertEquals(transform("0.5"), transform("xtr.math.mantissa(2)"));
        assertEquals(transform("4"), transform("xtr.math.pow(2, 2)"));
//        assertEquals(transform("0.5963038027787421"), transform("xtr.math.random"));
//        assertEquals(transform("485"), transform("xtr.math.randomInt(500)"));
        assertEquals(transform("3"), transform("xtr.math.round(2.5)"));
        assertEquals(transform("0.8414709848078965"), transform("xtr.math.sin(1)"));
        assertEquals(transform("2"), transform("xtr.math.sqrt(4)"));
        assertEquals(transform("60"), transform("xtr.math.sum([10, 20, 30])"));
    }
}
