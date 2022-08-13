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

import com.datasonnet.document.DefaultDocument;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.datasonnet.TestUtils.resourceAsString;
import static com.datasonnet.TestUtils.stacktraceFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ImportTest {
    @Test
    void import_simple() {
        var result = new Mapper("import 'output.json'", Collections.emptyList(),
                Collections.singletonMap("output.json", "{\"a\": 5}")).transform(DefaultDocument.NULL_INSTANCE);
        assertEquals("{\"a\":5}", result.getContent());
    }

    @Test
    void import_parse_error_states_linenumber() {
        try {
            new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a b")).transform(DefaultDocument.NULL_INSTANCE);
            fail("Import should fail");
        } catch (IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Could not parse transformation script"), "Stacktrace does not indicate the issue");
            assertTrue(e.getCause().getMessage().contains("Expected end-of-input:1:3, found \"b\""), "Found message: " + e.getMessage());
            assertTrue(stacktrace.contains("(output.json:1:3)"), "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void import_eval_error_states_linenumber() {
        try {
            new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a.b")).transform(DefaultDocument.NULL_INSTANCE);
            fail("Execution should fail");
        } catch (IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(e.getCause().getMessage().contains("Unknown variable: a"), "Found message: " + e.getCause().getMessage());
            assertTrue(stacktrace.contains("output.json:1:1"), "Stacktrace does not indicate the issue");
            assertTrue(stacktrace.contains("(main):1:1"), "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void import_libsonnet_no_parse_error() throws Exception {
        try {
            var lib = resourceAsString("importTest.libsonnet");
            var json = resourceAsString("importLibsonnetTest.json");
            new Mapper("local testlib = import 'importTest.libsonnet'; local teststr = import 'importLibsonnetTest.json'; { greeting: testlib.sayHello('World') }", Collections.emptyList(),
                    Map.of("importTest.libsonnet", lib, "importLibsonnetTest.json", json));
        } catch (IllegalArgumentException e) {
            fail("This test should not fail, libsonnets are lazily evaluated");
        }
    }

    @Test
    void import_libsonnet_parse_error_states_linenumber() throws Exception {
        try {
            var libErr = resourceAsString("importTestFail.libsonnet");
            new Mapper("local testlib = import 'importTestFail.libsonnet'; { greeting: testlib.sayHello('World') }",
                    Collections.emptyList(), Collections.singletonMap("importTestFail.libsonnet", libErr)).transform(DefaultDocument.NULL_INSTANCE);
            fail("This test should fail");
        } catch (IllegalArgumentException e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Could not parse transformation script"), "Stacktrace does not indicate the issue");
            assertTrue(e.getCause().getMessage().contains("Expected \"}\":2:39, found \"XXXXX\\n}\\n\""), "Found message: " + e.getMessage());
            assertTrue(stacktrace.contains("(importTestFail.libsonnet:2:39)"), "Stacktrace does not indicate the issue");
        }
    }
}
