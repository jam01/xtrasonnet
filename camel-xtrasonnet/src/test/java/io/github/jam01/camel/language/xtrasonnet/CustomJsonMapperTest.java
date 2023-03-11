package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomJsonMapperTest extends CamelTestSupport {
    public final JsonMapper custom = new JsonMapper();

    public CustomJsonMapperTest() {
        custom.setDateFormat(new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss"));
        custom.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC.getId()));
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        registry.bind("customMapper", custom);
    }

    public static final String json = """
            { "date": "Wed, 4 Jul 2001 12:08:56" }""";
    public static final Example object = new Example(Date.from(LocalDateTime.of(2001, 7, 4, 12, 8, 56).toInstant(ZoneOffset.UTC)));

    @Test
    public void read_custom_mapper() throws JSONException {
        var exp = new XtrasonnetExpression("payload");
        exp.setOutputMediaType(MediaTypes.APPLICATION_JSON);
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Document.of(object)), String.class);
        JSONAssert.assertEquals(json, res, true);
    }

    @Test
    public void write_custom_mapper() throws JSONException {
        var exp = new XtrasonnetExpression("payload");
        exp.setResultType(Example.class);
        exp.init(context());

        var res = exp.evaluate(createExchangeWithBody(Document.of(json, MediaTypes.APPLICATION_JSON)), Example.class);
        assertEquals(object, res);
    }

    public static class Example {
        public Example() {
        }

        public Example(Date date) {
            this.date = date;
        }

        public Date date;

        public void setDate(Date date) {
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Example example = (Example) o;

            return Objects.equals(date, example.date);
        }

        @Override
        public int hashCode() {
            return date != null ? date.hashCode() : 0;
        }
    }
}
