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

public class BetterNumsTest {

    @Test
    public void bigIntegersStayExactAbove2Pow53() {
        // avg([9007199254740993, 9007199254740995]) = 9007199254740994 exactly
        assertEquals(transform("9007199254740994"),
                transform("xtr.math.avg([9007199254740993, 9007199254740995])"));

        // sum stays exact too
        assertEquals(transform("18014398509481988"),
                transform("xtr.math.sum([9007199254740993, 9007199254740995])"));
    }

    @Test
    public void ceilFloorClampWithDecimals() {
        assertEquals(transform("2"), transform("xtr.math.ceil(1.01)"));
        assertEquals(transform("4"), transform("xtr.math.floor(4.99)"));

        // negatives (these catch common “ceil/floor wrong for negatives” regressions)
        assertEquals(transform("-1"), transform("xtr.math.ceil(-1.2)"));
        assertEquals(transform("-2"), transform("xtr.math.floor(-1.2)"));

        // clamp uses NumberMath compare now; test the boundary behavior
        assertEquals(transform("1.2"), transform("xtr.math.clamp(1.1, 1.2, 2)"));
        assertEquals(transform("2"), transform("xtr.math.clamp(3, 1.2, 2)"));
    }

    @Test
    public void roundPrecisionAndPowIntExponent() {
        // precision rounding should be stable
        assertEquals(transform("2.35"), transform("xtr.math.round(2.345, 'half-up', 2)"));
        assertEquals(transform("2.34"), transform("xtr.math.round(2.345, 'half-down', 2)"));

        // pow now takes Int exponent in the new implementation; keep a sanity check
        assertEquals(transform("1024"), transform("xtr.math.pow(2, 10)"));
    }


    @Test
    public void largeRadixConversionsStayExact() {
        // 9007199254740993 (2^53 + 1) in various bases:
        // bin  = 100000000000000000000000000000000000000000000000000001
        // hex  = 20000000000001
        // oct  = 400000000000000001

        assertEquals(transform("'100000000000000000000000000000000000000000000000000001'"),
                transform("xtr.numbers.toBinary('9007199254740993')"));

        assertEquals(transform("'20000000000001'"),
                transform("xtr.numbers.toHex('9007199254740993')"));

        assertEquals(transform("'400000000000000001'"),
                transform("xtr.numbers.toOctal('9007199254740993')"));

        // round-trip: toHex -> ofHex
        assertEquals(transform("9007199254740993"),
                transform("xtr.numbers.ofHex(xtr.numbers.toHex('9007199254740993'))"));
    }

    @Test
    public void negativesWorkInToAndOf() {
        assertEquals(transform("-15"), transform("xtr.numbers.ofHex('-f')"));
        assertEquals(transform("'-f'"), transform("xtr.numbers.toHex(-15)"));

        assertEquals(transform("'-1111'"), transform("xtr.numbers.toBinary(-15)"));
        assertEquals(transform("15"), transform("xtr.numbers.ofBinary('1111')"));
    }


    @Test
    public void maxMinWorkAbove2Pow53() {
        assertEquals(transform("9007199254740995"),
                transform("xtr.max([9007199254740993, 9007199254740995])"));

        assertEquals(transform("9007199254740993"),
                transform("xtr.min([9007199254740993, 9007199254740995])"));
    }

    @Test
    public void sumByUsesExactNumericFold() {
        // identity sumBy
        assertEquals(transform("9007199254740995"),
                transform("xtr.arrays.sumBy([9007199254740993, 2], function(x) x)"));

        // non-trivial sumBy
        assertEquals(transform("60"),
                transform("xtr.arrays.sumBy([10, 20, 30], function(x) x)"));
    }


    @Test
    public void readWrite_roundTrip_preservesHighPrecisionDecimal34Digits() {
        // 34 significant digits (DECIMAL128 max precision) with a decimal point.
        // If this ever goes through Double, it will truncate to ~17 digits.
        String payload = """
        "{\\"n\\":0.1234567890123456789012345678901234}"
        """;

        // read JSON -> write JSON (as string)
        String out = transform("xtr.write(xtr.read(payload, 'application/json'), 'application/json')", payload);

        // xtr.write returns a JSON string literal, so expected is quoted.
        assertEquals(
                transform(payload),
                out
        );
    }

    @Test
    public void readWrite_roundTrip_preservesBigIntegerBeyondLong() {
        // Beyond Long.MaxValue (9223372036854775807), still valid JSON integer.
        // Any double path will lose exactness (and may format in scientific notation).
        String payload = """
        "{\\"n\\":9223372036854775808}"
        """;

        String out = transform("xtr.write(xtr.read(payload, 'application/json'), 'application/json')", payload);

        assertEquals(
                transform(payload),
                out
        );
    }

    @Test
    public void readWrite_roundTrip_preservesBigDecimalThatIsDoubleFiniteButNotExact() {
        // This is finite as a double, so a “parse as double if possible” visitor will accept it,
        // but it is NOT exactly representable in double.
        // If parsed as double and then printed, you'll typically see a shortened/rounded form.
        String payload = """
        "{\\"n\\":1.234567890123456789}"
        """;

        String out = transform("xtr.write(xtr.read(payload, 'application/json'), 'application/json')", payload);

        assertEquals(
                transform(payload),
                out
        );
    }

    @Test
    public void write_preservesLargeIntLiteralWithoutScientificNotation() {
        // Even without read(), ensure that writing a literal doesn't round or scientific-format it.
        String out = transform("xtr.write({n: 9007199254740993}, 'application/json')");
        assertEquals(transform("'{\"n\":9007199254740993}'"), out);
    }


    @Test
    public void sumAndAvg_areExactAbove2Pow53() {
        // If any double math happens, you can get the wrong result here.
        assertEquals(
                transform("18014398509481988"),
                transform("xtr.math.sum([9007199254740993, 9007199254740995])")
        );

        assertEquals(
                transform("9007199254740994"),
                transform("xtr.math.avg([9007199254740993, 9007199254740995])")
        );
    }

    @Test
    public void overflowPromotesBeyondInt64_doesNotWrap() {
        // Long.MaxValue + 1 should not wrap negative.
        // We don't care what internal numeric type it becomes, only the value.
        assertEquals(
                transform("9223372036854775808"),
                transform("9223372036854775807 + 1")
        );
    }

    @Test
    public void subtractAndMultiplyStayExactWithVeryLargeInts() {
        assertEquals(
                transform("9223372036854775806"),
                transform("9223372036854775807 - 1")
        );

        // 3037000500^2 = 9223372037000250000 (slightly above Long.MaxValue)
        // This is a classic edge case: multiplication must promote.
        assertEquals(
                transform("9223372037000250000"),
                transform("3037000500 * 3037000500")
        );
    }

    @Test
    public void divMaintainsDecimal128PrecisionForNonTerminating() {
        // non-terminating decimal in base10. We mostly want to ensure it doesn't produce "Infinity" or something wild,
        // and that it's stable (not double-ish rounded to ~17 digits).
        // Expect a DECIMAL128-style rounded decimal expansion.
        assertEquals(
                transform("0.3333333333333333333333333333333333"),
                transform("1 / 3")
        );
    }

    @Test
    public void decimalLiteral_stringificationKeepsAllDigits() {
        assertEquals(
                transform("'0.1234567890123456789012345678901234'"),
                transform("std.toString(0.1234567890123456789012345678901234)")
        );
    }
}
