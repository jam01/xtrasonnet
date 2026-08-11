package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each document must reach the variable it is named for, whatever order the caller's Map iterates in.
 * Map.of randomizes that order per JVM run, so these cases are repeated and use deliberately
 * hash-adversarial names -- a single green run would prove nothing.
 */
public class NamedInputsTest {

    private static final String SCRIPT = "{ a: alpha.v, b: bravo.v, c: charlie.v }";
    private static final String EXPECTED = "{\"a\":\"A\",\"b\":\"B\",\"c\":\"C\"}";

    private static Document<String> doc(String v) {
        return Document.of("{ \"v\": \"" + v + "\" }", MediaTypes.APPLICATION_JSON);
    }

    private static String transformWith(Map<String, Document<?>> inputs) {
        return Transformer.builder(SCRIPT)
                .withInputNames("alpha", "bravo", "charlie")
                .build()
                .transform(Documents.Null(), inputs, MediaTypes.APPLICATION_JSON)
                .getContent();
    }

    @Test
    public void bindsByNameNotByDeclarationOrder() {
        // deliberately supplied in an order that does not match the declared order
        Map<String, Document<?>> inputs = new LinkedHashMap<>();
        inputs.put("charlie", doc("C"));
        inputs.put("alpha", doc("A"));
        inputs.put("bravo", doc("B"));

        assertEquals(EXPECTED, transformWith(inputs));
    }

    @Test
    public void bindsByNameForEveryPermutation() {
        String[][] orders = {
                {"alpha", "bravo", "charlie"}, {"alpha", "charlie", "bravo"},
                {"bravo", "alpha", "charlie"}, {"bravo", "charlie", "alpha"},
                {"charlie", "alpha", "bravo"}, {"charlie", "bravo", "alpha"}
        };
        Map<String, Document<?>> values = Map.of(
                "alpha", doc("A"), "bravo", doc("B"), "charlie", doc("C"));

        for (String[] order : orders) {
            Map<String, Document<?>> inputs = new LinkedHashMap<>();
            for (String name : order) {
                inputs.put(name, values.get(name));
            }
            assertEquals(EXPECTED, transformWith(inputs), "failed for supply order " + String.join(",", order));
        }
    }

    /**
     * Map.of iteration order is randomized per JVM via an internal SALT, and HashMap order depends
     * on key hashes, so a single green run of this proves little on its own -- it is the repetition
     * plus the explicit permutations above that make the regression detectable.
     */
    @RepeatedTest(20)
    public void bindsByNameWithUnorderedMaps() {
        assertEquals(EXPECTED, transformWith(Map.of(
                "alpha", doc("A"), "bravo", doc("B"), "charlie", doc("C"))));

        Map<String, Document<?>> hashed = new HashMap<>();
        hashed.put("alpha", doc("A"));
        hashed.put("bravo", doc("B"));
        hashed.put("charlie", doc("C"));
        assertEquals(EXPECTED, transformWith(hashed));
    }

    @Test
    public void unknownInputNameIsReported() {
        Map<String, Document<?>> inputs = new LinkedHashMap<>();
        inputs.put("alpha", doc("A"));
        inputs.put("bravo", doc("B"));
        inputs.put("delta", doc("D")); // never declared

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transformWith(inputs));
        assertTrue(ex.getMessage().contains("delta"), "should name the offending input: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("alpha"), "should list the declared inputs: " + ex.getMessage());
    }

    @Test
    public void inputsOfDifferentMediaTypesBindToTheirOwnNames() {
        Map<String, Document<?>> inputs = new LinkedHashMap<>();
        inputs.put("charlie", Document.of("<v>C</v>", MediaTypes.APPLICATION_XML));
        inputs.put("alpha", doc("A"));
        inputs.put("bravo", doc("B"));

        String result = Transformer.builder("{ a: alpha.v, b: bravo.v, c: charlie.v._text }")
                .withInputNames("alpha", "bravo", "charlie")
                .build()
                .transform(Documents.Null(), inputs, MediaTypes.APPLICATION_JSON)
                .getContent();

        // a mis-binding here would also mean reading a document with the wrong plugin
        assertEquals(EXPECTED, result);
    }

    @Test
    public void payloadIsNotAcceptedAsANamedInput() {
        // the payload argument owns that parameter; accepting the name here would overwrite it
        var ex = assertThrows(IllegalArgumentException.class, () -> new Transformer("payload")
                .transform(Document.of("1", MediaTypes.APPLICATION_JSON),
                        Map.of("payload", Document.of("2", MediaTypes.APPLICATION_JSON)),
                        MediaTypes.APPLICATION_JSON));

        assertTrue(ex.getMessage().contains("'payload' is not a named input"), "Found message: " + ex.getMessage());
    }
}
