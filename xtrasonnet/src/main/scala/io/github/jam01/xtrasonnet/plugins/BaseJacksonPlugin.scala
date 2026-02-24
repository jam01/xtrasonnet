package io.github.jam01.xtrasonnet.plugins

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import io.github.jam01.xtrasonnet.spi.BasePlugin

object BaseJacksonPlugin {
  private val ObjectMapper = new ObjectMapper

  import com.fasterxml.jackson.databind.JsonNode
  import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeType, ObjectNode}

  import scala.util.control.TailCalls.{TailRec, done, tailcall}

  def objectOfJsonNode(node: JsonNode): java.lang.Object =
    objectOfJsonNodeTR(node).result

  private def objectOfJsonNodeTR(node: JsonNode): TailRec[java.lang.Object] =
    node.getNodeType match {
      case JsonNodeType.NULL => done(null)
      case JsonNodeType.BOOLEAN => done(if (node.asBoolean()) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
      case JsonNodeType.NUMBER => done(node.numberValue())
      case JsonNodeType.STRING => done(node.asText())
      case JsonNodeType.OBJECT =>
        val objNode = node.asInstanceOf[ObjectNode]
        val result = new java.util.LinkedHashMap[String, java.lang.Object]() // TODO: preserve order optional

        val it = objNode.properties().iterator()

        def loop(): TailRec[java.lang.Object] =
          if (!it.hasNext) done(result)
          else {
            val entry = it.next()
            tailcall(objectOfJsonNodeTR(entry.getValue)).flatMap { v =>
              result.put(entry.getKey, v)
              tailcall(loop())
            }
          }

        loop()

      case JsonNodeType.ARRAY =>
        val arrNode = node.asInstanceOf[ArrayNode]
        val result = new java.util.ArrayList[java.lang.Object]()

        val it = arrNode.elements()

        def loop(): TailRec[java.lang.Object] =
          if (!it.hasNext) done(result)
          else {
            val elem = it.next()
            tailcall(objectOfJsonNodeTR(elem)).flatMap { v =>
              result.add(v)
              tailcall(loop())
            }
          }

        loop()

      case x =>
        done(throw new IllegalArgumentException("Unsupported JsonNodeType: " + x))
    }
}

abstract class BaseJacksonPlugin extends BasePlugin {
  protected def objectMapper(): ObjectMapper = BaseJacksonPlugin.ObjectMapper

  protected def assertObjectNode(node: JsonNode, msg: String): Unit = {
    if (!node.isObject) throw new IllegalArgumentException(msg)
  }

  protected def assertArrayNode(node: JsonNode, msg: String): Unit = {
    if (!node.isArray) throw new IllegalArgumentException(msg)
  }
}
