package com.datasonnet;/*-
 * Copyright 2019-2021 the original author or authors.
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

public class ArraysTest {
    @Test
    void testArrays_countBy() {
        assertEquals("3", transform("ds.arrays.countBy([1,2,3,4,5], function(it) it > 2)"));
    }

    @Test
    void testArrays_divideBy() {
        assertEquals("[[1,2],[3,4],[5]]", transform("ds.arrays.splitEvery([1,2,3,4,5], 2)"));
        assertEquals("[[1,2,3],[4,5]]", transform("ds.arrays.splitEvery([1,2,3,4,5], 3)"));
    }

    @Test
    void testArrays_drop() {
        assertEquals("[3,4,5]", transform("ds.arrays.drop([1,2,3,4,5], 2)"));
        assertEquals("[2,3,4,5]", transform("ds.arrays.drop([1,2,3,4,5], 1)"));
        assertEquals("[]", transform("ds.arrays.drop([1,2,3,4,5], 10)"));
    }

    @Test
    void testArrays_dropWhile() {
        assertEquals("[3,4,5]", transform("ds.arrays.dropWhile([1,2,3,4,5], function(item) item < 3)"));
    }

    @Test
    void testArrays_duplicates() {
        assertEquals("[1,2,3]", transform("ds.arrays.duplicates([1,2,3,4,5,3,2,1])"));
    }

    @Test
    void testArrays_every() {
        assertEquals("true", transform("ds.arrays.all([1,1,1], function(item) item == 1)"));
        assertEquals("false", transform("ds.arrays.all([1,2,1], function(item) item == 1)"));
    }

    @Test
    void testArrays_firstWith() {
        assertEquals("2", transform("ds.arrays.find([1,2,3], function(item) (item % 2) == 0)"));
        assertEquals("null", transform("ds.arrays.find([1,2,3], function(item) (item % 10) == 0)"));
    }

    @Test
    void testArrays_deepFlatten() {
        assertEquals("[1,2,3,1,2,null,a]", transform("ds.arrays.deepFlatten([[1,2,3,[1,2]], [null,'a']])"));
    }

    @Test
    void testArrays_indexOf() {
        assertEquals("2", transform("ds.arrays.indexOf([1,2,3,4,5,3], 3)"));
        assertEquals("2", transform("ds.arrays.indexOf(['Mariano', 'Leandro', 'Julian', 'Julian'], 'Julian')"));
        assertEquals("1", transform("ds.arrays.indexOf([1,2,3], 2)"));
        assertEquals("-1", transform("ds.arrays.indexOf([1,2,3], 5)"));
        assertEquals("1", transform("ds.arrays.indexOf([1,2,3,2], 2)"));
        assertEquals("2", transform("ds.arrays.indexOf('Hello', 'l')"));
        assertEquals("-1", transform("ds.arrays.indexOf('Hello', 'x')"));
    }

    @Test
    void testArrays_indexWhere() {
        assertEquals("2", transform("ds.arrays.indexWhere([1,2,3,4,5,3], function(item) item == 3)"));
        assertEquals("2", transform("ds.arrays.indexWhere(['Mariano', 'Leandro', 'Julian', 'Julian'], function(item) item == 'Julian')"));
    }

    @Test
    void testArrays_join() {
        assertEquals("[{l:{id:1,v:a},r:{id:1,v:c}},{l:{id:1,v:b},r:{id:1,v:c}}]",
                transform("ds.arrays.innerJoin([{'id':1,'v':'a'},{'id':1,'v':'b'}],[{'id':1,'v':'c'}], function(item) item.id, function(item) item.id)"));
    }

    @Test
    void testArrays_lastIndexOf() {
        assertEquals("1", transform("ds.arrays.lastIndexOf([1,2,3], 2)"));
        assertEquals("-1", transform("ds.arrays.lastIndexOf([1,2,3], 5)"));
        assertEquals("3", transform("ds.arrays.lastIndexOf([1,2,3,2], 2)"));
        assertEquals("3", transform("ds.arrays.lastIndexOf('Hello', 'l')"));
        assertEquals("-1", transform("ds.arrays.lastIndexOf('Hello', 'x')"));
    }

    @Test
    void testArrays_leftJoin() {
        assertEquals("[{l:{id:1,v:a},r:{id:1,v:c}},{l:{id:1,v:b},r:{id:1,v:c}},{l:{id:2,v:d}}]",
                transform("ds.arrays.leftJoin([{'id':1,'v':'a'},{'id':1,'v':'b'},{'id':2,'v':'d'}],[{'id':1,'v':'c'},{'id':3,'v':'e'}], function(item) item.id,function(item) item.id)"));
    }

    @Test
    void testArrays_occurrences() {
        assertEquals("{1:2,2:2,3:2,4:1,6:1}", transform("ds.arrays.occurrences([1,2,3,4,3,2,1,6], function(item) item)"));
    }

    @Test
    void testArrays_outerJoin() {
        assertEquals("[{l:{id:1,v:a},r:{id:1,v:c}},{l:{id:1,v:b},r:{id:1,v:c}},{l:{id:2,v:d}},{r:{id:3,v:e}}]",
                transform("ds.arrays.rightJoin([{'id':1,'v':'a'},{'id':1,'v':'b'},{'id':2,'v':'d'}],[{'id':1,'v':'c'},{'id':3,'v':'e'}], function(item) item.id,function(item) item.id)"));
    }

    @Test
    void testArrays_partition() {
        assertEquals("{success:[0,2,4],failure:[1,3,5]}", transform("ds.arrays.partition([0,1,2,3,4,5], function(item) ((item % 2) ==0) )"));
    }

    @Test
    void testArrays_slice() {
        long start = System.currentTimeMillis();
        assertEquals("[1,2,3,4]", transform("ds.arrays.slice([0,1,2,3,4,5], 1, 5)"));
        assertEquals("[1,2,3,3]", transform("ds.arrays.slice([0,1,2,3,3,3], 1, 5)"));
    }

    @Test
    void testArrays_some() {
        assertEquals("true", transform("ds.arrays.any([1,2,3], function(item) (item % 2) == 0)"));
        assertEquals("true", transform("ds.arrays.any([1,2,3], function(item) (item % 2) == 1)"));
        assertEquals("true", transform("ds.arrays.any([1,2,3], function(item) item == 3)"));
        assertEquals("false", transform("ds.arrays.any([1,2,3], function(item) item == 4)"));
    }

    @Test
    void testArrays_splitAt() {
        assertEquals("{l:[A,B],r:[C]}", transform("ds.arrays.splitAt(['A','B','C'], 2)"));
        assertEquals("{l:[A],r:[B,C]}", transform("ds.arrays.splitAt(['A','B','C'], 1)"));
    }

    @Test
    void testArrays_splitWhere() {
        assertEquals("{l:[A],r:[B,C,D]}", transform("ds.arrays.splitWhere(['A','B','C','D'], function(item) item=='B')"));
        assertEquals("{l:[A,B],r:[C,D]}", transform("ds.arrays.splitWhere(['A','B','C','D'], function(item) item=='C')"));
    }

    @Test
    void testArrays_sumBy() {
        assertEquals("6", transform("ds.arrays.sumBy([{a:1},{a:2},{a:3}], function(item) item.a)"));
    }

    @Test
    void testArrays_take() {
        assertEquals("[A,B]", transform("ds.arrays.take(['A','B','C'], 2)"));
    }

    @Test
    void testArrays_takeWhile() {
        assertEquals("[0,1]", transform("ds.arrays.takeWhile([0,1,2,1], function(item) item <= 1)"));
    }
}