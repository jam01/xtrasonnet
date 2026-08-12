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

public class DurationTest {
    @Test
    public void of() {
        assertEquals(transform("'P20Y3M1DT12H30M45S'"), transform("""
                local parts = {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                };

                xtr.duration.of(parts)"""));
    }

    @Test
    public void toParts() {
        assertEquals(transform("""
                {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                }"""), transform("xtr.duration.toParts('P20Y3M1DT12H30M45S')"));
    }

    @Test
    public void toPartsAndOfRoundTrip_dateAndTime() {
        assertEquals(transform("'P1DT2H3M4S'"),
                transform("xtr.duration.of(xtr.duration.toParts('P1DT2H3M4S'))"));

        // more “period-heavy”
        assertEquals(transform("'P2Y3M4DT5H'"),
                transform("xtr.duration.of(xtr.duration.toParts('P2Y3M4DT5H'))"));
    }

    @Test
    public void toPartsAndOfRoundTrip_dateOnlyAndTimeOnlyStyle() {
        // date-only
        assertEquals(transform("'P3D'"),
                transform("xtr.duration.of(xtr.duration.toParts('P3D'))"));

        // an explicit 0D collapses to the canonical time-only form
        assertEquals(transform("'PT2H'"),
                transform("xtr.duration.of(xtr.duration.toParts('P0DT2H'))"));
    }

    @Test
    public void ofIsCanonical() {
        assertEquals(transform("'PT1H'"), transform("xtr.duration.of({hours: 1})"));
        assertEquals(transform("'P1Y'"), transform("xtr.duration.of({years: 1})"));
        assertEquals(transform("'PT0S'"), transform("xtr.duration.of({})"));
        assertEquals(transform("'PT1M30S'"), transform("xtr.duration.of({seconds: 90})"));
    }

    @Test
    public void ofRejectsUnknownParts() {
        var ex = assertThrows(RuntimeException.class, () -> transform("xtr.duration.of({hour: 1})"));
        assertTrue(ex.getMessage().contains("Unexpected duration part: hour"), ex.getMessage());
    }

    @Test
    public void toPartsTimeOnly() {
        assertEquals(transform("""
                        {
                            years: 0, months: 0, days: 0,
                            hours: 4, minutes: 5, seconds: 6
                        }"""),
                transform("xtr.duration.toParts('PT4H5M6S')"));
    }

    @Test
    public void toPartsDateOnlyIncludesTimeParts() {
        assertEquals(transform("""
                        {
                            years: 0, months: 0, days: 40,
                            hours: 0, minutes: 0, seconds: 0
                        }"""),
                transform("xtr.duration.toParts('P40D')"));
    }

    @Test
    public void toPartsNegative() {
        assertEquals(transform("""
                        {
                            years: 0, months: 0, days: -1,
                            hours: -2, minutes: -30, seconds: 0
                        }"""),
                transform("xtr.duration.toParts('-P1DT2H30M')"));

        assertEquals(transform("""
                        {
                            years: 0, months: 0, days: 0,
                            hours: -4, minutes: 0, seconds: 0
                        }"""),
                transform("xtr.duration.toParts('PT-4H')"));
    }

    @Test
    public void toPartsInvalid() {
        var ex = assertThrows(RuntimeException.class, () -> transform("xtr.duration.toParts('4 hours')"));
        assertTrue(ex.getMessage().contains("Invalid ISO-8601 duration: 4 hours"), ex.getMessage());

        // 'T' with no time components is invalid, as is a bare 'P'
        assertThrows(RuntimeException.class, () -> transform("xtr.duration.toParts('P1DT')"));
        assertThrows(RuntimeException.class, () -> transform("xtr.duration.toParts('P')"));
    }
}
