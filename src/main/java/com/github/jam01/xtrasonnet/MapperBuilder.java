package com.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.document.MediaType;
import com.github.jam01.xtrasonnet.document.MediaTypes;
import com.github.jam01.xtrasonnet.spi.DataFormatPlugin;
import com.github.jam01.xtrasonnet.spi.DataFormatService;
import com.github.jam01.xtrasonnet.spi.Library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MapperBuilder {
    private final String script;
    private Iterable<String> inputNames = Collections.emptySet();
    private Map<String, String> imports = Collections.emptyMap();
    private List<Library> libs = Collections.emptyList();
    private DataFormatService service = DataFormatService.DEFAULT;
    private boolean asFunction = true;
    private MediaType defaultOutput = MediaTypes.APPLICATION_JSON;

    public MapperBuilder(String script) {
        this.script = script;
    }

    // TODO: 8/11/20 defensively copy all collections and check for nulls?
    public MapperBuilder withInputNames(Iterable<String> inputNames) {
        Objects.requireNonNull(inputNames);

        this.inputNames = inputNames;
        return this;
    }

    public MapperBuilder withInputNames(String... inputNames) {
        this.inputNames = Arrays.asList(inputNames);
        return this;
    }

    public MapperBuilder withInputNamesFrom(Map<String, String> imports) {
        this.inputNames = imports.keySet();
        return this;
    }

    public MapperBuilder withImports(Map<String, String> imports) {
        Objects.requireNonNull(imports);

        this.imports = imports;
        return this;
    }

    public MapperBuilder withLibrary(Library lib) {
        Objects.requireNonNull(lib);
        if (libs.isEmpty()) {
            libs = new ArrayList<>(2);
        }
        libs.add(lib);

        return this;
    }

    public MapperBuilder wrapAsFunction(boolean asFunction) {
        this.asFunction = asFunction;
        return this;
    }

    public MapperBuilder configurePlugins(Consumer<List<DataFormatPlugin>> configurer) {
        List<DataFormatPlugin> plugins = new ArrayList<>(4);
        configurer.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public MapperBuilder extendPlugins(Consumer<List<DataFormatPlugin>> extender) {
        List<DataFormatPlugin> plugins = new ArrayList<>(this.service.getPlugins());
        extender.accept(plugins);
        this.service = new DataFormatService(plugins);
        return this;
    }

    public MapperBuilder withDefaultOutput(MediaType output) {
        this.defaultOutput = output;
        return this;
    }

    public Mapper build() {
        return new Mapper(script, inputNames, imports, asFunction, libs, service, defaultOutput);
    }
}
