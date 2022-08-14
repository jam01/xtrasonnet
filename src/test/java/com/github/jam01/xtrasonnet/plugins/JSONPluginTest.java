package com.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.Mapper;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaTypes;
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
                .transform(new Document.BasicDocument<>(foobar, MediaTypes.APPLICATION_JSON));

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_null() throws JSONException {
        var doc = new Mapper("payload")
                .transform(Document.BasicDocument.NULL_INSTANCE);

        assertEquals("null", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() throws JSONException {
        var doc = new Mapper("{ foo: 'bar' }")
                .transform(Document.BasicDocument.NULL_INSTANCE);

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_null() {
        var doc = new Mapper("null")
                .transform(Document.BasicDocument.NULL_INSTANCE);

        assertEquals("null", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_indent() {
        var doc = new Mapper("{ foo: 'bar' }")
                .transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JSON.withParameter(DefaultJSONPlugin.PARAM_FORMAT, "true"));
        assertEquals("""
                {
                    "foo": "bar"
                }""", doc.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }
}
