package com.datasonnet.plugins

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.datasonnet.spi.AbstractDataFormatPlugin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, BooleanNode, NullNode, NumericNode, ObjectNode, TextNode}
import ujson.Value

import scala.jdk.CollectionConverters.IteratorHasAsScala

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
}
