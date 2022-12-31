package io.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import ujson.Value;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class BasePlugin implements DataFormatPlugin {
    public static final String PARAM_FORMAT = "fmt";

    protected final Set<MediaType> supportedTypes = new LinkedHashSet<>();
    protected final Set<String> readerParams = new LinkedHashSet<>();
    protected final Set<String> writerParams = new LinkedHashSet<>();
    protected final Set<Class<?>> readerSupportedClasses = new LinkedHashSet<>();
    protected final Set<Class<?>> writerSupportedClasses = new LinkedHashSet<>();

    @Override
    public Value read(Document<?> doc) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public boolean canRead(Document<?> doc) {
        MediaType requestedType = doc.getMediaType();
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, readerParams) &&
                    // TODO: 8/12/22 add null handling test method to abstract class
                    (doc.getContent() == null || canReadClass(doc.getContent().getClass()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(MediaType requestedType, Class<?> clazz) {
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, writerParams) &&
                    canWriteClass(clazz)) {
                return true;
            }
        }

        return false;
    }

    protected boolean canReadClass(Class<?> cls) {
        for (Class<?> supported : readerSupportedClasses) {
            if (supported.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canWriteClass(Class<?> clazz) {
        for (Class<?> supported : writerSupportedClasses) {
            if (clazz.isAssignableFrom(supported)) {
                return true;
            }
        }
        return false;
    }

    private boolean parametersAreSupported(MediaType requestedType, Set<String> supported) {
        for (String param : requestedType.getParameters().keySet()) {
            if (!(MediaTypes.PARAM_QUALITY_FACTOR.equals(param) || MediaTypes.PARAM_CHARSET.equals(param))) {
                // if it's not known params q or charset, or a general prefix, and it's not supported param, we fail
                boolean matched = false;
                for (String supportedParam : supported) {
                    if (param.matches(supportedParam)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }

        return true;
    }
}
