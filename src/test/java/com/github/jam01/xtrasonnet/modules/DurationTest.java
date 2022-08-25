package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
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
}
