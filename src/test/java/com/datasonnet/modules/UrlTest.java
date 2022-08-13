package com.datasonnet.modules;

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
