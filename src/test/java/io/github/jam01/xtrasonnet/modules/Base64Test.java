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

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base64Test {

    @Test
    public void decode() {
        Assertions.assertEquals(TestUtils.transform("'Hello World'"), TestUtils.transform("xtr.base64.decode('SGVsbG8gV29ybGQ=')"));
    }

    @Test
    public void encode() {
        Assertions.assertEquals(TestUtils.transform("'SGVsbG8gV29ybGQ='"), TestUtils.transform("xtr.base64.encode('Hello World')"));
    }
}
