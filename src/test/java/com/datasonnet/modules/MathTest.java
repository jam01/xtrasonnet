package com.datasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathTest {

    @Test
    public void main() {
        assertEquals(transform("1"), transform("tro.math.abs(-1)"));
        assertEquals(transform("0"), transform("tro.math.acos(1)"));
        assertEquals(transform("1.5707963267948966"), transform("tro.math.asin(1)"));
        assertEquals(transform("0.7853981633974483"), transform("tro.math.atan(1)"));
        assertEquals(transform("2"), transform("tro.math.avg([1,2,3])"));
        assertEquals(transform("2"), transform("tro.math.ceil(1.01)"));
        assertEquals(transform("10"), transform("tro.math.clamp(100, 0, 10)"));
        assertEquals(transform("1"), transform("tro.math.cos(0)"));
        assertEquals(transform("7.38905609893065"), transform("tro.math.exp(2)"));
        assertEquals(transform("2"), transform("tro.math.exponent(2)"));
        assertEquals(transform("4"), transform("tro.math.floor(4.99)"));
        assertEquals(transform("0.6931471805599453"), transform("tro.math.log(2)"));
        assertEquals(transform("0.5"), transform("tro.math.mantissa(2)"));
        assertEquals(transform("4"), transform("tro.math.pow(2, 2)"));
//        assertEquals(transform("0.5963038027787421"), transform("tro.math.random"));
//        assertEquals(transform("485"), transform("tro.math.randomInt(500)"));
        assertEquals(transform("3"), transform("tro.math.round(2.5)"));
        assertEquals(transform("0.8414709848078965"), transform("tro.math.sin(1)"));
        assertEquals(transform("2"), transform("tro.math.sqrt(4)"));
        assertEquals(transform("60"), transform("tro.math.sum([10, 20, 30])"));
    }
}
