# xtr.math

## abs
`abs(num: Number): Number`

Returns the absolute value of the given `num`.

**Example**
```
xtr.math.abs(-1)
```
**Result**
```
1
```

<br/>
## acos
`acos(num: Number): Number`

Returns the arc cosine of the given `num`.

**Example**
```
xtr.math.acos(1)
```
**Result**
```
0
```

<br/>
## asin
`asin(num: Number): Number`

Returns the arc sine of the given `num`.

**Example**
```
xtr.math.asin(1)
```
**Result**
```
1.5707963267948966
```

<br/>
## atan
`atan(num: Number): Number`

Returns the arc tangent of the given `num`.

**Example**
```
xtr.math.atan(1)
```
**Result**
```
0.7853981633974483
```

<br/>
## avg
`avg(arr: Array[Number]): Number`

Returns the average of the numbers in `arr`.

**Example**
```
xtr.math.avg([1,2,3])
```
**Result**
```
2
```

<br/>
## ceil
`ceil(num: Number): Number`

Returns the ceiling (aka round up) of the given `num`.

**Example**
```
xtr.math.ceil(1.01)
```
**Result**
```
2
```

<br/>
## clamp
`clamp(num: Number, min: Number, max: Number): Number`

Returns `num` if it exists between `min` and `max`, otherwise the one that is closest.

**Example**
```
xtr.math.clamp(100, 0, 10)
```
**Result**
```
10
```

<br/>
## cos
`cos(num: Number): Number`

Returns the cosine of the given`num`.

**Example**
```
xtr.math.cos(0)
```
**Result**
```
1
```

<br/>
## exp
`exp(num: Number): Number`

Returns Euler's number e, to the power of `num`.

**Example**
```
xtr.math.exp(2)
```
**Result**
```
7.38905609893065
```

<br/>
## exponent
`exponent(num: Number): Number`

Returns the exponent portion of the double-precision binary floating-point representation (IEEE 754:binary64) of the given `num`.

**Example**
```
xtr.math.exponent(2)
```
**Result**
```
2
```

<br/>
## floor
`floor(num: Number): Number`

Returns the floor (aka round down) of the given `num`.

**Example**
```
xtr.math.floor(4.99)
```
**Result**
```
4
```

<br/>
## log
`log(num: Number): Number`

Returns the natural logarithm of the given `num`.

**Example**
```
xtr.math.log(2)
```
**Result**
```
0.6931471805599453
```

<br/>
## mantissa
`mantissa(num: Number): Number`

Returns the fraction portion (aka significand) of the double-precision binary floating-point representation (IEEE 754:binary64) of the given `num`.

**Example**
```
xtr.math.mantissa(2)
```
**Result**
```
0.5
```

<br/>
## pow
`pow(num: Number1, num: Number2)`

Returns the value of `num1` raised to the power of `num2`.

**Example**
```
xtr.math.pow(2, 2)
```
**Result**
```
4
```

<br/>
## random
`random`

Returns a pseudo-random double-precision floating-point number between `0` and `1`.

**Example**
```
xtr.math.random
```
**Result**
```
0.5963038027787421
```

<br/>
## randomInt
`randomInt(num: Number): Number`

Returns a pseudo-random integer between 0 (inclusive) and the given`num` (exclusive).

**Example**
```
xtr.math.randomInt(500)
```
**Result**
```
485
```

<br/>
## round
`round(num: Number, mode: String = 'half-up', precision: Number = '0'): Number`

Rounds the given `num` using the `mode` and `precision` requested. Supported modes are `up`, `down`, `half-up`, `half-down`, `ceiling`, `floor`, and `half-even`.

**Example**
```
xtr.math.round(2.5)
```
**Result**
```
3
```

<br/>
## sin
`sin(num: Number): Number`

Returns the sine of the given `num`.

**Example**
```
xtr.math.sin(1)
```
**Result**
```
0.8414709848078965
```

<br/>
## sqrt
`sqrt(num: Number): Number`

Returns the square root of the given `num`.

**Example**
```
xtr.math.sqrt(4)
```
**Result**
```
2
```

<br/>
## sum
`sum(arr: Array[Number])`

Returns the sum of all numbers in `arr`.

**Example**
```
xtr.math.sum([10, 20, 30])
```
**Result**
```
60
```

<br/>
## tan
`tan(num: Number): Number`

Returns the tangent of the given `num`.

**Example**
```
xtr.math.tan(1)
```
**Result**
```
1.5574077246549023
```
