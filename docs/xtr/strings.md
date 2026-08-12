# xtr.strings

## appendIfMissing
`appendIfMissing(str1: String, str2: String): String`

Returns `str1`, appended with `str2` if it does not already end with `str2`.

**Example**
```
xtr.strings.appendIfMissing('Hello', ' World')
```
**Result**
```
'Hello World'
```

<br/>
## capitalize
`capitalize(str: String): String`

Returns the capitalized version of `str`, by capitalizing the first letter of every word.

**Example**
```
xtr.strings.capitalize('hello world')
```
**Result**
```
'Hello World'
```

<br/>
## charCode
`charCode(str: String): Number`

Returns the character-code for the given `str`, a single character.

**Example**
```
xtr.strings.charCode('*')
```
**Result**
```
42
```

<br/>
## charCodeAt
`charCodeAt(str: String, num: Number): Number`

Returns the character-code for the character at the given `num` index in `str`.

**Example**
```
xtr.strings.charCodeAt('_*_', 1)
```
**Result**
```
42
```

<br/>
## ofCharCode
`ofCharCode(num: Number): String`

Returns the character for the given character-code.

**Example**
```
xtr.strings.ofCharCode(42)
```
**Result**
```
'*'
```

<br/>
## isAlpha
`isAlpha(str: String|Number|Boolean): Boolean`

Returns `true` if the given `str` contains only alphabetic characters, otherwise `false`.

**Example**
```
xtr.strings.isAlpha('abcde')
```
**Result**
```
true
```

<br/>
## isAlphanumeric
`isAlphanumeric(str: String|Number|Boolean): Boolean`

Returns `true` if the given `str` contains only alphanumeric characters, otherwise `false`.

**Example**
```
xtr.strings.isAlphanumeric('a1b2cd3e4')
```
**Result**
```
true
```

<br/>
## isLowerCase
`isLowerCase(str: String): Boolean`

Returns `true` if the alphabetic characters in the given `str` are all lowercase, otherwise `false`.

**Example**
```
xtr.strings.isLowerCase('hello')
```
**Result**
```
true
```

<br/>
## isNumeric
`isNumeric(str: String|Number|Boolean): Boolean`

Returns `true` if the given `str` contains only numeric characters.

**Example**
```
xtr.strings.isNumeric('34634')
```
**Result**
```
true
```

<br/>
## isUpperCase
`isUpperCase(str: String): Boolean`

Returns `true` if the alphabetic characters in the given `str` are all uppercase, otherwise `false`.

**Example**
```
xtr.strings.isUpperCase('HELLO')
```
**Result**
```
true
```

<br/>
## leftPad
`leftPad(str: String|Number, offset: Number, pad: String): String`

Returns `str` prepended with enough repetitions of `pad` required to meet the given `offset` size; a `str` already that long -- including any negative or zero `offset` -- is returned unchanged. Only the first character of `pad` is used, and an empty `pad` is an error. A `Number` value for `str` is stringified the same way as `xtr.toString`.

**Example**
```
xtr.strings.leftPad('Hello', 10, ' ')
```
**Result**
```
'     Hello'
```

<br/>
## match
`match(str: String, regex: String): Array[String|Null] | Null`

Matches the entire `str` against the given [Java regular expression](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html) `regex`.

Returns an `Array` with the entire match followed by the value of each capture group, where groups that did not participate in the match are `null`. Returns `null` if `str` does not match, which composes with the `??` operator.

**Example**
```
xtr.strings.match('user@example.com', '(\\w+)@([\\w.]+)')
```
**Result**
```
['user@example.com', 'user', 'example.com']
```

<br/>
## matches
`matches(str: String, regex: String): Boolean`

Reports whether the entire `str` matches the given [Java regular expression](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html) `regex`.

**Example**
```
xtr.strings.matches('say user@example.com twice', '\\w+@[\\w.]+')
```
**Result**
```
false
```

<br/>
## numOrdinalOf
`numOrdinalOf(num: Number|String): String`

Returns the numeric ordinal name for the given `num`. A `String` is accepted if its contents are numeric; anything else is an error.

**Example**
```
xtr.strings.numOrdinalOf(1)
```
**Result**
```
'1st'
```

