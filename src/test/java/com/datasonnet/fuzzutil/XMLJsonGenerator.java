package com.datasonnet.fuzzutil;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLJsonGenerator extends Generator<String> {

    private static final DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();

    private Generator<String> stringGenerator = new AlphaStringGenerator();

    public XMLJsonGenerator() {
        super(String.class);
    }



    public void configure(Dictionary dict) throws IOException {
        stringGenerator = new DictionaryBackedStringGenerator(dict.value(), stringGenerator);
    }


    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {

        Map root = makeRoot(random, status);

        if (stringGenerator == null) {
            stringGenerator = gen().type(String.class);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

    }

    private Map makeRoot(SourceOfRandomness random, GenerationStatus status) {
        Map root = new HashMap();
        root.put(makeString(random, status), makeElement(random, status));
        return root;
    }

    private Map makeElement(SourceOfRandomness random, GenerationStatus status) {
        Map element = new HashMap();

        // mostly have ordered elements
        if(random.nextInt() > 0.1) {
            // mostly make them numbers
            if(random.nextInt() > 0.9) {
                element.put("~", "" + random.nextInt());
            } else {
                element.put("~", random.nextInt());
            }
        }
        addAttributes(element, random, status);
        if(random.nextBoolean()) {
            addText(element, random, status);
            if(random.nextBoolean()) {
                addMixedElements(element, random, status);
            }
        } else {
            if(random.nextBoolean()) {
                addMixedElements(element, random, status);
            }
            if(random.nextBoolean()) {
                addStructuredElements(element, random, status);
            }
        }
        return element;
    }

    private void addMixedElements(Map element, SourceOfRandomness random, GenerationStatus status) {
        int mixed = random.nextInt(5);
        for(int ii = 0; ii < mixed; ii++) {
            if(random.nextBoolean()) {
                element.put("$" + random.nextInt(), gen().type(String.class).generate(random, status));
            } else if(random.nextBoolean()) {
                element.put("#" + ii, gen().type(String.class).generate(random, status));
            }
        }
    }

    private void addStructuredElements(Map element, SourceOfRandomness random, GenerationStatus status) {
        int keys = random.nextInt(3);
        for(int ii = 0; ii < keys; ii++) {
            if(random.nextBoolean()) {
                element.put(makeString(random, status), makeElement(random, status));
            } else {
                List elements = new ArrayList();
                int count = random.nextInt(1, 3);
                for(int jj = 0; jj < count; jj++) {
                    elements.add(makeElement(random, status));
                }
                element.put(makeString(random, status), elements);
            }
        }
    }

    private void addAttributes(Map element, SourceOfRandomness random, GenerationStatus status) {
        int attributes = random.nextInt(3);
        for(int ii = 0; ii < attributes; ii++) {
            element.put("@" + makeString(random, status), gen().type(String.class).generate(random, status));
        }
    }

    private void addText(Map element, SourceOfRandomness random, GenerationStatus status) {
        element.put("$", gen().type(String.class).generate(random, status));
    }

    private String makeString(SourceOfRandomness random, GenerationStatus status) {
        return stringGenerator.generate(random, status);
    }
}