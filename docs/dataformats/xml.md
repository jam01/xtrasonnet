# eXtensible Markup Language

## Supported MediaTypes 
* `application/xml`
* `application/*+xml`

## xtrasonnet convention
Representing XML as JSON is not an obvious exercise, but a few conventions exist. Different conventions make different choices about what XML characteristics are important and which aren't, for example how they represent attributes or namespaces if they're kept at all.

xtrasonnet's built in XML data format plugin is based on the BadgerFish convention, with some modifications and extensions. There are three conversion modes: `simplified`, `badger` (default), and `extended`.

Consider the following XML document:

```xml
<reports ns="http://www.w3.org/2005/Atom" xmlns:georss="http://www.georss.org/georss">
    <entry>
        <title>A large tree branch is blocking the road</title>
        <link rel="self" href="http://open311.sfgov.org/dev/V1/reports/637619.xml"/>
        <author><name>John Doe</name></author>
        <georss:point>40.7111 -73.9565</georss:point>
        <category label="Damaged tree" term="tree-damage" scheme="https://open311.sfgov.org/dev/V1/categories/006.xml">006</category>
        <content type="xml" ns="http://open311.org/spec/georeport-v1">
            <report_id>637619</report_id>
            <status>created</status>
            <status_notes />
            <policy>The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code</policy>
        </content>
    </entry>
    <entry>
        <title>A large tree branch is blocking the road</title>
        <link rel="self" href="http://open311.sfgov.org/dev/V1/reports/637620.xml"/>
        <author><name>John Doe</name></author>
        <georss:point>40.7111 -73.9565</georss:point>
        <category label="Damaged tree" term="tree-damage" scheme="https://open311.sfgov.org/dev/V1/categories/006.xml">006</category>
        <content type="xml" ns="http://open311.org/spec/georeport-v1">
            <report_id>637620</report_id>
            <status>created</status>
            <status_notes />
            <policy><![CDATA[The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code]]></policy>
        </content>
    </entry>
</reports>
```

### simplified

In the simplified mode the following rules apply:

* Attributes are ignored; equivalent to `exclude=attrs`
* XML namespaces are ignored; equivalent to `exclude=xmlns`
* Elements' name form is their local name; equivalent to `nameform=local`
* Elements with only children elements result in an `Object`, with an entry for each child
* Elements with only text result in a `String`
* Elements with text and children elements result in an `Object`, with an entry for each child while ignoring the text
* Elements with the same name result in an `Array`
* Text fragments are concatenated, those are plain text content and cdata elements; equivalent to `trimtext=true`
* Empty elements result in `Null`

```json
{
  "reports": {
    "entry": [
      {
        "title": "A large tree branch is blocking the road",
        "link": null,
        "author": {
          "name": "John Doe"
        },
        "point": "40.7111 -73.9565",
        "category": "006",
        "content": {
          "report_id": "637619",
          "status": "created",
          "status_notes": null,
          "policy": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code"
        }
      },
      {
        "title": "A large tree branch is blocking the road",
        "link": null,
        "author": {
          "name": "John Doe"
        },
        "point": "40.7111 -73.9565",
        "category": "006",
        "content": {
          "report_id": "637620",
          "status": "created",
          "status_notes": null,
          "policy": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code"
        }
      }
    ]
  }
}
```

### badger

In the badger mode the following rules apply:

* Every element results in an `Object`
* Elements with the same name result in an `Array`
* Attributes are included in an `_attr` entry
* XML namespaces are included in an `_xmlns` entry, with the default namespace under a `_def` key 
* Text content is included in a `_text` entry
* Text fragments are concatenated, those are plain text content and cdata elements; equivalent to `trimtext=true`
* Empty elements result in an empty `Object`

