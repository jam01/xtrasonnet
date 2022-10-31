package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.jetbrains.annotations.Nullable;
import scala.io.Codec;
import scala.io.Source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public final class ResourceResolver {
    public static String asString(String resource, @Nullable Charset charset) throws IOException {
        charset = charset == null ? Charset.defaultCharset() : charset;
        try (InputStream is = asStream(resource)) {
            return Source.fromInputStream(is, new Codec(charset)).mkString();
        }
    }

    public static InputStream asStream(String resource) throws IOException {
        InputStream is = null;
        int idx = resource.indexOf(':');
        if (idx == -1 || resource.startsWith("classpath:")) {
            resource = resource.substring(idx + 1);
            URL url = findClasspathResource(resource);

            if (url != null) {
                is = url.openStream();
            }
        } else if (resource.startsWith("http:") || resource.startsWith("https:")) {
            URLConnection con = new URL(resource).openConnection();
            con.setUseCaches(false);

            try {
                is = con.getInputStream();
            } catch (IOException ex) {
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection) con).disconnect();
                }
                throw ex;
            }
        } else if (resource.startsWith("file")) {
            resource = resource.substring(idx + 1);
            var file = new File(resource);
            if (file.exists() && file.canRead()) {
                if (file.isDirectory()) throw new IllegalArgumentException(resource + "cannot be read because it is a directory");
                is = new FileInputStream(file);
            }
        } else {
            throw new IllegalArgumentException("Unsupported scheme: " + resource.substring(0, idx));
        }

        if (is == null) {
            throw new FileNotFoundException(resource + " cannot be read because it does not exist");
        }

        return is;
    }

    private static URL findClasspathResource(String resource) {
        URL url = null;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            url = tccl.getResource(resource);
        }

        if (url == null) {
            ResourceResolver.class.getClassLoader().getResource(resource);
        }

        if (url == null) {
            url = ResourceResolver.class.getResource(resource);
        }

        if (url == null) {
            url = ClassLoader.getSystemResource(resource);
        }
        return url;
    }
}
