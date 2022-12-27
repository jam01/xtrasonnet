# xtrasonnet

## _extensible jsonnet transformations_ 
For detailed information see the [xtrasonnet docs](https://jam01.github.io/xtrasonnet/).

### xtrasonnet is an extensible, jsonnet-based, data transformation engine for Java or any JVM-based language.

![xtrasonnet](docs/assets/images/xtrasonnet.drawio.png)

#### xtrasonnet is an extension of databricks' [sjsonnet](https://github.com/databricks/sjsonnet), a Scala implementation of Google's [jsonnet](https://github.com/google/jsonnet). xtrasonnet enables extensibility, adds support for data formats other than JSON, and adds data transformation facilities through the `xtr` library and some additions to the jsonnet language itself.

```java
String output = new Transformer(xtrasonnet).transform(input);
```

<table>
<thead><tr><td>input</td><td>xtrasonnet</td><td>output</td></tr></thead>
<tbody>
<tr><td>

```
{ 
    "message": "Hello World" 
}
```

</td>
<td>

```
/** xtrasonnet
input payload application/json
output application/xml
*/
{
    root: {
        msg: payload.message,
        at: xtr.datetime.now()
    }
}
```

</td>
<td>

```
<?xml version='1.0' encoding='UTF-8'?>
<root>
	<msg>Hello World</msg>
	<at>2022-08-14T00:19:35.731362Z</at>
</root>
```

</td></tr>
</tbody>
</table>

## How extensible?
xtrasonnet has two points of extensibility:
* _Custom functions_: users can write native (e.g.: Java or Scala) functions as a `Library` and utilize them from their transformation code. 
* _Any* data format_: users can write a custom `DataFormatPlugin` and transform from/to a given data format. 

\* Any format that can be expressed as jsonnet elements.

## What formats are supported?
xtrasonnet includes a `DataFormatPlugin` for each of the following: 
* JSON (application/json)
* XML (application/xml)
* CSV (application/csv)
* Java (application/x-java-object)
* text/plain

## What kind of additions to the jsonnet language?
There are two main additions motivated to facilitate data transformation applications:

### Null-safe select `?.`
This allows developers to select, and chain, properties arbitrarily without testing existence.

<table>
<thead><tr><td>xtrasonnet</td><td>Output</td></tr></thead>
<tbody>
<tr><td>

```jsonnet
local myObj = {
    keyA: { first: { second: 'value' } },
    keyB: { first: { } }
};

{
    a: myObj?.keyA?.first?.second,
    b: myObj?.keyB?.first?.second,
    c: myObj?.keyC?.first?.second
}
```

</td>
<td>

```jsonnet
{
    a: 'value',
    b: null,
    c: null
}
```

</td></tr>
</tbody>
</table>


### Null coalescing operator `??`
This allows developers to tersely test for `null` and provide a default value. For example

<table>
<thead><tr><td>xtrasonnet</td><td>Output</td></tr></thead>
<tbody>
<tr><td>

```jsonnet
local myObj = {
    keyA: { first: { second: 'value' } },
    keyB: { first: { } }
};

{
    a: myObj?.keyA?.first?.second,
    b: myObj?.keyB?.first?.second ?? 'defaultB',
    c: myObj?.keyC?.first?.second ?? 'defaultC'
}
```

</td>
<td>

```jsonnet
{
    a: 'value',
    b: 'defaultB',
    c: 'defaultC'
}
```

</td></tr>
</tbody>
</table>

### What kind of functions are available?

For a full reference see the [`xtr` docs](https://josemontoya.io/xtrasonnet/datasonnet/latest/index.html).

The `xtr` library is written natively (vs written as jsonnet code) and provides an extensive set of functions.

Included are slight variations of the general purpose functions found in the jsonnet's `std` library, such as `map`, `filter`, and `flatpMap` plus some additional ones like `groupBy`. More specific functions are also included like `objectFrom[Array]` to facilitate composing an `Object` from an `Array`, and `orderBy` to sort elements.

**but wait, there's more!**

Developers will also find functions grouped by following set of modules, accessed in the form of `xtr.[module].[function]`:
* `datetime`: operations on date and time values, like `compare` and `plus(duration)`
* `crypto`: encrypt, decrypt, or hash data
* `arrays`: extended set of array operations, like `distinctBy` and `partition`
* `objects`: extended set of object operations, like `leftJoin`
* `strings`: operations on strings, like `truncate` and `wrapIfMissing`
* `base64`: encode and decode data in base64
* `url`: encode and decode data for URLs

and a few more.
