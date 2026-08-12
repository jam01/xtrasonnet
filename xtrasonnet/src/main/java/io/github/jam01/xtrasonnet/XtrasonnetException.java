package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
/**
 * Base type for failures raised by a {@link Transformer}.
 * <p>
 * {@link #getMessage()} carries the underlying failure and its position in the script, not just a
 * fixed prefix, so that anything logging the message alone -- which is most frameworks and error
 * handlers -- shows the user something actionable. The subtypes let callers tell a malformed script
 * apart from a transformation that failed on its input.
 *
 * @see XtrasonnetParseException
 * @see XtrasonnetEvaluationException
 */
public class XtrasonnetException extends RuntimeException {
    public XtrasonnetException(String message) {
        super(message);
    }

    public XtrasonnetException(String message, Throwable cause) {
        super(message, cause);
    }
}
