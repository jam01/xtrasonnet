package com.datasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
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

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsTest {
    @Test
    void testStrings_appendIfMissing() {
        assertEquals("abcxyz", transform("tro.strings.appendIfMissing('abc', 'xyz')"));
        assertEquals("abcxyz", transform("tro.strings.appendIfMissing('abcxyz', 'xyz')"));
        assertEquals("xyzaxyz", transform("tro.strings.appendIfMissing('xyza', 'xyz')"));
        assertEquals("xyz", transform("tro.strings.appendIfMissing('', 'xyz')"));
    }

    @Test
    void testStrings_camelize() {
        assertEquals("customerFirstName", transform("tro.strings.camelize('customer_first_name')"));
        assertEquals("customerFirstName", transform("tro.strings.camelize('_customer_first_name')"));
        assertEquals("customerFirstName", transform("tro.strings.camelize('_______customer_first_name')"));
    }

    @Test
    void testStrings_capitalize() {
        assertEquals("Customer", transform("tro.strings.capitalize('customer')"));
        assertEquals("Customer First Name", transform("tro.strings.capitalize('customer_first_name')"));
        assertEquals("Customer Name", transform("tro.strings.capitalize('customer NAME')"));
        assertEquals("Customer Name", transform("tro.strings.capitalize('customerName')"));
    }

    @Test
    void testStrings_charCode() {
        assertEquals("77", transform("tro.strings.charCode('Master')"));
        assertEquals("77", transform("tro.strings.charCode('M')"));
    }

    @Test
    void testStrings_charCodeAt() {
        assertEquals("67", transform("tro.strings.charCodeAt('charCodeAt', 4)"));
        assertEquals("65", transform("tro.strings.charCodeAt('charCodeAt', 8)"));
    }

    @Test
    void testStrings_dasherize() {
        assertEquals("customer", transform("tro.strings.dasherize('customer')"));
        assertEquals("customer-first-name", transform("tro.strings.dasherize('customer_first_name')"));
        assertEquals("customer-name", transform("tro.strings.dasherize('customer NAME')"));
        assertEquals("customer-name", transform("tro.strings.dasherize('customerName')"));
    }

    @Test
    void testStrings_fromCharCode() {
        assertEquals("C", transform("tro.strings.fromCharCode(67)"));
        assertEquals("A", transform("tro.strings.fromCharCode(65)"));
    }

    @Test
    void testStrings_isAlpha() {
        assertEquals("true", transform("tro.strings.isAlpha('sdfvxer')"));
        assertEquals("false", transform("tro.strings.isAlpha('ecvt4')"));
        assertEquals("true", transform("tro.strings.isAlpha(true)"));
        assertEquals("false", transform("tro.strings.isAlpha(45)"));
    }

    @Test
    void testStrings_isAlphanumeric() {
        assertEquals("true", transform("tro.strings.isAlphanumeric('sdfvxer')"));
        assertEquals("true", transform("tro.strings.isAlphanumeric('ecvt4')"));
        assertEquals("true", transform("tro.strings.isAlphanumeric(true)"));
        assertEquals("true", transform("tro.strings.isAlphanumeric(45)"));
    }

    @Test
    void testStrings_isLowerCase() {
        assertEquals("true", transform("tro.strings.isLowerCase('sdfvxer')"));
        assertEquals("false", transform("tro.strings.isLowerCase('ecvt4')"));
        assertEquals("false", transform("tro.strings.isLowerCase('eCvt')"));
    }

    @Test
    void testStrings_isNumeric() {
        assertEquals("false", transform("tro.strings.isNumeric('sdfvxer')"));
        assertEquals("true", transform("tro.strings.isNumeric('5334')"));
        assertEquals("true", transform("tro.strings.isNumeric(100)"));
    }

    @Test
    void testStrings_isUpperCase() {
        assertEquals("true", transform("tro.strings.isUpperCase('SDFVXER')"));
        assertEquals("false", transform("tro.strings.isUpperCase('ECVT4')"));
        assertEquals("false", transform("tro.strings.isUpperCase('EcVT')"));
    }

    @Test
    void testStrings_isWhitespace() {
        assertEquals("true", transform("tro.strings.isWhitespace('')"));
        assertEquals("true", transform("tro.strings.isWhitespace('       ')"));
        assertEquals("false", transform("tro.strings.isWhitespace('   abc    ')"));
    }

    @Test
    void testStrings_leftPad() {
        assertEquals("   ", transform("tro.strings.leftPad('',3)"));
        assertEquals("  bat", transform("tro.strings.leftPad('bat',5)"));
        assertEquals("bat", transform("tro.strings.leftPad('bat',3)"));
        assertEquals("bat", transform("tro.strings.leftPad('bat',-1)"));
        assertEquals(" 45", transform("tro.strings.leftPad(45,3)"));
    }

    @Test
    void testStrings_ordinalize() {
        assertEquals("1st", transform("tro.strings.ordinalize(1)"));
        assertEquals("2nd", transform("tro.strings.ordinalize(2)"));
        assertEquals("3rd", transform("tro.strings.ordinalize(3)"));
        assertEquals("111th", transform("tro.strings.ordinalize(111)"));
        assertEquals("22nd", transform("tro.strings.ordinalize(22)"));
    }

    @Test
    void testStrings_pluralize() {
        assertEquals("helps", transform("tro.strings.pluralize('help')"));
        assertEquals("boxes", transform("tro.strings.pluralize('box')"));
        assertEquals("mondays", transform("tro.strings.pluralize('monday')"));
        assertEquals("mondies", transform("tro.strings.pluralize('mondy')"));
    }

    @Test
    void testStrings_prependIfMissing() {
        assertEquals("xyzabc", transform("tro.strings.prependIfMissing('abc', 'xyz')"));
        assertEquals("xyzabc", transform("tro.strings.prependIfMissing('xyzabc', 'xyz')"));
        assertEquals("xyzaxyz", transform("tro.strings.prependIfMissing('axyz', 'xyz')"));
        assertEquals("xyz", transform("tro.strings.prependIfMissing('', 'xyz')"));
    }

    @Test
    void testStrings_repeat() {
        assertEquals("", transform("tro.strings.repeat('e', 0)"));
        assertEquals("eee", transform("tro.strings.repeat('e', 3)"));
        assertEquals("", transform("tro.strings.repeat('e', -2)"));
    }

    @Test
    void testStrings_rightPad() {
        assertEquals("   ", transform("tro.strings.rightPad('',3)"));
        assertEquals("bat  ", transform("tro.strings.rightPad('bat',5)"));
        assertEquals("bat", transform("tro.strings.rightPad('bat',3)"));
        assertEquals("bat", transform("tro.strings.rightPad('bat',-1)"));
        assertEquals("45 ", transform("tro.strings.rightPad(45,3)"));
    }

    @Test
    void testStrings_singularize() {
        assertEquals("help", transform("tro.strings.singularize('helps')"));
        assertEquals("box", transform("tro.strings.singularize('boxes')"));
        assertEquals("monday", transform("tro.strings.singularize('mondays')"));
        assertEquals("mondy", transform("tro.strings.singularize('mondies')"));
    }

    @Test
    void testStrings_substringAfter() {
        assertEquals("", transform("tro.strings.substringAfter('', '-')"));
        assertEquals("bc", transform("tro.strings.substringAfter('abc', 'a')"));
        assertEquals("c", transform("tro.strings.substringAfter('abc', 'b')"));
        assertEquals("cba", transform("tro.strings.substringAfter('abcba', 'b')"));
        assertEquals("", transform("tro.strings.substringAfter('abc', 'd')"));
        assertEquals("abc", transform("tro.strings.substringAfter('abc', '')"));
    }

    @Test
    void testStrings_substringAfterLast() {
        assertEquals("", transform("tro.strings.substringAfterLast('', '-')"));
        assertEquals("xy", transform("tro.strings.substringAfterLast('abcaxy', 'a')"));
        assertEquals("c", transform("tro.strings.substringAfterLast('abc', 'b')"));
        assertEquals("a", transform("tro.strings.substringAfterLast('abcba', 'b')"));
        assertEquals("", transform("tro.strings.substringAfterLast('abc', 'd')"));
        assertEquals("", transform("tro.strings.substringAfterLast('abc', '')"));
    }

    @Test
    void testStrings_substringBefore() {
        assertEquals("", transform("tro.strings.substringBefore('', '-')"));
        assertEquals("", transform("tro.strings.substringBefore('abc', 'a')"));
        assertEquals("a", transform("tro.strings.substringBefore('abc', 'b')"));
        assertEquals("a", transform("tro.strings.substringBefore('abcba', 'b')"));
        assertEquals("", transform("tro.strings.substringBefore('abc', 'd')"));
        assertEquals("", transform("tro.strings.substringBefore('abc', '')"));
    }

    @Test
    void testStrings_substringBeforeLast() {
        assertEquals("", transform("tro.strings.substringBeforeLast('', '-')"));
        assertEquals("", transform("tro.strings.substringBeforeLast('abc', 'a')"));
        assertEquals("a", transform("tro.strings.substringBeforeLast('abc', 'b')"));
        assertEquals("abc", transform("tro.strings.substringBeforeLast('abcba', 'b')"));
        assertEquals("", transform("tro.strings.substringBeforeLast('abc', 'd')"));
        assertEquals("abc", transform("tro.strings.substringBeforeLast('abc', '')"));
    }

    @Test
    void testStrings_underscore() {
        assertEquals("customer", transform("tro.strings.underscore('customer')"));
        assertEquals("customer_first_name", transform("tro.strings.underscore('customer-first-name')"));
        assertEquals("customer_name", transform("tro.strings.underscore('customer NAME')"));
        assertEquals("customer_name", transform("tro.strings.underscore('customerName')"));
    }

    @Test
    void testStrings_unwrap() {
        assertEquals("abc", transform("tro.strings.unwrap('abc', \"'\")"));
        assertEquals("ABabcBA", transform("tro.strings.unwrap('AABabcBAA', 'A')"));
        assertEquals("A", transform("tro.strings.unwrap('A', '#')"));
        assertEquals("#A", transform("tro.strings.unwrap('A#', '#')"));
    }

    @Test
    void testStrings_withMaxSize() {
        assertEquals("123", transform("tro.strings.withMaxSize('123', 10)"));
        assertEquals("123", transform("tro.strings.withMaxSize('123', 3)"));
        assertEquals("12", transform("tro.strings.withMaxSize('123', 2)"));
        assertEquals("", transform("tro.strings.withMaxSize('', 0)"));
    }

    @Test
    void testStrings_wrapIfMissing() {
        assertEquals("'abc'", transform("tro.strings.wrapIfMissing('abc', \"'\")"));
        assertEquals("'abc'", transform("tro.strings.wrapIfMissing(\"'abc'\", \"'\")"));
        assertEquals("'abc'", transform("tro.strings.wrapIfMissing('abc', \"'\")"));
    }

    @Test
    void testStrings_wrapWith() {
        assertEquals("'abc'", transform("tro.strings.wrap('abc', \"'\")"));
        assertEquals("''abc'", transform("tro.strings.wrap(\"'abc\", \"'\")"));
    }
}
