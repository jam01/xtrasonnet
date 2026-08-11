# Using xtrasonnet programmatically

Once you've included the xtrasonnet dependency in your classpath, you can create a `Transformer` object to evaluate a jsonnet transformation. Here's a simple JSON-to-JSON example:

```java
var myJsonnet = """
            {
                firstKey: payload.key1,
                secondKey: payload.key2
            }""";
var myTransformer = new Transformer(myJsonnet);


// transform our input when it's available
var myPayload = """
        { 
            "key1": "value1",
            "key2": "value2"
        }""";
var output = myTransformer.transform(myPayload);


// the expected results
assert output.equals("""
            {"firstKey":"value1","secondKey":"value2"}""");
```

## Fine tuning the transformer

The `Transformer` class requires at least the jsonnet transformation template you wish the evaluate, but developers can further control the transformation behavior by passing more arguments, here's an extended example using a `TransformerBuilder`:

```java
var myTransformer = Transformer.builder(myJsonnet)
        .withInputNames("second", "third") // (1)
        .withLibrary(myCustomLib) // (2)
        .extendPlugins((plugins) -> plugins.add(myCustomPlugin)) // (3)
        .build();
```

1. Signal to the transformer to expect inputs other than the `payload` input
2. Extend the available functions with a custom `Library`
3. Extend the supported data formats with a custom `DataFormatPlugin`

## Fine tuning the transformation

Developers can also exert more control on the behavior of the transformation at the point they're ready to evaluate it, by passing more arguments to the `transform` method. To do so we leverage `Document` and `MediaType` objects:

```java
Document<OutputStream> output = myTransformer.transform(
        Document.of(myInput, MediaTypes.APPLICATION_JSON), // (1)
        Map.of("second", mySecInput, "third", myThirdInput), // (2)
        MediaTypes.APPLICATION_XML, // (3)
        OutputStream.class); // (4)
```

1. A `Document` object with the input content and the media type that describes its format.
2. A `java.util.Map` containing the inputs, other than the `payload` input, that the transformation expects.
3. The `MediaType` object representing the output format to be returned, if supported.
4. The type of the object to be returned, if supported. `transform` returns a `Document` wrapping it.

Inputs are bound by name, so the iteration order of the map you pass does not matter. Passing a name
the transformation did not declare with `withInputNames` is an error rather than being ignored.

## Reusing a transformer across threads

Building a `Transformer` compiles the transformation, which is the expensive part, so you will want to
reuse one. **A `Transformer` is not safe to share between threads.** Evaluation mutates caches that
belong to the underlying jsonnet engine — object fields are memoised into maps that are not
synchronised, and the objects holding them are shared by every call on that transformer.

Use one transformer per thread, or pool them:

```java
var pool = new ConcurrentLinkedQueue<Transformer>();

Transformer transformer = pool.poll();
if (transformer == null) transformer = Transformer.builder(myJsonnet).build();
try {
    return transformer.transform(myPayload);
} finally {
    pool.add(transformer);
}
```

Overlapping calls on one transformer are rejected with an exception naming the thread that holds it,
rather than being allowed to corrupt those caches silently. `camel-xtrasonnet` pools transformers this
way, so Camel routes are safe without any work on your part.

## Header present

If the transformation jsonnet includes an [xtrasonnet header](../header) the behavior to be expected is as follows:

* If the `.transform(String)` method is used, a payload document is created internally with media type `MediaTypes.UNKNOWN` (equivalent to `unknown/unknown`), and the requested output set to `MediaTypes.ANY` (equivalent to `*/*`). Then the following rules apply. 
* If the `MediaType` of any given input is `MediaTypes.UNKNOWN` (equivalent to `unknown/unknown`) then the header will be queried for a matching input directive. If the header does not specify, then the transformer will attempt to read it as JSON.
* If the requested output `MediaType` is `MediaTypes.ANY` (equivalent to `*/*`) then the header will be queried for an output directive. If the header does not specify one, then the transformer will attempt to write the output as JSON.
