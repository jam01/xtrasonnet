package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.header.Header;
import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.spi.Library;
import io.github.jam01.xtrasonnet.spi.PluginException;
import org.apache.camel.Exchange;
import sjsonnet.Importer;
import sjsonnet.Materializer$;
import sjsonnet.Val;

public final class CML extends Library {
    private static final CML INSTANCE = new CML();
    private final ThreadLocal<Exchange> exchange = new ThreadLocal<>();

    private CML() {
    }

    public static CML getInstance() {
        return INSTANCE;
    }

    public ThreadLocal<Exchange> getExchange() {
        return exchange;
    }

    @Override
    public String namespace() {
        return "cml";
    }

    @Override
    public Set<String> libsonnets() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Val.Func> functions(DataFormatService dataFormats, Header header, Importer importer) {
        Map<String, Val.Func> answer = new HashMap<>();
        answer.put("properties", makeSimpleFunc(
                Collections.singletonList("key"), //parameters list
                params -> properties(params.get(0))));
        answer.put("header", makeSimpleFunc(
                Collections.singletonList("key"), //parameters list
                params -> header(params.get(0), dataFormats)));
        answer.put("exchangeProperty", makeSimpleFunc(
                Collections.singletonList("key"), //parameters list
                params -> exchangeProperty(params.get(0), dataFormats)));

        return answer;
    }

    private Val properties(Val key) {
        if (key instanceof Val.Str) {
            return new Val.Str(dummyPosition(), exchange.get().getContext().resolvePropertyPlaceholders("{{" + ((Val.Str) key).value() + "}}"));
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val header(Val key, DataFormatService dataformats) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getMessage().getHeader(((Val.Str) key).value()), dataformats);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val exchangeProperty(Val key, DataFormatService dataformats) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getProperty(((Val.Str) key).value()), dataformats);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val valFrom(Object obj, DataFormatService dataformats) {
        Document<?> doc;
        if (obj instanceof Document) {
            doc = (Document<?>) obj;
        } else {
            doc = Document.of(obj, MediaTypes.APPLICATION_JAVA);
        }

        try {
            return Materializer$.MODULE$.reverse(dummyPosition(), dataformats.mandatoryRead(doc));
        } catch (PluginException e) {
            throw new IllegalStateException(e);
        }
    }
}
