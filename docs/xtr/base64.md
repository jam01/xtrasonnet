# xtr.base64

## decode
`decode(value: String | Number): String`

Returns the Base64-decoded `data`.

**Example**
```
xtr.base64.decode('SGVsbG8gV29ybGQ=')
```
**Result**
```
'Hello World'
```

<br/>
## encode
`encode(value: String | Number): String`

Returns the Base64-encoded `data`.

**Example**
```
xtr.base64.encode('Hello World')
```
**Result**
```
'SGVsbG8gV29ybGQ='
```