[//]: # ( todo: document algo)
<br/>
## pluralize
`pluralize(value: String): String`

Returns the plural of the given `value`.

**Example**
```
xtr.strings.pluralize('car')
```
**Result**
```
'cars'
```

<br/>
## prependIfMissing
`prependIfMissing(str1: String, str2: String): String`

Returns `str1`, prepended with `str2` if it does not already start with `str2`.

**Example**
```
xtr.strings.prependIfMissing('World', 'Hello ')
```
**Result**
```
'Hello World'
```

<br/>
## repeat
`repeat(str: String, num: Number): String`

Returns `str` repeated `num` times.

**Example**
```
xtr.strings.repeat('hey ', 2)
```
**Result**
```
'hey hey '
```

<br/>
## rightPad
`rightPad(str: String|Number, offset: Number, pad: String): String`

Returns `str` appended with enough repetitions of `pad` required to meet the given `offset` size; a `str` already that long -- including any negative or zero `offset` -- is returned unchanged. Only the first character of `pad` is used, and an empty `pad` is an error. A `Number` value for `str` is stringified the same way as `xtr.toString`.

**Example**
```
xtr.strings.rightPad('Hello', 10, ' ')
```
**Result**
```
'Hello     '
```

[//]: # (todo: document algo)
<br/>
## scan
`scan(str: String, regex: String): Array[Array[String|Null]]`

Finds every match of the given [Java regular expression](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html) `regex` within `str`.

Returns an `Array` with one element per match, each an `Array` of the entire match followed by the value of each capture group, where groups that did not participate in the match are `null`. Returns an empty `Array` if there are no matches.

**Example**
```
xtr.strings.scan('write to a1@b1.com or a2@b2.com', '(\\w+)@([\\w.]+)')
```
**Result**
```
[['a1@b1.com', 'a1', 'b1.com'], ['a2@b2.com', 'a2', 'b2.com']]
```

<br/>
## singularize
`singularize(value: String): String`

Returns the singular of the given `value`.

**Example**
```
xtr.strings.singularize('cars')
```
**Result**
```
'car'
```

<br/>
## substringAfter
`substringAfter(value: String, sep: String): String`

Returns the contents of `value` after the first occurrence of `sep`, or an empty `String` if `value` does not contain `sep`.

**Example**
```
xtr.strings.substringAfter('!XHelloXWorldXAfter', 'X')
```
**Result**
```
'HelloXWorldXAfter'
```

<br/>
## substringAfterLast
`substringAfterLast(value: String, sep: String): String`

Returns the contents of `value` after the last occurrence of `sep`, or an empty `String` if `value` does not contain `sep`.

**Example**
```
xtr.strings.substringAfterLast('!XHelloXWorldXAfter', 'X')
```
**Result**
```
'After'
```

<br/>
## substringBefore
`substringBefore(value: String, sep: String): String`

Returns the contents of `value` before the first occurrence of `sep`, or an empty `String` if `value` does not contain `sep`.

**Example**
```
xtr.strings.substringBefore('!XHelloXWorldXAfter', 'X')
```
**Result**
```
'!'
```

<br/>
## substringBeforeLast
`substringBeforeLast(value: String, sep: String): String`

Returns the contents of `value` before the last occurrence of `sep`, or an empty `String` if `value` does not contain `sep`.

**Example**
```
xtr.strings.substringBeforeLast('!XHelloXWorldXAfter', 'X')
```
**Result**
```
'!XHelloXWorld'
```

<br/>
## toCamelCase
`toCamelCase(str: String): String`

Returns the toCamelCased version of `str`, by removing all spaces and underscores, and capitalizing the first letter of every word after the first.

**Example**
```
xtr.strings.toCamelCase('Hello to_everyone')
```
**Result**
```
'helloToEveryone'
```

<br/>
## toKebabCase
`toKebabCase(str: String): String`

Returns the kebab-case version of `str`, by changing alphabetic characters to lowercase, and replacing all spaces and underscores for dashes.

**Example**
```
xtr.strings.toKebabCase('Hello World_X')
```
**Result**
```
'hello-world-x'
```

<br/>
## toSnakeCase
`toSnakeCase(str: String): String`

Returns the snake_case version of the given `str`, by prepending uppercase characters with an underscore, changing alphabetic characters to lowercase, and replacing all spaces for underscores.

**Example**
```
xtr.strings.toSnakeCase('Hello WorldX')
```
**Result**
```
'hello_world_x'
```

<br/>
## unwrap
`unwrap(value: String, wrapper: String): String`

Returns `value` without the given `wrapper` as prefix and suffix, when both are present. When only one is present it is moved to the other side: a prefix-only match strips the prefix and appends the `wrapper`, a suffix-only match strips the suffix and prepends it.

**Example**
```
xtr.strings.unwrap('_Hello, world!_', '_')
```
**Result**
```
'Hello, world!'
```

<br/>
## wrap
`wrap(value: String, wrapper: String): String`

Returns `str`, prepended and appended with `wrap`.

**Example**
```
xtr.strings.wrap('_Hello, world!', '_')
```
**Result**
```
'__Hello, world!_'
```

<br/>
## wrapIfMissing
`wrapIfMissing(value: String, wrapper: String): String`

Returns `str`, prepended and appended with `wrap`, if not found.

**Example**
```
xtr.strings.wrapIfMissing('_Hello, world!', '_')
```
**Result**
```
'_Hello, world!_'
```
