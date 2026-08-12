# xtr

## contains
### Array contains
`contains(container: Array, value: Any): Boolean`

Returns `true` if `arr` contains the given `value`, otherwise `false`.

**Example**
```
xtr.contains([1, 2, 3], 1)
```
**Result**
```
true
```

<br/>
### String contains
`contains(container: String, value: String): Boolean`

Returns `true` if `str1` contains `str2`, otherwise `false`.

**Example**
```
xtr.contains('Lorem ipsum', 'Lorem')
```
**Result**
```
true
```

<br/>
## endsWith
`endsWith(main: String, sub: String): Boolean`

Returns `true` if `str1` ends with `str2`, otherwise `false`.

**Example**
```
xtr.endsWith('Lorem ipsum', 'ipsum')
```
**Result**
```
true
```

<br/>
## entries
`entries(obj: Object[A]): Array[Object[String|A]]`

Returns an `Array[Object[String|A]]` with one `Object[String|A]` for every entry in `obj`. The result has the form of `[{ key: String, value: A }]`.

**Example**
```
xtr.entries({ scala: '3.1.3', java: '19' })
```
**Result**
```
[
  {
    key: 'scala',
    value: '3.1.3'
  },
  {
    key: 'java',
    value: '19'
  }
]
```

<br/>
## filter
### filter func(value)
`filter(array: Array[A], func: Func[(A) => Boolean]): Array[A]`

Returns a new `Array[A]` containing the elements of `array` that satisfy the given `func`, which must accept an `A` value to test.

**Example**
```
xtr.filter([1, 2, 3, 4], function(item) item < 3)
```
**Result**
```
[1, 2]
```

<br/>
### filter func(value, idx)
`filter(array: Array[A], func: Func[(A, Number) => Boolean]): Array[A]`

Returns a new `Array[A]` containing the elements of `array` that satisfy the given `func`, which must accept an `A` value and its `Number` index to test.

**Example**
```
xtr.filter([1, 2, 3, 4], function(item, idx) idx > 2)
```
**Result**
```
[4]
```

<br/>
## filterNotEq
`filterNotEq(collection: Array[A], value: B): Array[A]`

Returns a new `Array[A]` containing the elements of `arr` that are not equal to the given `value`.

**Example**
```
xtr.filterNotEq([1, 2, 3, 4, 3, 4], 3)
```
**Result**
```
[1, 2, 4, 4]
```

<br/>
## filterNotIn
`filterNotIn(first: Array[A], second: Array[B]): Array[A]`

Returns a new `Array[A]` containing the elements of `first` that are not in the given `second`. `first` may also be an `Object`, in which case the entries whose keys are listed in `second` are removed.

**Example**
```
xtr.filterNotIn([1, 2, 3, 4, 3, 4], [3, 4])
```
**Result**
```
[1, 2]
```

<br/>
## filterObject
### filterObject func(value)
`filterObject(array: Object[A], func: Func[(A) => Boolean]): Object[A]`

Returns a new `Object[A]` containing the entries of `array` that satisfy the given `func`, which must accept an `A` value to test.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
};

xtr.filterObject(languages, function(lang) lang.isJvm)
```
**Result**
```
{
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true }
}
```

<br/>
### filterObject func(value, key)
`filterObject(array: Object[A], func: Func[(A, String) => Boolean]): Object[A]`

Returns a new `Object[A]` containing the entries of `array` that satisfy the given `func`, which must accept an `A` value and its corresponding `String` key to test.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
};

xtr.filterObject(languages, function(lang, name) !lang.isJvm || name == 'scala')
```
**Result**
```
{
    scala: { version: '3.1.3', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
}
```

<br/>
### filterObject func(value, key, idx)
`filterObject(array: Object[A], func: Func[(A, String, Number) => Boolean]): Object[A]`

Returns a new `Object[A]` containing the entries of `array` that satisfy the given `func`, which must accept an `A` value and its corresponding `String` key and `Number` index to test.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
};

