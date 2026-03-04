package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/**
 * Constants used by the xtrasonnet language in Apache Camel.
 * <p>
 * Defines header names for configuring body media type, output media type, and result type.
 * </p>
 */
public final class XtrasonnetConstants {
    /**
     * Header key for specifying the media type of the input body.
     */
    public static final String BODY_MEDIATYPE = "CamelXtrasonnetBodyMediaType";
    /**
     * Header key for specifying the media type of the output.
     */
    public static final String OUTPUT_MEDIATYPE = "CamelXtrasonnetOutputMediaType";
    /**
     * Header key for specifying the result type class.
     */
    public static final String RESULT_TYPE = "CamelXtrasonnetResultType";

    private XtrasonnetConstants() {
    }
}
