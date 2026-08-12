# xtr.arrays

## all
`all(value: Array[A], func: Func[(A) => Boolean]): Boolean`

Returns `true` if all elements in `value` satisfy the given `func`, otherwise `false`. `func` must accept an `A`.

**Example**
```
xtr.arrays.all([1, 2, 3], function(item) item > 0)
```
**Result**
```
true
```

<br/>
## any
`any(value: Array[A], func: Func[(A) => Boolean]): Boolean`

Returns `true` if any element in `value` satisfies the given `func`, otherwise `false`. `func` must accept an `A`.

**Example**
```
xtr.arrays.any([1, 2, 3], function(item) item > 1)
```
**Result**
```
true
```

<br/>
## break
`break(arr: Array[A], func: Func[(A) => Boolean]): Object[Array[A]]`

Returns an `Object` with two entries:

- `left` key with an `Array[A]` containing the elements of `arr` before the first element to satisfy the given `func`.
- `right` key with an `Array[A]` containing the remaining elements of `arr`.

**Example**
```
xtr.arrays.break([1, 2, 3, 4, 5], function(item) item % 2 == 0)
```
**Result**
```
{ left: [1], right: [2, 3, 4, 5] }
```

<br/>
## chunksOf
`chunksOf(array: Array[A], size: Number): Array[Array[A]]`

Returns a new `Array` of `Array[A]`, with every element containing the next `size` elements in `array`.

**Example**
```
xtr.arrays.chunksOf([1, 2, 3, 4, 5], 2)
```
**Result**
```
[[1, 2], [3, 4], [5]]
```

<br/>
## countBy
`countBy(arr: Array[A], func: Func[(A) => Boolean]): Number`

Returns a `Number` count of all the elements in `arr` that satisfy the given `func`, which must accept an `A`.

**Example**
```
xtr.arrays.countBy([1, 2, 3], function(item) item > 2)
```
**Result**
```
1
```

<br/>
## distinctBy
### distinctBy func(value)
`distinctBy(container: Array[A], func: Func[(A) => B]): Array[A]`

Returns a new `Array` with the distinct elements in `container` using the given `func` function for comparison. `func` must accept an `A`.

**Example**
```
xtr.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item) item % 3)
```
**Result**
```
[1, 2, 3]
```

The modulo operation on the elements yields `[1, 2, 0, 1, 2, 0]` meaning `1` and `4` share the same identity, therefore `1` is kept and `4` discarded. Same is true for `2` and `3` with `5` and `6`, respectively.

<br/>
### distinctBy func(value, idx)
`distinctBy(container: Array[A], func: Func[(A, Number) => B]): Array[A]`

Returns a new `Array` with the distinct elements in `container` using the given `func` function for comparison. `func` must accept an `A` and its `Number` index.

**Example**
```
xtr.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item, idx) (item + idx) % 3)
```
**Result**
```
[1, 2, 3]
```

The computation on the elements yields `[1, 0, 2, 1, 0, 2]`, meaning `4`, `5`, and `6` share an identity with `1`, `2`, and `3` respectively, so they are discarded.

<br/>
## drop
`drop(arr: Array[A], num: Number): Array[A]`

Returns a new `Array` with the elements in `arr` but dropping the first `num` elements.

**Example**
```
xtr.arrays.drop([1, 2, 3, 4, 5], 3)
```
**Result**
```
[4, 5]
```

<br/>
## dropWhile
`dropWhile(arr: Array[A], func: Func[(A) => Boolean]): Array[A]`

Returns a new `Array` with the elements in `arr`, but dropping the first elements while they satisfy the given `func`, which must accept an `A`.

**Example**
```
xtr.arrays.dropWhile([1, 2, 3, 4, 5], function(item) item * 3 < 10)
```
**Result**
```
[4, 5]
```

<br/>
## duplicatesBy
`duplicatesBy(array: Array[A], func: Func[(A) => B]): Array[A]`

