package com.github.jam01.xtrasonnet.spi;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.document.Document;
import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import ujson.Value;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractDataFormatPlugin implements DataFormatPlugin {
    public static final String DS_PARAM_INDENT = "indent";

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

    protected boolean paramPresent(MediaType type, String name) {
        return type.getParameters().containsKey(name);
    }

    protected boolean paramAbsent(MediaType type, String name) {
        return !type.getParameters().containsKey(name);
    }
    protected boolean paramEq(MediaType type, String name, String expected) {
        if (!type.getParameters().containsKey(name)) return false;
        return expected.equals(type.getParameters().get(name));
    }

    protected char paramAsChar(MediaType type, String name, char defaault) {
        if (!type.getParameters().containsKey(name)) return defaault;
        return type.getParameters().get(name).charAt(0);
    }

    protected List<String> paramAsList(MediaType type, String name, List<String> defaault) {
        if (!type.getParameters().containsKey(name)) return defaault;
        return Arrays.asList(type.getParameters().get(name).split(","));
    }
}
