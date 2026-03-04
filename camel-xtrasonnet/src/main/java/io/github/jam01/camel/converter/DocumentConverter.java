package io.github.jam01.camel.converter;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypeUtils;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.spi.TypeConverterRegistry;

/**
 * Camel type converter for converting between {@link Document} objects and other types.
 * <p>
 * Provides converters for media type strings and fallback conversion from Document to any type.
 * </p>
 */
@Converter(generateLoader = true)
public class DocumentConverter {
    /**
     * Converts a media type string to a {@link MediaType} object.
     *
     * @param mediaType the media type string to parse
     * @return the parsed MediaType instance
     */
    @Converter
    public static MediaType toMediaType(String mediaType) {
        return MediaTypeUtils.parseMediaType(mediaType);
    }

    /**
     * Fallback converter that attempts to convert a {@link Document} to the requested type.
     * <p>
     * If the value is not a Document, returns null. If the Document's content is null, returns null.
     * If the target type is assignable from the content's class, returns the content directly.
     * Otherwise, looks up a suitable type converter from the registry and uses it.
     * </p>
     *
     * @param type the target type class
     * @param exchange the Camel exchange (may be null)
     * @param value the value to convert (expected to be a Document)
     * @param registry the type converter registry
     * @param <T> the target type
     * @return the converted value, or null if conversion not possible
     */
    @SuppressWarnings("unchecked")
    @Converter(fallback = true)
    public static <T> T convertTo(Class<T> type, Exchange exchange, Object value, TypeConverterRegistry registry) {
        if (!Document.class.isAssignableFrom(value.getClass())) {
            return null;
        }

        Document<?> doc = ((Document<?>) value);
        if (doc.getContent() == null) {
            return null;
        }

        if (type.isAssignableFrom(doc.getContent().getClass())) {
            return (T) doc.getContent();
        }

        TypeConverter tc = registry.lookup(type, doc.getContent().getClass());
        if (tc != null) {
            return tc.convertTo(type, exchange, doc.getContent());
        }

        return null;
    }
}
