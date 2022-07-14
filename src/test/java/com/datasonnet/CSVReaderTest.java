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

public class CSVReaderTest {

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        Document<String> data = new DefaultDocument<String>(
                TestUtils.resourceAsString("readCSVTest.csv"),
                MediaTypes.APPLICATION_CSV
        );

        Mapper mapper = new Mapper("{ fName: payload[0][\"First Name\"] }");


        Document<String> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertEquals("{\"fName\":\"Eugene\"}", mapped.getContent());
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        Document<String> data = new DefaultDocument<>(
                TestUtils.resourceAsString("readCSVExtTest.csv"),
                MediaTypes.APPLICATION_CSV
        );
        String jsonnet = TestUtils.resourceAsString("readCSVExtTest.ds");

        Mapper mapper = new Mapper(jsonnet);


        Document<String> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mapped.getContent());
    }


}
