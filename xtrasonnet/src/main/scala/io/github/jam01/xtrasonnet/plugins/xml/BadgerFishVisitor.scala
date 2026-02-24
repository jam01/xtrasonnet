package io.github.jam01.xtrasonnet.plugins.xml

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeType, ObjectNode}
import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin.{DEFAULT_NS_KEY, EffectiveParams}
import io.github.jam01.xtrasonnet.plugins.JsonNodeVisitor
import sjsonnet.{EvalScope, JsonVisitor, Materializer, Val}
import upickle.core.{ArrVisitor, ObjVisitor, StringVisitor, Visitor}

import java.io.{StringWriter, Writer}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.IteratorHasAsScala

final class BadgerFishVisitor(val params: EffectiveParams) {

  // taken from scala.xml.Utility
  object Escapes {
    val pairs: Map[String, Char] = Map(
      "lt" -> '<',
      "gt" -> '>',
      "amp" -> '&',
      "quot" -> '"',
      "apos" -> '\''
    )
    val escMap: Map[Char, String] = (pairs - "apos").map { case (s, c) => c -> s"&$s;" }
  }
  import Escapes.escMap

  private def appendQuoted(s: String, sb: Writer): Writer = {
    val ch = if (s.contains('"')) '\'' else '"'
    sb.append(ch).append(s).append(ch)
  }

  /** Escapes for attribute values (includes &quot;). */
  def escape(text: String, s: Writer): Writer =
    text.iterator.foldLeft(s) { (s0, c) =>
      escMap.get(c) match {
        case Some(str) => s0.append(str)
        case _ if c >= ' ' || "\n\r\t".contains(c) => s0.append(c)
        case _ => s0
      }
    }

  /** Escapes for element text content (does NOT escape "). */
  private def escapeText(text: String, s: Writer): Writer = {
    val escTextMap = escMap - '"'
    text.iterator.foldLeft(s) { (s0, c) =>
      escTextMap.get(c) match {
        case Some(str) => s0.append(str)
        case _ if c >= ' ' || "\n\r\t".contains(c) => s0.append(c)
        case _ => s0
      }
    }
  }

  /**
   * Visitor-based entrypoint.
   *
   * This version accepts Val and walks it via Visitor
   * so the serialization logic lives in visitors rather than pattern matching.
   */
  def serialize(qname: String, value: Val, sb: Writer = new StringWriter())(implicit ev: EvalScope): Writer = {
    Materializer.apply0(value, new ElementVisitor(qname, sb)(ev))(ev)
    sb
  }

