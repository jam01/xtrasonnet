package com.datasonnet.plugins;

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
/*-
 * Changed:
 * - Re-implemented plugin under new interface, only keeping the central Schema building
 * Adopted:
 * - 695eba21d86ca7ab9b1812ce7689af41db4c83a4: fix up lost disablequotes support for CSVs
 */

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.PluginException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ujson.Value;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultCSVFormatPlugin extends BaseJacksonDataFormatPlugin {
    public static final String DS_PARAM_QUOTE_CHAR = "qchar";
    public static final String DS_PARAM_SEPARATOR_CHAR = "sep";
    public static final String DS_PARAM_ESCAPE_CHAR = "esc";
    public static final String DS_PARAM_HEADERS = "head";

    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final Map<Object, ObjectReader> READER_CACHE = new HashMap<>();

    static {
        CSV_MAPPER.enable(CsvParser.Feature.WRAP_AS_ARRAY);
    }

    public DefaultCSVFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_CSV);
        supportedTypes.add(MediaType.parseMediaType("text/csv"));

        readerParams.add(DS_PARAM_QUOTE_CHAR);
        readerParams.add(DS_PARAM_SEPARATOR_CHAR);
        readerParams.add(DS_PARAM_ESCAPE_CHAR);
        readerParams.add(DS_PARAM_HEADERS);

        writerParams.addAll(readerParams);

        readerSupportedClasses.add(InputStream.class);
        readerSupportedClasses.add(byte[].class);
        readerSupportedClasses.add(String.class);

        writerSupportedClasses.add(OutputStream.class);
        writerSupportedClasses.add(byte[].class);
        writerSupportedClasses.add(String.class);
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        ObjectReader reader = READER_CACHE.computeIfAbsent(doc.getMediaType().getParameters(), (p) -> {
            CsvSchema.Builder builder = baseBuilderFor(doc.getMediaType());
            if (paramAbsent(doc.getMediaType(), DS_PARAM_HEADERS)) { // no header param, return JSON Obj
                builder.setUseHeader(true);
                return CSV_MAPPER.readerFor(Map.class).with(builder.build());
            } else if (paramEq(doc.getMediaType(), DS_PARAM_HEADERS, "false")) { // skip headers, return JSON Arr[Arr]
                builder.setUseHeader(false);
                return CSV_MAPPER.readerFor(List.class).with(builder.build());
            } else {
                List<String> headers = paramAsList(doc.getMediaType(), DS_PARAM_HEADERS, Collections.emptyList());
                if (headers.size() > 0) { // headers found in param, return JSON Obj with param headers
                    builder.setUseHeader(false);
                    for (String header : headers) {
                        builder.addColumn(header);
                    }
                    return CSV_MAPPER.readerFor(Map.class).with(builder.build());
                }
                throw new IllegalArgumentException("'" + DS_PARAM_HEADERS + "' parameter must be a comma separated list of headers!");
            }
        });

        // Read data from CSV file
        try {
            if (String.class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = reader.readTree((String) doc.getContent());
                return ujsonFrom(result);
            } else if (byte[].class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = reader.readTree((String) doc.getContent());
                return ujsonFrom(result);
            } else if (InputStream.class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = reader.readTree((String) doc.getContent());
                return ujsonFrom(result);
            } else {
                throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
            }
        } catch (JsonProcessingException jpe) {
            throw new PluginException("Unable to convert CSV to JSON", jpe);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        JsonNode node = jsonNodeOf(input);
        assertArrayNode(node);
        JsonNode first = node.elements().next();

        ObjectWriter writer;
        CsvSchema.Builder builder = baseBuilderFor(mediaType);
        if (first.isObject() && paramAbsent(mediaType, DS_PARAM_HEADERS)) { // no header param, use first Obj for headers
            builder.setUseHeader(true);
            assertObjectNode(first);
            first.fieldNames().forEachRemaining(builder::addColumn);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else if (first.isObject() && paramEq(mediaType, DS_PARAM_HEADERS, "false")) { // skip headers, but still need columns - user first Obj
            builder.setUseHeader(false);
            first.fieldNames().forEachRemaining(builder::addColumn);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else if (first.isArray() &&
                (paramAbsent(mediaType, DS_PARAM_HEADERS) || paramEq(mediaType, DS_PARAM_HEADERS, "false"))) { // an array and no given headers
            builder.setUseHeader(false);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else {
            List<String> headers = paramAsList(mediaType, DS_PARAM_HEADERS, Collections.emptyList());
            if (first.isArray() && headers.size() > 0) { // headers
                builder.setUseHeader(true);
                for (String header : headers) {
                    builder.addColumn(header);
                }
                writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
            } else {
                throw new IllegalArgumentException("Unsupported combination of input and parameters."); // we give up
            }
        }

        try {
            if (targetType.isAssignableFrom(String.class)) {
                return (Document<T>) new DefaultDocument<>(writer.writeValueAsString(node),
                        MediaTypes.APPLICATION_CSV);
            }

            if (targetType.isAssignableFrom(OutputStream.class)) {
                OutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
                writer.writeValue(out, node);
                return (Document<T>) new DefaultDocument<>(out, MediaTypes.APPLICATION_CSV);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return (Document<T>) new DefaultDocument<>(writer.writeValueAsBytes(node),
                        MediaTypes.APPLICATION_CSV);
            }
            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canWrite before invoking write"));
        } catch (IOException e) {
            throw new PluginException("Unable to processing CSV", e);
        }
    }

    private CsvSchema.Builder baseBuilderFor(MediaType type) {
        CsvSchema.Builder builder = CsvSchema.builder();

        // no quotes or quote char
        if (paramEq(type, DS_PARAM_QUOTE_CHAR, "false")) {
            builder.disableQuoteChar();
        } else {
            builder.setQuoteChar(paramAsChar(type, DS_PARAM_QUOTE_CHAR, '"'));
        }

        // separator char
        builder.setColumnSeparator(paramAsChar(type, DS_PARAM_SEPARATOR_CHAR, ','));

        // escape
        builder.setEscapeChar(paramAsChar(type, DS_PARAM_ESCAPE_CHAR, '\\'));

        return builder;
    }
}
