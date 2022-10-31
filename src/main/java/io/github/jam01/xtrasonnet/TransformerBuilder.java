package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.spi.DataFormatPlugin;
import io.github.jam01.xtrasonnet.spi.Library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class TransformerBuilder {
    private final String script;
    private Set<String> inputNames = Collections.emptySet();
    private Set<Library> libs = Collections.emptySet();
    private DataFormatService service = DataFormatService.DEFAULT;

    public TransformerBuilder(String script) {
        this.script = script;
    }

    // TODO: 8/11/20 defensively copy all collections and check for nulls?
    public TransformerBuilder withInputNames(Set<String> inputNames) {
        Objects.requireNonNull(inputNames);

        this.inputNames = inputNames;
        return this;
    }

    public TransformerBuilder withInputNames(String... inputNames) {
        this.inputNames = new HashSet<>(Arrays.asList(inputNames));
        return this;
    }

    public TransformerBuilder withInputNamesOf(Map<String, Object> inputs) {
        this.inputNames = inputs.keySet();
        return this;
    }

    public TransformerBuilder withLibrary(Library lib) {
        Objects.requireNonNull(lib);
        if (libs.isEmpty()) {
            libs = new HashSet<>(2);
        }
        libs.add(lib);
        return this;
    }

    public TransformerBuilder configurePlugins(Consumer<List<DataFormatPlugin>> configurer) {
        List<DataFormatPlugin> plugins = new ArrayList<>(4);
        configurer.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public TransformerBuilder extendPlugins(Consumer<List<DataFormatPlugin>> extender) {
        List<DataFormatPlugin> plugins = new ArrayList<>(this.service.getPlugins());
        extender.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public Transformer build() {
        return new Transformer(script, inputNames, libs, service);
    }
}
