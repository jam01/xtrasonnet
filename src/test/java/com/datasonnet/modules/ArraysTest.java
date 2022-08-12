package com.datasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArraysTest {
    @Test
    public void all() {
        assertEquals(transform("true"), transform("tro.arrays.all([1, 2, 3], function(item) item > 0)"));
    }

    @Test
    public void any() {
        assertEquals(transform("true"), transform("tro.arrays.any([1, 2, 3], function(item) item > 1)"));
    }

    @Test
    public void countBy() {
        assertEquals(transform("1"), transform("tro.arrays.countBy([1, 2, 3], function(item) item > 2)"));
    }

    @Test
    public void deepFlatten() {
        assertEquals(transform("[1, 2, '3', 4, {}, 5, 6]"), transform("tro.arrays.deepFlatten([[1, 2], '3', [4, {}, [5, 6]]])"));
    }

    @Test
    public void distinctBy() {
        assertEquals(transform("[1, 2, 3]"), transform("tro.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item) item % 3)"));
        assertEquals(transform("[1, 2, 3, 4, 5, 6]"), transform("tro.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item, idx) item % (3 * idx))"));
    }

    @Test
    public void drop() {
        assertEquals(transform("[4, 5]"), transform("tro.arrays.drop([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void dropWhile() {
        assertEquals(transform("[4, 5]"), transform("tro.arrays.dropWhile([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void duplicates() {
        assertEquals(transform("[1, 2]"), transform("tro.arrays.duplicates([1, 2, 3, 1, 2])"));
    }

    @Test
    public void find() {
        assertEquals(transform("[4]"), transform("tro.arrays.find([1, 2, 3, 4, 5], function(item) item * 3 > 10)"));
        assertEquals(transform("[3]"), transform("tro.arrays.find([1, 2, 3, 4, 5], function(item, idx) item * (3 + idx) > 10)"));
    }

    @Test
    public void indexWhere() {
        assertEquals(transform("0"), transform("tro.arrays.indexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Disabled
    @Test
    public void indicesWhere() {
        assertEquals(transform("[1, 2, 3]"), transform("tro.arrays.indicesWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Disabled
    @Test
    public void lastIndexWhere() {
        assertEquals(transform("3"), transform("tro.arrays.lastIndexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)"));
    }

    @Test
    public void occurrencesBy() {
        assertEquals(transform("{ 'under4': 3, 'over4': 2 }"), transform("tro.arrays.occurrencesBy([1, 2, 3, 4, 5], function(item) if (item) < 4 then 'under4' else 'over4')"));
    }

    @Test
    public void partition() {
        assertEquals(transform("{ success: [1, 2, 3], failure: [4, 5] }"), transform("tro.arrays.partition([1, 2, 3, 4, 5], function(item) item < 4)"));
    }

    @Test
    public void splitAt() {
        assertEquals(transform("{ l: [1, 2, 3], r: [4, 5] }"), transform("tro.arrays.splitAt([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void splitEvery() {
        assertEquals(transform("[[1, 2], [3, 4], [5]]"), transform("tro.arrays.splitEvery([1, 2, 3, 4, 5], 2)"));
    }

    @Test
    public void splitWhere() {
        assertEquals(transform("{ l: [1], r: [2, 3, 4, 5] }"), transform("tro.arrays.splitWhere([1, 2, 3, 4, 5], function(item) item % 2 == 0)"));
    }

    @Test
    public void take() {
        assertEquals(transform("[1, 2, 3]"), transform("tro.arrays.take([1, 2, 3, 4, 5], 3)"));
    }

    @Test
    public void takeWhile() {
        assertEquals(transform("[1, 2, 3, 4]"), transform("tro.arrays.takeWhile([1, 2, 3, 4, 5], function(item) item * 2 < 9)"));
    }

    @Disabled
    @Test
    public void unzip() {
        assertEquals(transform("[[1, 2, 3], ['x', 'y', 'z']]"), transform("tro.arrays.unzip([[1, 'x'], [2, 'y'], [3, 'z']])"));
    }

    @Disabled
    @Test
    public void unzipAll() {
        assertEquals(transform("[[1, 2, 3], ['x', 'NA', 'z']]"), transform("tro.arrays.unzipAll([[1, 'x'], [2], [3, 'z']], 'NA')"));
    }

    @Disabled
    @Test
    public void zip() {
        assertEquals(transform("[[1, 'x'], [2, 'y'], [3, 'z']]"), transform("tro.arrays.zip([[1, 2, 3], ['x', 'y', 'z']])"));
    }

    @Disabled
    @Test
    public void zipAll() {
        assertEquals(transform("[[1, 'x'], [2, 'y'], [3, 'NA']]"), transform("ttro.arrays.zipAll([[1, 2, 3], ['x', 'y']], 'NA')"));
    }
}