```json
{
  "reports": {
    "_xmlns": {
      "georss": "http://www.georss.org/georss"
    },
    "_attr": {
      "ns": "http://www.w3.org/2005/Atom"
    },
    "entry": [
      {
        "title": {
          "_text": "A large tree branch is blocking the road"
        },
        "link": {
          "_attr": {
            "rel": "self",
            "href": "http://open311.sfgov.org/dev/V1/reports/637619.xml"
          }
        },
        "author": {
          "name": {
            "_text": "John Doe"
          }
        },
        "georss:point": {
          "_text": "40.7111 -73.9565"
        },
        "category": {
          "_attr": {
            "label": "Damaged tree",
            "term": "tree-damage",
            "scheme": "https://open311.sfgov.org/dev/V1/categories/006.xml"
          },
          "_text": "006"
        },
        "content": {
          "_attr": {
            "type": "xml",
            "ns": "http://open311.org/spec/georeport-v1"
          },
          "report_id": {
            "_text": "637619"
          },
          "status": {
            "_text": "created"
          },
          "status_notes": {},
          "policy": {
            "_text": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code"
          }
        }
      },
      {
        "title": {
          "_text": "A large tree branch is blocking the road"
        },
        "link": {
          "_attr": {
            "rel": "self",
            "href": "http://open311.sfgov.org/dev/V1/reports/637620.xml"
          }
        },
        "author": {
          "name": {
            "_text": "John Doe"
          }
        },
        "georss:point": {
          "_text": "40.7111 -73.9565"
        },
        "category": {
          "_attr": {
            "label": "Damaged tree",
            "term": "tree-damage",
            "scheme": "https://open311.sfgov.org/dev/V1/categories/006.xml"
          },
          "_text": "006"
        },
        "content": {
          "_attr": {
            "type": "xml",
            "ns": "http://open311.org/spec/georeport-v1"
          },
          "report_id": {
            "_text": "637620"
          },
          "status": {
            "_text": "created"
          },
          "status_notes": {},
          "policy": {
            "_text": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code"
          }
        }
      }
    ]
  }
}
```

### extended

In the extended mode the following rules apply:

* Every element results in an `Object`
* Elements with the same name result in an `Array`
* Attributes are included in an `_attr` entry
* XML namespaces are included in an `_xmlns` entry, with the default namespace under a `_def` key
* Plain text content is included in a `_text` entry
* CData text is included in a `_cdata` entry
* An element's position within its parent is included in an `_pos` entry, or as a suffix on its name if it's a plain or cdata text

!!! hint
    Extended mode is recommended in scenarios where mixed content is expected in an element, that is multiple text fragments and child elements. Or if reading and writing XML and it's desirable to keep the structure as identical as possible, since extended mode will keep elements order even if there are arrays, and keep individual text and cdata elements separate. 

```json
{
  "reports": {
    "_xmlns": {
      "georss": "http://www.georss.org/georss"
    },
    "_attr": {
      "ns": "http://www.w3.org/2005/Atom"
    },
    "entry": [
      {
        "title": {
          "_text1": "A large tree branch is blocking the road",
          "_pos": 1
        },
        "link": {
          "_attr": {
            "rel": "self",
            "href": "http://open311.sfgov.org/dev/V1/reports/637619.xml"
          },
          "_pos": 2
        },
        "author": {
          "name": {
            "_text1": "John Doe",
            "_pos": 1
          },
          "_pos": 3
        },
        "georss:point": {
          "_text1": "40.7111 -73.9565",
          "_pos": 4
        },
        "category": {
          "_attr": {
            "label": "Damaged tree",
            "term": "tree-damage",
            "scheme": "https://open311.sfgov.org/dev/V1/categories/006.xml"
          },
          "_text1": "006",
          "_pos": 5
        },
        "content": {
          "_attr": {
            "type": "xml",
            "ns": "http://open311.org/spec/georeport-v1"
          },
          "report_id": {
            "_text1": "637619",
            "_pos": 1
          },
          "status": {
            "_text1": "created",
            "_pos": 2
          },
          "status_notes": {
            "_pos": 3
          },
          "policy": {
            "_text1": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code",
            "_pos": 4
          },
          "_pos": 6
        },
        "_pos": 1
      },
      {
        "title": {
          "_text1": "A large tree branch is blocking the road",
          "_pos": 1
        },
        "link": {
          "_attr": {
            "rel": "self",
            "href": "http://open311.sfgov.org/dev/V1/reports/637620.xml"
          },
          "_pos": 2
        },
        "author": {
          "name": {
            "_text1": "John Doe",
            "_pos": 1
          },
          "_pos": 3
        },
        "georss:point": {
          "_text1": "40.7111 -73.9565",
          "_pos": 4
        },
        "category": {
          "_attr": {
            "label": "Damaged tree",
            "term": "tree-damage",
            "scheme": "https://open311.sfgov.org/dev/V1/categories/006.xml"
          },
          "_text1": "006",
          "_pos": 5
        },
        "content": {
          "_attr": {
            "type": "xml",
            "ns": "http://open311.org/spec/georeport-v1"
          },
          "report_id": {
            "_text1": "637620",
            "_pos": 1
          },
          "status": {
            "_text1": "created",
            "_pos": 2
          },
          "status_notes": {
            "_pos": 3
          },
          "policy": {
            "_cdata1": "The City will inspect and require the responsible party to correct within 24 hours and/or issue a Correction Notice or Notice of Violation of the Public Works Code",
            "_pos": 4
          },
          "_pos": 6
        },
        "_pos": 2
      }
    ],
    "_pos": 1
  }
}
```

