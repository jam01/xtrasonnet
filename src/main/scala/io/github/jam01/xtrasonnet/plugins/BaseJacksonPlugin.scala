package io.github.jam01.xtrasonnet.plugins

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.node._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import BaseJacksonPlugin.OBJECT_MAPPER
import io.github.jam01.xtrasonnet.spi.BasePlugin
import ujson._

import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsJava, SeqHasAsJava}

object BaseJacksonPlugin {
  private val OBJECT_MAPPER = new ObjectMapper
}

abstract class BaseJacksonPlugin extends BasePlugin {
  protected def OBJECT_MAPPER(): ObjectMapper = BaseJacksonPlugin.OBJECT_MAPPER

  protected def ujsonFrom(jsonNode: JsonNode): Value = jsonNode match {
    case b: BooleanNode => ujson.Bool(b.booleanValue())
    case n: NumericNode => ujson.Num(n.numberValue().doubleValue())
    case s: TextNode => ujson.Str(s.textValue())
    case o: ObjectNode => ujson.Obj.from(o.fields.asScala.map(entry => (entry.getKey, ujsonFrom(entry.getValue))))
    case a: ArrayNode => ujson.Arr.from(a.elements.asScala.map(ujsonFrom))
    case _: NullNode => ujson.Null
    case _ => throw new IllegalArgumentException("Jackson node " + jsonNode + " not supported!")
  }

  protected def jsonNodeOf(value: Value): JsonNode = value match {
    case Str(value) => new TextNode(value);
    case Obj(value) => new ObjectNode(OBJECT_MAPPER.getNodeFactory, value.map { case (k, v) => (k, jsonNodeOf(v)) }.asJava)
    case Arr(value) => new ArrayNode(OBJECT_MAPPER.getNodeFactory, value.map(jsonNodeOf).asJava)
    case Num(value) => new DoubleNode(value);
    case bool: Bool => BooleanNode.valueOf(value.bool);
    case Null => NullNode.getInstance();
  }

  protected def assertObjectNode(node: JsonNode, msg: String): Unit = {
    if (!node.isObject) throw new IllegalArgumentException(msg)
  }

  protected def assertArrayNode(node: JsonNode, msg: String): Unit = {
    if (!node.isArray) throw new IllegalArgumentException(msg)
  }
}
