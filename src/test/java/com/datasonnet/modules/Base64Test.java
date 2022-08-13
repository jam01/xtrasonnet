package com.datasonnet.modules;

import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base64Test {

    @Test
    public void decode() {
        assertEquals(transform("'Hello World'"), transform("tro.base64.decode('SGVsbG8gV29ybGQ=')"));
    }

    @Test
    public void encode() {
        assertEquals(transform("'SGVsbG8gV29ybGQ='"), transform("tro.base64.encode('Hello World')"));
    }
}