xtr.filterObject(languages, function(lang, name, idx) idx == 0 || name == 'python')
```
**Result**
```
{
    scala: { version: '3.1.3', isJvm: true},
    python: { version: '3.10.4', isJvm: false}
}
```

<br/>
## flatMap
### flatMap func(value)
`flatMap(array: Array[A], func: Func[(A) => Array[B]]): Array[B]`

Returns a new `Array[B]` containing the elements of every `Array[B]` obtained by applying the given `func` to all elements in `array`. `func` must accept an `A` value.

**Example**
```
xtr.flatMap([1, 3, 5], function(item) [item, item * item])
```
**Result**
```
[1, 1, 3, 9, 5, 25]
```

<br/>
### flatMap func(value, idx)
`flatMap(array: Array[A], func: Func[(A, Number) => Array[B]]): Array[B]`

Returns a new `Array[B]` containing the elements of every `Array[B]` obtained by applying the given `func` to all elements in `array`. `func` must accept an `A` value and its `Number` index.

**Example**
```
xtr.flatMap([1, 3, 5], function(item, idx) [item, item * idx])
```
**Result**
```
[1, 0, 3, 3, 5, 10]
```

<br/>
## flatMapObject
### flatMapObject func(value)
`flatMapObject(value: Object[A], func: Func[(A) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entries of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value.

**Example**
```
local candidateReqs = {
    req1: { skillsType: 'dev', required: ['java'], preferred: ['unit-testing'] },
    req2: { skillsType: 'ops', required: ['containers'], preferred: ['kubernetes'] }
};
local reqsWeight(req) = {
    [req.required[0]]: 5,
    [req.preferred[0]]: 2,
    [if req.skillsType == 'dev' then 'github']: 4,
    [if req.skillsType == 'ops' then 'jenkins']: 4
};

xtr.flatMapObject(candidateReqs, reqsWeight)
```
**Result**
```
{
    java: 5,
    'unit-testing': 2,
    github: 4,
    containers: 5,
    kubernetes: 2,
    jenkins: 4
}
```

<br/>
### flatMapObject func(value, key)
`flatMapObject(value: Object[A], func: Func[(A, String) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entries of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value and its corresponding `String` key.

**Example**
```
local candidateReqs = {
    dev: { required: ['java'], preferred: ['unit-testing'] },
    ops: { required: ['containers'], preferred: ['kubernetes'] }
};
local reqsWeight(req, type) = {
    [req.required[0]]: 5,
    [req.preferred[0]]: 2,
    [if type == 'dev' then 'github']: 4,
    [if type == 'ops' then 'jenkins']: 4
};

xtr.flatMapObject(candidateReqs, reqsWeight)
```
**Result**
```
{
    java: 5,
    'unit-testing': 2,
    github: 4,
    containers: 5,
    kubernetes: 2,
    jenkins: 4
}
```

<br/>
### flatMapObject func(value, key, idx)
`flatMapObject(value: Object[A], func: Func[(A, String, Number) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entries of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value and its corresponding `String` key and `Number` index.

**Example**
```
local candidateReqs = {
    dev: { required: ['java'], preferred: ['unit-testing'] },
    ops: { required: ['containers'], preferred: ['kubernetes'] }
};
local reqsWeight(req, type, idx) = {
    [req.required[0]]: 5,
    [req.preferred[0]]: if idx == 0 then 3 else 1,
    [if type == 'dev' then 'github']: if idx == 0 then 4 else 2,
    [if type == 'ops' then 'jenkins']: if idx == 0 then 4 else 2
};

xtr.flatMapObject(candidateReqs, reqsWeight)
```
**Result**
```
{
    java: 5,
    'unit-testing': 3,
    github: 4,
    containers: 5,
    kubernetes: 1,
    jenkins: 2
}
```

<br/>
## flatten
`flatten(array: Array[Array[A]]): Array[A]`

Returns a new `Array[A]` containing the elements of every `Array[A]` in `arr`.

**Example**
```
xtr.flatten([[1, 2], [3]])
```
**Result**
```
[1, 2, 3]
```

<br/>
## foldLeft
`foldLeft(arr: Array[A], init: Any, func: Func[(A, Any) => Any]): Any`

From left to right in `arr`, applies the given `func` to the first element with `init`, then applies it to every subsequent element with the result of the previous invocation. `func` must accept an `A` value and `Any` value, which is `init` for the first invocation, and the result of the previous one for all others.

Returns the `Any` result of the final `func` invocation.

!!! hint
    `fold` functions usually mutate the "accumulator" value on each invocation, thus "folding" the collection into a single value.

**Example**
```
xtr.foldLeft([1, 2, 3], 0, function(item, acc) item + acc)
```
**Result**
```
6
```

<br/>
## foldRight
`foldRight(arr: Array[A], init: Any, func: Func[(A, Any) => Any]): Any`

From right to left in `arr`, applies the given `func` to the first element with `init`, then applies it to every subsequent element with the result of the previous invocation. `func` must accept an `A` value and `Any` value, which is `init` for the first invocation, and the result of the previous one for all others.

Returns the `Any` result of the final `func` invocation.

!!! hint
    `fold` functions usually mutate the "accumulator" value on each invocation, thus "folding" the collection into a single value.

**Example**
```
xtr.foldRight(['Lorem', 'ipsum', 'dolor'], '', function(item, acc) acc + ' ' + item)
```
**Result**
```
' dolor ipsum Lorem'
```

<br/>
## groupBy
### Array groupBy func(value)
`groupBy(container: Array[A], func: Func[(A) => String]): Object[Array[A]]`

Returns an `Object[Array[A]]` where the keys are the results of applying the given `func` to all elements in `container`, and their corresponding values are the `container` elements for which the `func` invocation resulted in such key. `func` must accept an `A` value.

**Example**
```
local languages = [
    { name: 'scala', version: '3.1.3', isJvm: true },
    { name: 'java', version: '19', isJvm: true },
    { name: 'python', version: '3.10.4', isJvm: false }
];

xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')
```
**Result**
```
{
    jvmLangs: [
        { name: 'scala', version: '3.1.3', isJvm: true },
        { name: 'java', version: '19', isJvm: true }
    ],
    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
}
```

<br/>
### Array groupBy func(value, idx)
`groupBy(container: Array[A], func: Func[(A, Number) => String]): Object[Array[A]]`

Returns an `Object[Array[A]]` where the keys are the results of applying the given `func` to all elements in `container`, and their corresponding values are the `container` elements for which the `func` invocation resulted in such key. `func` must accept an `A` value and its `Number` index.

**Example**
```
local languages = [
    { name: 'scala', version: '3.1.3', isJvm: true },
    { name: 'java', version: '19', isJvm: true },
    { name: 'python', version: '3.10.4', isJvm: false }
];
local langFunc(lang, idx) = if idx == 0 then 'preferred'
    else if lang.isJvm then 'jvmLangs'
    else 'others';

xtr.groupBy(languages, langFunc)
```
**Result**
```
{
    preferred: [{ name: 'scala', version: '3.1.3', isJvm: true }],
    jvmLangs: [{ name: 'java', version: '19', isJvm: true }],
    others: [{ name: 'python', version: '3.10.4', isJvm: false }]
}
```

<br/>
### Object groupBy func(value)
`groupBy(container: Object[A], func: Func[(A) => String]): Object[Object[A]]`

Returns an `Object[Object[A]]` where the keys are the results of applying the given `func` to all elements in `container`, and their corresponding values are the `container` elements for which the `func` invocation resulted in such key. `func` must accept an `A` value.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
};

xtr.groupBy(languages, function(lang) if lang.isJvm then 'jvmLangs' else 'others')
```
**Result**
```
{
    jvmLangs: {
        scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
        java: { version: '19', isJvm: true, project: 'jdk.java.net' }
    },
    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
}
```

<br/>
### Object groupBy func(value, key)
`groupBy(container: Object[A], func: Func[(A, String) => String]): Object[Object[A]]`

Returns an `Object[Object[A]]` where the keys are the results of applying the given `func` to all elements in `container`, and their corresponding values are the `container` elements for which the `func` invocation resulted in such key.`func` must accept an `A` value and its corresponding `String` key.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
};
local langFunc(lang, name) = if name == 'scala' then 'preferred'
    else if lang.isJvm then 'jvmLangs'
    else 'others';

xtr.groupBy(languages, langFunc)
```
**Result**
```
{
    preferred: { scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' }},
    jvmLangs: { java: { version: '19', isJvm: true, project: 'jdk.java.net' }},
    others: { python: { version: '3.10.4', isJvm: false, project: 'python.org' }}
}
```

<br/>
## indexOf
### Array indexOf
`indexOf(container: Array, value: Any): Number`

Returns the index of the first element in `container` equal to the given `value`, or `-1` if there is none.

**Example**
```
xtr.indexOf([1, 2, 3, 2], 2)
```
**Result**
```
1
```

<br/>
### String indexOf
`indexOf(container: String, value: String): Number`

Returns the index of the first occurrence of `value` in `container`, or `-1` if there is none.

**Example**
```
xtr.indexOf('hello', 'l')
```
**Result**
```
2
```

<br/>
## indicesOf
### Array indicesOf
`indicesOf(container: Array, value: Any): Array[Number]`

Returns an `Array[Number]` with the indices of the elements in `arr` that equal `value`.

**Example**
```
xtr.indicesOf([1, 7, 3, 4, 7], 7)
```
**Result**
```
[1, 4]
```

<br/>
### String indicesOf
`indicesOf(container: String, value: String): Array[Number]`

Returns an `Array[Number]` with the indices of the substrings in `str1` that equal `str2`.

**Example**
```
xtr.indicesOf('lorem ipsum dolor', 'lo')
```
**Result**
```
[0, 14]
```

<br/>
## isArray
`isArray(v: Any): Boolean`

Returns `true` if `value` is an `Array`, otherwise `false`.

**Example**
```
xtr.isArray([1, 2])
```
**Result**
```
true
```

<br/>
## isBlank
`isBlank(value: String): Boolean`

Returns `true` if `str` is empty or contains whitespace characters only, otherwise `false`.

**Example**
```
xtr.isBlank('   ')
```
**Result**
```
true
```

<br/>
## isBoolean
`isBoolean(v: Any): Boolean`

Returns `true` if `value` is a `Boolean`, otherwise `false`.

**Example**
```
xtr.isBoolean(false)
```
**Result**
```
true
```

<br/>
## isDecimal
`isDecimal(value: Number): Boolean`

Returns `true` if `num` is a decimal, otherwise `false`.

**Example**
```
xtr.isDecimal(2.5)
```
**Result**
```
true
```

<br/>
## isEmpty
### Array isEmpty
`isEmpty(container: Array): Boolean`

Returns `true` if `arr` is empty, otherwise `false`.

**Example**
```
xtr.isEmpty([])
```
**Result**
```
true
```

<br/>
### Object isEmpty
`isEmpty(container: Object): Boolean`

Returns `true` if `obj` is empty, otherwise `false`.

**Example**
```
xtr.isEmpty({})
```
**Result**
```
true
```

<br/>
### String isEmpty
`isEmpty(container: String): Boolean`

Returns `true` if `str` is empty, otherwise `false`.

**Example**
```
xtr.isEmpty('')
```
**Result**
```
true
```

<br/>
## isFunction
`isFunction(v: Any): Boolean`

Returns `true` if `value` is a `Function`, otherwise `false`.

**Example**
```
local increment(item) = item + 1;

xtr.isFunction(increment)
```
**Result**
```
true
```

<br/>
## isInteger
`isInteger(value: Number): Boolean`

Returns `true` if `num` is an integer, otherwise `false`.

**Example**
```
xtr.isInteger(2)
```
**Result**
```
true
```

<br/>
## isNumber
`isNumber(v: Any): Boolean`

Returns `true` if `value` is a `Number`, otherwise `false`.

**Example**
```
xtr.isNumber(2)
```
**Result**
```
true
```

<br/>
## isObject
`isObject(v: Any): Boolean`

Returns `true` if `value` is an `Object`, otherwise `false`.

**Example**
```
xtr.isObject({})
```
**Result**
```
true
```

<br/>
## isString
`isString(v: Any): Boolean`

Returns `true` if `value` is a `String`, otherwise `false`.

**Example**
```
xtr.isString('Lorem')
```
**Result**
```
true
```

<br/>
## join
### Array[Number] join
`join(array: Array[Number], sep: String): String`

Returns a new `String` composed of all the elements in `array` separated by the `sep`.

**Example**
```
xtr.join([0, 1, 1, 2, 3, 5, 8], ', ')
```
**Result**
```
'0, 1, 1, 2, 3, 5, 8'
```

<br/>
### Array[String] join
`join(array: Array[String], sep: String): String`

Returns a new `String` composed of all the elements in `array` separated by the `sep`.

**Example**
```
xtr.join(['hello', 'world', '!'], ' ')
```
**Result**
```
'hello world !'
```

<br/>
## keys
`keys(obj: Object): Array[String]`

Returns an `Array[String]` containing all the keys in `obj`.

**Example**
```
xtr.keys({ scala: '3.1.3', java: '19' })
```
**Result**
```
['scala', 'java']
```

<br/>
## lastIndexOf
### Array lastIndexOf
`lastIndexOf(container: Array, value: Any): Number`

Returns the index of the last element in `container` equal to the given `value`, or `-1` if there is none.

**Example**
```
xtr.lastIndexOf([1, 2, 3, 2], 2)
```
**Result**
```
3
```

<br/>
### String lastIndexOf
`lastIndexOf(container: String, value: String): Number`

Returns the index of the last occurrence of `value` in `container`, or `-1` if there is none.

**Example**
```
xtr.lastIndexOf('hello', 'l')
```
**Result**
```
3
```

<br/>
## length
### Array length
`length(arr: Array): Number`

Returns the size of `arr`.

**Example**
```
xtr.length([1, 2, 3])
```
**Result**
```
3
```

<br/>
### Func length
`length(func: Function): Number`

Returns the number of `func` parameters.

**Example**
```
local add(item, item2) = item + item2;

xtr.length(add)
```
**Result**
```
2
```

<br/>
### Object length
`length(obj: Object): Number`

Returns the number of entries in `obj`.

**Example**
```
xtr.length({ key: 'value' })
```
**Result**
```
1
```

<br/>
### String length
`length(str: String): Number`

Returns the number of characters in `str`.

**Example**
```
xtr.length('hello, world!')
```
**Result**
```
13
```

<br/>
## map
### map func(value)
`map(value: Array[A], func: Func[(A) => B]): Array[B]`

Returns a new `Array[B]` with the results of applying `func` to all elements in `value`. `func` must accept an `A`.

**Example**
```
xtr.map([1, 2, 3, 4], function(item) item * item)
```
**Result**
```
[1, 4, 9, 16]
```

<br/>
### map func(value, idx)
`map(value: Array[A], func: Func[(A, Number) => B]): Array[B]`

Returns a new `Array[B]` with the results of applying `func` to all elements in `value`. `func` must accept an `A` and its `Number` index.

**Example**
```
xtr.map([1, 2, 3, 4], function(item, idx) item * idx)
```
**Result**
```
[0, 2, 6, 12]
```

<br/>
## mapEntries
### mapEntries func(value)
`mapEntries(value: Object[A], func: Func[(A) => B]): Array[B]`

Returns an `Array[B]` with the results of applying `func` to all entries in `value`. `func` must accept an `A`.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
    java: { version: '19', isJvm: true, project: 'jdk.java.net' },
    python: { version: '3.10.4', isJvm: false, project: 'python.org' }
};

xtr.mapEntries(languages, function(lang) lang.project)
```
**Result**
```
['scala-lang.org', 'jdk.java.net', 'python.org']
```

<br/>
### mapEntries func(value, key)
`mapEntries(value: Object[A], func: Func[(A, String) => B]): Array[B]`

Returns an `Array[B]` with the results of applying `func` to all entries in `value`. `func` must accept an `A` and its corresponding `String` key.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
    java: { version: '19', isJvm: true, project: 'jdk.java.net' }
};

xtr.mapEntries(languages, function(lang, name) {
    name: name,
    version: lang.version
})
```
**Result**
```
[
    { name: 'scala', version: '3.1.3' },
    { name: 'java', version: '19' }
]
```

<br/>
### mapEntries func(value, key, idx)
`mapEntries(value: Object[A], func: Func[(A, String, Number) => B]): Array[B]`

Returns an `Array[B]` with the results of applying `func` to all entries in `value`. `func` must accept an `A` and its corresponding `String` key and `Number` index.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true, project: 'scala-lang.org' },
    java: { version: '19', isJvm: true, project: 'jdk.java.net' }
};
local langFunc(lang, name, idx) = {
    name: name,
    [if idx == 0 then 'preferred']: true
};

