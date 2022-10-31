package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.PluginException;
import ujson.Null$;
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

public class DefaultCSVPlugin extends BaseJacksonPlugin {
    public static final String PARAM_QUOTE_CHAR = "quotechar";
    public static final String PARAM_SEPARATOR_CHAR = "separator";
    public static final String PARAM_ESCAPE_CHAR = "escapechar";
    public static final String PARAM_HEADER_LINE = "header";
    public static final String PARAM_COLUMNS = "columns";

    public static final String HEADER_LN_PRESENT_VALUE = "present";
    public static final String HEADER_LN_ABSENT_VALUE = "absent";

    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    private static final Map<Object, ObjectReader> READER_CACHE = new HashMap<>();

    static {
        CSV_MAPPER.enable(CsvParser.Feature.WRAP_AS_ARRAY);
    }

    public DefaultCSVPlugin() {
        supportedTypes.add(MediaTypes.TEXT_CSV);
        supportedTypes.add(MediaType.parseMediaType("application/csv"));

        readerParams.add(PARAM_QUOTE_CHAR);
        readerParams.add(PARAM_SEPARATOR_CHAR);
        readerParams.add(PARAM_ESCAPE_CHAR);
        readerParams.add(PARAM_HEADER_LINE);
        readerParams.add(PARAM_COLUMNS);

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
            return Null$.MODULE$;
        }

        ObjectReader reader = READER_CACHE.computeIfAbsent(doc.getMediaType().getParameters(), (p) -> {
            CsvSchema.Builder builder = baseBuilderFor(doc.getMediaType());

            // assume header line present unless explicitly a value other than "present"
            boolean headerln = doc.getMediaType().notContainsParameter(PARAM_HEADER_LINE) || doc.getMediaType().isParameterEqual(PARAM_HEADER_LINE, HEADER_LN_PRESENT_VALUE);
            if (headerln) {
                builder.setUseHeader(true); // returning an Obj
                return CSV_MAPPER.readerFor(Map.class).with(builder.build());
            } else {
                builder.setUseHeader(false);
                char separator = doc.getMediaType().getParameterAsChar(PARAM_SEPARATOR_CHAR, CsvSchema.DEFAULT_COLUMN_SEPARATOR);
                List<String> columns = doc.getMediaType().getParameterAsList(PARAM_COLUMNS, separator, Collections.emptyList());
                if (columns.size() > 0) { // columns found in param, return Obj with param columns
                    for (String column : columns) {
                        builder.addColumn(column);
                    }
                    return CSV_MAPPER.readerFor(Map.class).with(builder.build());
                }
                return CSV_MAPPER.readerFor(List.class).with(builder.build()); // skip columns, returns Arr[Arr]
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
        assertArrayNode(node, "Writing CSV requires an Array, found: " + node.getNodeType().name());
        JsonNode first = node.elements().next();

        ObjectWriter writer;
        CsvSchema.Builder builder = baseBuilderFor(mediaType);

        // assume header line present unless explicitly a value other than "present"
        boolean headerln = mediaType.notContainsParameter(PARAM_HEADER_LINE) || mediaType.getParameterAsBoolean(PARAM_HEADER_LINE, HEADER_LN_PRESENT_VALUE);
        char separator = mediaType.getParameterAsChar(PARAM_SEPARATOR_CHAR, CsvSchema.DEFAULT_COLUMN_SEPARATOR);
        List<String> paramColumns = mediaType.getParameterAsList(PARAM_COLUMNS, separator, Collections.emptyList());

        if (first.isObject() && headerln) { // no header param, use first Obj for headers
            builder.setUseHeader(true);
            assertObjectNode(first, "The combination of parameters given requires an Object, found: " + node.getNodeType().name());
            first.fieldNames().forEachRemaining(builder::addColumn);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else if (first.isObject()) { // skip headers, but still need columns -- use first Obj fieldNames as dummies
            builder.setUseHeader(false);
            first.fieldNames().forEachRemaining(builder::addColumn);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else if (first.isArray() && mediaType.notContainsParameter(PARAM_HEADER_LINE)) { // an array and doesn't explicitly want header
            builder.setUseHeader(false);
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else if (first.isArray() && headerln) {
            if (paramColumns.isEmpty()) throw new IllegalArgumentException("Cannot satisfy parameter " + PARAM_HEADER_LINE + " for an Arr without column names in " + PARAM_COLUMNS);
            builder.setUseHeader(true);
            for (String column : paramColumns) {
                builder.addColumn(column);
            }
            writer = CSV_MAPPER.writerFor(JsonNode.class).with(builder.build());
        } else {
            throw new IllegalArgumentException("Unsupported combination of input and parameters."); // we give up
        }

        try {
            if (targetType.isAssignableFrom(String.class)) {
                return (Document<T>) new Document.BasicDocument<>(writer.writeValueAsString(node),
                        MediaTypes.TEXT_CSV);
            }

            if (targetType.isAssignableFrom(OutputStream.class)) {
                OutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
                writer.writeValue(out, node);
                return (Document<T>) new Document.BasicDocument<>(out, MediaTypes.TEXT_CSV);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return (Document<T>) new Document.BasicDocument<>(writer.writeValueAsBytes(node),
                        MediaTypes.TEXT_CSV);
            }
            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canWrite before invoking write"));
        } catch (IOException e) {
            throw new PluginException("Unable to processing CSV", e);
        }
    }

    private CsvSchema.Builder baseBuilderFor(MediaType type) {
        CsvSchema.Builder builder = CsvSchema.builder();

        // no quotes or quote char
        if (type.isParameterEqual(PARAM_QUOTE_CHAR, "")) {
            builder.disableQuoteChar();
        } else {
            builder.setQuoteChar(type.getParameterAsChar(PARAM_QUOTE_CHAR, CsvSchema.DEFAULT_QUOTE_CHAR));
        }

        // separator char
        builder.setColumnSeparator(type.getParameterAsChar(PARAM_SEPARATOR_CHAR, CsvSchema.DEFAULT_COLUMN_SEPARATOR));

        // escape char
        builder.setEscapeChar(type.getParameterAsChar(PARAM_ESCAPE_CHAR, (char) CsvSchema.DEFAULT_ESCAPE_CHAR));

        return builder;
    }
}
