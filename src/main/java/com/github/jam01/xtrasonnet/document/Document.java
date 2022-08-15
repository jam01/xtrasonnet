package com.github.jam01.xtrasonnet.document;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/***
 * The document interface represents the document used by the mapper.
 *
 * @param <T> the content
 */
public interface Document<T> {
    T getContent();

    MediaType getMediaType();

    Document<T> withMediaType(MediaType mediaType);

    static <T> Document<T> of(T value) {
        return new BasicDocument<>(value);
    }

    static <T> Document<T> of(T value, MediaType mediaType) {
        return new BasicDocument<>(value, mediaType);
    }

    /***
     * The DefaultDocument class
     *
     * @param <T> the content
     */
    class BasicDocument<T> implements Document<T> {
        private final T content;
        private final MediaType mediaType;

        public final static Document<Object> NULL_INSTANCE = new BasicDocument<>(null, MediaTypes.APPLICATION_JAVA);

        public BasicDocument(T content) {
            this(content, null);
        }

        public BasicDocument(T content, MediaType mediaType) {
            this.content = content;
            if (mediaType != null) {
                this.mediaType = mediaType;
            } else {
                this.mediaType = MediaTypes.UNKNOWN;
            }
        }

        @Override
        public Document<T> withMediaType(MediaType mediaType) {
            return new BasicDocument<>(this.getContent(), mediaType);
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
}