xtr.mapEntries(languages, langFunc)
```
**Result**
```
[{ name: 'scala', preferred: true }, { name: 'java' }]
```

<br/>
## mapObject
### mapObject func(value)
`mapObject(value: Object[A], func: Func[(A) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entry of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value, and must return an `Object` with a single entry; returning an `Object` with more than one entry is an error.

**Example**
```
xtr.mapObject({ scala: '3.1.3', java: '19' }, function(version) { [version]: true })
```
**Result**
```
{ '3.1.3': true, '19': true }
```

<br/>
### mapObject func(value, key)
`mapObject(value: Object[A], func: Func[(A, String) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entry of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value and its corresponding `String` key, and must return an `Object` with a single entry.

**Example**
```
xtr.mapObject({ scala: '3.1.3', java: '19' }, function(version, name) { [name]: 'v' + version })
```
**Result**
```
{ scala: 'v3.1.3', java: 'v19' }
```

<br/>
### mapObject func(value, key, idx)
`mapObject(value: Object[A], func: Func[(A, String, Number) => Object[B]]): Object[B]`

Returns a new `Object[B]` containing the entry of every `Object[B]` obtained by applying the given `func` to all entries in `value`. `func` must accept an `A` value and its corresponding `String` key and `Number` index, and must return an `Object` with a single entry.

**Example**
```
xtr.mapObject({ scala: '3.1.3', java: '19' }, function(version, name, idx) { [name + idx]: version })
```
**Result**
```
{ scala0: '3.1.3', java1: '19' }
```

<br/>
## max
### Array[Boolean] max
`max(array: Array[Boolean]): Boolean`

Returns the max `Boolean` in `arr`, with `true` being "bigger" than `false`.

**Example**
```
xtr.max([false, false, true])
```
**Result**
```
true
```

<br/>
### Array[Number] max
`max(array: Array[Number]): Number`

Returns the max `Number` in `arr`.

**Example**
```
xtr.max([0, 8, 2, 100])
```
**Result**
```
100
```

<br/>
### Array[String] max
`max(array: Array[String]): String`

Returns the max `String` in `arr`.

**Example**
```
xtr.max(['Lorem', 'zzz', 'ipsum', 'dolor'])
```
**Result**
```
'zzz'
```

<br/>
## maxBy
### maxBy func(_) => Boolean
`maxBy(array: Array[A], func: Func[(A) => Boolean]): A`

Returns the max `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', isPreferred: false },
    { name: 'python', version: '3.1.14', isPreferred: false },
    { name: 'scala', version: '3.1.3', isPreferred: true }
];

xtr.maxBy(languages, function(lang) lang.isPreferred)
```
**Result**
```
{ name: 'scala', version: '3.1.3', isPreferred: true }
```

<br/>
### maxBy func(_) => Number
`maxBy(array: Array[A], func: Func[(A) => Number]): A`

Returns the max `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', weight: 2 },
    { name: 'python', version: '3.1.14', weight: 2 },
    { name: 'scala', version: '3.1.3', weight: 4 }
];

