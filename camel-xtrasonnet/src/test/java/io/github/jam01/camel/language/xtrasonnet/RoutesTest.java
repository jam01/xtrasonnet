package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.camel.model.language.XtrasonnetExpression;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.github.jam01.camel.builder.XtrasonnetBuilder.xtrasonnet;

public class RoutesTest extends CamelSpringTestSupport {
    MockEndpoint mock;
    MockEndpoint xmlMock;

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        mock = getMockEndpoint("mock:result");
        xmlMock = getMockEndpoint("mock:xml-result");
        super.beforeTestExecution(context);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("io/github/jam01/camel/language/xtrasonnet/camel-context.xml");
    }
    @Test
    public void test_filter() throws InterruptedException {
        var under = Map.of("lineItems", List.of(0, 50));
        var over = Map.of("lineItems", List.of(0, 50, 150));

        // java
        mock.setExpectedMessageCount(1);
        mock.expectedBodiesReceived(over);

        template.sendBody("direct:filter", under);
        template.sendBody("direct:filter", over);

        mock.assertIsSatisfied();

        // xml
        xmlMock.setExpectedMessageCount(1);
        xmlMock.expectedBodiesReceived(over);

        template.sendBody("direct:xml-filter", under);
        template.sendBody("direct:xml-filter", over);

        xmlMock.assertIsSatisfied();
    }

    @Test
    public void test_transform() throws InterruptedException {
        var xml = """
                <lineItems>
                    <lineItem>0</lineItem>
                    <lineItem>50</lineItem>
                    <lineItem>150</lineItem>
                </lineItems>
                """;

        var json = """
                {"lineItem":[{"_text":"0"},{"_text":"50"},{"_text":"150"}]}""";

        // java
        mock.setExpectedMessageCount(1);
        mock.expectedBodiesReceived(json);

        template.sendBody("direct:transform", xml);

        mock.assertIsSatisfied();

        // xml
        xmlMock.setExpectedMessageCount(1);
        xmlMock.expectedBodiesReceived(json);

        template.sendBody("direct:xml-transform", xml);

        xmlMock.assertIsSatisfied();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        var props = new Properties(1);
        props.put("toGreet", "world");

        return props;
    }

    @Test
    public void test_cml() throws InterruptedException {
        // java
        mock.setExpectedMessageCount(1);
        mock.expectedBodiesReceived("hello world");

        template.sendBody("direct:cml", null);

        mock.assertIsSatisfied();

        // xml
        xmlMock.setExpectedMessageCount(1);
        xmlMock.expectedBodiesReceived("hello world");

        template.sendBody("direct:xml-cml", null);

        mock.assertIsSatisfied();
    }

    @Test
    public void test_resource() throws InterruptedException {
        // java
        mock.setExpectedMessageCount(1);
        mock.expectedHeaderReceived("myHeader", "hello world!");

        template.sendBody("direct:resource", null);

        mock.assertIsSatisfied();

        // xml
        xmlMock.setExpectedMessageCount(1);
        xmlMock.expectedHeaderReceived("myHeader", "hello world!");

        template.sendBody("direct:xml-resource", null);

        xmlMock.assertIsSatisfied();
    }

    @Test
    public void test_builders() throws InterruptedException {
        // java
        mock.setExpectedMessageCount(1);
        mock.expectedHeaderReceived("valueBuilder", "hello world!");
        mock.expectedHeaderReceived("expressionClause", "hello world!");
        mock.expectedHeaderReceived("langDsl", "hello world!");

        template.sendBody("direct:builders", null);

        mock.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:filter")
                        .filter(xtrasonnet("xtr.arrays.any(payload.lineItems, function(item) item > 100)"))
                        .to("mock:result");

                from("direct:transform")
                        .transform(xtrasonnet("payload.lineItems", String.class)
                                    .bodyMediaType(MediaTypes.APPLICATION_XML)
                                    .outputMediaType(MediaTypes.APPLICATION_JSON))
                        .to("mock:result");

                from("direct:cml")
                        .setBody(xtrasonnet("'hello ' + cml.properties('toGreet')", String.class))
                        .to("mock:result");

                from("direct:resource")
                        .setHeader("myHeader", xtrasonnet("resource:classpath:myXtrasonnet.xtr", String.class))
                        .to("mock:result");

                from("direct:builders")
                        .setHeader("valueBuilder", xtrasonnet("'hello world!'", String.class))
                        .setHeader("expressionClause").expression(xtrasonnet("'hello world!'", String.class))
                        .setHeader("langDsl", new XtrasonnetExpression.Builder().expression("'hello world!'").resultType(String.class).end())
                        .to("mock:result");
            }
        };
    }
}
