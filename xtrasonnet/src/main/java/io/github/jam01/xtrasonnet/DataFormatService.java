package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.plugins.DefaultCSVPlugin;
import io.github.jam01.xtrasonnet.plugins.DefaultExcelPlugin;
import io.github.jam01.xtrasonnet.plugins.DefaultJSONPlugin;
import io.github.jam01.xtrasonnet.plugins.DefaultJavaPlugin;
import io.github.jam01.xtrasonnet.plugins.DefaultPlainTextPlugin;
import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin$;
import io.github.jam01.xtrasonnet.spi.DataFormatPlugin;
import io.github.jam01.xtrasonnet.spi.PluginException;
import sjsonnet.EvalScope;
import sjsonnet.Position;
import sjsonnet.Val;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class DataFormatService {
    private final List<DataFormatPlugin> plugins;
    public static final DataFormatService DEFAULT =
            new DataFormatService(Arrays.asList(new DefaultJSONPlugin(), new DefaultJavaPlugin(), DefaultXMLPlugin$.MODULE$,
                    new DefaultCSVPlugin(), new DefaultPlainTextPlugin(), new DefaultExcelPlugin()));

    public DataFormatService(List<DataFormatPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<DataFormatPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public Optional<DataFormatPlugin> thatCanWrite(MediaType output, Class<?> target) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canWrite(output, target)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public Optional<DataFormatPlugin> thatCanRead(Document<?> doc) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canRead(doc)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public <T> Document<T> mandatoryWrite(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        return thatCanWrite(mediaType, targetType)
                .orElseThrow(() -> new IllegalArgumentException("The output MediaType " + mediaType + " is not supported for " + targetType))
                .write(input, mediaType, targetType, ev);
    }

    public Val.Literal mandatoryRead(Document<?> doc, Position pos) throws PluginException {
        return thatCanRead(doc)
                .orElseThrow(() -> new IllegalArgumentException("The input MediaType " + doc.getMediaType() + " is not supported for " + doc.getContent().getClass()))
                .read(doc, pos);
    }
}