xtr.maxBy(languages, function(lang) lang.weight)
```
**Result**
```
{ name: 'scala', version: '3.1.3', weight: 4 }
```

<br/>
### maxBy func(_) => String
`maxBy(array: Array[A], func: Func[(A) => String]): A`

Returns the max `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', score: 'B' },
    { name: 'python', version: '3.1.14', score: 'B' },
    { name: 'scala', version: '3.1.3', score: 'S' }
];

xtr.maxBy(languages, function(lang) lang.score)
```
**Result**
```
{ name: 'scala', version: '3.1.3', score: 'S' }
```

<br/>
## min
### Array[Boolean] min
`min(array: Array[Boolean]): Boolean`

Returns the min `Boolean` in `arr`, with `false` being "smaller" than `true`.

**Example**
```
xtr.min([false, false, true])
```
**Result**
```
false
```

<br/>
### Array[Number] min
`min(array: Array[Number]): Number`

Returns the min `Number` in `arr`.

**Example**
```
xtr.min([0, 8, 2, 100])
```
**Result**
```
0
```

<br/>
### Array[String] min
`min(array: Array[String]): String`

Returns the min `String` in `arr`.

**Example**
```
xtr.min(['Lorem', 'AAA', 'ipsum', 'dolor'])
```
**Result**
```
'AAA'
```

<br/>
## minBy
### minBy func(_) => Boolean
`minBy(array: Array[A], func: Func[(A) => Boolean]): A`

Returns the min `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', isPreferred: false },
    { name: 'python', version: '3.1.14', isPreferred: false },
    { name: 'scala', version: '3.1.3', isPreferred: true }
];

