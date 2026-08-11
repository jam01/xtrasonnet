package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
/**
 * The transformation script itself is malformed: it could not be parsed or compiled.
 * <p>
 * This is a defect in the script, independent of any input, so it is worth distinguishing from
 * {@link XtrasonnetEvaluationException} -- a service can treat this as a misconfiguration rather
 * than as a bad request.
 */
public class XtrasonnetParseException extends XtrasonnetException {
    public XtrasonnetParseException(String message) {
        super(message);
    }

    public XtrasonnetParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
