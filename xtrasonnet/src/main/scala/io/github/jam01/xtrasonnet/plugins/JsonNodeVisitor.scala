package io.github.jam01.xtrasonnet.plugins

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, JsonNodeType}
import sjsonnet.JsonVisitor
import ujson.Transformer
import upickle.core.{ArrVisitor, ObjVisitor, Visitor}

import java.math.MathContext

object JsonNodeVisitor extends JsonVisitor[JsonNode, JsonNode] with Transformer[JsonNode] {
  override def visitNull(index: Int): JsonNode = JsonNodeFactory.instance.nullNode();
  override def visitFalse(index: Int): JsonNode = JsonNodeFactory.instance.booleanNode(false)
  override def visitTrue(index: Int): JsonNode = JsonNodeFactory.instance.booleanNode(true)
  override def visitString(s: CharSequence, index: Int): JsonNode = JsonNodeFactory.instance.textNode(s.toString)
  override def visitFloat64(d: Double, index: Int): JsonNode = JsonNodeFactory.instance.numberNode(d)
  override def visitInt64(i: Long, index: Int): JsonNode = JsonNodeFactory.instance.numberNode(i)
  override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): JsonNode = {
      if (decIndex == -1 && expIndex == -1) { // PERF: re-use some jackson or ujson?
        s.toString.toLongOption match {
          case Some(l) => JsonNodeFactory.instance.numberNode(l)
          case None    => JsonNodeFactory.instance.numberNode(new java.math.BigDecimal(s.toString, MathContext.DECIMAL128))
        }
      } else {
        s.toString.toDoubleOption match {
          case Some(d) if !d.isInfinite => JsonNodeFactory.instance.numberNode(d)
          case _                        => JsonNodeFactory.instance.numberNode(new java.math.BigDecimal(s.toString, MathContext.DECIMAL128))
        }
      }
    }

  override def visitJsonableObject(length: Int, index: Int): ObjVisitor[JsonNode, JsonNode] = new ObjVisitor[JsonNode, JsonNode] {
    val objNode = JsonNodeFactory.instance.objectNode() // PERF: size Map per length?
    var key: String = _

    override def visitKey(index: Int): Visitor[_, _] = upickle.core.StringVisitor
    override def visitKeyValue(v: Any): Unit = key = v.toString
    override def subVisitor: Visitor[_, _] = JsonNodeVisitor
    override def visitValue(v: JsonNode, index: Int): Unit = objNode.set(key, v)
    override def visitEnd(index: Int): JsonNode = objNode
  }

  override def visitArray(length: Int, index: Int): ArrVisitor[JsonNode, JsonNode] = new ArrVisitor[JsonNode, JsonNode] {
    val arrNode = if (length < 0) JsonNodeFactory.instance.arrayNode() else JsonNodeFactory.instance.arrayNode(length)

    override def subVisitor: Visitor[_, _] = JsonNodeVisitor
    override def visitValue(v: JsonNode, index: Int): Unit = arrNode.add(v)
    override def visitEnd(index: Int): JsonNode = arrNode
  }

  override def transform[T](j: JsonNode, f: Visitor[_, T]): T =
    j.getNodeType match {
      case JsonNodeType.NULL => f.visitNull(-1)
      case JsonNodeType.BOOLEAN => if (j.asBoolean()) f.visitTrue(-1) else f.visitFalse(-1)
      case JsonNodeType.NUMBER => j.numberType() match {
        case JsonParser.NumberType.INT => f.visitInt32(j.asInt(), -1)
        case JsonParser.NumberType.LONG => f.visitInt64(j.asLong(), -1)
        case JsonParser.NumberType.BIG_INTEGER => f.visitFloat64StringParts(j.asText(), -1, -1, -1)
        case JsonParser.NumberType.FLOAT => f.visitFloat32(j.floatValue(), -1)
        case JsonParser.NumberType.DOUBLE => f.visitFloat64(j.asDouble(), -1)
        case JsonParser.NumberType.BIG_DECIMAL =>
          val s = j.asText()
          f.visitFloat64StringParts(
            s,
            s.indexOf('.'),
            s.indexWhere(c => (c | 0x20) == 'e'),
            -1
          )
      }
      case JsonNodeType.STRING => f.visitString(j.asText(), -1)
      case JsonNodeType.ARRAY =>
        val arrVisitor = f.visitArray(j.size(), -1)
        var i = 0
        while (i < j.size()) {
          val sub = arrVisitor.subVisitor
          arrVisitor.narrow.visitValue(transform(j.get(i), sub), -1)
          i += 1
        }
        arrVisitor.visitEnd(-1)
      case JsonNodeType.OBJECT =>
        val objVisitor = f.visitObject(j.size(), true, -1)
        j.forEachEntry { (k, v) =>
          objVisitor.visitKey(-1)
          objVisitor.visitKeyValue(objVisitor.visitKey(-1).visitString(k, -1))
          objVisitor.narrow.visitValue(
            transform(v, objVisitor.subVisitor),
            -1
          )
        }
        objVisitor.visitEnd(-1)
      case JsonNodeType.BINARY => f.visitBinary(j.binaryValue(), 0, j.binaryValue().length, -1)
      case _ => throw UnsupportedOperationException("Invalid JsonNode type " + j.getNodeType)
    }
}
