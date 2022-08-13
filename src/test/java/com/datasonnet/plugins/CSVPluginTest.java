package com.datasonnet.plugins;

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypeUtils;
import com.datasonnet.document.MediaTypes;
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
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(cars, MediaTypes.APPLICATION_CSV));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_noheader() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(carsNoheader,
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_HEADERS, "false")));

        JSONAssert.assertEquals(carsJsonNoheader, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_with_headers() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(carsNoheader,
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_HEADERS, "color,type")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_qchar() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(carsQchar,
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_QUOTE_CHAR, "'")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_sep() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(carsSep,
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_SEPARATOR_CHAR, "|")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_esc() throws JSONException {
        var doc = new Mapper("payload")
                .transform(new DefaultDocument<>(carsSep,
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_SEPARATOR_CHAR, "|")));

        JSONAssert.assertEquals(carsJson, doc.getContent(), true);
        assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void write_simple() throws JSONException {
        var doc = new Mapper(carsJson)
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_CSV);

        assertEquals(cars, doc.getContent());
        assertEquals(MediaTypes.APPLICATION_CSV, doc.getMediaType());
    }

    @Test
    public void write_object_noheader() throws JSONException {
        var doc = new Mapper(carsJson)
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(),
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_HEADERS, "false"));

        assertEquals(carsNoheader, doc.getContent());
        assertEquals(MediaTypes.APPLICATION_CSV, doc.getMediaType());
    }

    @Test
    public void write_array_noheader() throws JSONException {
        var doc = new Mapper(carsJsonNoheader)
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_CSV);

        assertEquals(carsNoheader, doc.getContent());
        assertEquals(MediaTypes.APPLICATION_CSV, doc.getMediaType());
    }

    @Test
    public void write_array_with_header() throws JSONException {
        var doc = new Mapper(carsJsonNoheader)
                .transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(),
                        MediaTypes.APPLICATION_CSV.withParameter(DefaultCSVFormatPlugin.DS_PARAM_HEADERS, "color,type"));

        assertEquals(cars, doc.getContent());
        assertEquals(MediaTypes.APPLICATION_CSV, doc.getMediaType());
    }
}
