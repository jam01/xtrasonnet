package com.datasonnet.plugins;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import ujson.Value;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class DefaultJavaFormatPlugin extends BaseJacksonDataFormatPlugin {
    private final JsonMapper mapper;
    public static final String DS_PARAM_DATE_FORMAT = "dateformat";
    public static final String DS_PARAM_TYPE = "type";

    private static final Map<Map<String, String>, ObjectMapper> MAPPER_CACHE = new HashMap<>();

    public DefaultJavaFormatPlugin() {
        this(null);
    }

    public DefaultJavaFormatPlugin(JsonMapper mapper) {
        if (mapper == null) {
            mapper = JsonMapper.builder()
                    .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                    .defaultDateFormat(new StdDateFormat())
                    .build();
        }

        this.mapper = mapper;

        supportedTypes.add(MediaTypes.APPLICATION_JAVA);
        supportedTypes.add(MediaType.parseMediaType("application/java"));

        readerParams.add(DS_PARAM_DATE_FORMAT);
        readerParams.add(DS_PARAM_TYPE);
        writerParams.addAll(readerParams);
    }

    @Override
    protected boolean canReadClass(Class<?> cls) {
        return true;
    }

    @Override
    protected boolean canWriteClass(Class<?> clazz) {
        return true;
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        ObjectMapper mapper = mapperFor(doc.getMediaType());
        // TODO: 8/11/22 can we go direct to ujson somehow?
        JsonNode inputAsNode = mapper.valueToTree(doc.getContent());
        return ujsonFrom(inputAsNode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        // TODO: 8/11/22 should throw instead? null a valid result when requesting anything?
        if (input == ujson.Null$.MODULE$) {
            return (Document<T>) DefaultDocument.NULL_INSTANCE;
        }

        ObjectMapper mapper = mapperFor(mediaType);
        T converted;

        try {
            Object inputAsJava = ujsonUtils.javaObjectFrom(input);
            if (targetType.isAssignableFrom(inputAsJava.getClass())) {
                converted = (T) inputAsJava;
            } else {
                converted = mapper.convertValue(inputAsJava, targetType);
            }
        } catch (IllegalArgumentException e) {
            throw new PluginException("Unable to convert to target type", e);
        }

        return new DefaultDocument<>(converted, mediaType.withParameter(DS_PARAM_TYPE, targetType.getName()));
    }

    private ObjectMapper mapperFor(MediaType mediaType) throws PluginException {
        Map<String, String> parameters = mediaType.getParameters();
        if (parameters.containsKey(DS_PARAM_DATE_FORMAT)) {
            return MAPPER_CACHE.computeIfAbsent(parameters, k -> {
                JsonMapper.Builder builder = new JsonMapper.Builder(mapper.copy());
                String datefmt = parameters.get(DS_PARAM_DATE_FORMAT);

                if (datefmt != null) {
                    builder.defaultDateFormat(new SimpleDateFormat(datefmt));
                }

                return builder.build();
            });
        }

        return mapper;
    }
}
