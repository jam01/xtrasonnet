package io.github.jam01.camel.converter;

/*-
 * Copyright 2022-2023 Jose Montoya.
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

@Converter(generateLoader = true)
public class DocumentConverter {
    @Converter
    public static MediaType toMediaType(String mediaType) {
        return MediaTypeUtils.parseMediaType(mediaType);
    }

    @SuppressWarnings("unchecked")
    @Converter(fallback = true)
    public static <T> T convertTo(Class<T> type, Exchange exchange, Object value, TypeConverterRegistry registry) {
        if (Document.class.isAssignableFrom(value.getClass())) {
            Document<?> doc = ((Document<?>) value);
            if (type.isAssignableFrom(doc.getContent().getClass())) {
                return (T) doc.getContent();
            }

            TypeConverter tc = registry.lookup(type, doc.getContent().getClass());
            if (tc != null) {
                return tc.convertTo(type, exchange, doc.getContent());
            }
        }
        return null;
    }
}
