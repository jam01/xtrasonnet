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


import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.fuzzutil.Dictionary;
import com.github.jam01.xtrasonnet.fuzzutil.XMLDocumentUtils;
import com.github.jam01.xtrasonnet.fuzzutil.XmlDocumentGenerator;
import com.github.jam01.xtrasonnet.fuzzutil.XMLJsonGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(JUnitQuickcheck.class)
public class XMLPropertyTest {


    @Property
    public void reversible(@From(XmlDocumentGenerator.class) @Dictionary("xml/xml.dict") Document dom) throws Exception {
        String xml = XMLDocumentUtils.documentToString(dom);
        // the round trip is performed by the use of the XML mime types
        Mapper mapper = new Mapper("payload");

        com.github.jam01.xtrasonnet.document.Document.BasicDocument<String> payload = new com.github.jam01.xtrasonnet.document.Document.BasicDocument<>(xml, MediaTypes.APPLICATION_XML.withParameter("badgerfish", "extended"));
        String output = mapper.transform(payload, Collections.emptyMap(), MediaTypes.APPLICATION_XML.withParameter("badgerfish", "extended")).getContent();

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document parsed = db.parse(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

        String jsonVersion = mapper.transform(payload, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertTrue("For input " + xml + " found output " + output + ", and JSON in the middle is " + jsonVersion, dom.isEqualNode(parsed));
    }

    @Property
    public void jsonSerializes(@From(XMLJsonGenerator.class) @Dictionary("xml/xml.dict") String json) throws Exception {
        Mapper mapper = new Mapper("payload");
        try {
            String xml = mapper.transform(new com.github.jam01.xtrasonnet.document.Document.BasicDocument<String>(json, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
        } catch(Throwable t) {
            t.printStackTrace();
            fail("Unable to convert to xml: " + json);
        }
    }
}
