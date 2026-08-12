package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
/**
 * The script parsed, but evaluating it against the given input failed -- for example selecting a
 * field of a null, or a type mismatch that only shows up for certain payloads.
 * <p>
 * Distinct from {@link XtrasonnetParseException}: the script may be fine and the input at fault, so
 * a service handling untrusted payloads can answer differently for the two.
 */
public class XtrasonnetEvaluationException extends XtrasonnetException {
    public XtrasonnetEvaluationException(String message) {
        super(message);
    }

    public XtrasonnetEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
