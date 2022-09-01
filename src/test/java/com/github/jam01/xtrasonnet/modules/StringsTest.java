package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
