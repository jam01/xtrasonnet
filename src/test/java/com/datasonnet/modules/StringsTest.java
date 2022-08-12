package com.datasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsTest {
    @Test
    public void appendIfMissing() {
        assertEquals(transform("'Hello World'"), transform("tro.strings.appendIfMissing('Hello', ' World')"));
    }

    @Test
    public void camelize() {
        assertEquals(transform("'helloToEveryone'"), transform("tro.strings.camelize('Hello to_everyone')"));
    }

    @Test
    public void capitalize() {
        assertEquals(transform("'Hello World'"), transform("tro.strings.capitalize('hello world')"));
    }

    @Test
    public void charCode() {
        assertEquals(transform("42"), transform("tro.strings.charCode('*')"));
        assertEquals(transform("42"), transform("tro.strings.charCodeAt('_*_', 1)"));
        assertEquals(transform("'*'"), transform("tro.strings.ofCharCode(42)"));
    }

    @Test
    public void dasherize() {
        assertEquals(transform("'hello-world-x'"), transform("tro.strings.dasherize('Hello World_X')"));
    }

    @Test
    public void isX() {
        assertEquals(transform("true"), transform("tro.strings.isAlpha('abcde')"));
        assertEquals(transform("true"), transform("tro.strings.isAlphanumeric('a1b2cd3e4')"));
        assertEquals(transform("true"), transform("tro.strings.isLowerCase('hello')"));
        assertEquals(transform("true"), transform("tro.strings.isNumeric('34634')"));
        assertEquals(transform("true"), transform("tro.strings.isUpperCase('HELLO')"));
    }

    @Test
    public void pad() {
        assertEquals(transform("'     Hello'"), transform("tro.strings.leftPad('Hello', 10, ' ')"));
        assertEquals(transform("'Hello     '"), transform("tro.strings.rightPad('Hello', 10, ' ')"));
    }

    @Test
    public void ordinalOf() {
        assertEquals(transform("'1st'"), transform("tro.strings.ordinalOf(1)"));
    }

    @Test
    public void pluralize() {
        assertEquals(transform("'cars'"), transform("tro.strings.pluralize('car')"));
    }

    @Test
    public void prependIfMissing() {
        assertEquals(transform("'Hello World'"), transform("tro.strings.prependIfMissing('World', 'Hello ')"));
    }

    @Test
    public void repeat() {
        assertEquals(transform("'hey hey '"), transform("tro.strings.repeat('hey ', 2)"));
    }

    @Test
    public void singularize() {
        assertEquals(transform("'car'"), transform("tro.strings.singularize('cars')"));
    }

    @Test
    public void substring() {
        assertEquals(transform("'HelloXWorldXAfter'"), transform("tro.strings.substringAfter('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'After'"), transform("tro.strings.substringAfterLast('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'!'"), transform("tro.strings.substringBefore('!XHelloXWorldXAfter', 'X')"));
        assertEquals(transform("'!XHelloXWorld'"), transform("tro.strings.substringBeforeLast('!XHelloXWorldXAfter', 'X')"));
    }

    @Test
    public void underscore() {
        assertEquals(transform("'hello_world_x'"), transform("tro.strings.underscore('Hello WorldX')"));
    }

    @Test
    public void unwrap() {
        assertEquals(transform("'Hello, world!'"), transform("tro.strings.unwrap('_Hello, world!_', '_')"));
    }

    @Test
    public void truncate() {
        assertEquals(transform("'Hello'"), transform("tro.strings.truncate('Hello, world!', 5)"));
    }

    @Test
    public void wrapIfMissing() {
        assertEquals(transform("'_Hello, world!_'"), transform("tro.strings.wrapIfMissing('_Hello, world!', '_')"));
    }
}