xtr.minBy(languages, function(lang) lang.isPreferred)
```
**Result**
```
{ name: 'java', version: '19', isPreferred: false }
```

<br/>
### minBy func(_) => Number
`minBy(array: Array[A], func: Func[(A) => Number]): A`

Returns the min `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', weight: 2 },
    { name: 'python', version: '3.1.14', weight: 2 },
    { name: 'scala', version: '3.1.3', weight: 4 }
];

xtr.minBy(languages, function(lang) lang.weight)
```
**Result**
```
{ name: 'java', version: '19', weight: 2 }
```

<br/>
### minBy func(_) => String
`minBy(array: Array[A], func: Func[(A) => String]): A`

Returns the min `A` by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', score: 'B' },
    { name: 'python', version: '3.1.14', score: 'B' },
    { name: 'scala', version: '3.1.3', score: 'S' }
];

xtr.minBy(languages, function(lang) lang.score)
```
**Result**
```
{ name: 'java', version: '19', score: 'B' }
```

<br/>
## parseNum
`parseNum(str: String): Number`

Returns the `Number` representation of the given `str`.

**Example**
```
parseNum('123.45')
```
Result
```
123.45
```

<br/>
## sortBy

`sortBy` also accepts an `Object` as `value`, sorting its entries. In both cases `func` may take a second parameter: the element's `Number` index for arrays, or the entry's `String` key for objects.

