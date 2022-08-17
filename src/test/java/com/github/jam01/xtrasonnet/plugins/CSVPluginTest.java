package com.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.Transformer;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVPluginTest {
    String cars = """
            color,type
            blue,bus
            yellow,truck
            """;

    String carsNoheader = cars.substring(cars.indexOf('\n') + 1); // removed first line
    String carsQchar = cars.replace('"', '\''); // use ' instead of " for quotes
    String carsSep = cars.replace(',', '|'); // use | instead of , as separator
//    String carsEsc = cars.replace(',', '|'); // use | instead of , as separator

    String carsJson = """
            [
                { "color": "blue", "type": "bus" },
                { "color": "yellow", "type": "truck" }
            ]
            """;
    String carsJsonNoheader = """
            [
                ["blue", "bus"],
                ["yellow", "truck"]
            ]
            """;

    @Test
    public void read_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(cars, MediaTypes.TEXT_CSV));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_noheader() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(carsNoheader,
                        MediaTypes.TEXT_CSV.withParameter(DefaultCSVPlugin.PARAM_HEADER_LINE, "absent")));

        JSONAssert.assertEquals(carsJsonNoheader, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_with_columns() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(carsNoheader,
                        MediaTypes.TEXT_CSV
                                .withParameter(DefaultCSVPlugin.PARAM_HEADER_LINE, "absent")
                                .withParameter(DefaultCSVPlugin.PARAM_COLUMNS, "color,type")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_qchar() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(carsQchar,
                        MediaTypes.TEXT_CSV.withParameter(DefaultCSVPlugin.PARAM_QUOTE_CHAR, "'")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_sep() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(carsSep,
                        MediaTypes.TEXT_CSV.withParameter(DefaultCSVPlugin.PARAM_SEPARATOR_CHAR, "|")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_esc() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(carsSep,
                        MediaTypes.TEXT_CSV.withParameter(DefaultCSVPlugin.PARAM_SEPARATOR_CHAR, "|")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() throws JSONException {
        var doc = new Transformer(carsJson)
                .transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.TEXT_CSV);

        assertEquals(cars, doc.getContent());
        assertEquals(MediaTypes.TEXT_CSV, doc.getMediaType());
    }

    @Test
    public void write_object_noheader() throws JSONException {
        var doc = new Transformer(carsJson)
                .transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(),
                        MediaTypes.TEXT_CSV.withParameter(DefaultCSVPlugin.PARAM_HEADER_LINE, "absent"));

        assertEquals(carsNoheader, doc.getContent());
        assertEquals(MediaTypes.TEXT_CSV, doc.getMediaType());
    }

    @Test
    public void write_array_noheader() throws JSONException {
        var doc = new Transformer(carsJsonNoheader)
                .transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.TEXT_CSV);

        assertEquals(carsNoheader, doc.getContent());
        assertEquals(MediaTypes.TEXT_CSV, doc.getMediaType());
    }

    @Test
    public void write_array_with_columns() throws JSONException {
        var doc = new Transformer(carsJsonNoheader)
                .transform(Document.BasicDocument.NULL_INSTANCE, Collections.emptyMap(),
                        MediaTypes.TEXT_CSV
                                .withParameter(DefaultCSVPlugin.PARAM_HEADER_LINE, "present")
                                .withParameter(DefaultCSVPlugin.PARAM_COLUMNS, "color,type"));

        assertEquals(cars, doc.getContent());
        assertEquals(MediaTypes.TEXT_CSV, doc.getMediaType());
    }
}
