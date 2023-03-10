# Java Object

## Supported MediaTypes
* `application/x-java-object`
* `application/java`

!!! Warning
    This format is not supported in the xtrasonnet Playground

## Custom `jackson.JsonMapper`
xtrasonnet Java Object support is built on top of the popular FasterXML Jackson library. Being a general purpose transformation language some choices are made around Jackson's `JsonMapper` configuration, such as:

* `enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)`
* `defaultDateFormat(new StdDateFormat())` // ISO-8601 for `java.util.Date`

In order to make different configurations or just further customize the mapper, developers can create the `DefaultJavaPlugin(JsonMapper)` constructor and provide their customized mapper.

```java
var myJavaPlugin = new DefaultJavaPlugin(myJsonMapper);
var myTransformer = Transformer.builder(myJsonnet)
    .extendPlugins(plugins -> {
        plugins.removeIf(plugin -> plugin instanceof DefaultJavaPlugin); // remove the default one
        plugins.add(0, myJavaPlugin);
    })
    .build();
```

## `java.util.Date` support
xtrasonnet defaults (de)serializing `Date` objects to UTC time, in order to workaround the type's lossy issues. Below is a brief description of the issues with `Date` that motivate a setting default time zone.

Up until Java 8, the only date-time type in Java was `java.util.Date`. `Date` represents an instant in time, in UTC. It has the following shortcomings:

* No unambiguous way to represent a date-only value; developers are forced to use a `Date` with its time set to zero.
* Implicitly (de)serializes with the system-local time zone, even though the type itself doesn't track a time zone.

To illustrate the issues with this type, consider the following scenario:

1. A `new Date()` taken at second zero of the year 2023 in a -06:00 server, results in an object with the following data `2023-01-01T06:00:00.000+00:00`
2. serializing such a date in that same server with an ISO-8601 format results in the string `2023-01-01T00:00:00.000-06:00`
3. serializing as a date only value in that same server results in the string `2023-01-01`
4. deserializing that date-only string in a server running in -05:00 would create a `Date` object with the following data `2023-01-01T05:00:00.000+00:00`
5. deserializing that date-only string in a server running in +01:00 would create a `Date` object with the following data `1999-12-31T23:00:00.000+00:00`

While (4.) may be OK even with the loss of one hour, (5.) may have some serious consequences.

The shortcomings of the `Date` type lead to the necessity of coordinating Java (de)serialization to use the same time zone; otherwise the resulting `Date` object risks losing time. For a library like xtrasonet UTC is the only sane time zone default.

For more information check out Jon Skeet's [All about java.util.Date](https://codeblog.jonskeet.uk/2017/04/23/all-about-java-util-date/)

## Supported reader parameters

### `dateformat`
The date and time format to use when serializing `java.util.Date` values as Strings, and deserializing from JSON Strings.

The given value must conform to the standard [Java Date Format patterns](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html).

## Supported writer parameters

### `type`
The desired output Java object type.

### `dateformat`
The date and time format to use when serializing `java.util.Date` values as Strings, and deserializing from JSON Strings.

The given value must conform to the standard [Java Date Format patterns](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html).
