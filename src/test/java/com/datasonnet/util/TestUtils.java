package com.datasonnet.util;

/*-
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.Mapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtils {
    public static String transform(String script) {
        return transform(script, null);
    }

    public static String transform(String script, String payload) {
        return new Mapper(script).transform(payload != null ? payload : "{}").replaceAll("\"", "");
    }

    public static String stacktraceFrom(Exception ex) {
        try (StringWriter out = new StringWriter(); PrintWriter errWriter = new PrintWriter(out)) {
            ex.printStackTrace(errWriter);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resourceAsString(String filePath) throws URISyntaxException, IOException {
        Path path = Paths.get(TestUtils.class.getClassLoader().getResource(filePath).toURI());
        return new String(Files.readAllBytes(path));
    }
}
