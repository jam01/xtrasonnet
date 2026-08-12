package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void powIsExact() {
        assertEquals(transform("1267650600228229401496703205376"), transform("xtr.math.pow(2, 100)"));
        // a whole-valued Float64 exponent takes the exact path too
        assertEquals(transform("1267650600228229401496703205376"), transform("xtr.math.pow(2, 100.0)"));
    }

    @Test
    public void powAcceptsFractionalExponents() {
        assertEquals(transform("1.4142135623730951"), transform("xtr.math.pow(2, 0.5)"));
        assertEquals(transform("8"), transform("xtr.math.pow(4, 1.5)"));
        assertEquals(transform("0.5"), transform("xtr.math.pow(4, -0.5)"));
    }

    @Test
    public void powRejectsResultsItCannotRepresent() {
        assertReported("non-zero base", () -> transform("xtr.math.pow(0, -1)"));
        assertReported("real result", () -> transform("xtr.math.pow(-1, 0.5)"));
    }

    /** Asserts the text appears somewhere in the thrown exception's chain. */
    private static void assertReported(String expected, org.junit.jupiter.api.function.Executable call) {
        var thrown = assertThrows(Exception.class, call);
        var chain = new StringBuilder();
        for (Throwable t = thrown; t != null; t = t.getCause()) chain.append(t.getMessage()).append(" | ");
        assertTrue(chain.toString().contains(expected), "expected <" + expected + "> in: " + chain);
    }

    @Test
    public void powAcceptsZeroAndNegativeExponents() {
        assertEquals(transform("1"), transform("xtr.math.pow(2, 0)"));
        assertEquals(transform("0.5"), transform("xtr.math.pow(2, -1)"));
    }
}
