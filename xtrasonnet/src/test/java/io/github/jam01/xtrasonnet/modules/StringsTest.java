package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import io.github.jam01.xtrasonnet.XtrasonnetException;
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringsTest {
    @Test
    public void appendIfMissing() {
        assertEquals(transform("'Hello World'"), transform("xtr.strings.appendIfMissing('Hello', ' World')"));
    }

    @Test
    public void toCamelCase() {
        assertEquals(transform("'helloToEveryone'"), transform("xtr.strings.toCamelCase('Hello to_everyone')"));
    }

    @Test
    public void capitalize() {
        assertEquals(transform("'Hello World'"), transform("xtr.strings.capitalize('hello world')"));
    }

    @Test
    public void charCode() {
        assertEquals(transform("42"), transform("xtr.strings.charCode('*')"));
        assertEquals(transform("42"), transform("xtr.strings.charCodeAt('_*_', 1)"));
        assertEquals(transform("'*'"), transform("xtr.strings.ofCharCode(42)"));
    }

    @Test
    public void toKebabCase() {
        assertEquals(transform("'hello-world-x'"), transform("xtr.strings.toKebabCase('Hello World_X')"));
    }

    @Test
    public void isX() {
        assertEquals(transform("true"), transform("xtr.strings.isAlpha('abcde')"));
        assertEquals(transform("true"), transform("xtr.strings.isAlphanumeric('a1b2cd3e4')"));
        assertEquals(transform("true"), transform("xtr.strings.isLowerCase('hello')"));
        assertEquals(transform("true"), transform("xtr.strings.isNumeric('34634')"));
        assertEquals(transform("true"), transform("xtr.strings.isUpperCase('HELLO')"));
    }

    @Test
    public void pad() {
        assertEquals(transform("'     Hello'"), transform("xtr.strings.leftPad('Hello', 10, ' ')"));
        assertEquals(transform("'Hello     '"), transform("xtr.strings.rightPad('Hello', 10, ' ')"));
    }

    @Test
    public void leftPad_doesNotRewriteTheValue() {
        assertEquals(transform("'00a b'"), transform("xtr.strings.leftPad('a b', 5, '0')"));
        assertEquals(transform("'****hello world'"), transform("xtr.strings.leftPad('hello world', 15, '*')"));
    }

    @Test
    public void pad_returnsTheValueWhenNoPaddingIsDue() {
        assertEquals(transform("'a b'"), transform("xtr.strings.leftPad('a b', 1, '0')"));
        assertEquals(transform("'a'"), transform("xtr.strings.leftPad('a', -3, '0')"));
        assertEquals(transform("'a'"), transform("xtr.strings.leftPad('a', 0, '0')"));
        assertEquals(transform("'a b'"), transform("xtr.strings.rightPad('a b', 1, '0')"));
        assertEquals(transform("'a'"), transform("xtr.strings.rightPad('a', -3, '0')"));
    }

    @Test
    public void pad_rejectsAnEmptyPad() {
        assertMessageContains("Expected a non-empty pad", () -> transform("xtr.strings.leftPad('a', 5, '')"));
        assertMessageContains("Expected a non-empty pad", () -> transform("xtr.strings.rightPad('a', 5, '')"));
    }

    @Test
    public void pad_usesTheFirstCharacterOfTheGivenPad() {
        assertEquals(transform("'xxa'"), transform("xtr.strings.leftPad('a', 3, 'xyz')"));
        assertEquals(transform("'axx'"), transform("xtr.strings.rightPad('a', 3, 'xyz')"));
    }

    @Test
    public void capitalize_withoutAnyAlphanumericCharacter() {
        assertEquals(transform("'---'"), transform("xtr.strings.capitalize('---')"));
        assertEquals(transform("''"), transform("xtr.strings.capitalize('')"));
        assertEquals(transform("'---'"), transform("xtr.strings.toSnakeCase('---')"));
        assertEquals(transform("''"), transform("xtr.strings.toSnakeCase('')"));
    }

    @Test
    public void charCode_outOfRange() {
        assertMessageContains("Expected a non-empty String", () -> transform("xtr.strings.charCode('')"));
        assertMessageContains("Expected an index within [0, 3), got: 3", () -> transform("xtr.strings.charCodeAt('abc', 3)"));
        assertMessageContains("Expected an index within [0, 3), got: -1", () -> transform("xtr.strings.charCodeAt('abc', -1)"));
    }

    private static void assertMessageContains(String expected, org.junit.jupiter.api.function.Executable call) {
        var thrown = assertThrows(XtrasonnetException.class, call);
        assertTrue(thrown.getMessage().contains(expected),
                "expected a message containing <" + expected + "> but was <" + thrown.getMessage() + ">");
    }

    @Test
    public void numOrdinalOf() {
        assertEquals(transform("'1st'"), transform("xtr.strings.numOrdinalOf(1)"));
    }

    @Test
    public void pluralize() {
        assertEquals(transform("'cars'"), transform("xtr.strings.pluralize('car')"));
    }

    @Test
    public void prependIfMissing() {
        assertEquals(transform("'Hello World'"), transform("xtr.strings.prependIfMissing('World', 'Hello ')"));
    }

    @Test
    public void repeat() {
        assertEquals(transform("'hey hey '"), transform("xtr.strings.repeat('hey ', 2)"));
    }

    @Test
    public void singularize() {
        assertEquals(transform("'car'"), transform("xtr.strings.singularize('cars')"));
    }

    @Test
    public void substring() {
        assertEquals(transform("'HelloXWorldXAfter'"), transform("xtr.strings.substringAfter('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'After'"), transform("xtr.strings.substringAfterLast('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'!'"), transform("xtr.strings.substringBefore('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'!XHelloXWorld'"), transform("xtr.strings.substringBeforeLast('!XHelloXWorldXAfter', 'X')"));
    }

    @Test
    public void substringWithMultiCharSeparator() {
        // substringAfter skipped a single char rather than the separator's length, leaving its tail
        // behind; substringAfterLast used split, which treats the separator as a regex
        assertEquals(transform("'b::c'"), transform("xtr.strings.substringAfter('a::b::c', '::')"));
        assertEquals(transform("'c'"), transform("xtr.strings.substringAfterLast('a::b::c', '::')"));
        assertEquals(transform("'a'"), transform("xtr.strings.substringBefore('a::b::c', '::')"));
        assertEquals(transform("'a::b'"), transform("xtr.strings.substringBeforeLast('a::b::c', '::')"));
    }

    @Test
    public void substringWithRegexMetacharSeparator() {
        // '.' must be a literal, not "any character"
        assertEquals(transform("'b.c'"), transform("xtr.strings.substringAfter('a.b.c', '.')"));
        assertEquals(transform("'c'"), transform("xtr.strings.substringAfterLast('a.b.c', '.')"));
    }

    @Test
    public void substringSeparatorAbsent() {
        // all four agree: absent separator yields the empty string
        assertEquals(transform("''"), transform("xtr.strings.substringAfter('abc', 'X')"));
        assertEquals(transform("''"), transform("xtr.strings.substringAfterLast('abc', 'X')"));
        assertEquals(transform("''"), transform("xtr.strings.substringBefore('abc', 'X')"));
        assertEquals(transform("''"), transform("xtr.strings.substringBeforeLast('abc', 'X')"));
    }

    @Test
    public void toSnakeCase() {
        assertEquals(transform("'hello_world_x'"), transform("xtr.strings.toSnakeCase('Hello WorldX')"));
    }

    @Test
    public void unwrap() {
        assertEquals(transform("'Hello, world!'"), transform("xtr.strings.unwrap('_Hello, world!_', '_')"));
    }

    @Test
    public void wrap() {
        assertEquals(transform("'__Hello, world!_'"), transform("xtr.strings.wrap('_Hello, world!', '_')"));
    }

    @Test
    public void wrapIfMissing() {
        assertEquals(transform("'_Hello, world!_'"), transform("xtr.strings.wrapIfMissing('_Hello, world!', '_')"));
    }
}
