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
import java.util.Set;
import java.util.stream.Stream;

import static com.github.jam01.xtrasonnet.TestUtils.stacktraceFrom;
import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.*;

public class TransformerTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Transformer transformer = new Transformer(jsonnet);
        assertEquals(expected, transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON)).getContent());
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
        Map<String, Document<?>> variables = Collections.singletonMap(variable, Document.of(value, MediaTypes.APPLICATION_JSON));
        Transformer transformer = new Transformer(jsonnet, variables.keySet());
        assertEquals(expected, transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent());
    }

    static Stream<String[]> variableProvider() {
        return Stream.of(
                new String[] { "{ [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
                new String[] { "{ \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
        );
    }

    @Test
    void parseErrorLineNumberWhenWrapped() {
        try {
            Transformer transformer = new Transformer("xtr.time.now() a");
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("Expected end-of-input:1:16"), "Found message: " + e.getCause().getMessage());
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            Transformer transformer = new Transformer("payload.foo");
            transformer.transform("{}");
            fail("Must fail to execute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("attempted to index a string with string foo"), "Found message: " + e.getCause().getMessage());
            assertTrue(stacktraceFrom(e).contains("(main):1:8"), "Stacktrace does not indicate the issue");
        }
    }

    @Disabled
    @Test
    void includedJsonnetLibraryWorks() {
        Transformer transformer = new Transformer("xtr.Util.select({a: {b: 5}}, 'a.b')");
        assertEquals("5", transformer.transform("{}"));
    }

    Map<String, Document> stringArgument(String key, String value) {
        return new HashMap<String, Document>() {{
            put(key, Document.of(value, MediaTypes.TEXT_PLAIN));
        }};
    }

    @Test
    void nonJsonArguments() {
        Transformer transformer = new Transformer("argument", Set.of("argument"));


        Map<String, Document<?>> map = Collections.singletonMap("argument", Document.of("value", MediaTypes.TEXT_PLAIN));

        Document<String> mapped = transformer.transform(Document.of("{}", MediaTypes.APPLICATION_JSON), map, MediaTypes.TEXT_PLAIN);

        //assertEquals(new DefaultDocument<String>("value", MediaTypes.TEXT_PLAIN), mapped);
        assertEquals("value", mapped.getContent());
        assertEquals(MediaTypes.TEXT_PLAIN, mapped.getMediaType());

    }

    @Test
    void testFieldsOrder() throws Exception {
        String jsonData = TestUtils.resourceAsString("fieldOrder.json");
        String datasonnet = TestUtils.resourceAsString("fieldOrder.ds");

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("v2", Document.of("v2value", MediaTypes.TEXT_PLAIN));
        variables.put("v1", Document.of("v1value", MediaTypes.TEXT_PLAIN));

        Transformer transformer = new Transformer(datasonnet, variables.keySet());


        String mapped = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());

        datasonnet = "/** xtrasonnet\n" +
                     "preserveOrder=false\n*/\n" + datasonnet;

        transformer = new Transformer(datasonnet, variables.keySet());


        mapped = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
    }
}