Returns a new `Array` with the elements in `array` whose identity, as computed by `func`, is shared by more than one element. Each
duplicated identity contributes the first element that produced it, in the order they appear in `array`.

**Example**
```
xtr.arrays.duplicatesBy([1, 2, 3, 1, 2], function(item) item)
```
**Result**
```
[1, 2]
```

**Example**
```
xtr.arrays.duplicatesBy([{ id: 1, n: 'a' }, { id: 2, n: 'c' }, { id: 1, n: 'b' }], function(item) item.id)
```
**Result**
```
[{ id: 1, n: 'a' }]
```

<br/>
## find
### find func(value)
`find(arr: Array[A], func: Func[(A) => Boolean]): Array[A]`

Returns a single element `Array` with the first `A` that satisfies the given `func`, which must accept an `A`.

**Example**
```
xtr.arrays.find([1, 2, 3, 4, 5], function(item) item * 3 > 10)
```
**Result**
```
[4]
```

<br/>
### find func(value, idx)
`find(arr: Array[A], func: Func[(A, Number) => Boolean]): Array[A]`

Returns a single element `Array` with the first `A` that satisfies the given `func`, which must accept an `A` and its `Number` index.

**Example**
```
xtr.arrays.find([1, 2, 3, 4, 5], function(item, idx) item * (3 + idx) > 10)
```
**Result**
```
[3]
```

<br/>
## flat
`flat(arr: Array[Any]): Array[Any]`

Returns a new single level `Array` with the contents of all `Array` in `arr`, recursively flattening each `Array` element found.

**Example**
```
xtr.arrays.flat([[1, 2], '3', [4, {}, [5, 6]]])
```
**Result**
```
[1, 2, '3', 4, {}, 5, 6]
```

<br/>
## indexWhere
`indexWhere(arr: Array[A], func: Func[(A) => Boolean]): Number`

Returns the `Number` index of the first element that satisfies the given `func`, otherwise `-1`. `func` must accept an `A`.

**Example**
```
xtr.arrays.indexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)
```
**Result**
```
0
```

<br/>
## indicesWhere
`indicesWhere(arr: Array[A], func: Func[(A) => Boolean]): Array[Number]`

Returns an `Array[Number]` with the indices of elements that satisfy the given `func`, which must accept exactly one parameter, an `A`.

**Example**
```
xtr.arrays.indicesWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)
```
**Result**
```
[0, 1, 2]
```

<br/>
## lastIndexWhere
`lastIndexWhere(arr: Array[A], func: Func[(A) => Boolean]): Number`

Returns the `Number` index of the last element in `arr` that satisfies the given `func`, otherwise `-1`. `func` must accept an `A`.

**Example**
```
xtr.arrays.lastIndexWhere([1, 2, 3, 4, 5], function(item) item * 3 < 10)
```
**Result**
```
2
```

<br/>
## occurrencesBy
`occurrencesBy(arr: Array[A], func: Func[(A) => String|Number|Boolean|Null]): Object[Number]`

Returns an `Object` with an entry for each unique identity of elements in `arr`. The value of each entry is the `Number` of elements in `arr` that produced such identity, using `func`, which must take an `A`. Entries appear in the order their identity was first produced.

**Example**
```
xtr.arrays.occurrencesBy([1, 2, 3, 4, 5], function(item) if item < 4 then 'under4' else 'over4')
```
**Result**
```
{ 'under4': 3, 'over4': 2 }
```

<br/>
## partition
`partition(arr: Array[A], func: Func[(A) => Boolean]): Object[Array[A]]`

Returns an `Object` with two entries:

- `pass` key with an `Array[A]` of the subset of elements in `arr` that satisfy the given `func`, which must take an `A`.
- `fail` key with an `Array[A]` of the subset of elements in `arr` that fail the given `func`, which must take an `A`.

**Example**
```
xtr.arrays.partition([1, 2, 3, 4, 5], function(item) item < 4)
```
**Result**
```
{ pass: [1, 2, 3], fail: [4, 5] }
```

