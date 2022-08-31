# xtr.base64

## decode
`decode(data: String): String`

Returns the Base64-decoded `data`.

**Example**
```
xtr.url.decode('SGVsbG8gV29ybGQ=')
```
**Result**
```
'Hello World'
```

<br/>
## encode
`encode(data: String): String`

Returns the Base64-encoded `data`.

**Example**
```
xtr.url.encode('Hello World')
```
**Result**
```
'SGVsbG8gV29ybGQ='
```
