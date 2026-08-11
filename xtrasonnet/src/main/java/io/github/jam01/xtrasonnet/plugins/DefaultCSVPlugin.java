package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.PluginException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCSVPlugin extends BaseJacksonPlugin {
    public static final String PARAM_QUOTE_CHAR = "quotechar";
    public static final String PARAM_SEPARATOR_CHAR = "separator";
    public static final String PARAM_ESCAPE_CHAR = "escapechar";
    public static final String PARAM_HEADER_LINE = "header";
    public static final String PARAM_COLUMNS = "columns";

    public static final String HEADER_LN_PRESENT_VALUE = "present";
    public static final String HEADER_LN_ABSENT_VALUE = "absent";

    // built rather than mutated after construction, so there is no window in which another thread
    // could observe it half-configured. A configured CsvMapper is safe to share.
    private static final CsvMapper CSV_MAPPER = CsvMapper.builder()
            .enable(CsvParser.Feature.WRAP_AS_ARRAY)
            .build();

    // Per instance, and concurrent: this was a static HashMap shared by every Transformer in the
    // JVM and mutated through computeIfAbsent, so concurrent inserts could corrupt it -- which broke
    // even callers who correctly kept one Transformer per thread. ObjectReader is immutable, so the
    // entries themselves are safe to share.
    private final Map<Map<String, String>, ObjectReader> readerCache = new ConcurrentHashMap<>();

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
    public JsonNode read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return NullNode.getInstance();
        }

        // MediaType.getParameters returns an unmodifiable map, so it is safe as a key
        ObjectReader reader = readerCache.computeIfAbsent(doc.getMediaType().getParameters(), (p) -> {
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
                if (!columns.isEmpty()) { // columns found in param, return Obj with param columns
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
                return reader.readTree((String) doc.getContent());
            } else if (byte[].class.isAssignableFrom(doc.getContent().getClass())) {
                return reader.readTree(((byte[]) doc.getContent()));
            } else if (InputStream.class.isAssignableFrom(doc.getContent().getClass())) {
                return reader.readTree((InputStream) doc.getContent());
            } else {
                throw unsupportedReadClass(doc);
            }
        } catch (JsonProcessingException jpe) {
            throw new PluginException("Unable to convert CSV to JSON", jpe);
        } catch (IOException e) {
            throw new PluginException("Unable to read the byte array", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(JsonNode node, MediaType mediaType, Class<T> targetType) throws PluginException {
        assertArrayNode(node, "Writing CSV requires an Array, found: " + node.getNodeType().name());
        if (node.isEmpty()) { // nothing to infer a schema from, and no rows to write
            return writeCsv(CSV_MAPPER.writerFor(JsonNode.class).with(baseBuilderFor(mediaType).build()), node, mediaType, targetType);
        }
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
            // name what was actually considered; the previous text was a dead end for the user
            throw new IllegalArgumentException(("Cannot write CSV from an Array of %s with %s=%s and %s=%s. "
                    + "Writing an Array of Arr with a header line requires %s to name the columns.").formatted(
                    first.getNodeType().name().toLowerCase(), PARAM_HEADER_LINE, headerln ? HEADER_LN_PRESENT_VALUE : HEADER_LN_ABSENT_VALUE,
                    PARAM_COLUMNS, paramColumns.isEmpty() ? "<unset>" : paramColumns, PARAM_COLUMNS));
        }

        return writeCsv(writer, node, mediaType, targetType);
    }

    @SuppressWarnings("unchecked")
    private <T> Document<T> writeCsv(ObjectWriter writer, JsonNode node, MediaType mediaType, Class<T> targetType) throws PluginException {
        try {
            if (targetType.isAssignableFrom(String.class)) {
                return (Document<T>) new Document.BasicDocument<>(writer.writeValueAsString(node),
                        MediaTypes.TEXT_CSV);
            }

            if (targetType.isAssignableFrom(OutputStream.class)) {
                // must be the ByteArrayOutputStream itself: wrapping it hands back a stream whose
                // bytes the caller has no way to reach
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                writer.writeValue(out, node);
                return (Document<T>) new Document.BasicDocument<>(out, MediaTypes.TEXT_CSV);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return (Document<T>) new Document.BasicDocument<>(writer.writeValueAsBytes(node),
                        MediaTypes.TEXT_CSV);
            }
            throw unsupportedWriteClass(mediaType, targetType);
        } catch (IOException e) {
            throw new PluginException("Unable to write CSV", e);
        }
    }

    private CsvSchema.Builder baseBuilderFor(MediaType type) {
        CsvSchema.Builder builder = CsvSchema.builder();
        builder.setLineSeparator("\r\n"); // https://www.ietf.org/rfc/rfc4180.html#section-2

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
