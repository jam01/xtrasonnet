package io.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.header.Header;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import sjsonnet.Importer;
import sjsonnet.Val;

import java.util.HashMap;
import java.util.Map;

public class LibraryTest {
    public static class TestLib extends Library {

        @Override
        public String namespace() {
            return "testlib";
        }

        @Override
        public Map<String, Val.Func> functions(DataFormatService dataFormats, Header header, Importer importer) {
            var res = new HashMap<String, Val.Func>();

            res.put("echo", builtin(new String[]{"param"}, (vals, pos, ev) -> new Val.Str(dummyPosition(), vals[0].asString() + " world!")));
            return res;
        }
    }

    @Test
    public void testLib() throws JSONException {
        var res = Transformer.builder("""
                {
                    test: testlib.echo('hello'),
                    xtr: xtr.toLowerCase('Hello World!')
                }""").withLibrary(new TestLib()).build().transform(Documents.Null());
        JSONAssert.assertEquals("""
                {
                    "test": "hello world!",
                    "xtr": "hello world!"
                }""", res.getContent(), true);
    }
}
