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
import com.datasonnet.spi.AbstractDataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import ujson.Value;

public class DefaultPlainTextFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultPlainTextFormatPlugin() {
        supportedTypes.add(MediaTypes.TEXT_PLAIN);

        readerSupportedClasses.add(String.class);
        writerSupportedClasses.add(String.class);
    }

    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        if (String.class.isAssignableFrom(doc.getContent().getClass())) {
            return ujsonUtils.strValueOf((String) doc.getContent());
        } else {
            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        if (targetType.isAssignableFrom(String.class)) {
            return (Document<T>) new DefaultDocument<>(ujsonUtils.stringValueOf(input), MediaTypes.TEXT_PLAIN);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }
}
