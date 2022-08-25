package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RootTest {

    @Test
    public void contains() {
        assertEquals(transform("true"), transform("xtr.contains([1, 2, 3], 1)"));
        assertEquals(transform("true"), transform("xtr.contains({ scala: '3.1.3' }, 'scala')"));
        assertEquals(transform("true"), transform("xtr.contains('Lorem ipsum', 'Lorem')"));
    }

    @Test
    public void endsWith() {
        assertEquals(transform("true"), transform("xtr.endsWith('Lorem ipsum', 'ipsum')"));
    }

    @Test
    public void entriesOf() {
        assertEquals(transform("""
                [
                  {
                    key: 'scala',
                    value: '3.1.3'
                  },
                  {
                    key: 'java',
                    value: '19'
                  }
                ]"""), transform("xtr.entriesOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void filter() {
        assertEquals(transform("[1, 2]"), transform("xtr.filter([1, 2, 3, 4], function(item) item < 3)"));
        assertEquals(transform("[4]"), transform("xtr.filter([1, 2, 3, 4], function(item, idx) idx > 2)"));
    }

    @Test
    public void filterNotEq() {
        assertEquals(transform("[1, 2, 4, 4]"), transform("xtr.filterNotEq([1, 2, 3, 4, 3, 4], 3)"));
    }

    @Test
    public void filterNotIn() {
        assertEquals(transform("[1, 2]"), transform("xtr.filterNotIn([1, 2, 3, 4, 3, 4], [3, 4])"));
    }

    @Test
    public void filterObject() {
        assertEquals(transform("""
                {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true }
                }"""), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang) lang.isJvm)"""));
        assertEquals(transform("""
                {
                    scala: { version: '3.1.3', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                }"""), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang, name) !lang.isJvm || name == 'scala')"""));
        assertEquals(transform("""
                {
                    scala: { version: '3.1.3', isJvm: true},
                    python: { version: '3.10.4', isJvm: false}
                }"""), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.filterObject(languages, function(lang, name, idx) idx == 0 || name == 'python')"""));
    }

    @Test
    public void flatMap() {
        assertEquals(transform("[1, 1, 3, 9, 5, 25]"), transform("xtr.flatMap([1, 3, 5], function(item) [item, item * item])"));
        assertEquals(transform("[1, 0, 3, 3, 5, 10]"), transform("xtr.flatMap([1, 3, 5], function(item, idx) [item, item * idx])"));
    }

    @Test
    public void flatMapObject() {
        assertEquals(transform("""
                {
                    java: 5,
                    'unit-testing': 2,
                    github: 4,
                    containers: 5,
                    kubernetes: 2,
                    jenkins: 4
                }"""), transform("""
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
        assertEquals(transform("""
                {
                    java: 5,
                    'unit-testing': 2,
                    github: 4,
                    containers: 5,
                    kubernetes: 2,
                    jenkins: 4
                }"""), transform("""
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
        assertEquals(transform("""
                {
                    java: 5,
                    'unit-testing': 3,
                    github: 4,
                    containers: 5,
                    kubernetes: 1,
                    jenkins: 2
                }"""), transform("""
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
        assertEquals(transform("[1, 2, 3]"), transform("xtr.flatten([[1, 2], [3]])"));
        assertEquals(transform("6"), transform("xtr.foldLeft([1, 2, 3], 0, function(item, acc) item + acc)"));
        assertEquals(transform("' dolor ipsum Lorem'"), transform("xtr.foldRight(['Lorem', 'ipsum', 'dolor'], '', function(item, acc) acc + ' ' + item)"));
        assertEquals(transform("""
                {
                    jvmLangs: [
                        { name: 'scala', version: '3.1.3', isJvm: true },
                        { name: 'java', version: '19', isJvm: true }
                    ],
                    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
                }"""), transform("""
                local languages = [
                    { name: 'scala', version: '3.1.3', isJvm: true },
                    { name: 'java', version: '19', isJvm: true },
                    { name: 'python', version: '3.10.4', isJvm: false }
                ];

                xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')"""));
        assertEquals(transform("""
                {
                    preferred: [{ name: 'scala', version: '3.1.3', isJvm: true }],
                    jvmLangs: [{ name: 'java', version: '19', isJvm: true }],
                    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
                }"""), transform("""
                local languages = [
                    { name: 'scala', version: '3.1.3', isJvm: true },
                    { name: 'java', version: '19', isJvm: true },
                    { name: 'python', version: '3.10.4', isJvm: false }
                ];
                local langFunc(lang, idx) = if idx == 0 then 'preferred'
                    else if lang.isJvm then 'jvmLangs'
                    else 'others';

                xtr.groupBy(languages, langFunc)"""));
        assertEquals(transform("""
                {
                    jvmLangs: {
                        scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                        java: { version: '19', isJvm: true, project: 'jdk.java.net' }
                    },
                    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
                }"""), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
                    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
                };

                xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')"""));
        assertEquals(transform("""
                {
                    preferred: { scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' }},
                    jvmLangs: { java: { version: '19', isJvm: true, project: 'jdk.java.net' }},
                    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
                }"""), transform("""
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
        assertEquals(transform("[1, 4]"), transform("xtr.indicesOf([1, 7, 3, 4, 7], 7)"));
        assertEquals(transform("[0, 14]"), transform("xtr.indicesOf('lorem ipsum dolor', 'lo')"));
    }

    @Test
    public void isType() {
        assertEquals(transform("true"), transform("xtr.isArray([1, 2])"));
        assertEquals(transform("true"), transform("xtr.isBoolean(false)"));
        assertEquals(transform("true"), transform("xtr.isDecimal(2.5)"));
        assertEquals(transform("true"), transform("""
                local increment(item) = item + 1;

                xtr.isFunction(increment)"""));
        assertEquals(transform("true"), transform("xtr.isInteger(2)"));
        assertEquals(transform("true"), transform("xtr.isNumber(2)"));
        assertEquals(transform("true"), transform("xtr.isObject({})"));
        assertEquals(transform("true"), transform("xtr.isString('Lorem')"));
    }

    @Test
    public void isBlank() {
        assertEquals(transform("true"), transform("xtr.isBlank('   ')"));
    }

    @Test
    public void isEmpty() {
        assertEquals(transform("true"), transform("xtr.isEmpty([])"));
        assertEquals(transform("true"), transform("xtr.isEmpty({})"));
        assertEquals(transform("true"), transform("xtr.isEmpty('')"));
    }

    @Test
    public void numIsCondition() {
        assertEquals(transform("true"), transform("xtr.isEven(2)"));
        assertEquals(transform("true"), transform("xtr.isOdd(1)"));
    }

    @Test
    public void join() {
        assertEquals(transform("'0, 1, 1, 2, 3, 5, 8'"), transform("xtr.join([0, 1, 1, 2, 3, 5, 8], ', ')"));
        assertEquals(transform("'hello world !'"), transform("xtr.join(['hello', 'world', '!'], ' ')"));
    }

    @Test
    public void keysOf() {
        assertEquals(transform("['scala', 'java']"), transform("xtr.keysOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void lower() {
        assertEquals(transform("'hello world!'"), transform("xtr.lower('Hello World!')"));
    }

    @Test
    public void map() {
        assertEquals(transform("[1, 4, 9, 16]"), transform("xtr.map([1, 2, 3, 4], function(item) item * item)"));
        assertEquals(transform("[0, 2, 6, 12]"), transform("xtr.map([1, 2, 3, 4], function(item, idx) item * idx)"));
    }

    @Test
    public void mapObject() {

    }

    @Test
    public void mapEntries() {
        assertEquals(transform("['scala-lang.org', 'jdk.java.net', 'python.org']"), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
                    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
                };

                xtr.mapEntries(languages, function(lang) lang.project)"""));
        assertEquals(transform("""
                [
                    { name: 'scala', version: '3.1.3' },
                    { name: 'java', version: '19' }
                ]"""), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
                    java: { version: '19', isJvm: true, project: 'jdk.java.net' }
                };

                xtr.mapEntries(languages, function(lang, name) {
                    name: name,
                    version: lang.version
                })"""));
        assertEquals(transform("[{ name: 'scala', preferred: true }, { name: 'java' }]"), transform("""
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
        assertEquals(transform("true"), transform("xtr.max([false, false, true])"));
        assertEquals(transform("100"), transform("xtr.max([0, 8, 2, 100])"));
        assertEquals(transform("'zzz'"), transform("xtr.max(['Lorem', 'zzz', 'ipsum', 'dolor'])"));
        assertEquals(transform("{ name: 'scala', version: '3.1.3', isPreferred: true }"), transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'python', version: '3.1.14', isPreferred: false }
                    { name: 'scala', version: '3.1.3', isPreferred: true },
                ];

                xtr.maxBy(languages, function(lang) lang.isPreferred)"""));
    }

    @Test
    public void maxBy() {
        assertEquals(transform("{ name: 'scala', version: '3.1.3', weight: 4 }"), transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 2 },
                    { name: 'python', version: '3.1.14', weight: 2 }
                    { name: 'scala', version: '3.1.3', weight: 4 },
                ];

                xtr.maxBy(languages, function(lang) lang.weight)"""));
        assertEquals(transform("{ name: 'scala', version: '3.1.3', code: 'S' }"), transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ];

                xtr.maxBy(languages, function(lang) lang.code)"""));
    }

    @Test
    public void min() {
        assertEquals(transform("false"), transform("xtr.min([false, false, true])"));
        assertEquals(transform("0"), transform("xtr.min([0, 8, 2, 100])"));
        assertEquals(transform("'AAA'"), transform("xtr.min(['Lorem', 'AAA', 'ipsum', 'dolor'])"));
    }

    @Test
    public void minBy() {
        assertEquals(transform("{ name: 'java', version: '19', isPreferred: false }"), transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'python', version: '3.1.14', isPreferred: false },
                    { name: 'scala', version: '3.1.3', isPreferred: true }
                ];

                xtr.minBy(languages, function(lang) lang.isPreferred)"""));
        assertEquals(transform("{ name: 'java', version: '19', weight: 2 }"), transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 2 },
                    { name: 'python', version: '3.1.14', weight: 2 },
                    { name: 'scala', version: '3.1.3', weight: 4 }
                ];

                xtr.minBy(languages, function(lang) lang.weight)"""));
        assertEquals(transform("{ name: 'java', version: '19', code: 'B' }"), transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ];

                xtr.minBy(languages, function(lang) lang.code)"""));
        assertEquals(transform("{ name: 'java', version: '19', isPreferred: false }"), transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'scala', version: '3.1.3', isPreferred: true },
                    { name: 'python', version: '3.1.14', isPreferred: false }
                ];

                xtr.minBy(languages, function(lang) lang.isPreferred)"""));
    }

    @Test
    public void parseNum() {
        assertEquals(transform("123.45"), transform("xtr.parseNum('123.45')"));
    }

    @Test
    public void orderBy() {
        assertEquals(transform("""
                [
                    { name: 'python', version: '3.1.14', weight: 2 },
                    { name: 'java', version: '19', weight: 3 },
                    { name: 'scala', version: '3.1.3', weight: 4 }
                ]"""), transform("""
                local languages = [
                    { name: 'java', version: '19', weight: 3 },
                    { name: 'scala', version: '3.1.3', weight: 4 },
                    { name: 'python', version: '3.1.14', weight: 2 }
                ];

                xtr.orderBy(languages, function(lang) lang.weight)"""));
        assertEquals(transform("""
                [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'python', version: '3.1.14', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' }
                ]"""), transform("""
                local languages = [
                    { name: 'java', version: '19', code: 'B' },
                    { name: 'scala', version: '3.1.3', code: 'S' },
                    { name: 'python', version: '3.1.14', code: 'B' }
                ];

                xtr.orderBy(languages, function(lang) lang.code)"""));
    }

    @Test
    public void range() {
        assertEquals(transform("[1, 2, 3, 4, 5]"), transform("xtr.range(1, 5)"));
    }

    @Test
    public void read() {
        assertEquals(transform("""
                {
                  hello: { '_text': 'world!' }
                }"""), transform("xtr.read('<hello>world!</hello>', 'application/xml')"));
        assertEquals(transform("""
                {
                  hello: { _txt: 'world!' }
                }"""), transform("xtr.read('<hello>world!</hello>', 'application/xml', { textkey: '_txt' })"));
    }

    @Disabled
    @Test
    public void readUrl() {
        assertEquals(transform("""
                {
                  hello: { '$': 'world!' }
                }"""), transform("xtr.readUrl('example.com/data', 'application/xml')"));
        assertEquals(transform("""
                {
                  hello: { _txt: 'world!' }
                }"""), transform("xtr.readUrl('example.com', 'application/xml', { textvaluekey: '_txt' })"));
    }

    @Test
    public void rmKey() {
        assertEquals(transform("{ scala: '3.1.3' }"), transform("xtr.rmKey({ scala: '3.1.3', java: '19' }, 'java')"));
    }

    @Test
    public void rmKeyIn() {
        assertEquals(transform("{}"), transform("xtr.rmKeyIn({ scala: '3.1.3', java: '19' }, ['java', 'scala'])"));
    }

    @Test
    public void replace() {
        assertEquals(transform("'hello, everyone!'"), transform("xtr.replace('hello, world!', 'world', 'everyone')"));
    }

    @Test
    public void reverse() {
        assertEquals(transform("[3, 2, 1]"), transform("xtr.reverse([1, 2, 3])"));
        assertEquals(transform("{ key2: 'value2', key1: 'value1' }"), transform("xtr.reverse({ key1: 'value1', key2: 'value2' })"));
        assertEquals(transform("'Lorem ipsum dolor'"), transform("xtr.reverse('rolod muspi meroL')"));
    }

    @Test
    public void sizeOf() {
        assertEquals(transform("3"), transform("xtr.sizeOf([1, 2, 3])"));
        assertEquals(transform("2"), transform("""
                local add(item, item2) = item + item2;

                xtr.sizeOf(add)"""));
        assertEquals(transform("1"), transform("xtr.sizeOf({ key: 'value' })"));
        assertEquals(transform("13"), transform("xtr.sizeOf('hello, world!')"));
    }

    @Test
    public void split() {
        assertEquals(transform("['hell', ', w', 'rld!']"), transform("xtr.split('hello, world!', 'o')"));
    }

    @Test
    public void startsWith() {
        assertEquals(transform("true"), transform("xtr.startsWith('hello, world!', 'hello')"));
        assertEquals(transform("""
                {
                    bool: 'true',
                    num: '365',
                    nil: 'null'
                }"""), transform("""
                {
                    bool: xtr.toString(true),
                    num: xtr.toString(365),
                    nil: xtr.toString(null)
                }"""));
    }

    @Test
    public void trim() {
        assertEquals(transform("'hello, world!'"), transform("xtr.trim('  hello, world!   ')"));
    }

    @Test
    public void typeOf() {
        assertEquals(transform("""
                {
                    bool: 'boolean',
                    num: 'number',
                    nil: 'null',
                    arr: 'array',
                    obj: 'object',
                    func: 'function'
                }"""), transform("""
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
        assertEquals(transform("'HELLO WORLD!'"), transform("xtr.upper('Hello World!')"));
    }

    @Disabled
    @Test
    public void uuid() {
        assertEquals(transform("'8eae62af-d2dc-4759-8316-ce6eeca0b61c'"), transform("xtr.uuid()"));
    }

    @Test
    public void valuesOf() {
        assertEquals(transform("['3.1.3', '19']"), transform("xtr.valuesOf({ scala: '3.1.3', java: '19' })"));
    }

    @Test
    public void write() {
        assertEquals(transform("'{\"hello\":\"world\",\"arr\":[],\"nil\":null}'"), transform("xtr.write({ hello: 'world', arr: [], nil: null }, 'application/json')"));
        assertEquals(transform("'{\\n    \"hello\": \"world\",\\n    \"arr\": [\\n        \\n    ]\\n}'"), transform("xtr.write({ hello: 'world', arr: [] }, 'application/json', { fmt: true })"));
    }
}