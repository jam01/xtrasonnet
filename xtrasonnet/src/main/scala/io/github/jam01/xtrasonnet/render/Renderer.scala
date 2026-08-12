package io.github.jam01.xtrasonnet.render

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import upickle.core.{ArrVisitor, ObjVisitor, Visitor}
import upickle.core.Visitor.Delegate

final case class DeepDelegate[T, V](delegate: Visitor[T, V]) extends Delegate[T, V](delegate) {
  override def visitUInt64(i: Long, index: Int): V = {
    if (i < 0) visitFloat64StringParts(java.lang.Long.toUnsignedString(i), -1, -1, index)
    else visitInt64(i, index)
  }

  override def visitInt32(i: Int, index: Int): V =
    visitInt64(i, index)

  override def visitFloat32(f: Float, index: Int): V =
    visitFloat64(f, index)

  override def visitInt64(i: Long, index: Int): V =
    visitFloat64StringParts(i.toString, -1, -1, index)

  override def visitFloat64(d: Double, index: Int): V = {
    d match {
      case Double.PositiveInfinity => visitString("Infinity", -1)
      case Double.NegativeInfinity => visitString("-Infinity", -1)
      case d if java.lang.Double.isNaN(d) => visitString("NaN", -1)
      case d =>
        val i = d.toLong
        if (i == d) visitInt64(i, index)
        else visitFloat64String(d.toString, index)
    }
  }

  override def visitArray(length: Int, index: Int): ArrVisitor[T, V] = {
    val arrVis: ArrVisitor[T, V] = delegate.visitArray(length, index)
    new ArrVisitor[T, V] {
      override def subVisitor: Visitor[_, _] = DeepDelegate(arrVis.subVisitor)
      override def visitValue(v: T, index: Int): Unit = arrVis.visitValue(v, index)
      override def visitEnd(index: Int): V = arrVis.visitEnd(index)
    }
  }

  override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[T, V] = {
    val objVis = delegate.visitObject(length, jsonableKeys, index)
    new ObjVisitor[T, V] {
      override def visitKey(index: Int): Visitor[?, ?] = objVis.visitKey(index)
      override def visitKeyValue(v: Any): Unit = objVis.visitKeyValue(v)
      override def subVisitor: Visitor[?, ?] = new DeepDelegate(objVis.subVisitor)
      override def visitValue(v: T, index: Int): Unit = objVis.visitValue(v, index)
      override def visitEnd(index: Int): V = objVis.visitEnd(index)
    }
  }
}

object Renderer {
  def bytesRenderer(indent: Int, escapeUnicode: Boolean) = DeepDelegate(ujson.BytesRenderer(indent, escapeUnicode))
  def stringRenderer(indent: Int, escapeUnicode: Boolean) = DeepDelegate(ujson.StringRenderer(indent, escapeUnicode))
}
