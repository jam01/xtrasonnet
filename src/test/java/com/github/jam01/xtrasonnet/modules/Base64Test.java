package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base64Test {

    @Test
    public void decode() {
        assertEquals(transform("'Hello World'"), transform("xtr.base64.decode('SGVsbG8gV29ybGQ=')"));
    }

    @Test
    public void encode() {
        assertEquals(transform("'SGVsbG8gV29ybGQ='"), transform("xtr.base64.encode('Hello World')"));
    }
}
