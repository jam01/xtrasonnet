package com.datasonnet;
/*-
 * Copyright 2019-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static com.datasonnet.util.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsTest {
    @Test
    void testStrings_appendIfMissing() {
        assertEquals("abcxyz", transform("ds.strings.appendIfMissing('abc', 'xyz')"));
        assertEquals("abcxyz", transform("ds.strings.appendIfMissing('abcxyz', 'xyz')"));
        assertEquals("null", transform("ds.strings.appendIfMissing(null, '')"));
        assertEquals("xyzaxyz", transform("ds.strings.appendIfMissing('xyza', 'xyz')"));
        assertEquals("xyz", transform("ds.strings.appendIfMissing('', 'xyz')"));
    }

    @Test
    void testStrings_camelize() {
        assertEquals("customerFirstName", transform("ds.strings.camelize('customer_first_name')"));
        assertEquals("customerFirstName", transform("ds.strings.camelize('_customer_first_name')"));
        assertEquals("customerFirstName", transform("ds.strings.camelize('_______customer_first_name')"));
        assertEquals("null", transform("ds.strings.camelize(null)"));
    }

    @Test
    void testStrings_capitalize() {
        assertEquals("Customer", transform("ds.strings.capitalize('customer')"));
        assertEquals("Customer First Name", transform("ds.strings.capitalize('customer_first_name')"));
        assertEquals("Customer Name", transform("ds.strings.capitalize('customer NAME')"));
        assertEquals("Customer Name", transform("ds.strings.capitalize('customerName')"));
        assertEquals("null", transform("ds.strings.capitalize(null)"));
    }

    @Test
    void testStrings_charCode() {
        assertEquals("77", transform("ds.strings.charCode('Master')"));
        assertEquals("77", transform("ds.strings.charCode('M')"));
    }

    @Test
    void testStrings_charCodeAt() {
        assertEquals("67", transform("ds.strings.charCodeAt('charCodeAt', 4)"));
        assertEquals("65", transform("ds.strings.charCodeAt('charCodeAt', 8)"));
    }

    @Test
    void testStrings_dasherize() {
        assertEquals("customer", transform("ds.strings.dasherize('customer')"));
        assertEquals("customer-first-name", transform("ds.strings.dasherize('customer_first_name')"));
        assertEquals("customer-name", transform("ds.strings.dasherize('customer NAME')"));
        assertEquals("customer-name", transform("ds.strings.dasherize('customerName')"));
        assertEquals("null", transform("ds.strings.dasherize(null)"));
    }

    @Test
    void testStrings_fromCharCode() {
        assertEquals("C", transform("ds.strings.fromCharCode(67)"));
        assertEquals("A", transform("ds.strings.fromCharCode(65)"));
    }

    @Test
    void testStrings_isAlpha() {
        assertEquals("true", transform("ds.strings.isAlpha('sdfvxer')"));
        assertEquals("false", transform("ds.strings.isAlpha('ecvt4')"));
        assertEquals("true", transform("ds.strings.isAlpha(true)"));
        assertEquals("false", transform("ds.strings.isAlpha(45)"));
    }

    @Test
    void testStrings_isAlphanumeric() {
        assertEquals("true", transform("ds.strings.isAlphanumeric('sdfvxer')"));
        assertEquals("true", transform("ds.strings.isAlphanumeric('ecvt4')"));
        assertEquals("true", transform("ds.strings.isAlphanumeric(true)"));
        assertEquals("true", transform("ds.strings.isAlphanumeric(45)"));
    }

    @Test
    void testStrings_isLowerCase() {
        assertEquals("true", transform("ds.strings.isLowerCase('sdfvxer')"));
        assertEquals("false", transform("ds.strings.isLowerCase('ecvt4')"));
        assertEquals("false", transform("ds.strings.isLowerCase('eCvt')"));
        assertEquals("true", transform("ds.strings.isLowerCase(true)"));
        assertEquals("false", transform("ds.strings.isLowerCase(45)"));
    }

    @Test
    void testStrings_isNumeric() {
        assertEquals("false", transform("ds.strings.isNumeric('sdfvxer')"));
        assertEquals("true", transform("ds.strings.isNumeric('5334')"));
        assertEquals("true", transform("ds.strings.isNumeric(100)"));
    }

    @Test
    void testStrings_isUpperCase() {
        assertEquals("true", transform("ds.strings.isUpperCase('SDFVXER')"));
        assertEquals("false", transform("ds.strings.isUpperCase('ECVT4')"));
        assertEquals("false", transform("ds.strings.isUpperCase('EcVT')"));
        assertEquals("false", transform("ds.strings.isUpperCase(true)"));
        assertEquals("false", transform("ds.strings.isUpperCase(45)"));
    }

    @Test
    void testStrings_isWhitespace() {
        assertEquals("false", transform("ds.strings.isWhitespace(null)"));
        assertEquals("true", transform("ds.strings.isWhitespace('')"));
        assertEquals("true", transform("ds.strings.isWhitespace('       ')"));
        assertEquals("false", transform("ds.strings.isWhitespace('   abc    ')"));
        assertEquals("false", transform("ds.strings.isWhitespace(true)"));
        assertEquals("false", transform("ds.strings.isWhitespace(45)"));
    }

    @Test
    void testStrings_leftPad() {
        assertEquals("null", transform("ds.strings.leftPad(null,3)"));
        assertEquals("   ", transform("ds.strings.leftPad('',3)"));
        assertEquals("  bat", transform("ds.strings.leftPad('bat',5)"));
        assertEquals("bat", transform("ds.strings.leftPad('bat',3)"));
        assertEquals("bat", transform("ds.strings.leftPad('bat',-1)"));
        assertEquals(" 45", transform("ds.strings.leftPad(45,3)"));
        assertEquals("      true", transform("ds.strings.leftPad(true,10)"));
    }

    @Test
    void testStrings_ordinalize() {
        assertEquals("1st", transform("ds.strings.ordinalize(1)"));
        assertEquals("2nd", transform("ds.strings.ordinalize(2)"));
        assertEquals("3rd", transform("ds.strings.ordinalize(3)"));
        assertEquals("111th", transform("ds.strings.ordinalize(111)"));
        assertEquals("22nd", transform("ds.strings.ordinalize(22)"));
        assertEquals("null", transform("ds.strings.ordinalize(null)"));
    }

    @Test
    void testStrings_pluralize() {
        assertEquals("null", transform("ds.strings.pluralize(null)"));
        assertEquals("helps", transform("ds.strings.pluralize('help')"));
        assertEquals("boxes", transform("ds.strings.pluralize('box')"));
        assertEquals("mondays", transform("ds.strings.pluralize('monday')"));
        assertEquals("mondies", transform("ds.strings.pluralize('mondy')"));
    }

    @Test
    void testStrings_prependIfMissing() {
        assertEquals("xyzabc", transform("ds.strings.prependIfMissing('abc', 'xyz')"));
        assertEquals("xyzabc", transform("ds.strings.prependIfMissing('xyzabc', 'xyz')"));
        assertEquals("null", transform("ds.strings.prependIfMissing(null, '')"));
        assertEquals("xyzaxyz", transform("ds.strings.prependIfMissing('axyz', 'xyz')"));
        assertEquals("xyz", transform("ds.strings.prependIfMissing('', 'xyz')"));
    }

    @Test
    void testStrings_repeat() {
        assertEquals("", transform("ds.strings.repeat('e', 0)"));
        assertEquals("eee", transform("ds.strings.repeat('e', 3)"));
        assertEquals("", transform("ds.strings.repeat('e', -2)"));
    }

    @Test
    void testStrings_rightPad() {
        assertEquals("null", transform("ds.strings.rightPad(null,3)"));
        assertEquals("   ", transform("ds.strings.rightPad('',3)"));
        assertEquals("bat  ", transform("ds.strings.rightPad('bat',5)"));
        assertEquals("bat", transform("ds.strings.rightPad('bat',3)"));
        assertEquals("bat", transform("ds.strings.rightPad('bat',-1)"));
        assertEquals("45 ", transform("ds.strings.rightPad(45,3)"));
        assertEquals("true      ", transform("ds.strings.rightPad(true,10)"));
    }

    @Test
    void testStrings_singularize() {
        assertEquals("null", transform("ds.strings.singularize(null)"));
        assertEquals("help", transform("ds.strings.singularize('helps')"));
        assertEquals("box", transform("ds.strings.singularize('boxes')"));
        assertEquals("monday", transform("ds.strings.singularize('mondays')"));
        assertEquals("mondy", transform("ds.strings.singularize('mondies')"));
    }

    @Test
    void testStrings_substringAfter() {
        assertEquals("null", transform("ds.strings.substringAfter(null, \"'\")"));
        assertEquals("", transform("ds.strings.substringAfter('', '-')"));
        assertEquals("bc", transform("ds.strings.substringAfter('abc', 'a')"));
        assertEquals("c", transform("ds.strings.substringAfter('abc', 'b')"));
        assertEquals("cba", transform("ds.strings.substringAfter('abcba', 'b')"));
        assertEquals("", transform("ds.strings.substringAfter('abc', 'd')"));
        assertEquals("abc", transform("ds.strings.substringAfter('abc', '')"));
    }

    @Test
    void testStrings_substringAfterLast() {
        assertEquals("null", transform("ds.strings.substringAfterLast(null, \"'\")"));
        assertEquals("", transform("ds.strings.substringAfterLast('', '-')"));
        assertEquals("xy", transform("ds.strings.substringAfterLast('abcaxy', 'a')"));
        assertEquals("c", transform("ds.strings.substringAfterLast('abc', 'b')"));
        assertEquals("a", transform("ds.strings.substringAfterLast('abcba', 'b')"));
        assertEquals("", transform("ds.strings.substringAfterLast('abc', 'd')"));
        assertEquals("", transform("ds.strings.substringAfterLast('abc', '')"));
    }

    @Test
    void testStrings_substringBefore() {
        assertEquals("null", transform("ds.strings.substringBefore(null, \"'\")"));
        assertEquals("", transform("ds.strings.substringBefore('', '-')"));
        assertEquals("", transform("ds.strings.substringBefore('abc', 'a')"));
        assertEquals("a", transform("ds.strings.substringBefore('abc', 'b')"));
        assertEquals("a", transform("ds.strings.substringBefore('abcba', 'b')"));
        assertEquals("", transform("ds.strings.substringBefore('abc', 'd')"));
        assertEquals("", transform("ds.strings.substringBefore('abc', '')"));
    }

    @Test
    void testStrings_substringBeforeLast() {
        assertEquals("null", transform("ds.strings.substringBeforeLast(null, \"'\")"));
        assertEquals("", transform("ds.strings.substringBeforeLast('', '-')"));
        assertEquals("", transform("ds.strings.substringBeforeLast('abc', 'a')"));
        assertEquals("a", transform("ds.strings.substringBeforeLast('abc', 'b')"));
        assertEquals("abc", transform("ds.strings.substringBeforeLast('abcba', 'b')"));
        assertEquals("", transform("ds.strings.substringBeforeLast('abc', 'd')"));
        assertEquals("abc", transform("ds.strings.substringBeforeLast('abc', '')"));
    }

    @Test
    void testStrings_underscore() {
        assertEquals("customer", transform("ds.strings.underscore('customer')"));
        assertEquals("customer_first_name", transform("ds.strings.underscore('customer-first-name')"));
        assertEquals("customer_name", transform("ds.strings.underscore('customer NAME')"));
        assertEquals("customer_name", transform("ds.strings.underscore('customerName')"));
        assertEquals("null", transform("ds.strings.underscore(null)"));
    }

    @Test
    void testStrings_unwrap() {
        assertEquals("null", transform("ds.strings.unwrap(null, '')"));
        assertEquals("abc", transform("ds.strings.unwrap('abc', \"'\")"));
        assertEquals("ABabcBA", transform("ds.strings.unwrap('AABabcBAA', 'A')"));
        assertEquals("A", transform("ds.strings.unwrap('A', '#')"));
        assertEquals("#A", transform("ds.strings.unwrap('A#', '#')"));
    }

    @Test
    void testStrings_withMaxSize() {
        assertEquals("null", transform("ds.strings.withMaxSize(null, 10)"));
        assertEquals("123", transform("ds.strings.withMaxSize('123', 10)"));
        assertEquals("123", transform("ds.strings.withMaxSize('123', 3)"));
        assertEquals("12", transform("ds.strings.withMaxSize('123', 2)"));
        assertEquals("", transform("ds.strings.withMaxSize('', 0)"));
    }

    @Test
    void testStrings_wrapIfMissing() {
        assertEquals("null", transform("ds.strings.wrapIfMissing(null, \"'\")"));
        assertEquals("'abc'", transform("ds.strings.wrapIfMissing('abc', \"'\")"));
        assertEquals("'abc'", transform("ds.strings.wrapIfMissing(\"'abc'\", \"'\")"));
        assertEquals("'abc'", transform("ds.strings.wrapIfMissing('abc', \"'\")"));
    }

    @Test
    void testStrings_wrapWith() {
        assertEquals("null", transform("ds.strings.wrapWith(null, \"'\")"));
        assertEquals("'abc'", transform("ds.strings.wrapWith('abc', \"'\")"));
        assertEquals("''abc'", transform("ds.strings.wrapWith(\"'abc\", \"'\")"));
    }
}
