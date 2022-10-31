# Header

The xtrasonnet header is a regular jsonnet comment of the following form:

```jsonnet
/** xtrasonnet
input payload application/json
output application/xml

// disregard order of elements
preserveOrder=false
*/
```

## Input directives

The input directives signal to xtrasonnet what the expected media types are at design time for given inputs. For more information about media types see the section on [data formats](../dataformats/).

The `payload` name is reserved for the "main" transformation input. All other inputs would have custom names.

If a given input to xtrasonnet is of an explicit media type at runtime, the header directive will be ignored.

## Output directive

The output directive signals to xtrasonnet the desired media type, at design time, to output. For more information about media types see the section on [data formats](../dataformats/).

If programmatically, an explicit output media type is specified, the header directive will be ignored.

## Comments

Comments within the xtrasonnet header must start with two slashes `//`, otherwise they will be treated as unrecognized directives or options.

## Preserve order

Developers may disregard the order of elements in `Objects` which may speed up execution. 

Default value is `true`.
