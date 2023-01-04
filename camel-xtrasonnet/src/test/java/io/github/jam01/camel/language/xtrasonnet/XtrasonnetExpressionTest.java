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

public class XtrasonnetExpressionTest extends CamelTestSupport {
    @Test
    public void simple_expression() {
        var exp = new XtrasonnetExpression("{ foo: 'bar' }");
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Documents.Null()), Document.class);
        assertEquals(Map.of("foo", "bar"), res.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(res.getMediaType()));
    }

    @Test
    public void simple_expression_object() {
        var exp = new XtrasonnetExpression("{ foo: 'bar' }");
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Documents.Null()), Map.class);
        assertEquals(Map.of("foo", "bar"), res);
    }

    @Test
    public void simple_expression_unknown_body() {
        var exp = new XtrasonnetExpression("payload");
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Map.of("foo", "bar")), Map.class);
        assertEquals(Map.of("foo", "bar"), res);
    }

    @Test
    public void simple_expression_java_body() {
        var exp = new XtrasonnetExpression("payload");
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Document.of(Map.of("foo", "bar"), MediaTypes.APPLICATION_JAVA)), Map.class);
        assertEquals(Map.of("foo", "bar"), res);
    }

    @Test
    public void simple_predicate() {
        var exp = new XtrasonnetExpression("2 + 2 == 4");
        exp.init(context());

        var res = exp.matches(createExchangeWithBody(Documents.Null()));
        assertTrue(res);
    }

    @Test
    public void override_expression() {
        var exp = new XtrasonnetExpression("{ root: payload }");
        exp.init(context());

        var ex = createExchangeWithBody("{\"foo\": \"bar\"}");
        ex.setProperty(XtrasonnetConstants.BODY_MEDIATYPE, "application/json");
        ex.setProperty(XtrasonnetConstants.OUTPUT_MEDIATYPE, "application/xml");
        ex.setProperty(XtrasonnetConstants.RESULT_TYPE, "java.lang.String");

        var res = exp.evaluate(ex, Object.class);
        assertEquals("<?xml version='1.0' encoding='UTF-8'?><root><foo>bar</foo></root>", res);
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        var props = new Properties(1);
        props.put("myProp", "val1");

        return props;
    }

    @Test
    public void cml_lib() {
        var exp = new XtrasonnetExpression("""
                {
                    prop: cml.properties('myProp'),
                    head: cml.header('myHeader'),
                    ex_prop: cml.exchangeProperty('myExProp')
                }
                """);
        exp.init(context());

        var ex = createExchangeWithBody(Documents.Null());
        ex.getMessage().setHeader("myHeader", "val2");
        ex.setProperty("myExProp", "val3");

        var res = exp.evaluate(ex, Map.class);
        assertEquals(Map.of("prop", "val1", "head", "val2", "ex_prop", "val3"), res);
    }

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
