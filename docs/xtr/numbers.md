# xtr.numbers

## ofBinary
`ofBinary(value: String | Number): Number`

Returns the `Number` representation for the given binary `value`.

**Example**
```
xtr.numbers.ofBinary(1100100)
```
**Result**
```
100
```

<br/>
## ofHex
`ofHex(value: String | Number): Number`

Returns the `Number` representation for the given hexadecimal `value`.

**Example**
```
xtr.numbers.ofHex('F')
```
**Result**
```
15
```

<br/>
## ofOctal
`ofOctal(str: String | Number): Number`

Returns the `Number` representation for the given octal `str`.

**Example**
```
xtr.numbers.ofOctal(107136)
```
**Result**
```
36446
```

<br/>
## ofRadix
`ofRadix(value: String | Number, num: Number): Number`

Returns the `Number` representation for the given Base-`num` `value`. The radix must be between `2` and `36`.

**Example**
```
xtr.numbers.ofRadix('10', 3)
```
**Result**
```
3
```

<br/>
## toBinary
`toBinary(value: Number | String): String`

Returns the binary representation for the given `number`.

**Example**
```
xtr.numbers.toBinary(100)
```
**Result**
```
'1100100'
```

<br/>
## toHex
`toHex(value: Number | String): String`

Returns the hexadecimal representation for the given `number`.

**Example**
```
xtr.numbers.toHex(15)
```
**Result**
```
'F'
```

<br/>
## toOctal
`toOctal(value: Number | String): String`

Returns the octal representation for the given `number`.

**Example**
```
xtr.numbers.toOctal(36446)
```
**Result**
```
'107136'
```

<br/>
## toRadix
`toRadix(value: Number | String, num: Number): String`

Returns the Base-`n` representation for the given `value` as a `String`.

**Example**
```
xtr.numbers.toRadix('3', 3)
```
**Result**
```
'10'
```