## Reader parameters
### `mode`
The xtrasonnet XML to JSON convention to use when reading XML.

Allowed values are `simplified`, `badger` (default), `extended`.

### `textkey`
The key to use for text elements.

Default is `_text`.

### `attrkey`
The key to use for attribute elements.

Default is `_attr`.

### `cdatakey`
The key to use for CData elements in `extended` mode.

Default is `_cdata`.

### `poskey`
The key to use for element positions in `extended` mode.

Default is `_pos`.

### `xmlnsaware`
Whether to validate and process XML Namespaces as defined by the 1.1 XML specification

Allowed values are `true` (default) and `false`.

### `xmlnskey`
The key to use for XML namespace elements.

Default is `_xmlns`

### `xmlns.*`
XML namespace declarations to reference fully qualified elements independent of the prefix present.

Declarations are of the form `xmlns.<prefix>=<URI>` e.g.: `xmlns.soap="http://www.w3.org/2003/05/soap-envelope/"`.

Any elements found matching the given URI will be implicitly transformed to use the given prefix to facilitate referencing elements that may use arbitrary prefixes.

!!! note
    If a namespace declaration prefix is being used by a different URI, elements of the non-matching URI will have their prefix "bumped" by adding a `_n` where `n` is a number that makes the resulting prefix an unique value.

### `nameform`
The desired form of element names.

Allowed values are `qname` (qualified name) and `local` (local name, minus domain qualification).

!!! Warning
    If `nameform=local` and `xmlnsaware=false` then element names will be a best-effort transformation where the value is the content of the name after the first colon symbol `':'`, instead of proper XML namespace processing. 

### `trimtext`
Whether to trim text elements.

Allowed values are `true` (default) and `false`.

### `exclude`
Data components to exclude when reading XML.

Allowed values are:

* `attrs` XML attribute elements
* `xmlns` XML namespace data

## Writer parameters
### `textkey`
The key to expect for text elements.

Default is `_text`.

### `attrkey`
The key to expect for attribute elements.

Default is `_attr`.

### `cdatakey`
The key to expect for CData elements.

Default is `_cdata`.

### `poskey`
The key to expect for element positions.

Default is `_poskey`.

### `xmlnskey`
The key to expect for XML namespace elements.

Default is `_xmlns`

### `xmlversion`
The xml version to use on the XML document declaration

Default is `1.0`.

### `emptytags`
Whether to output XML [empty element tags](https://www.w3.org/TR/xml/#sec-starttags) e.g.: `<element/>` for given elements. The selected elements should be separated by whitespace

Allowed values are `null`, `string`, and `object`.

### `exclude`
Data components to exclude when writing XML.

Allowed values are:

* `xml-declaration` an XML document declaration, also see [`xmlversion`](#xmlversion)
* `attrs` XML attribute elements
