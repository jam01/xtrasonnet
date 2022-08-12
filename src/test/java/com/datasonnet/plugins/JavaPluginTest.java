package com.datasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaPluginTest {
    Car car = new Car("blue", "bus");
    Map<String, String> carMap = Map.of("color", "blue", "type", "bus");
    String carJson = """
                { "color": "blue", "type": "bus" }""";

    @Test
    public void read_simple() throws JSONException {
        var doc = new Mapper("payload").transform(new DefaultDocument<>(carMap));

        JSONAssert.assertEquals(carJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_object() throws JSONException {
        var doc = new Mapper("payload").transform(new DefaultDocument<>(car));

        JSONAssert.assertEquals(carJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_null() throws JSONException {
        var doc = new Mapper("payload").transform(DefaultDocument.NULL_INSTANCE);

        assertEquals("null", doc.getContent()); // "null" is a valid JSON doc
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() {
        var doc = new Mapper("{ color: 'blue', type: 'bus' }")
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JAVA, Map.class);

        assertEquals(carMap, doc.getContent());
        assertEquals("application/x-java-object;type=java.util.Map", doc.getMediaType().toString());
    }

    @Test
    public void write_object() {
        var doc = new Mapper("{ color: 'blue', type: 'bus' }")
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JAVA, Car.class);

        assertEquals(car, doc.getContent());
        assertEquals("application/x-java-object;type=com.datasonnet.plugins.JavaPluginTest$Car", doc.getMediaType().toString());
    }

    @Test
    public void write_null() throws JSONException {
        var doc = new Mapper("null")
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JAVA);

        JSONAssert.assertEquals(null, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JAVA, doc.getMediaType());
    }

    public static class Car {
        // needed by Jackson
        public Car() {
            this(null, null);
        }
        public Car(String color, String type) {
            this.color = color;
            this.type = type;
        }

        public String color;

        public String type;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Car car = (Car) o;

            if (!Objects.equals(color, car.color)) return false;
            return Objects.equals(type, car.type);
        }
    }
}
