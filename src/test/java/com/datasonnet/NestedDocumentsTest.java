package com.datasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NestedDocumentsTest {

    @Test
    public void testNestedDocs() {
        String xml = "<root/>";
        String json = "{ \"hello\": \"world!\" }";

        Map<String, Document<String>> nested = new HashMap<>(2);
        nested.put("xml", new DefaultDocument<>(xml, MediaTypes.APPLICATION_XML));
        nested.put("json", new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON));

        Map<String, Document<?>> inputs = Collections.singletonMap("nested", new DefaultDocument<>(nested, MediaTypes.APPLICATION_JAVA));
        String result = new MapperBuilder("nested")
                .withInputNames("nested")
                .build()
                .transform(DefaultDocument.NULL_INSTANCE, inputs, MediaTypes.APPLICATION_JSON)
                .getContent();
        Assert.assertEquals("{\"json\":{\"hello\":\"world!\"},\"xml\":{\"root\":{\"~\":1}}}", result);
    }
}
