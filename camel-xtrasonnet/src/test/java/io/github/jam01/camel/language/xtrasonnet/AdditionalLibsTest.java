package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.header.Header;
import io.github.jam01.xtrasonnet.spi.Library;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import sjsonnet.Importer;
import sjsonnet.Val;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdditionalLibsTest extends CamelTestSupport {
    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        registry.bind("testlib", new TestLib());
    }

    @Test
    public void cust_lib() {
        var exp = new XtrasonnetExpression("testlib.echo('hello')");
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Documents.Null()), String.class);
        assertEquals("hello world!", res);
    }

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
}
