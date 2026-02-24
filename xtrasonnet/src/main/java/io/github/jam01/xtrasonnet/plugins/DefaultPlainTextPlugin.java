package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.BasePlugin;
import io.github.jam01.xtrasonnet.spi.PluginException;
import sjsonnet.EvalScope;
import sjsonnet.Position;
import sjsonnet.Val;

public class DefaultPlainTextPlugin extends BasePlugin {
    public DefaultPlainTextPlugin() {
        supportedTypes.add(MediaTypes.TEXT_PLAIN);

        readerSupportedClasses.add(String.class);
        writerSupportedClasses.add(String.class);
    }

    @Override
    public Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        if (doc.getContent() == null) {
            return new Val.Null(pos);
        }

        if (String.class.isAssignableFrom(doc.getContent().getClass())) {
            return new Val.Str(pos, (String) doc.getContent());
        } else {
            throw new PluginException("Unsupported document content class, use the test method canRead before invoking read");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        if (!(input instanceof Val.Str)) {
            throw new PluginException("Input for Plain Text writer must be a String, got " + input.prettyName());
        }

        if (targetType.isAssignableFrom(String.class)) {
            return (Document<T>) new Document.BasicDocument<>(input.asString(), MediaTypes.TEXT_PLAIN);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }
}
