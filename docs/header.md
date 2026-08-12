# Header

The xtrasonnet header is a regular jsonnet comment of the following form:

```jsonnet
/** xtrasonnet
input payload application/json
output application/xml

// this is a comment
preserveOrder=false
*/
```

## Input directives

The input directives signal to xtrasonnet what the expected media types are at design time for given inputs. For more information about media types see the section on [data formats](../dataformats/).

The `payload` name is reserved for the "main" transformation input. All other inputs would have custom names.

If a given input to xtrasonnet is of an explicit media type at runtime, the header directive will be ignored.

Each input may be declared once; repeating a name is an error. To share media type parameters across
declarations, see [sharing parameters](#sharing-media-type-parameters) below.

## Output directive

The output directive signals to xtrasonnet the desired media type, at design time, to output. For more information about media types see the section on [data formats](../dataformats/).

If programmatically, an explicit output media type is specified, the header directive will be ignored.

Like inputs, the output may be declared once.

## Sharing media type parameters

Media type parameters can be declared once and shared, rather than repeated on every line. Directives
layer general to specific, the more specific winning per parameter:

1. `dataformat <mediatype>` — its parameters apply to every input *and* the output of that media type
2. `input * <mediatype>` — its parameters apply to every input of that media type
3. `input <name> <mediatype>` / `output <mediatype>` — the declaration itself

```jsonnet
/** xtrasonnet
dataformat application/csv;quotechar='
input * application/csv;separator=|
input payload application/csv;header=absent
input other application/csv
output application/csv
*/
```

Here `payload` is read as CSV with quote char `'`, separator `|`, and no header line; `other` is read
the same but with a header line; and the output is written with quote char `'` and default settings
otherwise.

## Comments

Comments within the xtrasonnet header must start with two slashes `//`, otherwise they will be treated as unrecognized directives or options.

## Preserve order

Developers may disregard the order of elements in `Objects` which may speed up execution. 

Default value is `true`.
