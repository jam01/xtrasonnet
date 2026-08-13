# xtr.objects

## all
`all(value: Object[A], func: Func[(A, String) => Boolean]): Boolean`

Returns `true` if all entries in `value` satisfy the given `func`, otherwise `false`. `func` must accept an `A` and its corresponding `String` key.

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
`any(value: Object[A], func: Func[(A, String) => Boolean]): Boolean`

Returns `true` if any entry in `value` satisfies the given `func`, otherwise `false`. `func` must accept an `A` and its corresponding `String` key.

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
`distinctBy(container: Object[A], func: Func[(A) => B]): Object[A]`

Returns a new `Object` with the distinct entries in `container` using the given `func` function for comparison. `func` must accept an `A`.

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
`distinctBy(container: Object[A], func: Func[(A, String) => B]): Object[A]`

Returns a new `Object` with the distinct entries in `container` using the given `func` function for comparison. `func` must accept an `A` and its corresponding `String` key.

**Example**
```
local languages = {
    first: { name: 'scala', version: '3.1.3', isJvm: true },
    second: { name: 'java', version: '19', isJvm: true },
    third: { name: 'java', version: '18', isJvm: true }
};

xtr.objects.distinctBy(languages, function(lang, key)
    if (lang.name == 'java') then lang.version
    else key
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
### fromArray
`fromArray(arr: Array[A], keyF: Func[(A) => String]): Object[A]`

Returns a new `Object` with an entry for every element in `arr`, whose key is the result of applying the given `keyF` to the element, and whose value is the element itself. `keyF` must return a `String`; any other type is an error. `keyF` may instead accept two parameters, the element and its `Number` index. Elements that produce the same key keep only the last one.

**Example**
```
local languages = [
    { name: 'scala', version: '3.1.3' },
    { name: 'java', version: '19' }
];

xtr.objects.fromArray(languages, function(lang) lang.name)
```
**Result**
```
{
    scala: { name: 'scala', version: '3.1.3' },
    java: { name: 'java', version: '19' }
}
```

<br/>
### fromArray with valueF
`fromArray(arr: Array[A], keyF: Func[(A) => String], valueF: Func[(A) => B]): Object[B]`

Returns a new `Object` with an entry for every element in `arr`, whose key is the result of applying the given `keyF` to the element, and whose value is the result of applying the given `valueF` to it.

**Example**
```
local languages = [
    { name: 'scala', version: '3.1.3' },
    { name: 'java', version: '19' }
];

xtr.objects.fromArray(languages, function(lang) lang.name, function(lang) lang.version)
```
**Result**
```
{
    scala: '3.1.3',
    java: '19'
}
```

<br/>
## fullEqJoin
### fullEqJoin
`fullEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL` or in `arrR`, joining those that exist in both with a shallow merge, and using the given `funcIdL` and `funcIdR` identity functions to compute equality. On key collision the merged entry keeps the `arrL` object's value. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order, with unmatched `arrR` objects appended last in their order.

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
`fullEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null], funcJoin: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL` or in `arrR`, joining those that exist in both with the given `funcJoin` function, and using the given `funcIdL` and `funcIdR` identity functions to compute equality. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order, with unmatched `arrR` objects appended last in their order.

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
`innerEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in both `arrL` _and_ `arrR`, using the given `funcIdL` and `funcIdR` identity functions to compute equality, and joined using a shallow merge. On key collision the merged entry keeps the `arrL` object's value. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order.

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
`innerEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null], funcJoin: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in both `arrL` _and_ `arrR`, using the given `funcIdL` and `funcIdR` identity functions to compute equality, and joined using the given `funcJoin` function. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order.

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
### leftEqJoin
`leftEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL`, joined using a shallow merge with those that also exist in `arrR`, using the given `funcIdL` and `funcIdR` identity functions to compute equality. On key collision the merged entry keeps the `arrL` object's value. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order.

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
`leftEqJoin(arrL: Array[Object[A]], arrR: Array[Object[B]], funcIdL: Func[(Object[A]) => String|Number|Boolean|Null], funcIdR: Func[(Object[B]) => String|Number|Boolean|Null], funcJoin: Func[(Object[A], Object[B]) => Object[C]]): Array[Object[C]]`

Returns a new `Array` with all the objects that exist in `arrL`, joined using the given `funcJoin` function with those that also exist in `arrR`, using the given `funcIdL` and `funcIdR` identity functions to compute equality. Rows follow `arrL`'s order, each expanded to its `arrR` matches in their order.

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
