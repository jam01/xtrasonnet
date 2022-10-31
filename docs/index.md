---
hide:
- navigation
- toc
---

# **_extensible jsonnet transformations_**

## xtrasonnet is an extensible, jsonnet-based, data transformation engine for Java or any JVM-based language. 

<br/>

<figure markdown>
![xtrasonnet](assets/images/xtrasonnet.drawio.png)
</figure>

### xtrasonnet is an extension of databricks' [sjsonnet](https://github.com/databricks/sjsonnet), a Scala implementation of Google's [jsonnet](https://github.com/google/jsonnet). xtrasonnet enables extensibility, adds support for data formats other than JSON, and adds data transformation facilities through the `xtr` library and few additions to the jsonnet language itself.

<div class="container p-0">
    <div class="row">
        <div class="col-5 d-flex flex-column">
            ``` json
            {
                "message": "hello, world!"
            }
            ```
        </div>
        <div class="col-2 d-flex justify-content-center align-items-center">
            ➡
        </div>
        <div class="col-5">
            ``` jsonnet
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
        </div>
    </div>
    <div class="row d-flex justify-content-center">
        ➡
    </div>
    <div class="row">
        <div class="col">
            ``` xml
            <?xml version='1.0' encoding='UTF-8'?>
            <root>
                <msg>hello, world!</msg>
                <at>2022-08-14T00:19:35.731362Z</at>
            </root>
            ```
        </div>
    </div>
</div>

## How extensible?
xtrasonnet has two points of extensibility:

* _Custom functions_: users can write native (e.g.: Java or Scala) functions as a `Library` and utilize them from their transformation code.
* _Any* data format_: users can write a custom `DataFormatPlugin` and transform from/to a given data format.

\* Any format that can be expressed as jsonnet elements.

## What kind of additions to the jsonnet language?
There are two main additions motivated to facilitate data transformation applications:

### Null-safe select `?.`
This allows developers to select, and chain, properties arbitrarily without testing existence.

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

➡

```jsonnet
{
    a: 'value',
    b: null,
    c: null
}
```



### Null coalescing operator `??`
This allows developers to tersely test for `null` and provide a default value. For example

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

➡

```jsonnet
{
    a: 'value',
    b: 'defaultB',
    c: 'defaultC'
}
```
