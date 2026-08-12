# xtr.duration

The duration module leverages the [ISO-8601 duration](https://en.wikipedia.org/wiki/ISO_8601#Durations) format.

## of
`of(parts: Object[Number]): String`

Returns the ISO-8601 duration of the given `parts`, an `Object` of the form:

```
{
    years: Number, months: Number, days: Number,
    hours: Number, minutes: Number, seconds: Number
}
```

where all elements are optional and default to zero; keys outside this form are an error. The
result is in canonical form: zero-valued parts are omitted, e.g. `{hours: 2}` yields `'PT2H'`,
and all-zero parts yield `'PT0S'`.

**Example**
```
local parts = {
    years: 20, months: 3, days: 1,
    hours: 12, minutes: 30, seconds: 45
};

xtr.duration.of(parts)
```
**Result**
```
'P20Y3M1DT12H30M45S'
```

<br/>
## toParts
`toParts(duration: String): Object[Number]`

Returns the constituent parts of the given `duration`, as an `Object` of the form:

```
{
    years: Number, months: Number, days: Number,
    hours: Number, minutes: Number, seconds: Number
}
```

All six parts are always present. Time-only durations like `'PT4H'` and negative durations like
`'-P1DT2H'` are supported; a leading minus sign negates every part.

**Example**
```
xtr.duration.toParts('P20Y3M1DT12H30M45S')
```
**Result**
```
{
    years: 20, months: 3, days: 1,
    hours: 12, minutes: 30, seconds: 45
}
```