### sortBy func(_) => Boolean
`sortBy(value: Array[A], func: Func[(A) => Boolean]): Array[A]`

Returns a new `Array[A]` with the contents of `value` sorted by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', isPreferred: false },
    { name: 'scala', version: '3.1.3', isPreferred: true },
    { name: 'python', version: '3.1.14', isPreferred: false }
];

xtr.sortBy(languages, function(lang) lang.isPreferred)
```
**Result**
```
[
    { name: 'java', version: '19', isPreferred: false },
    { name: 'python', version: '3.1.14', isPreferred: false },
    { name: 'scala', version: '3.1.3', isPreferred: true }
]
```

<br/>
### sortBy func(_) => Number
`sortBy(value: Array[A], func: Func[(A) => Number]): Array[A]`

Returns a new `Array[A]` with the contents of `value` sorted by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', weight: 3 },
    { name: 'scala', version: '3.1.3', weight: 4 },
    { name: 'python', version: '3.1.14', weight: 2 }
];

xtr.sortBy(languages, function(lang) lang.weight)
```
**Result**
```
[
    { name: 'python', version: '3.1.14', weight: 2 },
    { name: 'java', version: '19', weight: 3 },
    { name: 'scala', version: '3.1.3', weight: 4 }
]
```

<br/>
### sortBy func(_) => String
`sortBy(value: Array[A], func: Func[(A) => String]): Array[A]`

