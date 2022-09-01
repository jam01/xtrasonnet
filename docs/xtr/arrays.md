# xtr.arrays

## all
`all(arr: Array[A], predicate: Func[(A) => Boolean]): Boolean`

Returns `true` if all elements in `arr` satisfy the given `predicate`, otherwise `false`. `predicate` must accept an `A`.

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
`any(arr: Array[A], function: Func[(A) => Boolean]): Boolean`

Returns `true` if any element in `arr` satisfies the given `predicate`, otherwise `false`. `predicate` must accept an `A`.

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
`break(arr: Array[A], predicate: Func[(A) => Boolean]): Object[Array[A]]`

Returns an `Object` with two entries:

- `left` key with an `Array[A]` containing the elements of `arr` before the first element to satisfy the given `predicate`.
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
`chunksOf(arr: Array[A], size: Number): Array[Array[A]]`

Returns a new `Array` of `Array[A]`, with every element containing the next `size` elements in `arr`.

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
`countBy(arr: Array[A], predicate: Func[(A) => Boolean]): Number`

Returns a `Number` count of all the elements in `array` that satisfy the given `predicate`, which must accept and `A`.

**Example**
```
xtr.arrays.countBy([1, 2, 3], function(item) item > 2)
```
**Result**
```
1
```

<br/>
## deepFlatten
`deepFlatten(arr: Array[Array[A]]): Array[Any]`

Returns a new single level `Array` with the contents of all `Array` in `arr`, recursively flattening each `Array` element found.

**Example**
```
xtr.arrays.deepFlatten([[1, 2], '3', [4, {}, [5, 6]]])
```
**Result**
```
[1, 2, '3', 4, {}, 5, 6]
```

<br/>
## distinctBy
### distinctBy func(value)
`distinctBy(arr: Array[A], identity: Func[(A) => B]): Array[A]`

Returns a new `Array` with the distinct elements in `arr` using the given `identity` function for comparison. `identity` must accept an `A`.

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
`distinctBy(arr: Array[A], identity: Func[(A, Number) => B]): Array[A]`

Returns a new `Array` with the distinct elements in `arr` using the given `identity` function for comparison. `identity` must accept an `A`.

**Example**
```
xtr.arrays.distinctBy([1, 2, 3, 4, 5, 6], function(item, idx) item % (3 * idx))
```
**Result**
```
[1, 2, 3, 4, 5, 6]
```

The modulo operation on the elements yields `[0, 2, 3, 4, 5, 6]` where all are distinct, so all elements are kept.

<br/>
## drop
`drop(arr: Array[A], n: Number): Array[A]`

Returns a new `Array` with the elements in `arr` but dropping the first `n` elements.

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
`dropWhile(arr: Array[A], predicate: Func[(A) => Boolean]): Array[A]`

Returns a new `Array` with the elements in `arr`, but dropping the first elements while they satisfy the given `predicate`, which must accept an `A`.

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
`duplicatesBy(arr: Array[A]): Array[A]`

Returns a new `Array` with the element in `arr` that are duplicated.

**Example**
```
xtr.arrays.duplicatesBy([1, 2, 3, 1, 2])
```
**Result**
```
[1, 2]
```

<br/>
## find
### find func(value)
`find(arr: Array[A], predicate: Func[(A) => Boolean]): [A]`

Returns a single element `Array` with the first `A` that satisfies the given `predicate`, which must accept an `A`.

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
`find(arr: Array[A], predicate: Func[(A, Number) => Boolean]): [A]`

Returns a single element `Array` with the first `A` that satisfies the given `predicate`, which must accept an `A` and its `Number` index.

**Example**
```
xtr.arrays.find([1, 2, 3, 4, 5], function(item, idx) item * (3 + idx) > 10)
```
**Result**
```
[3]
```

<br/>
## indexWhere
`indexWhere(arr: Array[A], predicate: Func[(A) => Boolean]): Number`

Returns the `Number` index of the first element that satisfies the given `predicate`, otherwise `-1`. `predicate` which must accept an `A`.

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
`indicesWhere(arr: Array[A], predicate: Func[(A) => Boolean]): Array[Number]`

Returns an `Array[Number]` with the indices of elements that satisfy the given `predicate`, which must accept an `A`.

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
`lastIndexWhere(arr: Array[A], predicate: Func[(A) => Boolean]): Number`

Returns the `Number` index of the last element in `arr` that satisfies the given `predicate`, otherwise `-1`. `predicate` which must accept an `A`.

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
`occurrencesBy(arr: Array[A], identity: Func[(A) => String]): Object[Number]`

