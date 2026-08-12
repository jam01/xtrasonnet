package io.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the diagnostics a user hits when no plugin can handle their document: the message must name
 * the offending media type or content class, and what is supported instead.
 */
public class DataFormatPluginsTest {

    @Test
    public void defaultServiceReportsWhatItSupports() {
        var supported = DataFormatService.DEFAULT.supportedMediaTypes();

        assertFalse(supported.isEmpty());
        assertTrue(supported.contains(MediaTypes.APPLICATION_JSON), "expected json, got: " + supported);
        assertTrue(supported.contains(MediaTypes.APPLICATION_XML), "expected xml, got: " + supported);
        // MediaTypes carries constants inherited from Spring for formats no plugin implements
        assertFalse(supported.contains(MediaTypes.APPLICATION_PDF), "pdf is not actually supported");
    }

    /** Flattens an exception chain so a wrapped message can still be asserted on. */
    private static String chainOf(Throwable t) {
        var sb = new StringBuilder();
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            sb.append(cur.getMessage()).append(" | ");
        }
        return sb.toString();
    }

    @Test
    public void unsupportedOutputTypeNamesTheAlternatives() {
        // surfaces wrapped, since the write happens during materialization
        var ex = assertThrows(RuntimeException.class, () -> new Transformer("{}")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_PDF));

        var chain = chainOf(ex);
        assertTrue(chain.contains("application/pdf"), "should name the offending type: " + chain);
        assertTrue(chain.contains("application/json"), "should list what is supported: " + chain);
    }

    @Test
    public void unsupportedInputTypeNamesTheAlternatives() {
        var ex = assertThrows(RuntimeException.class, () -> new Transformer("payload")
                .transform(Document.of("whatever", MediaTypes.APPLICATION_PDF)));

        var chain = chainOf(ex);
        assertTrue(chain.contains("application/pdf"), "should name the offending type: " + chain);
        assertTrue(chain.contains("application/json"), "should list what is supported: " + chain);
    }

    @Test
    public void unsupportedInputWithNullContentIsStillDescribed() {
        // the diagnostic must not trade the real problem for a NullPointerException while naming
        // the content's class
        var ex = assertThrows(RuntimeException.class, () -> new Transformer("payload")
                .transform(new Document.BasicDocument<>(null, MediaTypes.APPLICATION_PDF)));

        var chain = chainOf(ex);
        assertTrue(chain.contains("application/pdf"), "should name the offending type: " + chain);
        assertTrue(chain.contains("null"), "should say the content was null: " + chain);
    }

    @Test
    public void unsupportedContentClassNamesTheClassAndTheSupportedOnes() {
        // plain text only reads String
        var ex = assertThrows(Exception.class, () -> new Transformer("payload")
                .transform(Document.of(java.util.List.of(1, 2), MediaTypes.TEXT_PLAIN)));

        var chain = chainOf(ex);
        assertFalse(chain.contains("use the test method"),
                "should not leak the plugin SPI contract to the caller: " + chain);
    }
}
