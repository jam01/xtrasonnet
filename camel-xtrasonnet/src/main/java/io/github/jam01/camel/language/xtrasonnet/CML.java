package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import java.util.HashMap;
import java.util.Map;

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.header.Header;
import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.spi.Library;
import io.github.jam01.xtrasonnet.spi.PluginException;
import org.apache.camel.Exchange;
import sjsonnet.Importer;
import sjsonnet.Position;
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
    public Map<String, Val.Func> functions(DataFormatService dataFormats, Header header, Importer importer) {
        Map<String, Val.Func> answer = new HashMap<>();
        answer.put("properties", builtin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> properties(params[0])));
        answer.put("header", builtin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> header(params[0], dataFormats, pos)));
        answer.put("exchangeProperty", builtin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> exchangeProperty(params[0], dataFormats, pos)));
        answer.put("variable", builtin(
                new String[]{"key"}, // parameters list
                (params, pos, ev) -> variable(params[0], dataFormats, pos)));

        return answer;
    }

    private Val properties(Val key) {
        if (key instanceof Val.Str) {
            return new Val.Str(dummyPosition(), exchange.get().getContext().resolvePropertyPlaceholders("{{" + ((Val.Str) key).value() + "}}"));
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val header(Val key, DataFormatService dataformats, Position pos) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getMessage().getHeader(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val exchangeProperty(Val key, DataFormatService dataformats, Position pos) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getProperty(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val variable(Val key, DataFormatService dataformats, Position pos){
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getVariable(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    private Val valFrom(Object obj, DataFormatService dataformats, Position pos) {
        Document<?> doc;
        if (obj instanceof Document) {
            doc = (Document<?>) obj;
        } else {
            doc = Document.of(obj, MediaTypes.APPLICATION_JAVA);
        }

        try {
            return dataformats.mandatoryRead(doc, pos);
        } catch (PluginException e) {
            throw new IllegalStateException(e);
        }
    }
}
