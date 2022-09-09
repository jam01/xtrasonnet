# Data formats

xtrasonnet mechanism for supporting multiple data formats is influenced by REST and HTTP.

## Media types as identifiers

REST's Uniform Interface constraint allows for the possibility of a resource to be represented in multiple data formats. In HTTP the mechanism for identifying those formats is a media type string. This can be observed in action by requesting different media types for a given resource:

```
GET /my-resource
Accept: application/json
```

or if we prefer an XMl document...

```
GET /my-resource
Accept: application/xml
```

These two requests would return a JSON and XMl document, respectively, if the server could indeed represent `/my-resource` in both formats.


## Writing JSON

xtrasonnet's mechanism is similar to HTTP's. Developers can use media types to signal to the transformer which format to output. Because xtrasonnet is based on jsonnet, the canonical representation of any transformation is JSON; this means transformation results can always be represented as JSON.

**Standalone**
```jsonnet
/**
output application/json
*/
[
  { color: 'blue', type: 'bus' },
  { color: 'yellow', type: 'truck' }
]
```

**Programmatically**
```java
var myJsonnet = """
            [
              { color: 'blue', type: 'bus' },
              { color: 'yellow', type: 'truck' }
            ]""";
var myTransformer = new Transformer(myJsonnet);

var output = myTransfomer.transform(Documents.Null, 
        Collections.emptyMap,
        MediaTypes.APPLICATION_JSON,
        String.class);
```

## Writing other formats

Yet, based on the structure of the resulting JSON, other data formats may be possible. For example, the resulting JSON of the previous transformation

```json
[
  { "color": "blue", "type": "bus" },
  { "color": "yellow", "type": "truck" }
]
```

can be considered to be the equivalent of the following CSV

```csv
color,type
blue,bus
yellow,truck
```

and because xtrasonnet supports CSV out of the box, we can request it

**Standalone**
```jsonnet
/**
output text/csv
*/
[
  { color: 'blue', type: 'bus' },
  { color: 'yellow', type: 'truck' }
]
```

**Programmatically**
```java
var myJsonnet = """
            [
              { color: 'blue', type: 'bus' },
              { color: 'yellow', type: 'truck' }
            ]""";
var myTransformer = new Transformer(myJsonnet);

var output = myTransfomer.transform(Documents.Null, 
        Collections.emptyMap,
        MediaTypes.TEXT_CSV,
        String.class);
```

which would result in the following output

```csv
color,type
blue,bus
yellow,truck
```

## Reading input
Similarly, it's possible for xtrasonnet to read JSON or other formats if they follow the particular structure supported by the available data format plugins. For example, to transform the previous CSV result back to JSON...

**Standalone**
```jsonnet
/**
input text/csv
output application/json
*/
payload
```

**Programmatically**
```java
var myJsonnet = "payload";
var myTransformer = new Transformer(myJsonnet);

var myInput = """
            color,type
            blue,bus
            yellow,truck
            """;

var output = myTransfomer.transform(Document.of(myInput, MediaTypes.TEXT_CSV), 
        Collections.emptyMap,
        MediaTypes.APPLICATION_JSON,
        String.class);
```

which would result in 

```json
[
  { "color": "blue", "type": "bus" },
  { "color": "yellow", "type": "truck" }
]
```

## Parameters as instructions

In order to further control how the data format plugins read and write data, xtrasonnet leverages media type parameters. For example, in order to write the previous CSV example without a header line the following media type can be used

**Standalone**
```jsonnet
/**
output text/csv; header=absent
*/
[
  { color: 'blue', type: 'bus' },
  { color: 'yellow', type: 'truck' }
]
```

**Programmatically**
```java
var myJsonnet = """
            [
              { color: 'blue', type: 'bus' },
              { color: 'yellow', type: 'truck' }
            ]""";
var myTransformer = new Transformer(myJsonnet);

var output = myTransfomer.transform(Documents.Null, 
        Collections.emptyMap,
        MediaTypes.TEXT_CSV.withParameter("header", "absent"),
        String.class);
```

which would result in the following output

```csv
blue,bus
yellow,truck
```

!!! Warning
    The CSV example above uses a parameter that is part of the `text/csv` IANA media type registration. Not all parameters used by the xtrasonnet data format plugins are registered.
