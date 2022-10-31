package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedDocumentsTest {

    @Test
    public void testNestedDocs() {
        String xml = "<root/>";
        String json = "{ \"hello\": \"world!\" }";

        Map<String, Document<String>> nested = new HashMap<>(2);
        nested.put("xml", Document.of(xml, MediaTypes.APPLICATION_XML));
        nested.put("json", Document.of(json, MediaTypes.APPLICATION_JSON));

        Map<String, Document<?>> inputs = Collections.singletonMap("nested", Document.of(nested, MediaTypes.APPLICATION_JAVA));
        String result = Transformer.builder("nested")
                .withInputNames("nested")
                .build()
                .transform(Documents.Null(), inputs, MediaTypes.APPLICATION_JSON)
                .getContent();
        assertEquals("{\"json\":{\"hello\":\"world!\"},\"xml\":{\"root\":{}}}", result);
    }
}
