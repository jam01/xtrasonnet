package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;
import sjsonnet.Settings;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * preserveOrder is a tri-state: explicit wins, then the script's header, then the default. These pin
 * that precedence, and that configuring an unrelated option does not disturb it.
 */
public class TransformerSettingsTest {
    // out of alphabetical order, so the two orderings are distinguishable
    private static final String OUT_OF_ORDER = "{ b: 1, a: 2 }";
    private static final String PRESERVED = "{\"b\":1,\"a\":2}";
    private static final String SORTED = "{\"a\":2,\"b\":1}";

    @Test
    public void preserveOrder_defaultsToWhatTheHeaderSays() {
        // a header with no preserveOrder directive means true, as does no header at all
        assertEquals(PRESERVED, new Transformer(OUT_OF_ORDER).transform(Documents.Null()).getContent());
        assertEquals(SORTED, new Transformer("""
                /** xtrasonnet
                preserveOrder=false
                */
                """ + OUT_OF_ORDER).transform(Documents.Null()).getContent());
    }

    @Test
    public void unrelatedSettings_doNotOverrideTheHeader() {
        // an unrelated option must not reach through to the header's preserveOrder
        var res = Transformer.builder(OUT_OF_ORDER)
                .withDefaultOutput(MediaTypes.APPLICATION_JSON)
                .build()
                .transform(Documents.Null());

        assertEquals(PRESERVED, res.getContent());
    }

    @Test
    public void explicitPreserveOrder_overridesTheHeader() {
        var script = """
                /** xtrasonnet
                preserveOrder=true
                */
                """ + OUT_OF_ORDER;

        assertEquals(SORTED, Transformer.builder(script)
                .withPreserveOrder(false)
                .build().transform(Documents.Null()).getContent());
        assertEquals(PRESERVED, Transformer.builder(script)
                .withPreserveOrder(true)
                .build().transform(Documents.Null()).getContent());
    }

    @Test
    public void defaultOutput_appliesWhenNothingElseNamesOne() {
        var res = Transformer.builder("{ root: 'x' }")
                .withDefaultOutput(MediaTypes.APPLICATION_XML)
                .build()
                .transform(Documents.Null(), Map.of(), MediaTypes.ANY);

        assertEquals("<?xml version='1.0' encoding='UTF-8'?><root>x</root>", res.getContent());
    }

    @Test
    public void defaultInput_appliesWhenNothingElseNamesOne() {
        var res = Transformer.builder("payload.root")
                .withDefaultInput(MediaTypes.APPLICATION_XML)
                .build()
                .transform(Document.of("<root>x</root>", MediaTypes.UNKNOWN), Map.of(),
                        MediaTypes.APPLICATION_JSON);

        assertEquals("{\"_text\":\"x\"}", res.getContent());
    }

    @Test
    public void settingsAndConvenienceKnobs_compose() {
        // both write to one builder, so neither silently discards the other
        var settings = TransformerSettings.builder()
                .preserveOrder(false)
                .defaultOutput(MediaTypes.APPLICATION_JSON)
                .build();

        assertEquals(PRESERVED, Transformer.builder(OUT_OF_ORDER)
                .withSettings(settings)
                .withPreserveOrder(true) // last call wins for this option, the rest survive
                .build().transform(Documents.Null()).getContent());
    }

    @Test
    public void sjsonnetSettings_isTheEscapeHatchAndIsTakenWhole() {
        // handing over a complete Settings is read as meaning all of it, preserveOrder included
        var settings = TransformerSettings.builder()
                .sjsonnetSettings(new Settings(false, false, false, 1000, false, 1000, 128, 500, false, false, 0))
                .build();

        assertEquals(Boolean.FALSE, settings.preserveOrder());
        assertEquals(SORTED, Transformer.builder(OUT_OF_ORDER)
                .withSettings(settings)
                .build().transform(Documents.Null()).getContent());
    }

    @Test
    public void defaults_leavePreserveOrderUnset() {
        assertNull(TransformerSettings.DEFAULT.preserveOrder());
        assertEquals(MediaTypes.APPLICATION_JSON, TransformerSettings.DEFAULT.defInputMediaType());
        assertEquals(MediaTypes.APPLICATION_JSON, TransformerSettings.DEFAULT.defOutputMediaType());
    }

    @Test
    public void builder_rejectsNonsense() {
        assertThrows(IllegalArgumentException.class,
                () -> TransformerSettings.builder().maxParserRecursionDepth(0));
        assertThrows(NullPointerException.class,
                () -> TransformerSettings.builder().defaultOutput(null));
        assertThrows(NullPointerException.class,
                () -> TransformerSettings.builder().sjsonnetSettings(null));
    }

    @Test
    public void toBuilder_derivesAVariant() {
        var base = TransformerSettings.builder().defaultOutput(MediaTypes.APPLICATION_XML).build();
        var derived = base.toBuilder().preserveOrder(true).build();

        assertEquals(MediaTypes.APPLICATION_XML, derived.defOutputMediaType());
        assertEquals(Boolean.TRUE, derived.preserveOrder());
        assertNull(base.preserveOrder(), "the original must be untouched");
    }

    @Test
    public void withSettingsReplacesEveryOptionSetBeforeIt() {
        // pinned in both orders, because the asymmetry is a contract rather than an accident
        var settingsLast = Transformer.builder("{ root: 'x' }")
                .withDefaultOutput(MediaTypes.APPLICATION_XML)
                .withSettings(TransformerSettings.builder().preserveOrder(true).build())
                .build()
                .transform(Document.of("{}", MediaTypes.APPLICATION_JSON));
        assertEquals("{\"root\":\"x\"}", settingsLast.getContent(),
                "withSettings replaces the earlier withDefaultOutput");

        var settingsFirst = Transformer.builder("{ root: 'x' }")
                .withSettings(TransformerSettings.builder().preserveOrder(true).build())
                .withDefaultOutput(MediaTypes.APPLICATION_XML)
                .build()
                .transform(Document.of("{}", MediaTypes.APPLICATION_JSON));
        assertTrue(settingsFirst.getContent().toString().contains("<root>x</root>"),
                "a convenience call after withSettings layers onto it: " + settingsFirst.getContent());
    }
}