  /** A Visitor that writes exactly one XML element named `qname` into `out`. */
  private final class ElementVisitor(qname: String, out: Writer)(implicit ev: EvalScope)
    extends JsonVisitor[Any, Unit] {

    // --- primitives ---------------------------------------------------------
    override def visitFalse(index: Int): Unit = visitString("false", index)
    override def visitTrue(index: Int): Unit = visitString("true", index)
    override def visitInt64(i: Long, index: Int): Unit = visitFloat64(i.toDouble, index)

    override def visitNull(index: Int): Unit = {
      out.append('<').append(qname)
      if (params.emptyTagsNull) out.append("/>")
      else out.append('>').append("</").append(qname).append('>')
    }

    override def visitString(s: CharSequence, index: Int): Unit = {
      val str = if (s == null) "" else s.toString
      out.append('<').append(qname)
      if (str.isEmpty && params.emptyTagsStr) out.append("/>")
      else {
        out.append('>')
        escapeText(str, out)
        out.append("</").append(qname).append('>')
      }
    }

    override def visitFloat64(d: Double, index: Int): Unit = {
      out.append('<').append(qname).append('>')
      out.append(d.toString)
      out.append("</").append(qname).append('>')
    }

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Unit = {
      out.append('<').append(qname).append('>')
      out.append(s)
      out.append("</").append(qname).append('>')
    }

    // --- arrays -------------------------------------------------------------
    override def visitArray(length: Int, index: Int): ArrVisitor[Any, Unit] = {
      // <qname>  serialize(qname, eachItem)  </qname>
      out.append('<').append(qname).append('>')

      new ArrVisitor[Any, Unit] {
        override def subVisitor: Visitor[_, _] =
          new ElementVisitor(qname, out) // each array element becomes <qname>...</qname>

        override def visitValue(v: Any, index: Int): Unit = () // side-effects already happened
        override def visitEnd(index: Int): Unit = {
          out.append("</").append(qname).append('>')
        }
      }
    }

    // --- objects ------------------------------------------------------------
    override def visitJsonableObject(length: Int, index: Int): ObjVisitor[Any, Unit] = {
      out.append('<').append(qname)

      new ObjVisitor[Any, Unit] {
        private var currentKey: String = _
        private val children = new mutable.LinkedHashMap[String, JsonNode]()

        // We buffer *values* so we can flatten + sort
        private val valueBufferVisitor: Visitor[_, _] = JsonNodeVisitor

        override def subVisitor: Visitor[_, _] = valueBufferVisitor
        override def visitKey(index: Int): Visitor[_, _] = StringVisitor
        override def visitKeyValue(v: Any): Unit = currentKey = if (v == null) "" else v.asInstanceOf[String]

        override def visitValue(v: Any, index: Int): Unit = {
          val childValue = v.asInstanceOf[JsonNode]
          val childKey = currentKey

          if (childKey == params.xmlnsKey) {
            // xmlns object => append attributes in encounter order
            childValue.asInstanceOf[ObjectNode].properties().forEach { kvv =>
              val k = kvv.getKey
              val vv = kvv.getValue
              out.append(" xmlns")
              if (k != null && k != DEFAULT_NS_KEY) out.append(':').append(k)
              out.append('=')
              val tmp = new StringWriter()
              val raw: String = if (vv != null && vv.isTextual) vv.asText() else ""
              escape(raw, tmp)
              appendQuoted(tmp.toString, out)
            }
          } else if (childKey == params.attrKey) {
            // @ object => append attributes in encounter order
            childValue.asInstanceOf[ObjectNode].properties().forEach { akav =>
              val ak = akav.getKey
              val av = akav.getValue
              out.append(' ').append(ak).append('=')
              val tmp = new StringWriter()
              val raw = if (av != null && av.isTextual) av.asText() else ""
              escape(raw, tmp)
              appendQuoted(tmp.toString, out)
            }
          } else {
            children.addOne(childKey -> childValue)
          }
        }

        override def visitEnd(index: Int): Unit = {
          // empty tags handling
          if (children.isEmpty && params.emptyTagsObj) {
            out.append("/>")
            return
          }

          if (params.emptyTagsStr && children.size == 1) {
            val (k, v) = children.head
            if (k.startsWith(params.textKey) && v.isTextual && v.asText().isEmpty) {
              out.append("/>")
              return
            }
          }

          // long form
          out.append('>')

          val flattened: ArrayBuffer[(String, JsonNode)] =
            children.toSeq.foldLeft(new ArrayBuffer[(String, JsonNode)]()) { (acc, kv) =>
              val (k, v) = kv
              if (k == params.posKey) acc
              else v match {
                case arr: ArrayNode =>
                  if (arr.isEmpty) acc.addOne(k -> v)
                  else acc.addAll(arr.values().asScala.map(it => k -> it))
                case _ =>
                  acc.addOne(k -> v)
              }
            }

          // sort using ordering key in objects, and numeric suffix in text/cdata keys
          val sorted = flattened.sortBy { case (k, v) =>
              v match {
                case inner: ObjectNode =>
                  val childVal0 = inner.get(params.posKey)
                  if (childVal0 == null) Int.MaxValue
                  else childVal0.getNodeType match {
                    case JsonNodeType.MISSING => Int.MaxValue
                    case JsonNodeType.NUMBER => childVal0.asInt()
                    case JsonNodeType.STRING => childVal0.asInt()
                    case x =>
                      throw new IllegalArgumentException("Invalid ordering key value: " + x)
                  }
                case _ =>
                  if (k.nonEmpty && k.last.isDigit) {
                    if (k.startsWith(params.textKey)) k.substring(params.textKey.length).toInt
                    else if (k.startsWith(params.cdataKey)) k.substring(params.cdataKey.length).toInt
                    else Int.MaxValue
                  } else Int.MaxValue
              }
            }(Ordering[Int])

          // emit
          sorted.foreach { case (childKey, childVal) =>
            if (childKey.startsWith(params.textKey)) {
              escapeText(childVal.asText(), out)
            } else if (childKey.startsWith(params.cdataKey)) {
              val safe = childVal.asText().replaceAll("]]>", "]]]]><![CDATA[>")
              out.append("<![CDATA[").append(safe).append("]]>")
            } else {
              // recurse via Visitor (not pattern matching):
              JsonNodeVisitor.transform(childVal, new ElementVisitor(childKey, out))
            }
          }

          out.append("</").append(qname).append('>')
        }
      }
    }
  }
}
