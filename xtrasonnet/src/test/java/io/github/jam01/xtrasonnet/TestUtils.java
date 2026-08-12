package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.MediaTypes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {
    public static String transform(String script) {
        return transform(script, null);
    }

    public static String transform(String script, String payload) {
        return Transformer.builder(script)
                .withPreserveOrder(true)
                .withDefaultInput(MediaTypes.APPLICATION_JSON)
                .withDefaultOutput(MediaTypes.APPLICATION_JSON)
                .build().transform(payload);
    }

    public static String stacktraceFrom(Exception ex) {
        try (StringWriter out = new StringWriter(); PrintWriter errWriter = new PrintWriter(out)) {
            ex.printStackTrace(errWriter);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resourceAsString(String filePath) {
        try {
            Path path = Paths.get(TestUtils.class.getClassLoader().getResource(filePath).toURI());
            // UTF-8, not the platform default: fixtures are UTF-8 files, and reading them with the
            // host's encoding made non-ASCII assertions pass or fail depending on the machine
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File resourceAsFile(String filePath) {
        String file = TestUtils.class.getClassLoader().getResource(filePath).getFile();
        return new File(file);
    }
}