<br/>
## splitAt
`splitAt(array: Array[A], index: Number): Object[Array[A]]`

Returns an `Object` with two entries:

- `left` key with an `Array[A]` containing the elements of `array` before the `index` element.
- `right` key with an `Array[A]` containing the remaining elements of `array`.

**Example**
```
xtr.arrays.splitAt([1, 2, 3, 4, 5], 3)
```
**Result**
```
{ left: [1, 2, 3], right: [4, 5] }
```

<br/>
## sumBy
`sumBy(array: Array[A], func: Func[(A) => Number]): Number`

Returns the `Number` sum of the values obtained by applying the given `func` to every element in `array`. `func` must accept an `A` and return a `Number`.

**Example**
```
xtr.arrays.sumBy([{ price: 3 }, { price: 12 }], function(item) item.price)
```
**Result**
```
15
```

<br/>
## take
`take(array: Array[A], index: Number): Array[A]`

Returns a new `Array` with the elements in `array`, but only taking the first `index` elements.

**Example**
```
xtr.arrays.take([1, 2, 3, 4, 5], 3)
```
**Result**
```
[1, 2, 3]
```

<br/>
## takeWhile
`takeWhile(array: Array[A], func: Func[(A) => Boolean]): Array[A]`

Returns a new `Array` with the elements in `array`, but only taking the first elements that satisfy the given `func`, which must accept an `A`.

**Example**
```
xtr.arrays.takeWhile([1, 2, 3, 4, 5], function(item) item * 2 < 9)
```
**Result**
```
[1, 2, 3, 4]
```

<br/>
## unzip
`unzip(array: Array[Array[A]]): Array[Array[A]]`

Create n-number of `Arrays`, each containing the n-th element of every array in `array`.

Returns a new `Array` of equal size to the shortest array in `array`. Every n-th element in the result is an `Array` containing the n-th element of the arrays in `array`.

**Example**
```
xtr.arrays.unzip([[1, 'x'], [2, 'y'], [3, 'z']])
```
**Result**
```
[[1, 2, 3], ['x', 'y', 'z']]
```

## unzipAll
`unzipAll(array: Array[Array[A]], fill: B): Array[Array[A|B]]`

Create n-number of `Arrays`, each containing the n-th element of every array in `arr`, using a `fill` value for missing n-th elements.

Returns a new `Array` of equal size to the longest array in `arr`. Every n-th element in the result is an `Array` containing the n-th element of the arrays in `arr` that have such element, or `fill` for short arrays.

**Example**
```
xtr.arrays.unzipAll([[1, 'x'], [2], [3, 'z']], 'NA')
```
**Result**
```
[[1, 2, 3], ['x', 'NA', 'z']]
```

<br/>
## zip
`zip(arr1: Array[A], arr2: Array[B], arr3: Array[C], arr4: Array[D], arr5: Array[E]): Array[Array[A|B|C|D|E]]`

Combines corresponding elements of the given arrays. Accepts two to five arrays; `arr3` through `arr5` are optional.

Returns a new `Array` of equal size to the shortest array given. Every n-th element in the result is an `Array` containing the n-th element of the given arrays.

**Example**
```
xtr.arrays.zip([1, 2, 3], ['x', 'y', 'z'])
```
**Result**
```
[[1, 'x'], [2, 'y'], [3, 'z']]
```

## zipAll
`zipAll(array: Array[Array[A]], fill: B): Array[Array[A|B]]`

Combines corresponding elements of the arrays in `arr`, using a `fill` value for short arrays.

Returns a new `Array` of equal size to the longest array in `arr`. Every n-th element in the result is an `Array` containing the n-th element of the arrays in `arr` that have such element, or `fill` for short arrays.

**Example**
```
xtr.arrays.zipAll([[1, 2, 3], ['x', 'y']], 'NA')
```
**Result**
```
[[1, 'x'], [2, 'y'], [3, 'NA']]
```
