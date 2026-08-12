@NullMarked
package io.github.jam01.xtrasonnet.header;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

// Every type in this package is null-marked: an unannotated reference type is a commitment that it is
// never null, and anything that may be null carries @Nullable.
//
// Nothing enforces this at build time. The Java sources here are compiled by scalac (see the <skip> on
// maven-compiler-plugin in the pom), so an Error Prone based checker such as NullAway cannot be attached;
// the annotations are for consumers' IDEs and analyzers, and are maintained by hand.
import org.jspecify.annotations.NullMarked;
