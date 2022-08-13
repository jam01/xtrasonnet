package com.datasonnet.modules;

import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumbersTest {

    @Test
    public void from() {
        assertEquals(transform("100"), transform("tro.numbers.fromBinary(1100100)"));
        assertEquals(transform("15"), transform("tro.numbers.fromHex('F')"));
        assertEquals(transform("36446"), transform("tro.numbers.fromOctal(107136)"));
        assertEquals(transform("3"), transform("tro.numbers.fromRadix('10', 3)"));
    }

    @Test
    public void to() {
        assertEquals(transform("'1100100'"), transform("tro.numbers.toBinary(100)"));
        assertEquals(transform("'F'"), transform("tro.numbers.toHex(15)"));
//        assertEquals(transform("'107136'"), transform("tro.numbers.toOctal(36446)"));
        assertEquals(transform("'10'"), transform("tro.numbers.toRadix('3', 3)"));
    }
}
