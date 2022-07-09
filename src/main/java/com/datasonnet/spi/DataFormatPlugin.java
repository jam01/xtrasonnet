package com.datasonnet.spi;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import ujson.Value;

public interface DataFormatPlugin {

    default boolean canRead(Document<?> doc) {
        return false;
    }

    default boolean canWrite(MediaType mediaType, Class<?> clazz) {
        return false;
    }

    ujson.Value read(Document<?> doc) throws PluginException;

    <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException;
}
