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

public class UrlTest {

    @Test
    public void decode() {
        assertEquals(transform("'Hello World'"), transform("tro.url.decode('Hello+World')"));
    }

    @Test
    public void encode() {
        assertEquals(transform("'Hello+World'"), transform("tro.url.encode('Hello World')"));
    }
}
