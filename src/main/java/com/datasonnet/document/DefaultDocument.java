package com.datasonnet.document;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/***
 * The DefaultDocument class
 *
 * @param <T> the content
 */
public class DefaultDocument<T> implements Document<T> {
    private final T content;
    private final MediaType mediaType;

    public final static Document<Object> NULL_INSTANCE = new DefaultDocument<>(null, MediaTypes.APPLICATION_JAVA);

    public DefaultDocument(T content) {
        this(content, null);
    }

    public DefaultDocument(T content, MediaType mediaType) {
        this.content = content;
        if (mediaType != null) {
            this.mediaType = mediaType;
        } else {
            this.mediaType = MediaTypes.UNKNOWN;
        }
    }

    @Override
    public Document<T> withMediaType(MediaType mediaType) {
        return new DefaultDocument<>(this.getContent(), mediaType);
    }

    @Override
    public T getContent() {
        return content;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }
}
