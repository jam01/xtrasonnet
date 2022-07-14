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
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVWriterTest {

    @Test
    void testCSVWriter() throws URISyntaxException, IOException {

        Document<String> data = new DefaultDocument<>(
                TestUtils.resourceAsString("writeCSVTest.json"),
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("payload");


        Document<String> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV);
        assertEquals(MediaTypes.APPLICATION_CSV, mapped.getMediaType());

        String expected = TestUtils.resourceAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.getContent().trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document<String> data = new DefaultDocument<>(
                TestUtils.resourceAsString("writeCSVExtTest.json"),
                MediaTypes.APPLICATION_JSON
        );
        String datasonnet = TestUtils.resourceAsString("writeCSVExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV).getContent();
        String expected = TestUtils.resourceAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunction() throws URISyntaxException, IOException {

        Document data = new DefaultDocument<String>(
                TestUtils.resourceAsString("writeCSVTest.json"),
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("{ embeddedCSVValue: ds.write(payload, \"application/csv\") }");


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        String expected = "{\"embeddedCSVValue\":\"\\\"First Name\\\",\\\"Last Name\\\",Phone\\nWilliam,Shakespeare,\\\"(123)456-7890\\\"\\nChristopher,Marlow,\\\"(987)654-3210\\\"\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunctionExt() throws IOException, URISyntaxException {
        Document data = new DefaultDocument<String>(
                TestUtils.resourceAsString("writeCSVExtTest.json"),
                MediaTypes.APPLICATION_JSON
        );
        String datasonnet = TestUtils.resourceAsString("writeCSVFunctionExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        String expected = "{\"embeddedCSVValue\":\"'William'|'Shakespeare'|'(123)456-7890'\\n'Christopher'|'Marlow'|'(987)654-3210'\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

}
