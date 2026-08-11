# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

xtrasonnet is an extensible, jsonnet-based data transformation engine for the JVM. It extends databricks'
`sjsonnet` (a Scala implementation of Google's jsonnet) with: pluggable data formats (JSON, XML, CSV, Java
objects, Excel, plain text), a native function library (`xtr`), and language additions (fluent/infix call
syntax, `?.` null-safe select, `??` null coalescing, and precision-safe numeric semantics with
Int64/Float64/Dec128).

Maven multi-module build, two published modules:
- `xtrasonnet` — the core engine (Scala + Java, mixed source)
- `camel-xtrasonnet` — Apache Camel component/language binding wrapping the core engine

`benchmark/` and `playground/` exist at the repo root but are **not** Maven modules (not listed in the root
`pom.xml` `<modules>`). `playground/` is a standalone JS/rollup app (a browser-based editor); `benchmark/` is
an IntelliJ-only module. Neither is built by `mvn`.

## Build & test

```bash
mvn clean verify                 # full build: compiles Scala+Java, runs tests, checks license headers
mvn -pl xtrasonnet test          # test only the core engine module
mvn -pl camel-xtrasonnet test    # test only the camel module
```

Run a single test class or method (surefire, works with JUnit 5):
```bash
mvn -pl xtrasonnet test -Dtest=DatetimeTest
mvn -pl xtrasonnet test -Dtest=DatetimeTest#someMethodName
```

Test sources live under `xtrasonnet/src/test/java/...` (JUnit 5, one class roughly per `xtr` module or
plugin, e.g. `ArraysTest`, `DatetimeTest`, `XMLPluginTest`, `HeaderTest`) and `xtrasonnet/src/test/scala/...`.
`TestUtils.transform(script, payload)` is the common entry point used across tests — it builds a
`Transformer` with fixed settings (JSON in/out, order not preserved) and returns the transformed string.

CI (`.github/workflows/ci.yaml`) runs `mvn clean verify` on JDK 25 for every push; it also enforces license
headers via the `license-maven-plugin` (`mvn com.mycila:license-maven-plugin:check` locally). New source
files need the standard header (see `src/build/license-header.txt`) or must be added to that plugin's
`<excludes>` if intentionally exempt (e.g. files carried over unchanged from `datasonnet-mapper` or
`spring-framework`, per Apache-2.0 §4(c) attribution — see comments at the top of `Header.java`/`Xtr.scala`
for examples of how that attribution is recorded).

Project targets Java 21 (`maven.compiler.release`); CI itself builds with JDK 25.

## Architecture

### Mixed Scala/Java source in one module

`xtrasonnet/src/main/{scala,java}` compile together via `scala-maven-plugin` (Java compilation is delegated
to scalac — see `<maven-compiler-plugin><skip>true</skip>` in `xtrasonnet/pom.xml`). Roughly: the evaluation
engine, language extensions, and native function modules are Scala (they interface tightly with `sjsonnet`);
public-facing document/media-type/header types and the builder API are Java (they're the primary API surface
consumed by JVM callers). Don't assume a Java-only or Scala-only mental model when tracing a call — expect to
cross the boundary.

### Transformation pipeline

1. **`TransformerBuilder`** (Java) — fluent builder for input names, `Library` extensions, `TransformerSettings`,
   and `DataFormatService` (via `configurePlugins`/`extendPlugins`). Produces a `Transformer`.
2. **`Transformer`** (Scala, `Transformer.scala`) — the core engine, heavily based on `sjsonnet.Interpreter`:
   - Parses the `/** xtrasonnet ... */` header comment (`Header.parseHeader`) to learn declared input/output
     media types and `preserveOrder`.
   - Wraps the script body as a top-level jsonnet function taking `payload` plus any named inputs
     (`Transformer.asFunction`), following jsonnet's "parameterize entire config" pattern.
   - Builds a `FluentInterpreter` (extends `sjsonnet.Interpreter`) whose resolver plugs in a custom
     `FluentParser` (adds the infix/fluent call syntax) and wires library modules as resolvable external
     variables (`variableResolver`).
   - `transform(...)` reads the payload/inputs through `DataFormatService` into jsonnet `Val`s, evaluates the
     wrapped function, and writes the result back out through `DataFormatService` to the target media type.
   - `processError` rewrites line numbers in sjsonnet errors to account for the synthetic function wrapper
     line, so error messages point at the user's original script.