Returns a new `Array[A]` with the contents of `value` sorted by comparing the values returned by `func`, which must accept an `A`.

**Example**
```
local languages = [
    { name: 'java', version: '19', score: 'B' },
    { name: 'scala', version: '3.1.3', score: 'S' },
    { name: 'python', version: '3.1.14', score: 'B' }
];

xtr.sortBy(languages, function(lang) lang.score)
```
**Result**
```
[
    { name: 'java', version: '19', score: 'B' },
    { name: 'python', version: '3.1.14', score: 'B' },
    { name: 'scala', version: '3.1.3', score: 'S' }
]
```

<br/>
## range
`range(begin: Number, end: Number): Array[Number]`

Returns an `Array[Number]` containing numbers from `first` to `last`.

**Example**
```
xtr.range(1, 5)
```
**Result**
```
[1, 2, 3, 4, 5]
```

<br/>
## read
### read mediaType
`read(data: String, mimeType: String): Any`

Parses the `data` as the given `mimeType` using the data format plugins available to the `Transformer`.

**Example**
```
xtr.read('<hello>world!</hello>', 'application/xml')
```
**Result**
```
{
  hello: { '$': 'world!' }
}
```

<br/>
### read mediaType, params
`read(data: String, mimeType: String, params: Object): Any`

Parses the `data` as the given `mimeType` and `params` options using the data format plugins available to the `Transformer`.

**Example**
```
xtr.read('<hello>world!</hello>', 'application/xml', { textkey: '_txt' })
```
**Result**
```
{
  hello: { _txt: 'world!' }
}
```

<br/>
## readUrl
### readUrl mediaType
`readUrl(url: String, mimeType: String): Any`

Retrieves the data at `url` and parses it as the given `mimeType`. Supported schemes/protocols are `http`, `https`, `classpath`, and `file`.

Asumming `example.com` returns `<hello>world!</hello>`:

**Example**
```
xtr.readUrl('example.com/data', 'application/xml')
```
**Result**
```
{
  hello: { '$': 'world!' }
}
```

<br/>
### readUrl mediaType, params
`readUrl(url: String, mimeType: String, params: Object): Any`

Retrieves the data at `url` and parses it as the given `mimeType` with `params` options. Supported schemes/protocols are `http`, `https`, `classpath`, and `file`.

Asumming `example.com` returns `<hello>world!</hello>`:

**Example**
```
xtr.readUrl('example.com', 'application/xml', { textkey: '_txt' })
```
**Result**
```
{
  hello: { _txt: 'world!' }
}
```

<br/>
## rmKey
`rmKey(collection: Object[A], value: String): Object[A]`

Returns a new `Object[A]` containing the entries of `obj` minus the entry whose key equals the given `value`.

**Example**
```
xtr.rmKey({ scala: '3.1.3', java: '19' }, 'java')
```
**Result**
```
{ scala: '3.1.3' }
```

<br/>
## rmKeyIn
`rmKeyIn(first: Object[A], second: Array[String]): Object[A]`

Returns a new `Object[A]` containing the entries of `obj` minus the entries whose key is in the given `arr`.

