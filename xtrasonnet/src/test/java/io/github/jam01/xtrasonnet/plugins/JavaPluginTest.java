package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaPluginTest {
    Car car = new Car("blue", "bus");
    Map<String, String> carMap = Map.of("color", "blue", "type", "bus");
    String carJson = """
            { "color": "blue", "type": "bus" }""";
    OffsetDateTime dateTime =
            OffsetDateTime.of(2020, 12, 31, 6, 35, 25, 0, ZoneOffset.UTC);
    Date date = Date.from(dateTime.toInstant());

    String datesJson = """
            { "date": "2020-12-31T06:35:25.000+00:00", "offsetDateTime": "2020-12-31T06:35:25Z" }""";

    String datesJsonCustom1 = """
            { "date": "2020-12-31", "offsetDateTime": "2020-12-31T06:35:25Z" }""";

    String datesJsonCustom2 = """
            { "date": "2020-12-31 06:35:25", "offsetDateTime": "2020-12-31T06:35:25Z" }""";

    String datesJsonCustom3 = """
            { "date": "Thu, 31 Dec 2020 06:35:25 UTC", "offsetDateTime": "2020-12-31T06:35:25Z" }""";

    OffsetDateTime dateTime1 =
            OffsetDateTime.of(2025, 6, 15, 12, 6, 32, 500, ZoneOffset.ofHours(-6));
    Date date1 = Date.from(dateTime1.toInstant());

    String dates1Json = """
            { "date": "2025-06-15T18:06:32.000+00:00", "offsetDateTime": "2025-06-15T12:06:32.0000005-06:00" }""";

    String dates1JsonCustom1 = """
            { "date": "2025-06-15", "offsetDateTime": "2025-06-15T12:06:32.0000005-06:00" }""";

    String dates1JsonCustom2 = """
            { "date": "2025-06-15 18:06:32", "offsetDateTime": "2025-06-15T12:06:32.0000005-06:00" }""";

    String dates1JsonCustom3 = """
            { "date": "Sun, 15 Jun 2025 18:06:32 UTC", "offsetDateTime": "2025-06-15T12:06:32.0000005-06:00" }""";

    @Test
    public void read_simple() throws JSONException {
        var doc = new Transformer("payload").transform(Document.of(carMap, MediaTypes.APPLICATION_JAVA));

        JSONAssert.assertEquals(carJson, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_object() throws JSONException {
        var doc = new Transformer("payload").transform(Document.of(car, MediaTypes.APPLICATION_JAVA));

        JSONAssert.assertEquals(carJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_null() throws JSONException {
        var doc = new Transformer("payload").transform(Documents.Null());

        Assertions.assertEquals("null", doc.getContent()); // "null" is a valid JSON doc
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() {
        var doc = new Transformer("{ color: 'blue', type: 'bus' }")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_JAVA, Map.class);

        assertEquals(carMap, doc.getContent());
        assertEquals("application/x-java-object;type=java.util.Map", doc.getMediaType().toString());
    }

    @Test
    public void write_object() {
        var doc = new Transformer("{ color: 'blue', type: 'bus' }")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_JAVA, Car.class);

        assertEquals(car, doc.getContent());
        assertEquals("application/x-java-object;type=io.github.jam01.xtrasonnet.plugins.JavaPluginTest$Car", doc.getMediaType().toString());
    }

    @Test
    public void write_null() throws JSONException {
        var doc = new Transformer("null")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_JAVA);

        assertNull(doc.getContent());
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

    @Test
    public void readDates() throws JSONException {
        // date and datetime
        var doc = new Transformer("payload")
                .transform(Document.of(new Dates(date, dateTime), MediaTypes.APPLICATION_JAVA));

        JSONAssert.assertEquals(datesJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());

        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date, dateTime),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd")));

        JSONAssert.assertEquals(datesJsonCustom1, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date, dateTime),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss")));

        JSONAssert.assertEquals(datesJsonCustom2, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date, dateTime),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "EEE, dd MMM yyyy HH:mm:ss z")));

        JSONAssert.assertEquals(datesJsonCustom3, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        // date1 and datetime1
        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date1, dateTime1),
                        MediaTypes.APPLICATION_JAVA));

        JSONAssert.assertEquals(dates1Json, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date1, dateTime1),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd")));

        JSONAssert.assertEquals(dates1JsonCustom1, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date1, dateTime1),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss")));

        JSONAssert.assertEquals(dates1JsonCustom2, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());


        doc = new Transformer("payload")
                .transform(Document.of(new Dates(date1, dateTime1),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "EEE, dd MMM yyyy HH:mm:ss z")));

        JSONAssert.assertEquals(dates1JsonCustom3, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void writeDates() throws JSONException {
        // date and datetime
        var doc = new Transformer(datesJson)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA,
                        Dates.class);

        assertEquals(new Dates(date, dateTime), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(datesJsonCustom1)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd"),
                        Dates.class);

        assertEquals(new Dates(Date.from(date.toInstant().truncatedTo(ChronoUnit.DAYS)), dateTime), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(datesJsonCustom2)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss"),
                        Dates.class);

        assertEquals(new Dates(Date.from(date.toInstant().truncatedTo(ChronoUnit.SECONDS)), dateTime), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(datesJsonCustom3)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "EEE, dd MMM yyyy HH:mm:ss z"),
                        Dates.class);

        assertEquals(new Dates(date, dateTime), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        // date1 and datetime1
        doc = new Transformer(dates1Json)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA,
                        Dates.class);

        assertEquals(new Dates(date1, dateTime1), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(dates1JsonCustom1)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd"),
                        Dates.class);

        assertEquals(new Dates(Date.from(date1.toInstant().truncatedTo(ChronoUnit.DAYS)), dateTime1), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(dates1JsonCustom2)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss"),
                        Dates.class);

        assertEquals(new Dates(Date.from(date1.toInstant().truncatedTo(ChronoUnit.SECONDS)), dateTime1), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));


        doc = new Transformer(dates1JsonCustom3)
                .transform(Documents.Null(), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JAVA.withParameter(DefaultJavaPlugin.PARAM_DATE_FORMAT, "EEE, dd MMM yyyy HH:mm:ss z"),
                        Dates.class);

        assertEquals(new Dates(date1, dateTime1), doc.getContent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(doc.getMediaType()));
    }

    public static class Dates {
        // needed by Jackson for some reason
        public Dates() {
            this(null, null);
        }

        public Dates(Date date, OffsetDateTime offsetDateTime) {
            this.offsetDateTime = offsetDateTime;
            this.date = date;
        }

        public OffsetDateTime offsetDateTime;
        public Date date;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Dates dates = (Dates) o;

            if (!offsetDateTime.isEqual(dates.offsetDateTime))
                return false;
            return Objects.equals(date, dates.date);
        }
    }
}
