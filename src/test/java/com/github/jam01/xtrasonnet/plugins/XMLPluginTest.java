package com.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.Transformer;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.Documents;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;

import static com.github.jam01.xtrasonnet.TestUtils.resourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class XMLPluginTest {
    private final String root = """
            <root>value</root>""";
    private final String rootAsJsonSimple = """
            {"root":"value"}""";
    private final String rootAsJsonBasic = """
            {"root":{"_text":"value"}}""";
    private final String escaped = """
            <root attr="if a &amp; b &gt; c then &quot;You're right&quot;">
            if a &amp; b &gt; c then "You're right"
            </root>""";

    private final String escapedAsJson = """
            {"root":{"_attr":{"attr":"if a & b > c then \\"You're right\\""},"_text":"if a & b > c then \\"You're right\\""}}""";

    private final String emptyTags = "<root><empty/><nil/><hasText>value</hasText><emptyText/></root>";
    private final String emptyTagsAsJson = """
            {"root":{"empty":{},"nil":null,"hasText":{"_text":"value"},"emptyText":{"_text":""}}}""";

    @Test
    public void read_comprehensive_simplified() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/reports.xml"), MediaTypes.APPLICATION_XML
                        .withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.SIMPLIFIED_MODE_VALUE())
                        .withParameter(DefaultXMLPlugin.PARAM_XMLNS_AWARE(), "false")));

        JSONAssert.assertEquals(resourceAsString("xml/reports-simplified.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple_simplified() {
        var doc = new Transformer(rootAsJsonSimple)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.SIMPLIFIED_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(root).ignoreWhitespace());
    }

    @Test
    public void write_comprehensive_simplified() {
        var doc = new Transformer(resourceAsString("xml/reports-simplified.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.SIMPLIFIED_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/reports-simplified-nostylesheet.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    public void read_simple_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(root,
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())));

        JSONAssert.assertEquals(rootAsJsonBasic, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_comprehensive_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/reports.xml"),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())));

        JSONAssert.assertEquals(resourceAsString("xml/reports-basic.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_comprehensive_extended() throws JSONException {
        var doc = new Transformer("payload")
                .transform(
                        Document.of(resourceAsString("xml/reports.xml"),
                                MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.EXTENDED_MODE_VALUE())));

        JSONAssert.assertEquals(resourceAsString("xml/reports-extended.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple_basic() {
        var doc = new Transformer(rootAsJsonBasic)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(root).ignoreWhitespace());
    }

    @Test
    public void write_comprehensive_basic() {
        var doc = new Transformer(resourceAsString("xml/reports-basic.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/reports-nostylesheet.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    public void read_mixed_content_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/mixedcontent.xml"),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())));

        JSONAssert.assertEquals(resourceAsString("xml/mixedcontent-basic.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_mixed_content_extended() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/mixedcontent.xml"),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.EXTENDED_MODE_VALUE())));

        JSONAssert.assertEquals(resourceAsString("xml/mixedcontent-extended.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_comprehensive_extended() {
        var doc = new Transformer(resourceAsString("xml/mixedcontent-extended.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.EXTENDED_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/mixedcontent.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    public void read_override_qnames_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/qnames.xml"),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter("xmlns.b1", "http://example.org/ns/one")
                                .withParameter("xmlns.b2", "http://example.org/ns/two")
                                .withParameter("xmlns.b3", "http://example.org/ns/three")));

        JSONAssert.assertEquals(resourceAsString("xml/qnames-overriden.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_override_qnames_basic() {
        var doc = new Transformer(resourceAsString("xml/qnames-overriden.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter("xmlns.a1", "http://example.org/ns/one")
                                .withParameter("xmlns.a2", "http://example.org/ns/three")
                                .withParameter("xmlns.a3", "http://example.org/ns/two"));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/qnames.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    public void read_comprehensive_extended_custom_convention() throws JSONException {
        var doc = new Transformer("payload")
                .transform(
                        Document.of(resourceAsString("xml/reports.xml"),
                                MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.EXTENDED_MODE_VALUE())
                                        .withParameter(DefaultXMLPlugin.PARAM_TEXT_KEY(), "_text_")
                                        .withParameter(DefaultXMLPlugin.PARAM_ATTRIBUTE_KEY(), "_attr_")
                                        .withParameter(DefaultXMLPlugin.PARAM_CDATA_KEY(), "_cdata_")
                                        .withParameter(DefaultXMLPlugin.PARAM_XMLNS_KEY(), "_xmlns_")
                                        .withParameter(DefaultXMLPlugin.PARAM_ORDER_KEY(), "_pos_")));

        JSONAssert.assertEquals(resourceAsString("xml/reports-custom-convention.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_comprehensive_extended_custom_convention() {
        var doc = new Transformer(resourceAsString("xml/reports-custom-convention.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.EXTENDED_MODE_VALUE())
                                .withParameter(DefaultXMLPlugin.PARAM_TEXT_KEY(), "_text_")
                                .withParameter(DefaultXMLPlugin.PARAM_ATTRIBUTE_KEY(), "_attr_")
                                .withParameter(DefaultXMLPlugin.PARAM_CDATA_KEY(), "_cdata_")
                                .withParameter(DefaultXMLPlugin.PARAM_XMLNS_KEY(), "_xmlns_")
                                .withParameter(DefaultXMLPlugin.PARAM_ORDER_KEY(), "_pos_"));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/reports-nostylesheet.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }

    @Test
    public void write_simple_basic_custom_xmlversion() {
        var doc = new Transformer(rootAsJsonBasic)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter(DefaultXMLPlugin.PARAM_XML_VERSION(), "1.2"));

        assertTrue(doc.getContent().startsWith("<?xml version='1.2' encoding='UTF-8'?>"));
    }

    @Test
    public void write_simple_basic_omitxmldeclaration() {
        var doc = new Transformer(rootAsJsonBasic)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter(DefaultXMLPlugin.PARAM_EXCLUDE(), DefaultXMLPlugin.EXCLUDE_XML_DECLARATION_VALUE()));

        assertEquals(root, doc.getContent());
    }

    @Test
    public void read_escaped_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(escaped,
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())));

        JSONAssert.assertEquals(escapedAsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_escaped_basic() {
        var doc = new Transformer(escapedAsJson)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter(DefaultXMLPlugin.PARAM_EXCLUDE(), DefaultXMLPlugin.EXCLUDE_XML_DECLARATION_VALUE()));

        assertEquals(escaped.replaceAll("\\n", ""), doc.getContent());
    }

    @Test
    public void write_emptytags_basic() {
        var doc = new Transformer(emptyTagsAsJson)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter(DefaultXMLPlugin.PARAM_EMPTY_TAGS(), "null string object")
                                .withParameter(DefaultXMLPlugin.PARAM_EXCLUDE(), DefaultXMLPlugin.EXCLUDE_XML_DECLARATION_VALUE()));

        assertEquals(emptyTags, doc.getContent());
    }

    @Test
    public void write_null_simple_fails() {
        try {
            new Transformer("null").transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_XML);
            fail("Should not succeed");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Input for XML writer must be an Object"), "Failed with wrong message: " + e.getMessage());
        }
    }

    @Test
    public void read_override_qnames_should_bump_basic() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(resourceAsString("xml/qnames.xml"),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE())
                                .withParameter("xmlns.a2", "http://example.org/ns/two")));

        JSONAssert.assertEquals(resourceAsString("xml/qnames-overriden-bumped.json"), doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_arbitrary() {
        var doc = new Transformer(resourceAsString("xml/reports-arbitrary.json"))
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_XML.withParameter(DefaultXMLPlugin.PARAM_MODE(), DefaultXMLPlugin.BADGER_MODE_VALUE()));

        assertThat(doc.getContent(), CompareMatcher.isSimilarTo(resourceAsString("xml/reports-arbitrary.xml"))
                .ignoreWhitespace().normalizeWhitespace().throwComparisonFailure()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
    }
}
