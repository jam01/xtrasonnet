package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Root {

    @Test
    public void contains() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.contains([1, 2, 3], 1)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.contains({ scala: '3.1.3' }, 'scala')"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.contains('Lorem ipsum', 'Lorem')"));
    }

    @Test
    public void endsWith() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.endsWith('Lorem ipsum', 'ipsum')"));
    }

    @Test
    public void entriesOf() {
        Assertions.assertEquals(TestUtils.transform("""
                [
                  {
                    key: 'scala',
                    value: '3.1.3'
                  },
                  {
                    key: 'java',
                    value: '19'
                  }
                ]"""), TestUtils.transform("xtr.entriesOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void filter() {
        Assertions.assertEquals(TestUtils.transform("[1, 2]"), TestUtils.transform("xtr.filter([1, 2, 3, 4], function(item) item < 3)"));
        Assertions.assertEquals(TestUtils.transform("[4]"), TestUtils.transform("xtr.filter([1, 2, 3, 4], function(item, idx) idx > 2)"));
    }

    @Test
    public void filterNotEq() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 4, 4]"), TestUtils.transform("xtr.filterNotEq([1, 2, 3, 4, 3, 4], 3)"));
    }

    @Test
    public void filterNotIn() {
        Assertions.assertEquals(TestUtils.transform("[1, 2]"), TestUtils.transform("xtr.filterNotIn([1, 2, 3, 4, 3, 4], [3, 4])"));
    }

    @Test
    public void filterObject() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true }
                }"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang) lang.isJvm)"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    scala: { version: '3.1.3', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                }"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang, name) !lang.isJvm || name == 'scala')"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    scala: { version: '3.1.3', isJvm: true},
                    python: { version: '3.10.4', isJvm: false}
                }"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang, name, idx) idx == 0 || name == 'python')"""));
    }

    @Test
    public void flatMap() {
        Assertions.assertEquals(TestUtils.transform("[1, 1, 3, 9, 5, 25]"), TestUtils.transform("xtr.flatMap([1, 3, 5], function(item) [item, item * item])"));
        Assertions.assertEquals(TestUtils.transform("[1, 0, 3, 3, 5, 10]"), TestUtils.transform("xtr.flatMap([1, 3, 5], function(item, idx) [item, item * idx])"));
    }

    @Test
    public void flatMapObject() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                    java: 5,
                    'unit-testing': 2,
                    github: 4,
                    containers: 5,
                    kubernetes: 2,
                    jenkins: 4
                }"""), TestUtils.transform("""
                local candidateReqs = {
                    req1: { skillsType: 'dev', required: ['java'], preferred: ['unit-testing'] },
                    req2: { skillsType: 'ops', required: ['containers'], preferred: ['kubernetes'] }
                };
                local reqsWeight(req) = {
                    [req.required[0]]: 5,
                    [req.preferred[0]]: 2,
                    [if req.skillsType == 'dev' then 'github']: 4,
                    [if req.skillsType == 'ops' then 'jenkins']: 4
                };

                xtr.flatMapObject(candidateReqs, reqsWeight)"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    java: 5,
                    'unit-testing': 2,
                    github: 4,
                    containers: 5,
                    kubernetes: 2,
                    jenkins: 4
                }"""), TestUtils.transform("""
                local candidateReqs = {
                    dev: { required: ['java'], preferred: ['unit-testing'] },
                    ops: { required: ['containers'], preferred: ['kubernetes'] }
                };
                local reqsWeight(req, type) = {
                    [req.required[0]]: 5,
                    [req.preferred[0]]: 2,
                    [if type == 'dev' then 'github']: 4,
                    [if type == 'ops' then 'jenkins']: 4
                };

                xtr.flatMapObject(candidateReqs, reqsWeight)"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    java: 5,
                    'unit-testing': 3,
                    github: 4,
                    containers: 5,
                    kubernetes: 1,
                    jenkins: 2
                }"""), TestUtils.transform("""
                local candidateReqs = {
                    dev: { required: ['java'], preferred: ['unit-testing'] },
                    ops: { required: ['containers'], preferred: ['kubernetes'] }
                };
                local reqsWeight(req, type, idx) = {
                    [req.required[0]]: 5,
                    [req.preferred[0]]: if idx == 0 then 3 else 1,
                    [if type == 'dev' then 'github']: if idx == 0 then 4 else 2,
                    [if type == 'ops' then 'jenkins']: if idx == 0 then 4 else 2
                };

                xtr.flatMapObject(candidateReqs, reqsWeight)"""));
    }

    @Test
    public void groupBy() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3]"), TestUtils.transform("xtr.flatten([[1, 2], [3]])"));
        Assertions.assertEquals(TestUtils.transform("6"), TestUtils.transform("xtr.foldLeft([1, 2, 3], 0, function(item, acc) item + acc)"));
        Assertions.assertEquals(TestUtils.transform("' dolor ipsum Lorem'"), TestUtils.transform("xtr.foldRight(['Lorem', 'ipsum', 'dolor'], '', function(item, acc) acc + ' ' + item)"));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    jvmLangs: [
                        { name: 'scala', version: '3.1.3', isJvm: true },
                        { name: 'java', version: '19', isJvm: true }
                    ],
                    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
                }"""), TestUtils.transform("""
                local languages = [
                    { name: 'scala', version: '3.1.3', isJvm: true },
                    { name: 'java', version: '19', isJvm: true },
                    { name: 'python', version: '3.10.4', isJvm: false }
                ];

                xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    preferred: [{ name: 'scala', version: '3.1.3', isJvm: true }],
                    jvmLangs: [{ name: 'java', version: '19', isJvm: true }],
                    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
                }"""), TestUtils.transform("""
                local languages = [
                    { name: 'scala', version: '3.1.3', isJvm: true },
                    { name: 'java', version: '19', isJvm: true },
                    { name: 'python', version: '3.10.4', isJvm: false }
                ];
                local langFunc(lang, idx) = if idx == 0 then 'preferred'
                    else if lang.isJvm then 'jvmLangs'
                    else 'others';

                xtr.groupBy(languages, langFunc)"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    jvmLangs: {
                        scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                        java: { version: '19', isJvm: true, project: 'jdk.java.net' }
                    },
                    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
                }"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
                    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
                };

                xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')"""));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    preferred: { scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' }},
                    jvmLangs: { java: { version: '19', isJvm: true, project: 'jdk.java.net' }},
                    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
                }"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
                    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
                };
                local langFunc(lang, name) = if name == 'scala' then 'preferred'
                    else if lang.isJvm then 'jvmLangs'
                    else 'others';

                xtr.groupBy(languages, langFunc)"""));
    }

    @Test
    public void indicesOf() {
        Assertions.assertEquals(TestUtils.transform("[1, 4]"), TestUtils.transform("xtr.indicesOf([1, 7, 3, 4, 7], 7)"));
        Assertions.assertEquals(TestUtils.transform("[0, 14]"), TestUtils.transform("xtr.indicesOf('lorem ipsum dolor', 'lo')"));
    }

    @Test
    public void isType() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isArray([1, 2])"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isBoolean(false)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isDecimal(2.5)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("""
                local increment(item) = item + 1;

                xtr.isFunction(increment)"""));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isInteger(2)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isNumber(2)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isObject({})"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isString('Lorem')"));
    }

    @Test
    public void isBlank() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isBlank('   ')"));
    }

    @Test
    public void isEmpty() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isEmpty([])"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isEmpty({})"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isEmpty('')"));
    }

    @Test
    public void numIsCondition() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isEven(2)"));
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.isOdd(1)"));
    }

    @Test
    public void join() {
        Assertions.assertEquals(TestUtils.transform("'0, 1, 1, 2, 3, 5, 8'"), TestUtils.transform("xtr.join([0, 1, 1, 2, 3, 5, 8], ', ')"));
        Assertions.assertEquals(TestUtils.transform("'hello world !'"), TestUtils.transform("xtr.join(['hello', 'world', '!'], ' ')"));
    }

    @Test
    public void keysOf() {
        Assertions.assertEquals(TestUtils.transform("['scala', 'java']"), TestUtils.transform("xtr.keysOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void lower() {
        Assertions.assertEquals(TestUtils.transform("'hello world!'"), TestUtils.transform("xtr.lower('Hello World!')"));
    }

    @Test
    public void map() {
        Assertions.assertEquals(TestUtils.transform("[1, 4, 9, 16]"), TestUtils.transform("xtr.map([1, 2, 3, 4], function(item) item * item)"));
        Assertions.assertEquals(TestUtils.transform("[0, 2, 6, 12]"), TestUtils.transform("xtr.map([1, 2, 3, 4], function(item, idx) item * idx)"));
    }

    @Test
    public void mapObject() {

    }

    @Test
    public void mapEntries() {
        Assertions.assertEquals(TestUtils.transform("['scala-lang.org', 'jdk.java.net', 'python.org']"), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
                    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
                };

                xtr.mapEntries(languages, function(lang) lang.project)"""));
        Assertions.assertEquals(TestUtils.transform("""
                [
                    { name: 'scala', version: '3.1.3' },
                    { name: 'java', version: '19' }
                ]"""), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' }
                };

                xtr.mapEntries(languages, function(lang, name) {
                    name: name,
                    version: lang.version
                })"""));
        Assertions.assertEquals(TestUtils.transform("[{ name: 'scala', preferred: true }, { name: 'java' }]"), TestUtils.transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' }
                };
                local langFunc(lang, name, idx) = {
                    name: name,
                    [if idx == 0 then 'preferred']: true
                };

                xtr.mapEntries(languages, langFunc)"""));
    }

    @Test
    public void max() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.max([false, false, true])"));
        Assertions.assertEquals(TestUtils.transform("100"), TestUtils.transform("xtr.max([0, 8, 2, 100])"));
        Assertions.assertEquals(TestUtils.transform("'zzz'"), TestUtils.transform("xtr.max(['Lorem', 'zzz', 'ipsum', 'dolor'])"));
        Assertions.assertEquals(TestUtils.transform("{ name: 'scala', version: '3.1.3', isPreferred: true }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'python', version: '3.1.14', isPreferred: false }
                    { name: 'scala', version: '3.1.3', isPreferred: true },
                ];

                xtr.maxBy(languages, function(lang) lang.isPreferred)"""));
    }

    @Test
    public void maxBy() {
        Assertions.assertEquals(TestUtils.transform("{ name: 'scala', version: '3.1.3', weight: 4 }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 2 },
                    { name: 'python', version: '3.1.14', weight: 2 }
                    { name: 'scala', version: '3.1.3', weight: 4 },
                ];

                xtr.maxBy(languages, function(lang) lang.weight)"""));
        Assertions.assertEquals(TestUtils.transform("{ name: 'scala', version: '3.1.3', code: 'S' }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ];

                xtr.maxBy(languages, function(lang) lang.code)"""));
    }

    @Test
    public void min() {
        Assertions.assertEquals(TestUtils.transform("false"), TestUtils.transform("xtr.min([false, false, true])"));
        Assertions.assertEquals(TestUtils.transform("0"), TestUtils.transform("xtr.min([0, 8, 2, 100])"));
        Assertions.assertEquals(TestUtils.transform("'AAA'"), TestUtils.transform("xtr.min(['Lorem', 'AAA', 'ipsum', 'dolor'])"));
    }

    @Test
    public void minBy() {
        Assertions.assertEquals(TestUtils.transform("{ name: 'java', version: '19', isPreferred: false }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'python', version: '3.1.14', isPreferred: false },
                    { name: 'scala', version: '3.1.3', isPreferred: true }
                ];

                xtr.minBy(languages, function(lang) lang.isPreferred)"""));
        Assertions.assertEquals(TestUtils.transform("{ name: 'java', version: '19', weight: 2 }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 2 },
                    { name: 'python', version: '3.1.14', weight: 2 },
                    { name: 'scala', version: '3.1.3', weight: 4 }
                ];

                xtr.minBy(languages, function(lang) lang.weight)"""));
        Assertions.assertEquals(TestUtils.transform("{ name: 'java', version: '19', code: 'B' }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ];

                xtr.minBy(languages, function(lang) lang.code)"""));
        Assertions.assertEquals(TestUtils.transform("{ name: 'java', version: '19', isPreferred: false }"), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'scala', version: '3.1.3', isPreferred: true },
                    { name: 'python', version: '3.1.14', isPreferred: false }
                ];

                xtr.minBy(languages, function(lang) lang.isPreferred)"""));
    }

    @Test
    public void parseNum() {
        Assertions.assertEquals(TestUtils.transform("123.45"), TestUtils.transform("xtr.parseNum('123.45')"));
    }

    @Test
    public void orderBy() {
        Assertions.assertEquals(TestUtils.transform("""
                [
                    { name: 'python', version: '3.1.14', weight: 2 },
                    { name: 'java', version: '19', weight: 3 },
                    { name: 'scala', version: '3.1.3', weight: 4 }
                ]"""), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 3 },
                    { name: 'scala', version: '3.1.3', weight: 4 },
                    { name: 'python', version: '3.1.14', weight: 2 }
                ];

                xtr.orderBy(languages, function(lang) lang.weight)"""));
        Assertions.assertEquals(TestUtils.transform("""
                [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ]"""), TestUtils.transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' },
                    { name: 'python', version: '3.1.14', code: 'B' }
                ];

                xtr.orderBy(languages, function(lang) lang.code)"""));
    }

    @Test
    public void range() {
        Assertions.assertEquals(TestUtils.transform("[1, 2, 3, 4, 5]"), TestUtils.transform("xtr.range(1, 5)"));
    }

    @Test
    public void read() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                  hello: { '$': 'world!' }
                }"""), TestUtils.transform("xtr.read('<hello>world!</hello>', 'application/xml')"));
        Assertions.assertEquals(TestUtils.transform("""
                {
                  hello: { _txt: 'world!' }
                }"""), TestUtils.transform("xtr.read('<hello>world!</hello>', 'application/xml', { textvaluekey: '_txt' })"));
    }

    @Disabled
    @Test
    public void readUrl() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                  hello: { '$': 'world!' }
                }"""), TestUtils.transform("xtr.readUrl('example.com/data', 'application/xml')"));
        Assertions.assertEquals(TestUtils.transform("""
                {
                  hello: { _txt: 'world!' }
                }"""), TestUtils.transform("xtr.readUrl('example.com', 'application/xml', { textvaluekey: '_txt' })"));
    }

    @Test
    public void rmKey() {
        Assertions.assertEquals(TestUtils.transform("{ scala: '3.1.3' }"), TestUtils.transform("xtr.rmKey({ scala: '3.1.3', java: '19' }, 'java')"));
    }

    @Test
    public void rmKeyIn() {
        Assertions.assertEquals(TestUtils.transform("{}"), TestUtils.transform("xtr.rmKeyIn({ scala: '3.1.3', java: '19' }, ['java', 'scala'])"));
    }

    @Test
    public void replace() {
        Assertions.assertEquals(TestUtils.transform("'hello, everyone!'"), TestUtils.transform("xtr.replace('hello, world!', 'world', 'everyone')"));
    }

    @Test
    public void reverse() {
        Assertions.assertEquals(TestUtils.transform("[3, 2, 1]"), TestUtils.transform("xtr.reverse([1, 2, 3])"));
        Assertions.assertEquals(TestUtils.transform("{ key2: 'value2', key1: 'value1' }"), TestUtils.transform("xtr.reverse({ key1: 'value1', key2: 'value2' })"));
        Assertions.assertEquals(TestUtils.transform("'Lorem ipsum dolor'"), TestUtils.transform("xtr.reverse('rolod muspi meroL')"));
    }

    @Test
    public void sizeOf() {
        Assertions.assertEquals(TestUtils.transform("3"), TestUtils.transform("xtr.sizeOf([1, 2, 3])"));
        Assertions.assertEquals(TestUtils.transform("2"), TestUtils.transform("""
                local add(item, item2) = item + item2;

                xtr.sizeOf(add)"""));
        Assertions.assertEquals(TestUtils.transform("1"), TestUtils.transform("xtr.sizeOf({ key: 'value' })"));
        Assertions.assertEquals(TestUtils.transform("13"), TestUtils.transform("xtr.sizeOf('hello, world!')"));
    }

    @Test
    public void split() {
        Assertions.assertEquals(TestUtils.transform("['hell', ', w', 'rld!']"), TestUtils.transform("xtr.split('hello, world!', 'o')"));
    }

    @Test
    public void startsWith() {
        Assertions.assertEquals(TestUtils.transform("true"), TestUtils.transform("xtr.startsWith('hello, world!', 'hello')"));
        Assertions.assertEquals(TestUtils.transform("""
                {
                    bool: 'true',
                    num: '365',
                    nil: 'null'
                }"""), TestUtils.transform("""
                {
                    bool: xtr.toString(true),
                    num: xtr.toString(365),
                    nil: xtr.toString(null)
                }"""));
    }

    @Test
    public void test() {
        System.out.println(transform("xtr.toString({foo:'bar'})"));
    }

    @Test
    public void trim() {
        Assertions.assertEquals(TestUtils.transform("'hello, world!'"), TestUtils.transform("xtr.trim('  hello, world!   ')"));
    }

    @Test
    public void typeOf() {
        Assertions.assertEquals(TestUtils.transform("""
                {
                    bool: 'boolean',
                    num: 'number',
                    nil: 'null',
                    arr: 'array',
                    obj: 'object',
                    func: 'function'
                }"""), TestUtils.transform("""
                local func(it) = it;

                {
                    bool: xtr.typeOf(true),
                    num: xtr.typeOf(365),
                    nil: xtr.typeOf(null),
                    arr: xtr.typeOf([]),
                    obj: xtr.typeOf({}),
                    func: xtr.typeOf(func)
                }"""));
    }

    @Test
    public void upper() {
        Assertions.assertEquals(TestUtils.transform("'HELLO WORLD!'"), TestUtils.transform("xtr.upper('Hello World!')"));
    }

    @Disabled
    @Test
    public void uuid() {
        Assertions.assertEquals(TestUtils.transform("'8eae62af-d2dc-4759-8316-ce6eeca0b61c'"), TestUtils.transform("xtr.uuid()"));
    }

    @Test
    public void valuesOf() {
        Assertions.assertEquals(TestUtils.transform("['3.1.3', '19']"), TestUtils.transform("xtr.valuesOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void write() {
        Assertions.assertEquals(TestUtils.transform("'{\"hello\":\"world\",\"arr\":[],\"nil\":null}'"), TestUtils.transform("xtr.write({ hello: 'world', arr: [], nil: null }, 'application/json')"));
        Assertions.assertEquals(TestUtils.transform("'{\\n    \"hello\": \"world\",\\n    \"arr\": [\\n        \\n    ]\\n}'"), TestUtils.transform("xtr.write({ hello: 'world', arr: [] }, 'application/json', { indent: true })"));
    }
}