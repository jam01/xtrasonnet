package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.jspecify.annotations.Nullable;

import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.spi.DataFormatPlugin;
import io.github.jam01.xtrasonnet.spi.Library;
import sjsonnet.DefaultParseCache;
import sjsonnet.stdlib.StdLibModule$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    // stays null until something is actually configured, so that an untouched builder leaves the
    // script's header in charge of preserveOrder
    private TransformerSettings.@Nullable Builder settings;

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
        // LinkedHashSet so the generated top level parameters keep the declared order, which makes
        // error messages predictable. Binding is by name, so ordering is not load bearing.
        this.inputNames = new LinkedHashSet<>(Arrays.asList(inputNames));
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

    /**
     * Use these settings, replacing every option set so far.
     * <p>
     * A {@link TransformerSettings} is a complete configuration -- it holds a value for every option,
     * with no record of which were set deliberately and which were left at their default -- so this
     * cannot merge per option. Call it <em>before</em> the convenience methods
     * ({@link #withPreserveOrder}, {@link #withDefaultInput}, {@link #withDefaultOutput}) if you mean
     * to layer them on top; calling it after discards them.
     */
    public TransformerBuilder withSettings(TransformerSettings settings) {
        Objects.requireNonNull(settings);
        this.settings = settings.toBuilder();
        return this;
    }

    /**
     * Keep object fields in the order the script produced them.
     * <p>
     * Leave this alone to let the script's {@code preserveOrder} header directive decide.
     */
    public TransformerBuilder withPreserveOrder(boolean preserveOrder) {
        settings().preserveOrder(preserveOrder);
        return this;
    }

    /** How to read an input whose media type neither the caller nor the header names. */
    public TransformerBuilder withDefaultInput(MediaType mediaType) {
        settings().defaultInput(mediaType);
        return this;
    }

    /** How to write output whose media type neither the caller nor the header names. */
    public TransformerBuilder withDefaultOutput(MediaType mediaType) {
        settings().defaultOutput(mediaType);
        return this;
    }

    // the convenience knobs above and withSettings share one builder, so convenience calls made
    // after a withSettings layer onto it. withSettings itself replaces every option -- see its javadoc
    private TransformerSettings.Builder settings() {
        if (settings == null) {
            settings = TransformerSettings.builder();
        }
        return settings;
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
        return new Transformer(script, inputNames, libs, service,
                ResourcePath.root(), new DefaultParseCache(), ResourcePath.importer(),
                settings == null ? null : settings.build(),
                StdLibModule$.MODULE$.Default().module());
    }
}
