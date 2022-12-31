package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DurationTest {
    @Test
    public void of() {
        Assertions.assertEquals(TestUtils.transform("'P20Y3M1DT12H30M45S'"), TestUtils.transform("""
                local parts = {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                };

                xtr.duration.of(parts)"""));
    }

    @Test
    public void toParts() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                    years: 20, months: 3, days: 1,
                    hours: 12, minutes: 30, seconds: 45
                }"""), TestUtils.transform("xtr.duration.toParts('P20Y3M1DT12H30M45S')"));
    }
}
