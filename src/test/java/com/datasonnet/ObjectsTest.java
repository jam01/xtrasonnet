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

public class ObjectsTest {
    @Test
    void testObjects_divideBy() {
        String input = "{'a': 1, " +
                "'b' : true, " +
                "'c' : 2, " +
                "'d' : false, " +
                "'e' : 3}";
        String compare = "[{a:1,b:true},{c:2,d:false},{e:3}]";
        assertEquals(compare, transform("tro.objects.divideBy(" + input + ", 2)"));

        compare = "[{a:1,b:true,c:2},{d:false,e:3}]";
        assertEquals(compare, transform("tro.objects.divideBy(" + input + ", 3)"));
    }

    @Test
    void testObjects_everyEntry() {
        assertEquals("true", transform("tro.objects.allEntries({'a':'','b':'123'}, function(value) std.isString(value))"));
        assertEquals("false", transform("tro.objects.allEntries({'a':'','b':'123'}, function(value,key) key =='a')"));
        assertEquals("true", transform("tro.objects.allEntries({'b':''}, function(value,key) key == 'b')"));
    }

    @Test
    void testObjects_someEntry() {
        assertEquals("true", transform("tro.objects.anyEntry({ 'a' : true, 'b' : 1}, function(value,key) value == true)"));
        assertEquals("false", transform("tro.objects.anyEntry({ 'a' : true, 'b' : 1}, function(value,key) value == false)"));
        assertEquals("true", transform("tro.objects.anyEntry({ 'a' : true, 'b' : 1}, function(value,key) key == 'a')"));
    }

    @Test
    void testObjects_takeWhile() {
        assertEquals("{a:1,b:1}", transform("tro.objects.takeWhile({'a':1,'b':1,'c':5,'d':1}, function(value,key) value == 1)"));
        assertEquals("{a:1}", transform("tro.objects.takeWhile({'a':1,'b':1,'c':5,'d':1}, function(value,key) key == 'a')"));
    }
}
