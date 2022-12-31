package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

public class JSONPluginTest {

    // TODO: 8/11/22 add other JSON elements to this
    private final String foobar = """
                { "foo": "bar" }""";

    @Test
    public void read_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(foobar, MediaTypes.APPLICATION_JSON));

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_null() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Documents.Null());

        Assertions.assertEquals("null", doc.getContent());
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() throws JSONException {
        var doc = new Transformer("{ foo: 'bar' }")
                .transform(Documents.Null());

        JSONAssert.assertEquals(foobar, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_null() {
        var doc = new Transformer("null")
                .transform(Documents.Null());

        Assertions.assertEquals("null", doc.getContent());
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_indent() {
        var doc = new Transformer("{ foo: 'bar' }")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_JSON.withParameter(DefaultJSONPlugin.PARAM_FORMAT, "true"));
        Assertions.assertEquals("""
                {
                    "foo": "bar"
                }""", doc.getContent());
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }
}