**Example**
```
xtr.rmKeyIn({ scala: '3.1.3', java: '19' }, ['java', 'scala'])
```
**Result**
```
{}
```

<br/>
## replace
`replace(str1: String, str2: String, replacement: String): String`

Returns a new `String` with the contents of `str1`, with occurrences of `str2` replaced by `replacement`.

**Example**
```
xtr.replace('hello, world!', 'world', 'everyone')
```
**Result**
```
'hello, everyone!'
```

<br/>
## reverse
### Array reverse
`reverse(collection: Array): Array`

Returns a new `Array` with the elements of `arr` in reversed order.

**Example**
```
xtr.reverse([1, 2, 3])
```
**Result**
```
[3, 2, 1]
```

<br/>
### Object reverse
`reverse(collection: Object): Object`

Returns a new `Object` with the entries of `obj` in reversed order.

**Example**
```
xtr.reverse({ key1: 'value1', key2: 'value2' })
```
**Result**
```
{ key2: 'value2', key1: 'value1' }
```

<br/>
### String reverse
`reverse(collection: String): String`

Returns a new `String` with the characters of `str` in reversed order.

**Example**
```
xtr.reverse('rolod muspi meroL')
```
**Result**
```
'Lorem ipsum dolor'
```

<br/>
## split
`split(str1: String, str2: String): Array[String]`

Returns an `Array[String]` containing the chunks of `str1` split by the contents of `str2`.

**Example**
```
xtr.split('hello, world!', 'o')
```
**Result**
```
['hell', ', w', 'rld!']
```

<br/>
## startsWith
`startsWith(str1: String, str2: String): Boolean`

Returns `true` if `str1` starts with `str2`.

**Example**
```
xtr.startsWith('hello, world!', 'hello')
```
**Result**
```
true
```

<br/>
## toLowerCase
`toLowerCase(str: String): String`

Returns the lowercase representation of `str`.

**Example**
```
xtr.toLowerCase('Hello World!')
```
**Result**
```
'hello world!'
```

<br/>
## toString
`toString(value: Any): String`

Returns the `String` representation of `value`.

**Example**
```
{
    bool: xtr.toString(true),
    num: xtr.toString(365),
    nil: xtr.toString(null)
}
```
**Result**
```
{
    bool: 'true',
    num: '365',
    nil: 'null'
}
```

<br/>
## toUpperCase
`toUpperCase(str: String): String`

Returns the uppercase representation of `str`.

**Example**
```
xtr.toUpperCase('Hello World!')
```
**Result**
```
'HELLO WORLD!'
```

<br/>
## trim
`trim(str: String): String`

Returns a new `String` with the contents of `str` removed of leading and trailing whitespaces.

**Example**
```
xtr.trim('  hello, world!   ')
```
**Result**
```
'hello, world!'
```

<br/>
## type
`type(value: Any): String`

Returns the `String` name of the type of `value`.

**Example**
```
local func(it) = it;

{
    bool: xtr.type(true),
    num: xtr.type(365),
    nil: xtr.type(null),
    arr: xtr.type([]),
    obj: xtr.type({}),
    func: xtr.type(func)
}
```
**Result**
```
{
    bool: 'boolean',
    num: 'number',
    nil: 'null',
    arr: 'array',
    obj: 'object',
    func: 'function'
}
```

<br/>
## uuid
`uuid(): String`

Generates a new random UUID v4 `String`.

**Example**
```
xtr.uuid()
```
**Result**
```
'8eae62af-d2dc-4759-8316-ce6eeca0b61c'
```

<br/>
## values
`values(obj: Object[A]): Array[A]`

Returns an `Array[A]` containing all the values in `obj`.

**Example**
```
xtr.values({ scala: '3.1.3', java: '19' })
```
**Result**
```
['3.1.3', '19']
```

<br/>
## write
### write mediaType
`write(data: Any, mimeType: String): String`

Writes the `data` in the given `mimeType` format using the data format plugins available to the `Transformer`.

**Example**
```
xtr.write({ hello: 'world', arr: [], nil: null }, 'application/json')
```
**Result**
```
'{"hello":"world","arr":[],"nil":null}'
```

<br/>
### write mediaType, params
`write(data: Any, mimeType: String, params: Object): String`

Writes the `data` in the given `mimeType` format and `params` options using the data format plugins available to the `Transformer`.

**Example**
```
xtr.write({ hello: 'world', arr: [] }, 'application/json', { indent: true })
```
**Result**
```
'{\n    "hello": "world",\n    "arr": [\n        \n    ]\n}'
```
