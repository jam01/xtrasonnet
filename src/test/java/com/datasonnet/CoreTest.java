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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoreTest {
    @Test
    void test_contains() {
        assertEquals("true", transform("ds.contains([1,2,3,4,5] , 5)"));
        assertEquals("true", transform("ds.contains('Hello World' , 'World')"));
        assertEquals("true", transform("ds.contains('Hello World' , '[e-g]')"));
    }

    @Test
    void test_entriesOf() {
        String input = "{'test1':'x','test2':{'inTest3':'x','inTest4':{}},'test10':[{},{}]}";
        String compare = "[{key:test1,value:x},{key:test2,value:{inTest3:x,inTest4:{}}},{key:test10,value:[{},{}]}]";
        assertEquals(compare, transform("ds.entriesOf(" + input + ")"));
    }

    @Test
    void test_distinctBy() {
        assertEquals("[0,1,2,3,4]", transform("ds.distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item) item)"));
        assertEquals("[0,1,2,3,3,2,1,4]", transform("ds.distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item,index) index )"));
        assertEquals("{a:0,b:1}", transform("ds.distinctBy({'a':0, 'b':1, 'c':0}, function(value, key) value)"));
        assertEquals("{a:0,b:1}", transform("ds.distinctBy({'a':0, 'b':1, 'c':0}, function(value) value)"));
    }

    @Test
    void test_endsWith() {
        assertEquals("true", transform("ds.endsWith('Hello world', 'World')"));
    }

    @Test
    void test_filter() {
        assertEquals("[3,4,5]", transform("ds.filter([0,1,2,3,4,5], function(value) value >= 3)"));
        assertEquals("[3,4,5]", transform("ds.filter([0,1,2,3,4,5], function(value,index) index >= 3)"));
    }

    @Test
    void test_filterObject() {
        assertEquals("{a:1}", transform("ds.filterObject({'a': 1, 'b': 2}, function(value,key,index) index == 0)"));
        assertEquals("{}", transform("ds.filterObject({'a': 1, 'b': 2}, function(value) value ==0)"));
        assertEquals("{a:1}", transform("ds.filterObject({'a': 1, 'b': 2}, function(value,key) key == 'a' )"));
    }

    @Test
    void test_find() {
        assertEquals("[1,4]", transform("ds.indicesOf([1,2,3,4,2,5], 2)"));
        assertEquals("[0,2]", transform("ds.indicesOf('aba', 'a')"));
        assertEquals("[2,8]", transform("ds.indicesOf('I heart DataWeave', '\\\\w*ea\\\\w*(\\\\b)')"));
        // TODO Regex version may need work, doesnt seem to be 1:1 with DW
    }

    @Test
    void test_flatMap() {
        assertEquals("[3,5,1,2,5]", transform("ds.flatMap([[3,5],[1,2,5]], function(value) value)"));
        assertEquals("[3,6,1,3,7]", transform("ds.flatMap([[3,5],[1,2,5]], function(value,index) value + index)"));
    }

    @Test
    void test_flatten() {
        assertEquals("[0,0,1,1,2,3,5,8]", transform("ds.flatten([ [0.0, 0], [1,1], [2,3], [5,8] ])"));
        assertEquals("[null]", transform("ds.flatten([[null]])"));
        assertEquals("[null,null,null]", transform("ds.flatten([[null, null],[null]])"));
    }


    @Test
    void test_groupBy() {
        Mapper mapper = new Mapper("ds.groupBy([   " +
                "   { 'name': 'Foo', 'language': 'Java' }," +
                "   { 'name': 'Bar', 'language': 'Scala' }," +
                "   { 'name': 'FooBar', 'language': 'Java' }], function(item) item.language)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{Java:[{name:Foo,language:Java},{name:FooBar,language:Java}],Scala:[{name:Bar,language:Scala}]}", value);
        mapper = new Mapper("ds.groupBy([   " +
                "   { 'name': 'Foo', 'language': 'Java' }," +
                "   { 'name': 'Bar', 'language': 'Scala' }," +
                "   { 'name': 'FooBar', 'language': 'Java' }], function(item, index) std.toString(index))");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{0:[{name:Foo,language:Java}],1:[{name:Bar,language:Scala}],2:[{name:FooBar,language:Java}]}", value);
        assertEquals("{b:{a:b,e:b},d:{c:d}}", transform("ds.groupBy({ 'a' : 'b', 'c' : 'd', 'e': 'b'}, function(value) value)"));
        assertEquals("{a:{a:b},c:{c:d},e:{e:b}}", transform("ds.groupBy({ 'a' : 'b', 'c' : 'd', 'e': 'b'}, function(value,key) key)"));
        //string cast validation
        assertEquals("{1:{a:1},2:{c:2},3:{e:3}}", transform("ds.groupBy({ 'a':1, 'c' :2, 'e':3}, function(value) value)"));
    }

    @Test
    void test_isBlank() {
        assertEquals("true", transform("ds.isBlank(null)"));
        assertEquals("true", transform("ds.isBlank('      ')"));
    }

    @Test
    void test_isDecimal() {
        assertEquals("true", transform("ds.isDecimal(1.1)"));
        assertEquals("false", transform("ds.isDecimal(0.0)"));
    }

    @Test
    void test_isEmpty() {
        assertEquals("true", transform("ds.isEmpty(null)"));
        assertEquals("true", transform("ds.isEmpty([])"));
        assertEquals("false", transform("ds.isEmpty([1,2])"));
        assertEquals("true", transform("ds.isEmpty('')"));
        assertEquals("false", transform("ds.isEmpty('  ')"));
        assertEquals("true", transform("ds.isEmpty({})"));
        assertEquals("false", transform("ds.isEmpty({'a':1})"));
    }

    @Test
    void test_isEven() {
        assertEquals("true", transform("ds.isEven(2)"));
        assertEquals("false", transform("ds.isEven(3)"));
    }

    @Test
    void test_isInteger() {
        assertEquals("false", transform("ds.isInteger(1.5)"));
        assertEquals("true", transform("ds.isInteger(1.0)"));
        assertEquals("false", transform("ds.isInteger(1.9)"));
    }

    @Test
    void test_isOdd() {
        assertEquals("true", transform("ds.isOdd(1)"));
        assertEquals("false", transform("ds.isOdd(2)"));
    }

    @Test
    void test_joinBy() {
        assertEquals("1-2-3.5", transform("ds.join([1.0,2,3.5], '-')"));
        assertEquals("a-b-c", transform("ds.join(['a','b','c'], '-')"));
        assertEquals("a-b-c", transform("ds.join(['a','b','c'], '-')"));
        assertEquals("true-false-true", transform("ds.join([true,false,true], '-')"));
    }

    @Test
    void testObjects_keysOf() {
		assertEquals("[a,b]", transform("ds.keysOf({ 'a' : true, 'b' : 1})"));
    }

    @Test
    void test_lower() {
        assertEquals("hello world", transform("ds.lower('Hello World')"));
    }

    @Test
    void test_map() {
        assertEquals("[{obj:1},{obj:3}]", transform("ds.map([1,2], function(item,index) {'obj': item+index})"));
        assertEquals("[1,2]", transform("ds.map([1,2], function(item) item)"));
    }

    @Test
    void test_mapObject() {
        assertEquals("{b:{a:0},d:{c:1}}", transform("ds.mapObject({'a':'b','c':'d'}, function(value,key,index) { [value] : { [key]: index} } )"));
        assertEquals("{basic:14.99,premium:58,vip:403.99}", transform("ds.mapObject({'basic': 9.99, 'premium': 53, 'vip': 398.99}, function(value,key) {[key]: (value + 5)} )"));
        assertEquals("{value:398.99}", transform("ds.mapObject({'basic': 9.99, 'premium': 53, 'vip': 398.99}, function(value) {'value': value} )"));
    }

    @Test
    void test_match() {
        assertEquals("[me@mulesoft.com,me,mulesoft]", transform("ds.strings.match('me@mulesoft.com', '([a-z]*)@([a-z]*).com')"));
    }

    @Test
    void test_matches() {
        assertEquals("true", transform("ds.strings.matches('admin123', 'a.*\\\\d+')"));
        assertEquals("false", transform("ds.strings.matches('admin123', 'b.*\\\\d+')"));
    }

    @Test
    void test_max() {
        assertEquals("33", transform("ds.max([1,2,5,33,9])"));
        assertEquals("d", transform("ds.max(['a','b','d','c'])"));
        assertEquals("true", transform("ds.max([true,false])"));
    }

    @Test
    void test_maxBy() {
        assertEquals("33", transform("ds.maxBy([1,2,5,33,9], function(item) item)"));
        assertEquals("b", transform("ds.maxBy(['a','b'], function(item) item)"));
        assertEquals("true", transform("ds.maxBy([true,false], function(item) item)"));
        assertEquals("{a:3}", transform("ds.maxBy([ { 'a' : 1 }, { 'a' : 3 }, { 'a' : 2 } ], function(item) item.a)"));
    }

    @Test
    void test_min() {
        assertEquals("1", transform("ds.min([1,2,3,4,5])"));
        assertEquals("a", transform("ds.min(['a','b'])"));
        assertEquals("false", transform("ds.min([true,false])"));
    }

    @Test
    void test_minBy() {
        assertEquals("1", transform("ds.minBy([1,2,3,4,5], function(item) item)"));
        assertEquals("a", transform("ds.minBy(['a','b'], function(item) item)"));
        assertEquals("false", transform("ds.minBy([true,false], function(item) item)"));
        assertEquals("{a:1}", transform("ds.minBy([ { 'a' : 1 }, { 'a' : 3 }, { 'a' : 2 } ], function(item) item.a)"));
    }

    @Test
    void test_orderBy() {
        assertEquals("[0,1,1,2,3,5]", transform("ds.orderBy([0,5,1,3,2,1], function(item) item)"));
        assertEquals("[0,5,1,3,2,1]", transform("ds.orderBy([0,5,1,3,2,1], function(item,ind) ind)"));
        assertEquals("[a,b]", transform("ds.orderBy(['b','a'], function(item) item)"));
        assertEquals("[{letter:d},{letter:e}]", transform("ds.orderBy([{ letter: 'e' }, { letter: 'd' }], function(item) item.letter)"));
        assertEquals("[{letter:e},{letter:d}]", transform("ds.orderBy([{ letter: 'e' }, { letter: 'd' }], function(item,ind) ind)"));
        assertEquals("{a:5,c:4,d:3,e:2,z:1}", transform("ds.orderBy({d:3,a:5,e:2,z:1,c:4}, function(value,key) key)"));
        assertEquals("{z:1,d:3,c:4,a:5,e:20}", transform("ds.orderBy({d:3,a:5,e:20,z:1,c:4}, function(value) value)"));
        assertEquals("{z:1,d:3,c:4,a:5,e:20}", transform("ds.orderBy({d:3,a:5,e:20,z:1,c:4}, function(value,key) value)"));
    }

    @Test
    void test_mapEntries() {
        assertEquals("[0,1]", transform("ds.mapEntries({'a':'b','c':'d'}, function(value,key,index) index )"));
        assertEquals("[{b:{a:0}},{d:{c:1}}]", transform("ds.mapEntries({'a':'b','c':'d'}, function(value,key,index) { [value] : { [key]: index} }\n)"));
    }

    @Disabled
    @Test
    void test_read() {
        assertEquals("15", transform("ds.read()"));
    }

    @Test
    void test_readUrl() {
        assertEquals("1", transform("ds.readUrl('https://jsonplaceholder.typicode.com/posts/1').id"));
        assertEquals("Hello World!", transform("ds.readUrl('classpath://readUrlTest.json').message"));
    }

    @Test
    void test_foldLeft() {
        assertEquals("5", transform("ds.foldLeft([2,3], 0, function(acc,it) it+acc)"));
        assertEquals("10", transform("ds.foldLeft([1,2,3,4], 0, function(acc,it) acc+it)"));
        assertEquals("1234", transform("ds.foldLeft([1,2,3,4],'', function(acc,it) acc+''+it)"));
        assertEquals("null", transform("ds.foldLeft([], null, function(acc,it) acc+it)"));
    }

    @Test
    void test_replace() {
        assertEquals("7890", transform("ds.replace('123-456-7890', '.*-', '')"));
        assertEquals("a-c-2-d-f", transform("ds.replace('abc123def', '[b13e]', '-')"));
        assertEquals("adminID", transform("ds.replace('admin123', '123', 'ID')"));
    }

    @Test
    void test_scan() {
        assertEquals("[[anypt@mulesoft.com,anypt,mulesoft],[max@mulesoft.com,max,mulesoft]]", transform("ds.strings.scan('anypt@mulesoft.com,max@mulesoft.com', '([a-z]*)@([a-z]*).com')"));
    }

    @Test
    void test_sizeOf() {
        assertEquals("5", transform("ds.sizeOf([1,2,3,4,5])"));
        assertEquals("5", transform("ds.sizeOf('Hello')"));
        assertEquals("1", transform("ds.sizeOf({'a':0})"));
    }

    @Test
    void test_splitBy() {
        String input = "{" +
                "'split1': " + "ds.split('a-b-c','^*.b.')," +
                "'split2': " + "ds.split('hello world','\\\\s')," +
                "'split3': " + "ds.split('no match','^s')," +
                "'split4': " + "ds.split('no match','^n..')," +
                "'split5': " + "ds.split('a1b2c3d4A1B2C3D','^*[0-9A-Z]')," +
                "'split6': " + "ds.split('a-b-c','-')," +
                "'split7': " + "ds.split('hello world','')," +
                "'split8': " + "ds.split('first,middle,last',',')," +
                "'split9': " + "ds.split('no split','NO')" +
                "}";
        String comparison = "{split1:[a,c]," +
                "split2:[hello,world]," +
                "split3:[no match]," +
                "split4:[,match]," +
                "split5:[a,b,c,d]," +
                "split6:[a,b,c]," +
                "split7:[h,e,l,l,o, ,w,o,r,l,d]," +
                "split8:[first,middle,last]," +
                "split9:[no split]}";
        assertEquals(comparison, transform(input));
    }

    @Test
    void test_startsWith() {
        assertEquals("true", transform("ds.startsWith('Hello World', 'Hello')"));
    }

    @Test
    void test_range() {
        assertEquals("[0,1,2,3]", transform("ds.range(0, 3)"));
    }

    @Test
    void test_toString() {
        assertEquals("5", transform("ds.stringOf(5)"));
        assertEquals("true", transform("ds.stringOf(true)"));
        assertEquals("null", transform("ds.stringOf(null)"));
    }

    @Test
    void test_trim() {
        assertEquals("Hello     World", transform("ds.trim('  Hello     World     ')"));
    }

    @Test
    void test_typeOf() {
        assertEquals("array", transform("ds.typeOf([])"));
        assertEquals("object", transform("ds.typeOf({})"));
        assertEquals("string", transform("ds.typeOf('')"));
        assertEquals("function", transform("ds.typeOf(function(x) x)"));
        assertEquals("number", transform("ds.typeOf(0)"));
    }

    @Test
    void test_unzip() {
        assertEquals("[[0,1,2,3],[a,b,c,d],[c,c,c,c]]", transform("ds.unzip([ [0,'a','c'], [1,'b','c'], [2,'c','c'],[ 3,'d','c'] ])"));
    }

    @Test
    void test_upper() {
        assertEquals("HELLO WORLD", transform("ds.upper('HeLlO WoRlD')"));
    }

    @Test
    void test_uuid() {
        assertEquals(5, transform("ds.uuid()").split("-").length);
    }

    @Test
    void test_valuesOf() {
        assertEquals("[true,1,[],d]", transform("ds.valuesOf({ 'a' : true, 'b' : 1, 'c':[], 'd':'d'})"));
    }

    @Test
    void test_zip() {
        assertEquals("[[1,a],[2,b]]", transform("ds.zip([1,2,3,4,5], ['a','b'])"));
    }

    @Test
    void test_remove() {
        assertEquals("[2]", transform("ds.rmWhereEq([1,2,1],1)"));
        assertEquals("{b:2}", transform("ds.rm({a:1,b:2},'a')"));
    }

    @Test
    void test_removeAll() {
        assertEquals("[2]", transform("ds.rmWhereIn([1,2,1],[1,3])"));
        assertEquals("{b:2}", transform("ds.rmAll({a:1,b:2,c:3},['a','c'])"));
    }

    @Test
    void test_append() {
        assertEquals("[1,2,3,4]", transform("ds.append([1,2,3],4)"));
    }

    @Test
    void test_prepend() {
        assertEquals("[4,1,2,3]", transform("ds.prepend([1,2,3],4)"));
    }

    @Test
    void test_reverse() {
        assertEquals("{second:2,first:1}", transform("ds.reverse({first: '1', second: '2'})"));
        assertEquals("[4,3,2,1]", transform("ds.reverse([1,2,3,4])"));
        assertEquals("olleH", transform("ds.reverse('Hello')"));
    }

    @Test
    void testObjects_valuesOf() {
        assertEquals("[true,1,[],d]", transform("ds.valuesOf({ 'a' : true, 'b' : 1, 'c':[], 'd':'d'})"));
    }
}
