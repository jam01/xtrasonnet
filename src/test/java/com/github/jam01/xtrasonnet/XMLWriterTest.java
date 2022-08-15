package com.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;

import static com.github.jam01.xtrasonnet.TestUtils.resourceAsString;
import static com.github.jam01.xtrasonnet.TestUtils.stacktraceFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

public class XMLWriterTest {

    @Test
    void testOverrideNamespaces() throws Exception {
        String json = "{\"b:a\":{\"@xmlns\":{\"b\":\"http://example.com/1\",\"b1\":\"http://example.com/2\"},\"b1:b\":{}}}";
        String datasonnet = resourceAsString("xml/xmlOverrideNamespaces.ds");

        Transformer transformer = new Transformer(datasonnet);


        String mapped = transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        // original mapping is gone
        assertThat(mapped, not(containsString("b:a")));
        assertThat(mapped, not(containsString("b1:b")));

        // elements are in new namespaces
        assertThat(mapped, containsString("c:a"));
        assertThat(mapped, containsString("<b"));

        // namespaces defined
        assertThat(mapped, containsString("xmlns:c=\"http://example.com/1\""));
        assertThat(mapped, containsString("xmlns=\"http://example.com/2\""));
    }

    @Test
    void testNamespaceBump() throws Exception {
        String json = "{\"b:a\":{\"@xmlns\":{\"b\":\"http://example.com/1\",\"b1\":\"http://example.com/2\"},\"b1:b\":{}}}";

        String datasonnet = resourceAsString("xml/xmlNamespaceBump.ds");

        Transformer transformer = new Transformer(datasonnet);


        String mapped = transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();


        // original mapping is gone
        assertThat(mapped, not(containsString("b:a")));
        assertThat(mapped, not(containsString("b1:b")));

        // elements are in new namespaces
        assertThat(mapped, containsString("b1:a"));

        // namespaces defined
        assertThat(mapped, containsString("xmlns:b1=\"http://example.com/1\""));
        assertThat(mapped, containsString("http://example.com/2"));
    }

    @Test
    void testXMLWriterExt() throws Exception {
        String jsonData = resourceAsString("xml/readXMLExtTest.json");
        String datasonnet = resourceAsString("xml/writeXMLExtTest.ds");
        String expectedXml = resourceAsString("xml/readXMLExtTest.xml");

        Transformer transformer = new Transformer(datasonnet);

        Document<String> mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML);
        assertEquals(MediaTypes.APPLICATION_XML, mappedXml.getMediaType());
        assertThat(mappedXml.getContent(), CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testNoDoubleWrite() throws Exception {
        String jsonData = resourceAsString("xml/writeXMLExtDouble.json");
        String datasonnet = resourceAsString("xml/writeXMLExtTest.ds");
        String expectedXml = resourceAsString("xml/readXMLExtTest.xml");

        Transformer transformer = new Transformer(datasonnet);

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testXMLEscaped() throws Exception {
        String datasonnet =
                "{ root: {" +
                "  '@name': 'QueryText'," +
                "   '$1': 'dDocTitle <substring> foo <and> dDocCreatedDate >= bar'" +
                " } " +
                "}";
        String expectedXml = resourceAsString("xml/writeXMLEscapedTest.xml");

        Transformer transformer = new Transformer(datasonnet);
        String mappedXml = transformer.transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_XML.withParameter("badgerfish", "extended"), String.class).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testNonAscii() throws Exception {
        String jsonData = resourceAsString("xml/writerXmlNonAscii.json");
        String expectedXml = resourceAsString("xml/xmlNonAscii.xml");
        String datasonnet = resourceAsString("xml/xmlNonAscii.ds");

        Transformer transformer = new Transformer(datasonnet);

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML.withParameter("badgerfish", "simple")).getContent();

        assertEquals(expectedXml, mappedXml);

        //XMLUnit does not support non-ascii
        //assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testCDATA() throws Exception {
        String jsonData = resourceAsString("xml/xmlCDATA.json");
        String expectedXml = resourceAsString("xml/xmlCDATA.xml");
        String datasonnet = resourceAsString("xml/xmlNonAscii.ds");//Reuse existing one to avoid duplication

        Transformer transformer = new Transformer(datasonnet);

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testXMLMixedContent() throws Exception {
        String jsonData = resourceAsString("xml/xmlMixedContent.json");
        String expectedXml = resourceAsString("xml/xmlMixedContent.xml");

        Transformer transformer = new Transformer("local params = {\n" +
                "    \"XmlVersion\" : \"1.1\",\n" +
                "    \"badgerfish\" : \"extended\"\n" +
                "};xtr.write(payload, \"application/xml\", params)");


        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testEmptyElements() throws Exception {
        String jsonData = resourceAsString("xml/xmlEmptyElements.json");
        String expectedXml = resourceAsString("xml/xmlEmptyElementsNull.xml");
        String datasonnet = resourceAsString("xml/xmlEmptyElementsNull.ds");

        Transformer transformer = new Transformer(datasonnet);

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());

        expectedXml = resourceAsString("xml/xmlEmptyElementsNoNull.xml");
        datasonnet = resourceAsString("xml/xmlEmptyElementsNoNull.ds");

        transformer = new Transformer(datasonnet);


        mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testOmitXml() throws Exception {
        String jsonData = resourceAsString("xml/xmlEmptyElements.json");

        Transformer transformer = new Transformer("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml;OmitXmlDeclaration=true\n" +
                "*/\n" +
                "payload");

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertFalse(mappedXml.contains("<?xml"));

        transformer = new Transformer("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml;OmitXmlDeclaration=false\n" +
                "*/\n" +
                "payload");

        mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertTrue(mappedXml.startsWith("<?xml"));
    }

    @Test
    void testXMLRoot() throws Exception {
        String jsonData = resourceAsString("xml/xmlRoot.json");
        Transformer transformer = new Transformer("xtr.write(payload, \"application/xml\")");

        try {
            String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
            fail("Must fail to transform");
        } catch (IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Object must have only one root element"), "Stacktrace does not indicate the issue");
            assertTrue(stacktrace.contains("((main):1:10)"), "Stacktrace does not indicate the issue");
        }

        transformer = new Transformer("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml; RootElement=TestRoot\n" +
                "*/\n" +
                "payload");

        try {
            String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
        } catch (IllegalArgumentException e) {
            fail("This transformation should not fail");
        }
    }

    @Test
    void testNestedNamespaces() throws Exception {
        String jsonData = resourceAsString("xml/xmlNestedNamespaces.json");
        String expectedXml = resourceAsString("xml/xmlNestedNamespaces.xml");

        Transformer transformer = new Transformer("payload");

        String mappedXml = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML.withParameter("badgerfish", "extended")).getContent();
        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    public void testNull() {
        Transformer transformer = new Transformer("null");

        try {
            transformer.transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_XML);
            fail("Should not succeed");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Input for XML writer must be an Object"), "Failed with wrong message: " + e.getMessage());
        }
    }
}
