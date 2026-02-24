package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.spi.JLibrary;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import sjsonnet.Val;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    public static class TestLib extends JLibrary {

        @Override
        public String name() {
            return "testlib";
        }

        @Override
        public Map<String, Val.Func> functions() {
            var res = new HashMap<String, Val.Func>();

            res.put("echo", jbuiltin(new String[]{"param"}, (vals, pos, ev) -> new Val.Str(position(), vals[0].asString() + " world!")));
            return res;
        }
    }
}
