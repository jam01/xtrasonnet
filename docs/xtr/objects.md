# xtr.objects

## all
`all(obj, Object[A], predicate: Func[(A, String) => Boolean]): Boolean`

Returns `true` if all entries in `obj` satisfy the given `predicate`, otherwise `false`. `predicate` must accept an `A` and its corresponding `String` key.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
};

xtr.objects.all(languages, function(lang, name) lang.isJvm)
```
**Result**
```
false
```

<br/>
## any
`any(obj, Object[A], predicate: Func[(A, String) => Boolean]): Boolean`

Returns `true` if any entry in `obj` satisfies the given `predicate`, otherwise `false`. `predicate` must accept an `A` and its corresponding `String` key.

**Example**
```
local languages = {
    scala: { version: '3.1.3', isJvm: true },
    java: { version: '19', isJvm: true },
    python: { version: '3.10.4', isJvm: false }
};

xtr.objects.any(languages, function(lang, name) lang.isJvm)
```
**Result**
```
true
```

<br/>
## distinctBy
### distinctBy func(value)
`distinctBy(obj: Object[A], Func[(A) => B]): Object[A]`

Returns a new `Object` with the distinct entries in `obj` using the given `identity` function for comparison. `identity` must accept an `A`.

**Example**
```
local languages = {
    first: { name: 'scala', version: '3.1.3', isJvm: true },
    second: { name: 'java', version: '19', isJvm: true },
    third: { name: 'java', version: '18', isJvm: true }
};

xtr.objects.distinctBy(languages, function(lang) lang.name)
```
**Result**
```
{
    first: { name: 'scala', version: '3.1.3', isJvm: true },
    second: { name: 'java', version: '19', isJvm: true }
}
```

<br/>
### distinctBy func(value, key)
`distinctBy(obj: Object[A], Func[(A, String) => B]): Object[A]`

Returns a new `Object` with the distinct entries in `obj` using the given `identity` function for comparison. `identity` must accept an `A` and its corresponding `String` key.

**Example**
```
local languages = {
    first: { name: 'scala', version: '3.1.3', isJvm: true },
    second: { name: 'java', version: '19', isJvm: true },
    third: { name: 'java', version: '18', isJvm: true }
};

xtr.objects.distinctBy(languages, function(lang, ordinal)
    if (lang.name == 'java') then lang.version
    else ordinal
)
```
**Result**
```
{
    first: { name: 'scala', version: '3.1.3', isJvm: true },
    second: { name: 'java', version: '19', isJvm: true },
    third: { name: 'java', version: '18', isJvm: true }
}
```

<br/>
## fromArray
### fromArray func(value)
`fromArray(arr: Array[A], Func[(A) => Object[B]): Object[B]`

Returns a new `Object[B]` containing the entry of every `Object[B]` obtained by applying the given `function` to all elements in `arr`. `function` must accept an `A` value.

<br/>
### fromArray func(value, idx)
`fromArray(arr: Array[A], Func[(A, Number) => Object[B]]: Object[B]`

Returns a new `Object[B]` containing the entry of every `Object[B]` obtained by applying the given `function` to all elements in `arr`. `function` must accept an `A` value and its `Number` index.

<br/>
## fullEqJoin
### fullEqJoin
`fullEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL` or in `arrR`, joining those that exist in both with a shallow merge, and using the given `identity` functions to compute equality.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.fullEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId)
```
**Result**
```
[
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10308, customerId: 2, date: '2022-07-30' },
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10309, customerId: 2, date: '2022-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
        orderId: 10310, customerId: 77, date: '2022-07-03' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
]
```

<br/>
### fullEqJoin func(left, right) => joined
`fullEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean)score], join: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL` or in `arrR`, joining those that exist in both with the given `join` function, and using the given `identity` functions to compute equality.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.fullEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId,
    function(cust, order) { id: cust?.id, oId: order?.orderId })
```
**Result**
```
[
    { id: 2, oId: 10308 },
    { id: 2, oId: 10309 },
    { id: 77, oId: 10310 },
    { id: 17, oId: null },
    { id: null, oId: 10311 }
]
```

<br/>
## innerEqJoin
### innerEqJoin
`innerEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean)]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in both `arrL` _and_ `arrR`, using the given `identity` functions to compute equality, and joined using a shallow merge.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.innerEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId)
```
**Result**
```
[
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10308, customerId: 2, date: '2022-07-30' },
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10309, customerId: 2, date: '2022-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
        orderId: 10310, customerId: 77, date: '2022-07-03' }
]
```

<br/>
### innerEqJoin func(left, right) => joined
`innerEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean), join: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in both `arrL` _and_ `arrR`, using the given `identity` functions to compute equality, and joined using the given `join` function.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.innerEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId,
    function(cust, order) { id: cust.id, oId: order.orderId })
```
**Result**
```
[
    { id: 2, oId: 10308 },
    { id: 2, oId: 10309 },
    { id: 77, oId: 10310 }
]
```

<br/>
## leftEqJoin
### leftEqJoin func(left, right) => joined
`leftEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean)]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL`, joined using a shallow merge with those that also exist in `arrR`, using the given `identity` functions to compute equality.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.leftEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId)
```
**Result**
```
[
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10308, customerId: 2, date: '2022-07-30' },
    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
        orderId: 10309, customerId: 2, date: '2022-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
        orderId: 10310, customerId: 77, date: '2022-07-03' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
]
```

<br/>
### leftEqJoin func(left, right) => joined
`leftEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], identity: Func[(Object[A]) => String|Number|Boolean], identityR: Func[(Object[B]) => String|Number|Boolean), join: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL`, joined using the given `join` function with those that also exist in `arrR`, using the given `identity` functions to compute equality.

**Example**
```
local customers = [
    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
];

local orders = [
    { orderId: 10308, customerId: 2, date: '2022-07-30' },
    { orderId: 10309, customerId: 2, date: '2022-07-30' },
    { orderId: 10310, customerId: 77, date: '2022-07-03' },
    { orderId: 10311, customerId: 93, date: '2021-05-03' }
];

xtr.objects.leftEqJoin(customers, orders,
    function(cust) cust.id, function(order) order.customerId,
    function(cust, order) { id: cust.id, oId: order?.orderId })
```
**Result**
```
[
    { id: 2, oId: 10308 },
    { id: 2, oId: 10309 },
    { id: 77, oId: 10310 },
    { id: 17, oId: null }
]
```