Returns an `Object` with an entry for each unique identity of elements in `arr`. The value of each entry is the `Number` of elements in `arr` that produced such identity, using `identity`. `identity` must take an `A`.

**Example**
```
xtr.arrays.occurrencesBy([1, 2, 3, 4, 5], function(item) if (item) < 4 then 'under4' else 'over4')
```
**Result**
```
{ 'under4': 3, 'over4': 2 }
```

<br/>
## partition
`partition(arr: Array[A], predicate: Func[(A) => Boolean]): Object[A]`

Returns an `Object` with two entries:

- `pass` key with an `Array[A]` of the subset of elements in `arr` that satisfy the given `predicate`, which must take an `A`.
- `fail` key with an `Array[A]` of the subset of elements in `arr` that fail the given `predicate`, which must take an `A`.

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
`splitAt(arr: Array[A], n: Number): Object[A]`

Returns an `Object[A]` with two entries:

- `left` key with an `Array[A]` containing the elements of `arr` before the `n` element.
- `right` key with an `Array[A]` containing the remaining elements of `arr`.

**Example**
```
xtr.arrays.splitAt([1, 2, 3, 4, 5], 3)
```
**Result**
```
{ left: [1, 2, 3], right: [4, 5] }
```

<br/>
## take
`take(arr: Array[A], n: Number): Array[A]`

Returns a new `Array` with the elements in `arry`, but only taking the first `n` elements.

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
`takeWhile(arr: Array[A], predicate: Func[(A) => Boolean]): Array[A]`

Returns a new `Array` with the elements in `arr`, but only taking the first elements that satisfy the given `predicate`, which must accept an `A`.

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
`unzip(arr: Array[Array[A]]): Array[Array[A]]`

Create n-number of `Arrays`, each containing the n-th element of every array in `arr`.

Returns a new `Array` of equal size to the shortest array in `arr`. Every n-th element in the result is an `Array` containing the n-th element the arrays in `arr`.

**Example**
```
xtr.arrays.unzip([[1, 'x'], [2, 'y'], [3, 'z']])
```
**Result**
```
[[1, 2, 3], ['x', 'y', 'z']]
```

[//]: # (## `unzipAll&#40;arr: Array[Array[A]], fill: B&#41;: Array[Array[A|B]]`)
[//]: # (Create n-number of `Arrays`, each containing the n-th element of every array in `arr`, using a `fill` value for missing n-th elements.)
[//]: # ()
[//]: # (Returns a new `Array` of equal size to the longest array in `arr`. Every n-th element in the result is an `Array` containing the n-th element the arrays in `arr` that have such element or `fill` for short arrays.)
[//]: # ()
[//]: # (**Example**)
[//]: # (```)
[//]: # (xtr.arrays.unzipAll&#40;[[1, 'x'], [2], [3, 'z']], 'NA'&#41;)
[//]: # (```)
[//]: # (**Result**)
[//]: # (```)
[//]: # ([[1, 2, 3], ['x', 'NA', 'z']])
[//]: # (```)

<br/>
## zip
`zip(arr1: Array[A], arr2: Array[B], arrN: Array[C]*): Array[Array[A|B|C]]`

Combines corresponding elements of the given arrays.

Returns a new `Array` of equal size to the shortest array given. Every n-th element in the result is an `Array` containing the n-th element of the given arrays.

**Example**
```
xtr.arrays.zip([1, 2, 3], ['x', 'y', 'z'])
```
**Result**
```
[[1, 'x'], [2, 'y'], [3, 'z']]
```

[//]: # (## `zipAll&#40;arr: Array[Array[A]], fill: B&#41;: Array[Array[A|B]]`)
[//]: # (Combines corresponding elements of the arrays in `arr`, using a `fill` value for short arrays.)
[//]: # ()
[//]: # (Returns a new `Array` of equal size to the longest array in `arr`. Every n-th element in the result is an `Array` containing the n-th element of the arrays in `arr` that have such element or `fill` for short arrays.)
[//]: # ()
[//]: # (**Example**)
[//]: # (```)
[//]: # (xtr.arrays.zipAll&#40;[[1, 2, 3], ['x', 'y']], 'NA'&#41;)
[//]: # (```)
[//]: # (**Result**)
[//]: # (```)
[//]: # ([[1, 'x'], [2, 'y'], [3, 'NA']])
[//]: # (```)
