package io.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.plugins.JsonNodeVisitor$;
import io.github.jam01.xtrasonnet.plugins.LiteralVisitor;
import sjsonnet.EvalScope;
import sjsonnet.Materializer$;
import sjsonnet.Position;
import sjsonnet.Val;

public interface DataFormatPlugin {

    default boolean canRead(Document<?> doc) {
        return false;
    }

    default boolean canWrite(MediaType mediaType, Class<?> clazz) {
        return false;
    }

    JsonNode read(Document<?> doc) throws PluginException;

    <T> Document<T> write(JsonNode input, MediaType mediaType, Class<T> targetType) throws PluginException;

    default Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        return JsonNodeVisitor$.MODULE$.transform(read(doc), new LiteralVisitor(pos));
    }

    default <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        return write(Materializer$.MODULE$.apply0(input, JsonNodeVisitor$.MODULE$, ev), mediaType, targetType);
    }
}
