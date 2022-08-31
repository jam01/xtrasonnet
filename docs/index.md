---
hide:
- navigation
- toc
---

# **_extensible jsonnet transformations_**

## xtrasonnet is an extensible, template-based, data transformation engine for Java or any JVM-based language. It is an extension of databricks' [sjsonnet](https://github.com/databricks/sjsonnet) project, a Scala implementation of Google's [jsonnet](https://github.com/google/jsonnet).

<br/>

<figure markdown>
![xtrasonnet](assets/images/xtrasonnet.drawio.png)
</figure>

### xtrasonnet extends sjsonnet by enabling extensibility, adding support for data formats other than JSON, and offering data transformation facilities through the `xtr` library and a few additions to the jsonnet language itself.

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
