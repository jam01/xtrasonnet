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

public class ObjectsTest {
    @Test
    void testObjects_divideBy() {
        String input = "{'a': 1, " +
                "'b' : true, " +
                "'c' : 2, " +
                "'d' : false, " +
                "'e' : 3}";
        String compare = "[{a:1,b:true},{c:2,d:false},{e:3}]";
        assertEquals(compare, transform("ds.objects.divideBy(" + input + ", 2)"));

        compare = "[{a:1,b:true,c:2},{d:false,e:3}]";
        assertEquals(compare, transform("ds.objects.divideBy(" + input + ", 3)"));
    }

    @Test
    void testObjects_everyEntry() {
        assertEquals("true", transform("ds.objects.everyEntry({'a':'','b':'123'}, function(value) std.isString(value))"));
        assertEquals("false", transform("ds.objects.everyEntry({'a':'','b':'123'}, function(value,key) key =='a')"));
        assertEquals("true", transform("ds.objects.everyEntry({'b':''}, function(value,key) key == 'b')"));
        assertEquals("true", transform("ds.objects.everyEntry(null, function(value) std.isString(value))"));
        assertEquals("true", transform("ds.objects.everyEntry(null, function(value) std.isString(value))"));
    }

    @Test
    void testObjects_mergeWith() {
        String obj1, obj2;
        obj1 = "{'a': true, 'b': 1}";
        obj2 = "{'a': false, 'c': 'Test'}";

        assertEquals("{a:false,b:1,c:Test}", transform("ds.objects.mergeWith(" + obj1 + ", " + obj2 + ")"));
        assertEquals("{a:true,b:1}", transform("ds.objects.mergeWith(" + obj1 + ", null)"));
        assertEquals("{a:false,c:Test}", transform("ds.objects.mergeWith(null, " + obj2 + ")"));
    }

    @Test
    void testObjects_someEntry() {
        assertEquals("true", transform("ds.objects.someEntry({ 'a' : true, 'b' : 1}, function(value,key) value == true)"));
        assertEquals("false", transform("ds.objects.someEntry({ 'a' : true, 'b' : 1}, function(value,key) value == false)"));
        assertEquals("true", transform("ds.objects.someEntry({ 'a' : true, 'b' : 1}, function(value,key) key == 'a')"));
        assertEquals("false", transform("ds.objects.someEntry(null, function(value,key) key == 'a')"));
    }

    @Test
    void testObjects_takeWhile() {
        assertEquals("{a:1,b:1}", transform("ds.objects.takeWhile({'a':1,'b':1,'c':5,'d':1}, function(value,key) value == 1)"));
        assertEquals("{a:1}", transform("ds.objects.takeWhile({'a':1,'b':1,'c':5,'d':1}, function(value,key) key == 'a')"));
    }
}
