package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* spring-framework copyright/notice, per Apache-2.0 § 4.c */
/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Changed:
 * - Combined hasText and nullSafeEquals from StringUtils and ObjectUtils
 * - Removed support for arrays in nullSafeEquals
 */

import org.jetbrains.annotations.Nullable;

/**
 * Miscellaneous collection of utility methods. Mainly for internal use within the framework.
 */
public class Utils {
    /*
     * StringUtils: start
     *
     * @author Rod Johnson
     * @author Juergen Hoeller
     * @author Keith Donald
     * @author Rob Harrop
     * @author Rick Evans
     * @author Arjen Poutsma
     * @author Sam Brannen
     * @author Brian Clozel
     */
    /**
     * Check whether the given {@code String} contains actual <em>text</em>.
     * <p>More specifically, this method returns {@code true} if the
     * {@code String} is not {@code null}, its length is greater than 0,
     * and it contains at least one non-whitespace character.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not {@code null}, its
     * length is greater than 0, and it does not contain whitespace only
     * see #hasText(CharSequence)
     * see #hasLength(String)
     * see Character#isWhitespace
     */
    public static boolean hasText(@Nullable String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    /* StringUtils: end */

    /*
     * ObjectUtils: start
     *
     * <p>Thanks to Alex Ruiz for contributing several enhancements to this class!
     *
     * @author Juergen Hoeller
     * @author Keith Donald
     * @author Rod Johnson
     * @author Rob Harrop
     * @author Chris Beams
     * @author Sam Brannen
     */
    /**
     * Determine if the given objects are equal, returning {@code true} if
     * both are {@code null} or {@code false} if only one is {@code null}.
     *
     * @param o1 first Object to compare
     * @param o2 second Object to compare
     * @return whether the given objects are equal
     * @see Object#equals(Object)
     */
    public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }
    /* ObjectUtils: end */
}
