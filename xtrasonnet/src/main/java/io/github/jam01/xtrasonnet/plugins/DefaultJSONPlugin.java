package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2023 Jose Montoya.
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
import sjsonnet.Materializer$;
import sjsonnet.Position;
import sjsonnet.Val;
import ujson.BytesRenderer;
import ujson.StringRenderer;
import ujson.Value;

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class DefaultJSONPlugin extends BasePlugin {
    public DefaultJSONPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JSON);
        supportedTypes.add(new MediaType("application", "*+json"));

        writerParams.add(PARAM_FORMAT);

        readerSupportedClasses.add(java.lang.String.class);
        readerSupportedClasses.add(java.lang.CharSequence.class);
        readerSupportedClasses.add(java.nio.file.Path.class);
        readerSupportedClasses.add(java.io.File.class);
        readerSupportedClasses.add(java.nio.ByteBuffer.class);
        readerSupportedClasses.add(byte[].class);

        writerSupportedClasses.add(java.lang.String.class);
        writerSupportedClasses.add(java.lang.CharSequence.class);
        writerSupportedClasses.add(java.nio.ByteBuffer.class);
        writerSupportedClasses.add(java.io.OutputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        throw new UnsupportedOperationException("Use #read(Val, Position)");
    }

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        throw new UnsupportedOperationException("Use #write(Val, MediaType, Class<T>, EvalScope)");
    }

    @Override
    public Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        if (doc.getContent() == null) {
            return new Val.Null(new Position(null, 0));
        }

        Class<?> targetType = doc.getContent().getClass();

        if (String.class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromString((String) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        if (CharSequence.class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromCharSequence((CharSequence) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        if (Path.class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromPath((Path) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        if (File.class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromFile((File) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        if (ByteBuffer.class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromByteBuffer((ByteBuffer) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        if (byte[].class.isAssignableFrom(targetType)) {
            return ujson.Readable.fromByteArray((byte[]) doc.getContent()).transform(new LiteralVisitor(pos));
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        int indent = mediaType.getParameters().containsKey(PARAM_FORMAT) ? 4 : -1;

        if (targetType.isAssignableFrom(String.class)) {
            var sw = Materializer$.MODULE$.apply0(input, StringRenderer.apply(indent, false), ev);
            return new Document.BasicDocument<>(((T) sw.toString()), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(CharSequence.class)) {
            var sw = Materializer$.MODULE$.apply0(input, StringRenderer.apply(indent, false), ev);
            return new Document.BasicDocument<>(((T) sw.toString()), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(OutputStream.class)) {
            var out = Materializer$.MODULE$.apply0(input, BytesRenderer.apply(indent, false), ev);
            return new Document.BasicDocument<>((T) out, MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(ByteBuffer.class)) {
            var out = Materializer$.MODULE$.apply0(input, BytesRenderer.apply(indent, false), ev);
            return new Document.BasicDocument<>((T) ByteBuffer.wrap(out.toByteArray()), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(byte[].class)) {
            var out = Materializer$.MODULE$.apply0(input, BytesRenderer.apply(indent, false), ev);
            return new Document.BasicDocument<>((T) out.toByteArray(), MediaTypes.APPLICATION_JSON);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }
}
