package io.github.jam01.xtrasonnet;

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

import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.header.Header;
import io.github.jam01.xtrasonnet.header.HeaderParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class HeaderTest {
    String headerStr = """
            /** xtrasonnet
            preserveOrder=false
            // comment
            input payload application/xml;namespace-separator=":";text-value-key=__text
            input * application/xml;text-value-key=__text;cdatakey=__cdata
            input myvar application/csv;separator=|
            dataformat application/vnd.ms-excel;payload.param=xyz
            input dftest application/vnd.ms-excel
            output application/csv;xtr.csv.quote=""\"
            */
            [
              {
                  greetings: payload["test:root"].__text,
                  name: myVar["test:myvar"].__text
              }
            ]""";

    Header header = Header.parseHeader(headerStr);

    public HeaderTest() throws HeaderParseException {
    }

    @Test
    void testHeaderPreserveOrder() {
        assertFalse(header.isPreserveOrder());
    }

    @Test
    void testHeaderAllInputs()  {
        var params = header.getPayloadInput().orElseThrow(AssertionError::new).getParameters().keySet();
        assertTrue(params.contains("cdatakey"));
    }

    @Test
    void testHeaderNamedInputs() {
        Set<String> namedInputs = header.getInputs().keySet();
        assertTrue(namedInputs.contains("payload"));
        assertTrue(namedInputs.contains("myvar"));
    }

    @Test
    void testHeaderNamedInputCommaSeparated() {
        Map<String, String> parameters = header.getInput("payload").orElseThrow(AssertionError::new).getParameters();
        assertTrue(parameters.containsKey("namespace-separator"));
        assertTrue(parameters.containsKey("text-value-key"));
    }

    @Test
    void testHeaderDataformat() {
        var params = header.getInput("dftest").orElseThrow(AssertionError::new).getParameters();
        assertTrue(params.containsKey("payload.param"));
    }

    @Test
    void testHeaderOutput() {
        Set<String> keys = header.getOutput().orElseThrow(AssertionError::new).getParameters().keySet();
        assertTrue(keys.contains("xtr.csv.quote"));
    }

    @Test
    void testUnknownHeaderFails() {
        assertThrows(HeaderParseException.class,  ()  -> {
           Header.parseHeader("/** xtrasonnet\n" +
                   "nonsense\n" +
                   "*/");
        });
    }

    @Test
    void testUnterminatedHeaderFailsNicely() {
        assertThrows(HeaderParseException.class,  ()  -> {
            Header.parseHeader("/** xtrasonnet\n" +
                    "version=2.0\n");
        });
    }

    @Test
    public void testDefaultOutput() throws HeaderParseException {
        Header header1 = Header.parseHeader("/** xtrasonnet\n" +
                "output application/x-java-object;q=0.9\n" +
                "output application/json;q=1.0\n" +
                "*/");

        assertTrue(header1.getOutput().isPresent());
        Assertions.assertTrue(MediaTypes.APPLICATION_JSON.equalsTypeAndSubtype(header1.getOutput().get()));
    }

    @Test
    public void testDefaultInput() throws HeaderParseException {
        Header header1 = Header.parseHeader("/** xtrasonnet\n" +
                "input payload application/x-java-object;q=1.0\n" +
                "input payload application/json;q=0.9\n" +
                "*/");

        assertTrue(header1.getPayloadInput().isPresent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(header1.getPayloadInput().get()));
    }
}
