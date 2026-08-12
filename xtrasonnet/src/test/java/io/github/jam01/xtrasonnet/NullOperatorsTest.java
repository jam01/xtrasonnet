package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The null-safe select and null-coalescing operators come from the sjsonnet fork rather than from
 * this repository, and `??` had no test at all despite being documented. Without these, a fork bump
 * could silently drop either operator.
 */
public class NullOperatorsTest {

    private static final String MY_OBJ = """
            local myObj = {
                keyA: { first: { second: 'value' } },
                keyB: { first: { } }
            };
            """;

    @Test
    public void safeSelect_documentedExample() {
        assertEquals(transform("""
                        { a: 'value', b: null, c: null }"""),
                transform(MY_OBJ + """
                        {
                            a: myObj?.keyA?.first?.second,
                            b: myObj?.keyB?.first?.second,
                            c: myObj?.keyC?.first?.second
                        }"""));
    }

    @Test
    public void coalesce_documentedExample() {
        assertEquals(transform("""
                        { a: 'value', b: 'defaultB', c: 'defaultC' }"""),
                transform(MY_OBJ + """
                        {
                            a: myObj?.keyA?.first?.second,
                            b: myObj?.keyB?.first?.second ?? 'defaultB',
                            c: myObj?.keyC?.first?.second ?? 'defaultC'
                        }"""));
    }

    @Test
    public void coalesce_onlyReplacesNull() {
        assertEquals(transform("'fallback'"), transform("null ?? 'fallback'"));
        assertEquals(transform("'value'"), transform("'value' ?? 'fallback'"));
    }

    @Test
    public void coalesce_doesNotReplaceFalsyValues() {
        // null coalescing, not falsy coalescing
        assertEquals(transform("false"), transform("false ?? 'fallback'"));
        assertEquals(transform("0"), transform("0 ?? 'fallback'"));
        assertEquals(transform("''"), transform("'' ?? 'fallback'"));
        assertEquals(transform("[]"), transform("[] ?? 'fallback'"));
    }

    @Test
    public void coalesce_chains() {
        assertEquals(transform("'last'"), transform("null ?? null ?? 'last'"));
        assertEquals(transform("'middle'"), transform("null ?? 'middle' ?? 'last'"));
    }

    @Test
    public void safeSelect_onNullReceiverYieldsNull() {
        assertEquals(transform("null"), transform("local o = null; o?.anything"));
        assertEquals(transform("'d'"), transform("local o = null; o?.anything ?? 'd'"));
    }

    @Test
    public void safeSelect_onMissingKeyYieldsNull() {
        assertEquals(transform("null"), transform("local o = {}; o?.missing"));
    }

    @Test
    public void safeSelect_doesNotMaskPresentFalsyValues() {
        assertEquals(transform("false"), transform("local o = { k: false }; o?.k"));
        assertEquals(transform("null"), transform("local o = { k: null }; o?.k"));
    }
}
