package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.JsonNode;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.render.Renderer;
import io.github.jam01.xtrasonnet.spi.BasePlugin;
import io.github.jam01.xtrasonnet.spi.PluginException;
import sjsonnet.EvalScope;
import sjsonnet.Materializer$;
import sjsonnet.Position;
import sjsonnet.Val;
import ujson.ByteArrayParser$;
import ujson.ByteBufferParser$;
import ujson.CharSequenceParser$;
import ujson.InputStreamParser;
import ujson.InputStreamParser$;
import ujson.StringParser$;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultJSONPlugin extends BasePlugin {
    public DefaultJSONPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JSON);
        supportedTypes.add(new MediaType("application", "*+json"));

        writerParams.add(PARAM_FORMAT);

        readerSupportedClasses.add(String.class);
        readerSupportedClasses.add(CharSequence.class);
        readerSupportedClasses.add(Path.class);
        readerSupportedClasses.add(File.class);
        readerSupportedClasses.add(ByteBuffer.class);
        readerSupportedClasses.add(byte[].class);
        readerSupportedClasses.add(InputStream.class);

        writerSupportedClasses.add(String.class);
        writerSupportedClasses.add(CharSequence.class);
        writerSupportedClasses.add(ByteBuffer.class);
        writerSupportedClasses.add(OutputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    @Override
    public JsonNode read(Document<?> doc) throws PluginException {
        throw new UnsupportedOperationException("Use #read(Val, Position)");
    }

    @Override
    public <T> Document<T> write(JsonNode input, MediaType mediaType, Class<T> targetType) throws PluginException {
        throw new UnsupportedOperationException("Use #write(Val, MediaType, Class<T>, EvalScope)");
    }

    @Override
    public Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        if (doc.getContent() == null) {
            return new Val.Null(new Position(null, 0));
        }

        Class<?> targetType = doc.getContent().getClass();

        if (String.class.isAssignableFrom(targetType)) {
            return StringParser$.MODULE$.transform((String) doc.getContent(), new LiteralVisitor(pos));
        }

        if (CharSequence.class.isAssignableFrom(targetType)) {
            return CharSequenceParser$.MODULE$.transform((CharSequence) doc.getContent(), new LiteralVisitor(pos));
        }

        if (Path.class.isAssignableFrom(targetType)) {
            return fromPath((Path) doc.getContent(), new LiteralVisitor(pos));
        }

        if (File.class.isAssignableFrom(targetType)) {
            return fromPath(((File) doc.getContent()).toPath(), new LiteralVisitor(pos));
        }

        if (ByteBuffer.class.isAssignableFrom(targetType)) {
            return ByteBufferParser$.MODULE$.transform((ByteBuffer) doc.getContent(), new LiteralVisitor(pos));
        }

        if (byte[].class.isAssignableFrom(targetType)) {
            return ByteArrayParser$.MODULE$.transform((byte[]) doc.getContent(), new LiteralVisitor(pos));
        }

        if (InputStream.class.isAssignableFrom(targetType)) {
            return InputStreamParser$.MODULE$.transform(((InputStream) doc.getContent()), new LiteralVisitor(pos));
        }

        throw unsupportedReadClass(doc);
    }

    private static Val.Literal fromPath(Path s, LiteralVisitor v) {
        try (final var inputStream = Files.newInputStream(s)) {
            return InputStreamParser.transform(inputStream, v);
        } catch (IOException e) { throw new PluginException(e); }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        // UTF-8, not the platform default -- and it is now actually applied. This was computed and
        // then never read, so every byte-producing branch emitted the renderer's UTF-8 regardless of
        // what the caller asked for.
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        int indent = mediaType.getParameters().containsKey(PARAM_FORMAT) ? 4 : -1;

        if (targetType.isAssignableFrom(String.class)) {
            var sw = Materializer$.MODULE$.apply0(input, Renderer.stringRenderer(indent, false), ev);
            return new Document.BasicDocument<>(((T) sw.toString()), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(CharSequence.class)) {
            var sw = Materializer$.MODULE$.apply0(input, Renderer.stringRenderer(indent, false), ev);
            return new Document.BasicDocument<>(((T) sw.toString()), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(OutputStream.class)) {
            return new Document.BasicDocument<>((T) renderBytes(input, indent, charset, ev), MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(ByteBuffer.class)) {
            return new Document.BasicDocument<>((T) ByteBuffer.wrap(renderBytes(input, indent, charset, ev).toByteArray()),
                    MediaTypes.APPLICATION_JSON);
        }

        if (targetType.isAssignableFrom(byte[].class)) {
            return new Document.BasicDocument<>((T) renderBytes(input, indent, charset, ev).toByteArray(),
                    MediaTypes.APPLICATION_JSON);
        }

        throw unsupportedWriteClass(mediaType, targetType);
    }

    /**
     * The byte renderer emits UTF-8, which is what RFC 8259 s8.1 requires for interchange and what
     * callers get by default. Re-encode only when a different charset was explicitly requested.
     */
    private static ByteArrayOutputStream renderBytes(Val input, int indent, Charset charset, EvalScope ev) {
        if (StandardCharsets.UTF_8.equals(charset)) {
            return Materializer$.MODULE$.apply0(input, Renderer.bytesRenderer(indent, false), ev);
        }

        byte[] bytes = Materializer$.MODULE$.apply0(input, Renderer.stringRenderer(indent, false), ev)
                .toString().getBytes(charset);
        var out = new ByteArrayOutputStream(bytes.length);
        out.writeBytes(bytes);
        return out;
    }
}
