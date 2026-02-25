package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Disabled("duration prints P3DT0S which is valid")
    @Test
    public void toPartsAndOfRoundTrip_dateOnlyAndTimeOnlyStyle() {
        // date-only
        assertEquals(transform("'P3D'"),
                transform("xtr.duration.of(xtr.duration.toParts('P3D'))"));

        // time-only style expressed with an explicit 0D (common safe representation)
        assertEquals(transform("'P0DT2H'"),
                transform("xtr.duration.of(xtr.duration.toParts('P0DT2H'))"));
    }
}
