# text/csv

Comma Separated Values

## Reader parameters
### `quotechar`
The character for identifying quoted strings.

### `separator`
The character for identifying separate values.

### `escapechar`
The character for identifying an escape sequence.

### `header`
Whether there is a header line present. 

Allowed values are `present` or `absent`.

If marked as present the plugin will read each line into a jsonnet `Object` where the keys are the corresponding values of the header line, otherwise will read each line into an `Array` of `String`.

### `columns`
The columns names to use if a header line is not present in the input and jsonnet `Object`s are required.

The value must be a comma `','` separated list of column names. Alternatively, if the `separator` parameter is also present that character should be used.

## Writer parameters
### `quotechar`
The character for quoting strings if necessary.

### `separator`
The character for separating values.

### `escapechar`
The character for escaping a character sequence.

### `header`
Whether to output a header line.

Allowed values are `present` or `absent`.

### `columns`
The columns names to use if the jsonnet to output is an `Array` of `String` and the `header` parameter is also present.

The value must be a comma `','` separated list of column names. Alternatively, if the `separator` parameter is also present that character should be used.
