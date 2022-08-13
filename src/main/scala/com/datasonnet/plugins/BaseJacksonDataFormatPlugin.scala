package com.datasonnet.plugins

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.plugins.BaseJacksonDataFormatPlugin.OBJECT_MAPPER
import com.datasonnet.spi.AbstractDataFormatPlugin
import com.fasterxml.jackson.databind.node._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import ujson._

import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsJava, SeqHasAsJava}

object BaseJacksonDataFormatPlugin {
  protected val OBJECT_MAPPER = new ObjectMapper
}

abstract class BaseJacksonDataFormatPlugin extends AbstractDataFormatPlugin {
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
    case Obj(value) => new ObjectNode(OBJECT_MAPPER.getNodeFactory, value.map{case (k, v) => (k, jsonNodeOf(v))}.asJava)
    case Arr(value) => new ArrayNode(OBJECT_MAPPER.getNodeFactory, value.map(jsonNodeOf).asJava)
    case Num(value) => new DoubleNode(value);
    case bool: Bool => BooleanNode.valueOf(value.bool);
    case Null => NullNode.getInstance();
  }

  protected def assertObjectNode(firstObject: JsonNode): Unit = {
    if (!firstObject.isObject) throw new IllegalArgumentException("The combination of parameters given requires a JSON Object, found: " + firstObject.getNodeType.name)
  }

  protected def assertArrayNode(firstObject: JsonNode): Unit = {
    if (!firstObject.isArray) throw new IllegalArgumentException("The combination of parameters given requires a JSON Array, found: " + firstObject.getNodeType.name)
  }
}
