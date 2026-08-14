package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;
import sjsonnet.Parser;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FluentParser.exprSuffix2 is a verbatim copy of sjsonnet's Parser.exprSuffix2 that differs only by a
 * leading {@code Pass ~}. Being an {@code override}, an upstream grammar change silently does not apply
 * here -- no compile error, no failing test.
 * <p>
 * These two tests are the guard. The first fails when the sjsonnet fork is bumped, so whoever bumps it
 * re-reads Parser.exprSuffix2 against our copy and then moves the version here. The second pins what every
 * branch of the copied grammar currently parses to, so a hand-edit of the copy that breaks one of them
 * fails loudly.
 */
public class FluentParserDriftTest {
    /**
     * Bumping the sjsonnet dependency fails this test on purpose. To clear it: diff
     * sjsonnet.Parser#exprSuffix2 in the new version against FluentParser#exprSuffix2, carry over any
     * change, then update this constant and the version named in FluentParser's scaladoc.
     */
    private static final String CHECKED_AGAINST = "0.7.3-05";

    @Test
    public void copyWasCheckedAgainstTheSjsonnetOnTheClasspath() throws Exception {
        assertEquals(CHECKED_AGAINST, sjsonnetVersion(),
                "the sjsonnet fork moved: re-check FluentParser.exprSuffix2 against Parser.exprSuffix2, "
                        + "then update CHECKED_AGAINST and FluentParser's scaladoc");
    }

    @Test
    public void everyCopiedSuffixBranchStillParses() {
        // '.' select, and '?.' safe select
        assertEquals(transform("1"), transform("{ a: { b: 1 } }.a.b"));
        assertEquals(transform("null"), transform("{ a: null }.a?.b"));
        // '[' lookup and slice
        assertEquals(transform("2"), transform("[1, 2, 3][1]"));
        assertEquals(transform("[2, 3]"), transform("[1, 2, 3][1:]"));
        assertEquals(transform("[1, 3]"), transform("[1, 2, 3][::2]"));
        // '(' apply, with a named argument
        assertEquals(transform("3"), transform("(function(a, b) a + b)(1, 2)"));
        assertEquals(transform("3"), transform("(function(a, b) a + b)(1, b=2)"));
        // '{' object extension
        assertEquals(transform("{ a: 1, b: 2 }"), transform("{ a: 1 } { b: 2 }"));
        // and the suffix chaining that expr1 folds over
        assertEquals(transform("2"), transform("{ a: [{ b: 2 }] }.a[0].b"));
    }

    private static String sjsonnetVersion() throws Exception {
        var source = Parser.class.getProtectionDomain().getCodeSource();
        assertNotNull(source, "sjsonnet must be loaded from a jar for this check to mean anything");

        var manifest = new URL("jar:" + source.getLocation() + "!/META-INF/MANIFEST.MF");
        try (InputStream in = manifest.openStream()) {
            return new Manifest(in).getMainAttributes().getValue("Implementation-Version");
        }
    }
}
