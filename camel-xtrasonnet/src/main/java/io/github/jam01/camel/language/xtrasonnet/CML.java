package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.DataFormatService;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.JLibrary;
import io.github.jam01.xtrasonnet.spi.PluginException;
import org.apache.camel.Exchange;
import sjsonnet.Position;
import sjsonnet.Val;

import java.util.HashMap;
import java.util.Map;

/**
 * Camel Module Library (CML) provides xtrasonnet functions for accessing Camel exchange properties,
 * headers, variables, and properties within dataformats transformations.
 * <p>
 * This library is automatically available in xtrasonnet expressions used within Camel routes.
 * </p>
 */
public final class CML extends JLibrary {
    private static final CML INSTANCE = new CML(DataFormatService.DEFAULT);
    private final ThreadLocal<Exchange> exchange = new ThreadLocal<>();
    private final DataFormatService dataFormats;

    /**
     * Private constructor.
     *
     * @param dataFormats the data format service to use
     */
    private CML(DataFormatService dataFormats) {
        this.dataFormats = dataFormats;
    }

    /**
     * Returns the singleton instance of CML.
     *
     * @return the singleton instance
     */
    public static CML getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the thread-local exchange holder.
     * <p>
     * Used to associate the current Camel exchange with the xtrasonnet evaluation thread.
     * </p>
     *
     * @return the thread-local exchange holder
     */
    public ThreadLocal<Exchange> getExchange() {
        return exchange;
    }

    /**
     * Returns the library name.
     *
     * @return the name "cml"
     */
    @Override
    public String name() {
        return "cml";
    }

    /**
     * Returns the library functions provided by CML.
     * <p>
     * The functions include:
     * <ul>
     *   <li>{@code properties(key)}: resolves a property placeholder from Camel context</li>
     *   <li>{@code header(key)}: retrieves a header from the exchange message</li>
     *   <li>{@code exchangeProperty(key)}: retrieves an exchange property</li>
     *   <li>{@code variable(key)}: retrieves a variable from the exchange</li>
     * </ul>
     *
     * @return a map from function name to its implementation
     */
    @Override
    public Map<String, Val.Func> functions() {
        Map<String, Val.Func> answer = new HashMap<>();
        answer.put("properties", jbuiltin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> properties(params[0])));
        answer.put("header", jbuiltin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> header(params[0], dataFormats, pos)));
        answer.put("exchangeProperty", jbuiltin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> exchangeProperty(params[0], dataFormats, pos)));
        answer.put("variable", jbuiltin(
                new String[]{"key"}, //parameters list
                (params, pos, ev) -> variable(params[0], dataFormats, pos)));

        return answer;
    }

    /**
     * Implements the {@code properties} function.
     *
     * @param key the property key as a string
     * @return the resolved property value as a string
     * @throws IllegalArgumentException if key is not a string
     */
    private Val properties(Val key) {
        if (key instanceof Val.Str) {
            return new Val.Str(position(), exchange.get().getContext().resolvePropertyPlaceholders("{{" + ((Val.Str) key).value() + "}}"));
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    /**
     * Implements the {@code header} function.
     *
     * @param key the header key as a string
     * @param dataformats the data format service for conversion
     * @param pos the position in the source script (for error reporting)
     * @return the header value converted to a Val
     * @throws IllegalArgumentException if key is not a string
     */
    private Val header(Val key, DataFormatService dataformats, Position pos) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getMessage().getHeader(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    /**
     * Implements the {@code exchangeProperty} function.
     *
     * @param key the exchange property key as a string
     * @param dataformats the data format service for conversion
     * @param pos the position in the source script (for error reporting)
     * @return the exchange property value converted to a Val
     * @throws IllegalArgumentException if key is not a string
     */
    private Val exchangeProperty(Val key, DataFormatService dataformats, Position pos) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getProperty(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    /**
     * Implements the {@code variable} function.
     *
     * @param key the variable key as a string
     * @param dataformats the data format service for conversion
     * @param pos the position in the source script (for error reporting)
     * @return the variable value converted to a Val
     * @throws IllegalArgumentException if key is not a string
     */
    private Val variable(Val key, DataFormatService dataformats, Position pos) {
        if (key instanceof Val.Str) {
            return valFrom(exchange.get().getVariable(((Val.Str) key).value()), dataformats, pos);
        }
        throw new IllegalArgumentException("Expected String got: " + key.prettyName());
    }

    /**
     * Converts an arbitrary object to a Val using the data format service.
     * <p>
     * If the object is already a {@link Document}, uses it directly; otherwise wraps it in a Document with
     * {@link MediaTypes#APPLICATION_JAVA} media type.
     * </p>
     *
     * @param obj the object to convert
     * @param dataformats the data format service
     * @param pos the position in the source script (for error reporting)
     * @return the converted Val
     * @throws IllegalStateException if the data format plugin fails to read the document
     */
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
