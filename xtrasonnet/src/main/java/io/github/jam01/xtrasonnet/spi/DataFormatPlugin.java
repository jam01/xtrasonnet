package io.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.plugins.LiteralVisitor;
import sjsonnet.EvalScope;
import sjsonnet.Materializer$;
import sjsonnet.Position;
import sjsonnet.Val;
import ujson.Value;
import ujson.Value$;

public interface DataFormatPlugin {

    default boolean canRead(Document<?> doc) {
        return false;
    }

    default boolean canWrite(MediaType mediaType, Class<?> clazz) {
        return false;
    }

    ujson.Value read(Document<?> doc) throws PluginException;

    <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException;

    default Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        return read(doc).transform(new LiteralVisitor(pos));
    }

    default <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        return write(Materializer$.MODULE$.apply0(input, Value$.MODULE$, ev), mediaType, targetType);
    }
}
