package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.XtrasonnetException;
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumbersTest {

    @Test
    public void of() {
        assertEquals(transform("100"), transform("xtr.numbers.ofBinary(1100100)"));
        assertEquals(transform("15"), transform("xtr.numbers.ofHex('F')"));
        assertEquals(transform("36446"), transform("xtr.numbers.ofOctal(107136)"));
        assertEquals(transform("3"), transform("xtr.numbers.ofRadix('10', 3)"));
    }

    @Test
    public void to() {
        assertEquals(transform("'1100100'"), transform("xtr.numbers.toBinary(100)"));
        assertEquals(transform("'f'"), transform("xtr.numbers.toHex(15)"));
        assertEquals(transform("'107136'"), transform("xtr.numbers.toOctal(36446)"));
        assertEquals(transform("'10'"), transform("xtr.numbers.toRadix('3', 3)"));
    }

    @Test
    public void toRadix_rejectsARadixOutOfRange() {
        // BigInteger.toString(radix) falls back to radix 10 out of range, which would answer "255"
        assertMessageContains("Expected a radix within [2, 36], got: 99", () -> transform("xtr.numbers.toRadix(255, 99)"));
        assertMessageContains("Expected a radix within [2, 36], got: 1", () -> transform("xtr.numbers.toRadix(255, 1)"));
        assertMessageContains("Expected a radix within [2, 36], got: 0", () -> transform("xtr.numbers.toRadix(255, 0)"));
    }

    @Test
    public void ofRadix_rejectsARadixOutOfRange() {
        assertMessageContains("Expected a radix within [2, 36], got: 99", () -> transform("xtr.numbers.ofRadix('10', 99)"));
    }

    @Test
    public void of_rejectsDigitsOutsideTheRadix() {
        assertMessageContains("Expected a String of radix 16 digits, got: zz", () -> transform("xtr.numbers.ofHex('zz')"));
        assertMessageContains("Expected a String of radix 2 digits, got: 12", () -> transform("xtr.numbers.ofBinary('12')"));
        assertMessageContains("Expected a String of radix 8 digits, got: 9", () -> transform("xtr.numbers.ofOctal('9')"));
        assertMessageContains("Expected a String of radix 3 digits, got: 5", () -> transform("xtr.numbers.ofRadix('5', 3)"));
    }

    private static void assertMessageContains(String expected, org.junit.jupiter.api.function.Executable call) {
        var thrown = assertThrows(XtrasonnetException.class, call);
        assertTrue(thrown.getMessage().contains(expected),
                "expected a message containing <" + expected + "> but was <" + thrown.getMessage() + ">");
    }
}
