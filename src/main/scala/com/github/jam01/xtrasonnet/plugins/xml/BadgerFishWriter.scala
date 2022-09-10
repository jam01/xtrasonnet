package com.github.jam01.xtrasonnet.plugins.xml

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-
 * Adopted:
 * - 12270ae72d5432b916359b3c1747936ffaf26be1: Merge pull request #73 from datasonnet/ordering-keys
 *
 * Changed:
 * - 1570c045ab8e750305e1d86206f4cddeadabfedd: conformed badgerfish ordering behavior
 */

import com.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin.{DEFAULT_NS_KEY, EffectiveParams}
import ujson.{Arr, Bool, Null, Num, Obj, Str, Value}

import java.io.{StringWriter, Writer}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// See {@link scala.xml.Utility.serialize}
class BadgerFishWriter(val params: EffectiveParams) {

  // TODO: write docs and notice and coverage
  // taken from scala.xml.Utility
  object Escapes {
    /**
     * For reasons unclear escape and unescape are a long ways from
     * being logical inverses.
     */
    val pairs = Map(
      "lt" -> '<',
      "gt" -> '>',
      "amp" -> '&',
      "quot" -> '"',
      "apos" -> '\''
    )
    val escMap = (pairs - "apos") map { case (s, c) => c -> ("&%s;" format s) }
    val unescMap = pairs
  }

  import Escapes.escMap

  def serialize(name: String, value: ujson.Value, sb: Writer = new StringWriter()): Writer = {
    sb.append('<')
    val qname = name
    sb.append(qname)

    value match {
      case Null =>
        if (params.emptyTagsNull) sb.append("/>") else sb.append('>').append("</").append(qname).append('>')
      case Str(str) =>
        if (str.isEmpty && params.emptyTagsStr) sb.append("/>") else sb.append('>'); escapeText(str, sb); sb.append("</").append(qname).append('>')
      case Num(num) => sb.append('>').append(num.toString).append("</").append(qname).append('>')
      case bool: Bool => sb.append('>').append(bool.toString).append("</").append(qname).append('>')
      case Arr(arr) => sb.append('>'); arr.foreach(arrItm => serialize(qname, arrItm, sb)); sb.append("</").append(qname).append('>')
      case Obj(obj) =>
        val children = new mutable.LinkedHashMap[String, Value]()

        // append attributes in the order they appear
        obj.foreach({ case (childKey, childValue) =>
          if (childKey.equals(params.xmlnsKey)) {
            childValue.obj.foreach {
              case (key, value) =>
                sb append " xmlns%s=\"%s\"".format(
                  if (key != null && !key.equals(DEFAULT_NS_KEY)) ":" + key else "",
                  if (value.str != null) {
                    val sb2 = new StringWriter()
                    escape(value.str, sb2).toString
                  } else ""
                )
            }
          } else if (childKey.equals(params.attrKey)) {
            childValue.obj.foreach({ case (attrKey, attrVal) =>
              sb.append(' ')
              sb.append(attrKey)
              sb.append('=')
              val sb2 = new StringWriter()
              escape(attrVal.str, sb2)
              appendQuoted(sb2.toString, sb)
            })
          } else {
            children.addOne((childKey, childValue))
          }
        })

        if ((children.isEmpty && params.emptyTagsObj) || (params.emptyTagsStr && children.size == 1 && {
          val head = children.head
          head._1.startsWith(params.textKey) && head._2.str.isEmpty
        })) { // if empty tags set, and empty object or string
          sb append "/>"
        } else {
          // children, so use long form: <xyz ...>...</xyz>
          sb.append('>')

          children.toSeq
            // selectively flatten arrays
            .foldLeft(new ArrayBuffer[(String, Value)])((acc, value) =>
              if (!value._1.equals(params.orderKey)) {
                value._2 match {
                  case ujson.Arr(arr) => acc.addAll(arr.map(it => value._1 -> it))
                  case _ => acc.addOne(value)
                }
              } else acc)
            // sort using ordering key in objects, and index substring in text and cdatas
            .sortBy(entry => entry._2 match { // TODO: only do this if in extended mode
              case ujson.Obj(inner) =>
                val order = inner.get(params.orderKey)
                if (order.isEmpty) Integer.MAX_VALUE else order.get match {
                  case Str(value) => value.toInt
                  case Num(value) => value.toInt
                  case x => throw new IllegalArgumentException("Invalid ordering key value: " + x.value)
                }
              case _ =>
                if (entry._1.last.isDigit) {
                  if (entry._1.startsWith(params.textKey))
                    entry._1.substring(params.textKey.length).toInt
                  else if (entry._1.startsWith(params.cdataKey))
                    entry._1.substring(params.cdataKey.length).toInt
                  else Integer.MAX_VALUE
                }
                else Integer.MAX_VALUE
            })(Ordering[Int])
            .foreach {
              child =>
                val (chKey, chValue) = child
                if (chKey.startsWith(params.textKey)) {
                  escapeText(chValue.str, sb)
                } else if (chKey.startsWith(params.cdataKey)) {
                  sb.append("<![CDATA[%s]]>".format(chValue.str.replaceAll("]]>", "]]]]><![CDATA[>"))) // taken from scala.xml.PCData
                } else {
                  serialize(chKey, chValue, sb)
                }
            }

          sb.append("</")
          sb.append(qname)
          sb.append('>')
        }

        sb
    }
  }

  def appendQuoted(s: String, sb: Writer): Writer = {
    val ch = if (s contains '"') '\'' else '"'
    sb.append(ch).append(s).append(ch)
  }

  /**
   * Appends escaped string to `s`.
   */
  final def escape(text: String, s: Writer): Writer = {
    // Implemented per XML spec:
    // http://www.w3.org/International/questions/qa-controls
    text.iterator.foldLeft(s) { (s, c) =>
      escMap.get(c) match {
        case Some(str) => s append str
        case _ if c >= ' ' || "\n\r\t".contains(c) => s append c
        case _ => s // noop
      }
    }
  }

  /**
   * Appends escaped string to `s`, but not &quot;.
   */
  final def escapeText(text: String, s: Writer): Writer = {
    val escTextMap = escMap - '"' // Remove quotes from escMap
    text.iterator.foldLeft(s) { (s, c) =>
      escTextMap.get(c) match {
        case Some(str) => s append str
        case _ if c >= ' ' || "\n\r\t".contains(c) => s append c
        case _ => s // noop
      }
    }
  }
}
