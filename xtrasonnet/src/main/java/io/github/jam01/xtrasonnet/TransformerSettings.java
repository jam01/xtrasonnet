package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.jspecify.annotations.Nullable;
import sjsonnet.Settings;

import java.util.Objects;

/**
 * Settings for a {@link Transformer}, built by name.
 * <p>
 * This was a Scala class with default arguments, which Java callers cannot see, so configuring a
 * transformer meant handing over six unnamed positional values:
 * <pre>{@code new TransformerSettings(new Settings(true, false, false, false, 1000, false), ...)}</pre>
 * where only the first differs from the default. Nobody can read that -- the repository's own
 * CLAUDE.md described the test harness as "order not preserved" while that first {@code true} says
 * the opposite. Say it by name instead:
 * <pre>{@code TransformerSettings.builder().preserveOrder(true).build()}</pre>
 *
 * @see TransformerBuilder#withPreserveOrder(boolean)
 */
public final class TransformerSettings {
    /**
     * Everything left to its default, including {@code preserveOrder}, which then comes from the
     * script's header.
     */
    public static final TransformerSettings DEFAULT = builder().build();

    // null means "not set, take it from the script's header". Precedence is explicit setting, then
    // header declaration, then default -- so configuring an unrelated knob such as the default
    // output type does not disturb field ordering.
    private final @Nullable Boolean preserveOrder;
    private final boolean strict;
    private final boolean throwErrorForInvalidSets;
    private final boolean useNewEvaluator;
    private final int maxParserRecursionDepth;
    private final boolean brokenAssertionLogic;
    private final MediaType defInputMediaType;
    private final MediaType defOutputMediaType;

    private TransformerSettings(Builder b) {
        this.preserveOrder = b.preserveOrder;
        this.strict = b.strict;
        this.throwErrorForInvalidSets = b.throwErrorForInvalidSets;
        this.useNewEvaluator = b.useNewEvaluator;
        this.maxParserRecursionDepth = b.maxParserRecursionDepth;
        this.brokenAssertionLogic = b.brokenAssertionLogic;
        this.defInputMediaType = b.defInputMediaType;
        this.defOutputMediaType = b.defOutputMediaType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder holding these settings, for deriving a variant. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.preserveOrder = preserveOrder;
        b.strict = strict;
        b.throwErrorForInvalidSets = throwErrorForInvalidSets;
        b.useNewEvaluator = useNewEvaluator;
        b.maxParserRecursionDepth = maxParserRecursionDepth;
        b.brokenAssertionLogic = brokenAssertionLogic;
        b.defInputMediaType = defInputMediaType;
        b.defOutputMediaType = defOutputMediaType;
        return b;
    }

    /**
     * The underlying engine settings, resolving {@code preserveOrder} against the script's header.
     *
     * @param headerPreserveOrder what the script's header declared, used only if no explicit value
     *                            was set here
     */
    public Settings sjsSettings(boolean headerPreserveOrder) {
        return new Settings(preserveOrder != null ? preserveOrder : headerPreserveOrder,
                strict, throwErrorForInvalidSets, useNewEvaluator, maxParserRecursionDepth,
                brokenAssertionLogic);
    }

    /** Whether an explicit value was set, overriding whatever the script's header declares. */
    public @Nullable Boolean preserveOrder() {
        return preserveOrder;
    }

    /** The media type to read an input as when neither the caller nor the header names one. */
    public MediaType defInputMediaType() {
        return defInputMediaType;
    }

    /** The media type to write as when neither the caller nor the header names one. */
    public MediaType defOutputMediaType() {
        return defOutputMediaType;
    }

    public static final class Builder {
        private @Nullable Boolean preserveOrder = null;
        private boolean strict = false;
        private boolean throwErrorForInvalidSets = false;
        private boolean useNewEvaluator = false;
        private int maxParserRecursionDepth = 1000;
        private boolean brokenAssertionLogic = false;
        private MediaType defInputMediaType = MediaTypes.APPLICATION_JSON;
        private MediaType defOutputMediaType = MediaTypes.APPLICATION_JSON;

        private Builder() {
        }

        /**
         * Keep object fields in the order the script produced them. Leave this unset to let the
         * script's {@code preserveOrder} header directive decide, which is the default.
         */
        public Builder preserveOrder(boolean preserveOrder) {
            this.preserveOrder = preserveOrder;
            return this;
        }

        /** Reject constructs jsonnet's strict mode disallows. */
        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /** Fail rather than warn when a set operation is given input that is not a valid set. */
        public Builder throwErrorForInvalidSets(boolean throwErrorForInvalidSets) {
            this.throwErrorForInvalidSets = throwErrorForInvalidSets;
            return this;
        }

        /** How deeply the parser may recurse before giving up; guards against pathological scripts. */
        public Builder maxParserRecursionDepth(int maxParserRecursionDepth) {
            if (maxParserRecursionDepth < 1) {
                throw new IllegalArgumentException(
                        "maxParserRecursionDepth must be at least 1, got " + maxParserRecursionDepth);
            }

            this.maxParserRecursionDepth = maxParserRecursionDepth;
            return this;
        }

        /** How to read an input whose media type neither the caller nor the header names. */
        public Builder defaultInput(MediaType defInputMediaType) {
            this.defInputMediaType = Objects.requireNonNull(defInputMediaType);
            return this;
        }

        /** How to write output whose media type neither the caller nor the header names. */
        public Builder defaultOutput(MediaType defOutputMediaType) {
            this.defOutputMediaType = Objects.requireNonNull(defOutputMediaType);
            return this;
        }

        /**
         * Escape hatch for engine settings not surfaced above. Every value is taken from the given
         * instance, {@code preserveOrder} included -- handing over a complete {@link Settings} is
         * read as meaning all of it, so that one overrides the script's header.
         */
        public Builder sjsonnetSettings(Settings settings) {
            Objects.requireNonNull(settings);

            this.preserveOrder = settings.preserveOrder();
            this.strict = settings.strict();
            this.throwErrorForInvalidSets = settings.throwErrorForInvalidSets();
            this.useNewEvaluator = settings.useNewEvaluator();
            this.maxParserRecursionDepth = settings.maxParserRecursionDepth();
            this.brokenAssertionLogic = settings.brokenAssertionLogic();
            return this;
        }

        public TransformerSettings build() {
            return new TransformerSettings(this);
        }
    }
}
