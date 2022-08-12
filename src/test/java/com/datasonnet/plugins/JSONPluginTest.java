package com.datasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JSONPluginTest {

    // TODO: 8/11/22 add other JSON elements to this
    private final String foobar = """
                { "foo": "bar" }""";

    @Test
    public void read_simple() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(foobar, MediaTypes.APPLICATION_JSON));

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_null() throws JSONException {
        var doc = new Mapper("payload")
                .transform(DefaultDocument.NULL_INSTANCE);

        assertEquals("null", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() throws JSONException {
        var doc = new Mapper("{ foo: 'bar' }")
                .transform(DefaultDocument.NULL_INSTANCE);

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_null() {
        var doc = new Mapper("null")
                .transform(DefaultDocument.NULL_INSTANCE);

        assertEquals("null", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_indent() {
        var doc = new Mapper("{ foo: 'bar' }")
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JSON.withParameter(DefaultJSONFormatPlugin.DS_PARAM_INDENT, "true"));
        assertEquals("""
                {
                    "foo": "bar"
                }""", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }
}
