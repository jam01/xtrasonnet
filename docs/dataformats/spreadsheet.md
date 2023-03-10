# Office OpenXML Spreadsheet

Standardized format for representing spreadsheets, developed by Microsoft.

## Supported MediaTypes
* `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
* `application/vnd.ms-excel`

!!! Warning
    At this time only reading Spreadsheet files is supported.

!!! Warning
    This format is not supported in the xtrasonnet Playground

## Jsonnet representation

A spreadsheet such as 

|     | A   | B   | C   |
|-----|-----|-----|-----|
| 1   | a1  | b1  | c1  |
| 2   | a2  | b2  | c2  |

is converted into the following jsonnet 

``` json
{
    "Sheet1": [
        {"A":"a1","B":"b1","C":"c1"},
        {"A":"a2","B":"b2","C":"c2"}
    ]
}
```

where `Sheet1` is the name of the spreadsheet sheet.
