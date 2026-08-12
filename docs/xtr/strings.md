# xtr.strings

## appendIfMissing
`appendIfMissing(str: String, suffix: String): String`

Returns `str`, appended with `suffix` if it does not already end with `suffix`.

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
`charCode(char: String): String`

Returns the character-code for the given `char`.

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
`charCodeAt(str: String, index: Number): String`

Returns the character-code for the character at the given `index` in `str`.

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
`ofCharCode(code: Number): String`

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
`isAlpha(str: String): String`

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
`isAlphanumeric(str: String): String`

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
`isLowerCase(str: String): String`

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
`isNumeric(str: String): String`

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
`isUpperCase(str: String): String`

Returns `true` if the alphabetic characters in the given `str` are all uppercse, otherwise `false`.

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
`leftPad(str: String, size: Number, char: String): String`

Returns `str` prepended with enough repetitions of `char` required to meet the given `size`, otherwise returns `str` if its size is already equal or longer than `size`. Only the first character of `char` is used, and it must not be empty. `str` itself is never altered.

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
`numOrdinalOf(num: Number): String`

Returns the numeric ordinal name for the given `num`.

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
`pluralize(word: String): String`

Returns the plural of the given `word`.

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
`prependIfMissing(str: String, prefix: String): String`

Returns `str`, prepended with `prefix` if it does not already start with `prefix`.

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
`repeat(str: String, n: Number): String`

Returns `str` repeated `n` times.

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
`rightPad(str: String, size: Number, char: String): String`

Returns `str` appended with enough repetitions of `char` required to meet the given `size`, otherwise returns `str` if its size is already equal or longer than `size`. Only the first character of `char` is used, and it must not be empty.

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
`singularize(word: String): String`

Returns the singular of the given `word`.

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
`substringAfter(str1: String, str2: String): String`

Returns the contents of `str1` after the first occurrence of `str2`, otherwise returns `str1` if it does not contain `str2`.

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
`substringAfterLast(str1: String, str2: String): String`

Returns the contents of `str1` after the last occurrence of `str2`, otherwise returns `str1` if it does not contain `str2`.

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
`substringBefore(str1: String, str2: String): String`

Returns the contents of `str1` before the first occurrence of `str2`, otherwise returns `str1` if it does not contain `str2`.

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
`substringBeforeLast(str1: String, str2: String): String`

Returns the contents of `str1` before the last occurrence of `str2`, otherwise returns `str1` if it does not contain `str2`.

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
`unwrap(str: String, wrap: String): String`

Returns `str` without the given `wrap` as prefix and suffix, if found.

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
`wrap(str: String, wrap: String): String`

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
`wrapIfMissing(str: String, wrap: String): String`

Returns `str`, prepended and appended with `wrap`, if not found.

**Example**
```
xtr.strings.wrapIfMissing('_Hello, world!', '_')
```
**Result**
```
'_Hello, world!_'
```
