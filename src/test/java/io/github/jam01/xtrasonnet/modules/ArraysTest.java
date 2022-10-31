package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArraysTest {
    @Test
    public void all() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.arrays.all([1, 2, 3], function(item) item > 0)"));
    }

    @Test
    public void any() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.arrays.any([1, 2, 3], function(item) item > 1)"));
    }

    @Test
    public void countBy() {
        Assertions.assertEquals(TestUtils.transform("1"), TestUtils.transform("xtr.arrays.countBy([1, 2, 3], function(item) item > 2)"));
    }

    @Test
    public void flat() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, '3', 4, {}, 5, 6]"), TestUtils.transform("xtr.arrays.flat([[1, 2], '3', [4, {}, [5, 6]]])"));
    }

    @Test
    public void distinctBy() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3]"), TestUtils.transform("xtr.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item) item % 3)"));
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3, 4, 5, 6]"), TestUtils.transform("xtr.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item, idx) item % (3 * idx))"));
    }

    @Test
    public void drop() {
        Assertions.assertEquals(TestUtils.transform("[4, 5]"), TestUtils.transform("xtr.arrays.drop([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void dropWhile() {
        Assertions.assertEquals(TestUtils.transform("[4, 5]"), TestUtils.transform("xtr.arrays.dropWhile([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void duplicatesBy() {
        Assertions.assertEquals(TestUtils.transform("[1, 2]"), TestUtils.transform("xtr.arrays.duplicatesBy([1, 2, 3, 1, 2], function(item) item)"));
    }

    @Test
    public void find() {
        Assertions.assertEquals(TestUtils.transform("[4]"), TestUtils.transform("xtr.arrays.find([1, 2, 3, 4, 5], function(item) item * 3 > 10)"));
        Assertions.assertEquals(TestUtils.transform("[3]"), TestUtils.transform("xtr.arrays.find([1, 2, 3, 4, 5], function(item, idx) item * (3 + idx) > 10)"));
    }

    @Test
    public void indexWhere() {
        Assertions.assertEquals(TestUtils.transform("0"), TestUtils.transform("xtr.arrays.indexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void indicesWhere() {
        Assertions.assertEquals(TestUtils.transform("[0, 1, 2]"), TestUtils.transform("xtr.arrays.indicesWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void lastIndexWhere() {
        Assertions.assertEquals(TestUtils.transform("2"), TestUtils.transform("xtr.arrays.lastIndexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void occurrencesBy() {
        Assertions.assertEquals(TestUtils.transform("{ 'under4': 3, 'over4': 2 }"), TestUtils.transform("xtr.arrays.occurrencesBy([1, 2, 3, 4, 5], function(item) if (item) < 4 then 'under4' else 'over4')"));
    }

    @Test
    public void partition() {
        Assertions.assertEquals(TestUtils.transform("{ pass: [1, 2, 3], fail: [4, 5] }"), TestUtils.transform("xtr.arrays.partition([1, 2, 3, 4, 5], function(item) item < 4)"));
    }

    @Test
    public void splitAt() {
        Assertions.assertEquals(TestUtils.transform("{ left: [1, 2, 3], right: [4, 5] }"), TestUtils.transform("xtr.arrays.splitAt([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void chunksOf() {
        Assertions.assertEquals(TestUtils.transform("[[1, 2], [3, 4], [5]]"), TestUtils.transform("xtr.arrays.chunksOf([1, 2, 3, 4, 5], 2)"));
    }

    @Test
    public void breakk() {
        Assertions.assertEquals(TestUtils.transform("{ left: [1], right: [2, 3, 4, 5] }"), TestUtils.transform("xtr.arrays.break([1, 2, 3, 4, 5], function(item) item % 2 == 0)"));
    }

    @Test
    public void take() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3]"), TestUtils.transform("xtr.arrays.take([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void takeWhile() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3, 4]"), TestUtils.transform("xtr.arrays.takeWhile([1, 2, 3, 4, 5], function(item) item * 2 < 9)"));
    }

    @Test
    public void unzip() {
        Assertions.assertEquals(TestUtils.transform("[[1, 2, 3], ['x', 'y', 'z']]"), TestUtils.transform("xtr.arrays.unzip([[1, 'x'], [2, 'y'], [3, 'z']])"));
    }

//    @Disabled
//    @Test
//    public void unzipAll() {
//        assertEquals(transform("[[1, 2, 3], ['x', 'NA', 'z']]"), transform("xtr.arrays.unzipAll([[1, 'x'], [2], [3, 'z']], 'NA')"));
//    }

    @Test
    public void zip() {
        Assertions.assertEquals(TestUtils.transform("[[1, 'x'], [2, 'y'], [3, 'z']]"), TestUtils.transform("xtr.arrays.zip([1, 2, 3], ['x', 'y', 'z'])"));
    }

//    @Disabled
//    @Test
//    public void zipAll() {
//        assertEquals(transform("[[1, 'x'], [2, 'y'], [3, 'NA']]"), transform("txtr.arrays.zipAll([[1, 2, 3], ['x', 'y']], 'NA')"));
//    }
}
