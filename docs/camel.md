# Camel xtrasonnet Language
*Since Camel 3.20*

The Camel xtrasonnet component enables the use xtrasonnet transformations as [Expressions](https://camel.apache.org/manual/expression.html) or [Predicates](https://camel.apache.org/manual/predicate.html) in the [DSL](https://camel.apache.org/manual/dsl.html).

## Getting started

Add the following maven dependency

```xml
<dependency>
  <groupId>io.github.jam01</groupId>
  <artifactId>camel-xtrasonnet</artifactId>
  <version>0.5.0</version>
</dependency>
```

and simply create an xtrasonnet expression:

```java
import static io.github.jam01.camel.builder.XtrasonnetBuilder.xtrasonnet;

xtrasonnet(myXtrasonnet)
```

## Examples

Here is a simple example using a xtrasonnet expression as a predicate in a [Message Filter](https://camel.apache.org/components/3.20.x/eips/filter-eip.html), it will filter messages if any element in `lineItems` is over 100.

``` java
// lets route if a line item is over $100
from("direct:filter")
    .filter(xtrasonnet("xtr.arrays.any(payload.lineItems, function(item) item > 100)"))
    .to("mock:result");
```

And the XML DSL:

```xml
<route id="xml-filter">
    <from uri="direct:xml-filter"/>
    <filter>
        <language language="xtrasonnet">
            xtr.arrays.any(payload.lineItems, function(item) item > 100)
        </language>
        <to uri="mock:xml-result"/>
    </filter>
</route>
```

Here is an example of a simple xtrasonnet expression in a transformation EIP. 

```java
from("direct:transform")
    .transform(xtrasonnet("payload.lineItems", String.class)
        .bodyMediaType(MediaTypes.APPLICATION_XML)
        .outputMediaType(MediaTypes.APPLICATION_JSON))
    .to("mock:result");
```

And the XML DSL:

```xml
<route id="xml-transform">
    <from uri="direct:xml-transform"/>
    <setProperty name="CamelXtrasonnetBodyMediaType"><constant>application/xml</constant></setProperty>
    <setProperty name="CamelXtrasonnetOutputMediaType"><constant>application/json</constant></setProperty>
    <setProperty name="CamelXtrasonnetResultType"><constant>java.lang.String</constant></setProperty>
    <transform>
        <language language="xtrasonnet">payload.lineItems</language>
    </transform>
    <to uri="mock:xml-result"/>
</route>
```

## Specifying result type

The xtrasonnet expression will return a `io.github.jam01.xtrasonnet.document.Document` by default. The document preserves the content type metadata along with the contents of the result of the transformation. In predicates, however, the `Document` will be automatically unwrapped and the boolean content will be returned. Similarly any time you want the content in a specific type, like a String, you have to instruct the xtrasonnet to do so.

In Java DSL:

```java
xtrasonnet("payload.foo", String.class)
```

In XML DSL you use `CamelXtrasonnetResultType` as an exchange property or message header to provide a fully qualified classname:

```xml
<setProperty name="CamelXtrasonnetResultType"><constant>java.lang.String</constant></setProperty>
<language language="xtrasonnet">payload.foo</language>
```

!!! note
    If you wanted to specify result type through an exchange property or message header in the Java DSL, you can utilize the values in `XtrasonnetConstants`.

If the expression results in an Array, or an Object, you can instruct the expression to return you `List.class` or `Map.class`, respectively. However, the output media type must be `application/x-java-object` (default).

!!! note
    The default `Document` object is useful in situations where there are intermediate transformation steps, and so retaining the content metadata through a route execution is valuable.

## Specifying media types

A few options are provided for specifying the body and output media types. The xtrasonnet expression will look for a body media type in the following order:

1. If the body is a `Document`, it will use the metadata in the object
2. If the `bodyMediaType()` builder method was used, it will use its value
3. A `"CamelXtrasonnetBodyMediaType"` exchange property or message header
4. A `"Content-Type"` message header
5. The [xtrasonnet Header](../header) payload input media type directive
6. `application/x-java-object`

And for output media type:

1. If the `outputMediaType()` builder method was used, it will use its value
2. A `"CamelXtrasonnetBodyMediaType"` exchange property or message header
3. The [xtrasonnet Header](../header) output media type directive
4. `application/x-java-object`


!!! note
    If you wanted to specify media types through an exchange property or message header in the Java DSL, you can utilize the values in `XtrasonnetConstants`.

## cml Library

The Camel xtrasonnet component adds the following xtrasonnet functions that can be used to access the exchange:

| Function             | Argument     | Type   | Description                                                                                                                                     |
|----------------------|--------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| cml.properties       | property key | String | Lookup a property using the [Properties component](https://camel.apache.org/components/3.20.x/properties-component.html) (property placeholder) |
| cml.header           | header name  | String | Lookup a message header                                                                                                                         |
| cml.exchangeProperty | property key | String | Lookup an exchange property                                                                                                                     |

Here's an example showing some of these functions in use:

```java
from("direct:cml")
    .setBody(xtrasonnet("'hello ' + cml.properties('toGreet')", String.class))
    .to("mock:result");
```

And the XML DSL:

```xml
<route id="xml-cml">
    <from uri="direct:xml-cml"/>
    <setProperty name="CamelXtrasonnetResultType"><constant>java.lang.String</constant></setProperty>
    <setBody>
        <language language="xtrasonnet">'hello ' + cml.properties('toGreet')</language>
    </setBody>
    <to uri="mock:xml-result"/>
</route>
```

## Expression from resource

You can externalize the script and have Camel load it from a resource such as `"classpath:"`, `"file:"`, or `"http:"`.

This is done using the following syntax: `"resource:scheme:location"`, e.g.: to refer to a file on the classpath you can do:

```java
from("direct:resource")
    .setHeader("myHeader", xtrasonnet("resource:classpath:myXtrasonnet.xtr", String.class))
    .to("mock:result")
```
