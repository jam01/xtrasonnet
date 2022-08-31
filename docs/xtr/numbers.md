# xtr.numbers

## ofBinary
`ofBinary(binary: String | Number): Number`

Returns the `Number` representation for the given `binary`.

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
`ofHex(hexadecimal: String): Number`

Returns the `Number` representation for the given `hexadecimal`.

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
`ofOctal(octal: String | Number): Number`

Returns the `Number` representation for the given `octal`.

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
`ofRadix(value: String | Number, n: Number): Number`

Returns the `Number` representation for the given Base-`n` `value`

**Example**
```
xtr.numbers.ofRadixNumber('10', 3)
```
**Result**
```
3
```

<br/>
## toBinary
`toBinary(number: Number): String`

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
`toHex(number: Number): String`

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
`toOctal(number: Number): String`

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
`toRadix(value: Number, n: Number): String`

Returns the Base-`n` representation for the given `value` as a `String`.

**Example**
```
xtr.numbers.toRadix('3', 3)
```
**Result**
```
'10'
```
