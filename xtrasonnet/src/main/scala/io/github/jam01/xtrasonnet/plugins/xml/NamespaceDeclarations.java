package io.github.jam01.xtrasonnet.plugins.xml;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class NamespaceDeclarations {
    private final NamespaceSupport wrapped = new NamespaceSupport();
    private final Map<String, String> requested; // consider a bi-directional Map
    // one map of overrides per active context, scoped the same as wrapped's own declarations;
    // nearest (innermost, i.e. first) context wins on lookup, mirroring wrapped's inheritance
    private final Deque<Map<String, String>> overridden = new ArrayDeque<>();
    private final String[] parts = new String[3];  // keep reusing a single array

    public NamespaceDeclarations(Map<String, String> requested) {
        requested.forEach((uri, pfx) -> wrapped.declarePrefix(pfx, uri));
        this.requested = requested;
        overridden.push(new HashMap<>(4)); // root context, mirrors wrapped's implicit base context
    }

    // convenience of NamespaceSupport.declarePrefix that returns the potentially overridden prefix
    public String prefix(String prefix, String uri) {
        if (prefix.equals("xml") || prefix.equals("xmlns")) { // per NamespaceSupport.declarePrefix
            return prefix;
        }

        var override = prefix;
        if (requested.containsKey(uri)) { // if a prefix is requested for this uri, use that
            override = requested.get(uri);
            overridden.peek().put(prefix, override);
        } else if (requested.containsValue(prefix)
                || overriddenHasValue(prefix) // already used to override another prefix in an active context
                // reserved for the default declaration, which a literal prefix must not collide with
                || DefaultXMLPlugin.DEFAULT_NS_KEY().equals(prefix)) {
            var i = 1;
            do {
                // https://saxonica.plan.io/projects/saxonmirrorhe/repository/he/entry/src/main/java/net/sf/saxon/event/ComplexContentOutputter.java?utf8=%E2%9C%93&rev=he_mirror_saxon_11_4#L588
                // https://gitlab.gnome.org/GNOME/libxslt/-/blob/v1.1.36/libxslt/namespaces.c#L574
                // https://suika.suikawiki.org/www/markup/xml/nsfixup
                override = prefix + '_' + i;
                i++;
            } while (requested.containsValue(override) || wrapped.getURI(override) != null || overriddenHasValue(override));
            // keep trying if candidate is also requested, or already declared, or already used to override another

            overridden.peek().put(prefix, override);
        }

        wrapped.declarePrefix(override, uri);
        return override;
    }

    // convenience of NamespaceSupport.processName that returns the potentially override qname
    public String name(String qName, boolean isAttribute) {
        int index = qName.indexOf(':');
        if (index != -1) {
            var prefix = qName.substring(0, index);
            var override = overriddenValue(prefix);
            if (override != null) qName = override + qName.substring(index);
        }

        return wrapped.processName(qName, parts, isAttribute)[2]; // parts is a multi-return value placeholder
    }

    public void pushContext() {
        wrapped.pushContext();
        overridden.push(new HashMap<>(4));
    }

    public void popContext() {
        wrapped.popContext();
        overridden.pop();
    }

    // nearest-context-first lookup of the override recorded for a literal prefix, or null if none
    private String overriddenValue(String prefix) {
        for (Map<String, String> context : overridden) {
            var override = context.get(prefix);
            if (override != null) return override;
        }
        return null;
    }

    // whether some active context has already used this string as an override target
    private boolean overriddenHasValue(String value) {
        for (Map<String, String> context : overridden) {
            if (context.containsValue(value)) return true;
        }
        return false;
    }
}
