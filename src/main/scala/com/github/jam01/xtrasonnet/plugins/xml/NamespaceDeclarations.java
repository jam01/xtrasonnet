package com.github.jam01.xtrasonnet.plugins.xml;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.xml.sax.helpers.NamespaceSupport;

import java.util.HashMap;
import java.util.Map;

public class NamespaceDeclarations {
    private final NamespaceSupport wrapped = new NamespaceSupport();
    private final Map<String, String> requested; // consider a bi-directional Map
    private final Map<String, String> overridden = new HashMap<>(8); // consider a bi-directional Map
    private final String[] parts = new String[3];  // keep reusing a single array

    public NamespaceDeclarations(Map<String, String> requested) {
        requested.forEach((uri, pfx) -> wrapped.declarePrefix(pfx, uri));
        this.requested = requested;
    }

    // convenience of NamespaceSupport.declarePrefix that returns the potentially overridden prefix
    public String prefix(String prefix, String uri) {
        if (prefix.equals("xml") || prefix.equals("xmlns")) { // per NamespaceSupport.declarePrefix
            return prefix;
        }

        var override = prefix;
        if (requested.containsKey(uri)) { // if a prefix is requested for this uri, use that
            override = requested.get(uri);
            overridden.put(prefix, override);
        } else if (requested.containsValue(prefix)) { // if prefix is requested for another uri, find an override
            var i = 1;
            do {
                // https://saxonica.plan.io/projects/saxonmirrorhe/repository/he/entry/src/main/java/net/sf/saxon/event/ComplexContentOutputter.java?utf8=%E2%9C%93&rev=he_mirror_saxon_11_4#L588
                // https://gitlab.gnome.org/GNOME/libxslt/-/blob/v1.1.36/libxslt/namespaces.c#L574
                // https://suika.suikawiki.org/www/markup/xml/nsfixup
                override = prefix + '_' + i;
                i++;
            } while (requested.containsValue(override) || wrapped.getURI(override) != null || overridden.containsValue(override));
            // keep trying if candidate is also requested, or already declared, or already used to override another

            overridden.put(prefix, override);
        }

        wrapped.declarePrefix(override, uri);
        return override;
    }

    // convenience of NamespaceSupport.processName that returns the potentially override qname
    public String name(String qName, boolean isAttribute) {
        int index = qName.indexOf(':');
        if (index != -1) {
            var prefix = qName.substring(0, index);
            if (overridden.containsKey(prefix)) qName = overridden.get(prefix) + qName.substring(index);
        }

        return wrapped.processName(qName, parts, isAttribute)[2]; // parts is a multi-return value placeholder
    }

    public void pushContext() {
        wrapped.pushContext();
    }

    public void popContext() {
        wrapped.popContext();
    }
}