3. **`DataFormatService`** (Java, `DataFormatService.java`) — an ordered list of `DataFormatPlugin`s; first
   plugin whose `canRead`/`canWrite` matches wins (`thatCanRead`/`thatCanWrite`). `DEFAULT` registers JSON,
   Java, XML, CSV, plain text, and Excel plugins in that order. Custom plugins are added via
   `TransformerBuilder.configurePlugins`/`extendPlugins`.
4. **`DataFormatPlugin`** (Java interface, `spi/DataFormatPlugin.java`) — the format extension point: `read`
   converts a `Document` to a Jackson `JsonNode` (default `read(doc, pos)` then converts that to a jsonnet
   `Val.Literal` via `JsonNodeVisitor`/`LiteralVisitor`); `write` goes the other way via `Materializer`. Default
   plugins live in `xtrasonnet/src/main/java/.../plugins/` (Java) with some Scala counterparts in
   `.../plugins/` and `.../plugins/xml/` (e.g. `DefaultXMLPlugin`, `BadgerFishVisitor`/`BadgerFishHandler` for
   the XML↔JSON BadgerFish convention).
5. **`Library`** (Scala, `spi/Library.scala`) — the function-extension point (`FunctionModule` from sjsonnet).
   `Xtr` (`Xtr.scala`) is the built-in library assembling all `xtr.*` modules (`modules/Arrays.scala`,
   `Datetime.scala`, `Strings.scala`, `Objects.scala`, `Crypto.scala`, `Base64.scala`, `URL.scala`, `Math.scala`,
   `Numbers.scala`, `Duration.scala`). Each module's public functions become `xtr.<module>.<function>` in
   scripts. Custom libraries implement `Library` (Scala) or `JLibrary` (Java-friendlier abstract base) and are
   registered via `TransformerBuilder.withLibrary`.
6. **Numeric semantics** — Int64/Float64/Dec128 handling and rendering lives mainly in
   `modules/Numbers.scala` and `render/DecimalRenderer.scala`; this is a deliberate deviation from stock
   jsonnet/JSON double semantics (see README "Improved Numeric Semantics") and matters when touching anything
   number-parsing or number-rendering related.

### Header comments

`/** xtrasonnet ... */` header syntax (declared input/output media types, `preserveOrder`, `dataformat`
lines) is parsed by `header/Header.java` with its own line-oriented grammar (`INPUT_LINE`/`OUTPUT_LINE`
regexes). This is largely carried over from the original `datasonnet-mapper` project — see the attribution
comment block at the top of the file before changing parsing behavior, and keep the Apache-2.0 §4(c)
"Changes made" list updated if you modify inherited logic.

### camel-xtrasonnet

Apache Camel language/component binding (`camel-xtrasonnet/src/main/java/io/github/jam01/camel/...`) exposing
xtrasonnet as a Camel `Language`/`Expression` (`XtrasonnetLanguage`, `XtrasonnetExpression`,
`XtrasonnetBuilder`) plus a `DocumentConverter` for Camel's type conversion system, and a
`org.apache.camel.reifier.language.XtrasonnetExpressionReifier` hooking into Camel's model reification.
Depends on `xtrasonnet` as a regular Maven dependency (not a source dependency) — build the core module first
if working across both.

### Licensing note

Code is licensed under Elastic License 2.0 (not OSS-approved), except files/blocks explicitly carried over
from the Apache-2.0-licensed `datasonnet-mapper` project, which retain dual attribution comments per
Apache-2.0 §4(c) (see `Xtr.scala`, `Header.java`, `PluginException.java` for the pattern). Preserve these
attribution headers; don't strip them when refactoring.

## Docs site

`docs/` + `mkdocs.yml` build the project documentation site (published via `.github/workflows/docs.yaml`).
This is separate from Scaladoc/Javadoc generated at release time (see the `release` Maven profile in the root
and `xtrasonnet` `pom.xml`, which attaches sources + scaladoc jars).
