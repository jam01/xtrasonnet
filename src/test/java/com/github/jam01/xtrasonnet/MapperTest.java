package com.github.jam01.xtrasonnet;

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

import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.jam01.xtrasonnet.TestUtils.stacktraceFrom;
import static org.junit.jupiter.api.Assertions.*;

public class MapperTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Mapper mapper = new Mapper(jsonnet);
        assertEquals(expected, mapper.transform(new Document.BasicDocument<>(json, MediaTypes.APPLICATION_JSON)).getContent());
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "xtr.datetime.plus(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        Map<String, Document<?>> variables = Collections.singletonMap(variable, new Document.BasicDocument<>(value, MediaTypes.APPLICATION_JSON));
        Mapper mapper = new Mapper(jsonnet, variables.keySet());
        assertEquals(expected, mapper.transform(new Document.BasicDocument<String>(json, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent());
    }

    static Stream<String[]> variableProvider() {
        return Stream.of(
                new String[] { "{ [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
                new String[] { "{ \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
        );
    }

    @Test
    void parseErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) xtr.time.now() a", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(e.getMessage().contains("Could not parse transformation script"), "Found message: " + e.getMessage());
            assertTrue(stacktrace.contains("Expected end-of-input:1:34"), "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void parseErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("xtr.time.now() a", Collections.emptyList());
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("Expected end-of-input:1:16"), "Found message: " + e.getCause().getMessage());
        }
    }

    @Test
    void noTopLevelFunction() {
        try {
            Mapper mapper = new Mapper("{}", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) payload.foo", Collections.emptyList(), Collections.emptyMap(), false);
            mapper.transform("{}");
            fail("Muspayload.foot fail to execute");
        } catch(IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(e.getMessage().contains("Error evaluating xtrasonnet transformation"), "Found message: " + e.getMessage());
            assertTrue(e.getCause().getMessage().contains("attempted to index a string with string foo"), "Found message: " + e.getCause().getMessage());
            assertTrue(stacktrace.contains("(main):1:26"), "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("payload.foo", Collections.emptyList());
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("attempted to index a string with string foo"), "Found message: " + e.getCause().getMessage());
            assertTrue(stacktraceFrom(e).contains("(main):1:8"), "Stacktrace does not indicate the issue");
        }
    }

    @Disabled
    @Test
    void includedJsonnetLibraryWorks() {
        Mapper mapper = new Mapper("xtr.Util.select({a: {b: 5}}, 'a.b')", Collections.emptyList());
        assertEquals("5", mapper.transform("{}"));
    }

    Map<String, Document> stringArgument(String key, String value) {
        return new HashMap<String, Document>() {{
            put(key, new Document.BasicDocument<String>(value, MediaTypes.TEXT_PLAIN));
        }};
    }

    @Test
    void nonJsonArguments() {
        Mapper mapper = new Mapper("argument", Arrays.asList("argument"));


        Map<String, Document<?>> map = Collections.singletonMap("argument", new Document.BasicDocument<>("value", MediaTypes.TEXT_PLAIN));

        Document<String> mapped = mapper.transform(new Document.BasicDocument<String>("{}", MediaTypes.APPLICATION_JSON), map, MediaTypes.TEXT_PLAIN);

        //assertEquals(new DefaultDocument<String>("value", MediaTypes.TEXT_PLAIN), mapped);
        assertEquals("value", mapped.getContent());
        assertEquals(MediaTypes.TEXT_PLAIN, mapped.getMediaType());

    }

    @Test
    void noTopLevelFunctionArgs() {
        try {
            Mapper mapper = new Mapper("function() { test: \'HelloWorld\' } ", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function must have at least one argument"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void testFieldsOrder() throws Exception {
        String jsonData = TestUtils.resourceAsString("fieldOrder.json");
        String datasonnet = TestUtils.resourceAsString("fieldOrder.ds");

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("v2", new Document.BasicDocument<>("v2value", MediaTypes.TEXT_PLAIN));
        variables.put("v1", new Document.BasicDocument<>("v1value", MediaTypes.TEXT_PLAIN));

        Mapper mapper = new Mapper(datasonnet, variables.keySet());


        String mapped = mapper.transform(new Document.BasicDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());

        datasonnet = "/** DataSonnet\n" +
                     "version=2.0\n" +
                     "preserveOrder=false\n*/\n" + datasonnet;

        mapper = new Mapper(datasonnet, variables.keySet());


        mapped = mapper.transform(new Document.BasicDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
    }
}
