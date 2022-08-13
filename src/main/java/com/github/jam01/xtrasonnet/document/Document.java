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

}
