# xtr.datetime

The datetime module leverages the [ISO-8601 offset date-time](https://en.wikipedia.org/wiki/ISO_8601#Time_offsets_from_UTC) format for representing and manipulating date and time, and the [ISO-8601 duration](https://en.wikipedia.org/wiki/ISO_8601#Durations) format for temporal amounts. The ISO format is the recommendation of the IETF through [RFC 3339](https://www.rfc-editor.org/rfc/rfc3339): _Date and Time on the Internet_.

!!! hint
    Users that need to operate on other date and time formats can use the [`parse`](#parse) function to convert it to the supported format, operate on the result, and optionally output it to any other format with the [`format`](#format) function.

## atBeginningOfDay
`atBeginningOfDay(datetime: String): String`

Returns the given `datetime` at midnight.

**Example**
```
xtr.datetime.atBeginningOfDay('2020-12-31T23:19:35Z')
```
**Result**
```
'2020-12-31T00:00:00Z'
```

<br/>
## atBeginningOfHour
`atBeginningOfHour(datetime: String): String`

Returns the given `datetime` with the minutes and seconds set to zero.

**Example**
```
xtr.datetime.atBeginningOfHour('2020-12-31T23:19:35Z')
```
**Result**
```
'2020-12-31T23:00:00Z'
```

<br/>
## atBeginningOfMonth
`atBeginningOfMonth(datetime: String): String`

Returns the given `datetime` with the day set to first of the month, and the time set to midnight.

**Example**
```
xtr.datetime.atBeginningOfMonth('2020-12-31T23:19:35Z')
```
**Result**
```
'2020-12-01T00:00:00Z'
```

<br/>
## atBeginningOfWeek
`atBeginningOfWeek(datetime: String): String`

Returns the given `datetime` with the day set to first of the current week, and the time set to midnight.

**Example**
```
xtr.datetime.atBeginningOfWeek('2020-12-31T23:19:35Z')
```
**Result**
```
'2020-12-27T00:00:00Z'
```

<br/>
## atBeginningOfYear
`atBeginningOfYear(datetime: String): String`

Returns the given `datetime` with the day/month set to January 1st, and the time set to midnight.

**Example**
```
xtr.datetime.atBeginningOfYear('2020-12-31T23:19:35Z')
```
**Result**
```
'2020-01-01T00:00:00Z'
```

<br/>
## inOffset
`inOffset(datetime: String, offset: String): String`

Returns the given `datetime` in the given timezone `offset`, changing the date and time as appropriate.

**Example**
```
xtr.datetime.inOffset('2020-12-31T23:19:35Z', '-08:00')
```
**Result**
```
'2020-12-31T15:19:35-08:00'
```

<br/>
## compare
`compare(datetime1: String, datetime2: String): String`

Returns:

`1` if `datetime1` is after `datetime2`

`-1` if `datetime1` is before `datetime2`

`0` if `datetime1` and `datetime2` are the same

**Example**
```
xtr.datetime.compare('2020-12-31T23:19:35Z','2020-01-01T00:00:00Z')
```
**Result**
```
1
```

<br/>
## of
`of(parts: Object[Number|String]): String`

Returns the `String` representation of the date and time given in `parts`, an `Object` of the form:

```
{
    year: Number, month: Number, day: Number,
    hour: Number, minute: Number, second: Number,
    timezone: String
}
```

where all elements are optional.

**Example**
```
local parts = {
    'year': 2021,
    'timezone': '-08:00'
};

xtr.datetime.of(parts)
```
**Result**
```
'2021-01-01T00:00:00-08:00'
```

<br/>
## between
`between(datetime1: String, datetime2: String): String`

Returns the ISO-8601 duration between `datetime1` and `datetime2`.

**Example**
```
local date1 = '2019-09-20T18:53:41.425Z';
local date2 = '2019-09-14T18:53:41.425Z';

xtr.datetime.between(date1, date2)
```
**Result**
```
'-P6D'
```

<br/>
## format
`format(datetime: String, format: String): String`

Returns the given `datetime` formatted in the requested `format`.

**Example**
```
xtr.datetime.format('2019-09-20T18:53:41.425Z', 'yyyy/MM/dd')
```
**Result**
```
'2019/09/20'
```

<br/>
## isLeapYear
`isLeapYear(datetime: String): String`

Returns a `true` if `datetime` is in a leap year, otherwise `false`.

**Example**
```
xtr.datetime.isLeapYear('2019-09-14T18:53:41.425Z')
```
**Result**
```
false
```

<br/>
## minus
`minus(datetime: String, duration: String): String`

Returns the result of subtracting the specified ISO-8601 `duration` from the given `datetime`.

**Example**
```
xtr.datetime.minus('2019-09-20T18:53:41Z', 'P2D')
```
**Result**
```
'2019-09-18T18:53:41Z'
```

<br/>
## now
`now(): String`

Returns the current datetime.

**Example**
```
xtr.datetime.now()
```
**Result**
```
'2021-01-05T13:09:45.476375-05:00'
```

<br/>
## parse
`parse(datetime: String|Number, format: String): String`

Returns an ISO-8601 extended offset date-time from the given `datetime` using the specified `format`.

**Example**
```
xtr.datetime.parse('12/31/1990 10:10:10', 'MM/dd/yyyy HH:mm:ss')
```
**Result**
```
'1990-12-31T10:10:10Z'
```

Additionally, developers can parse Unix timestamps by passing `'unix'` as the `format`.

<br/>
## plus
`plus(datetime: String, duration: String): String`

Returns the result of adding the specified ISO-8601 `duration` to the given `datetime`.

**Example**
```
xtr.datetime.plus('2019-09-18T18:53:41Z', 'P2D')
```
**Result**
```
'2019-09-20T18:53:41Z'
```

<br/>
## toLocalDate
`toLocalDate(datetime: String): String`

Returns the given `datetime` without time or offset.

**Example**
```
xtr.datetime.toLocalDate('2019-07-04T18:53:41Z')
```
**Result**
```
'2019-07-04'
```

<br/>
## toLocalDateTime
`toLocalDateTime(datetime: String): String`

Returns the given `datetime` without an offset.

**Example**
```
xtr.datetime.toLocalDateTime('2019-07-04T21:00:00Z')
```
**Result**
```
'2019-07-04T21:00:00'
```

<br/>
## toLocalTime
`toLocalTime(datetime: String, format: String): String`

Returns the given `datetime` without date or offset.

**Example**
```
xtr.datetime.toLocalTime('2019-07-04T21:00:00Z')
```
**Result**
```
'21:00:00'
```

<br/>
## toParts
`toParts(datetime: String): Object[Number|String]`

Returns the constituent parts of the given `datetime`, as an `Object` of the form:

```
{
    year: Number, month: Number, day: Number,
    hour: Number, minute: Number, second: Number, nanosecond: Number,
    offset: String
}
```

**Example**
```
xtr.datetime.toParts('2019-07-04T21:00:00Z')
```
**Result**
```
{
    year: 2019, month: 7, day: 4,
    hour: 21, minute: 0, second: 0, nanosecond: 0,
    offset: 'Z'
}
```

<br/>
## today
`today(): String`

Returns the current day at midnight.

**Example**
```
xtr.datetime.today
```

**Result**
```
'2021-01-05T00:00:00-05:00'
```

<br/>
## tomorrow
`tomorrow(): String`

Returns the next day at midnight.

**Example**
```
xtr.datetime.tomorrow
```

**Result**
```
'2021-01-06T00:00:00-05:00'
```

<br/>
## yesterday
`yesterday(): String`

Returns the previous day at midnight.

**Example**
```
xtr.datetime.yesterday
```

**Result**
```
'2021-01-04T00:00:00-05:00'
```
