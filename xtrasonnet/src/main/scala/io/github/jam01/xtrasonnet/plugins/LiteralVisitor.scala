package io.github.jam01.xtrasonnet.plugins

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.Val.Literal
import sjsonnet.{Position, ValVisitor}
import ujson.JsVisitor
import upickle.core.{ArrVisitor, ObjVisitor}

class LiteralVisitor(pos: Position = new Position(null, 0)) extends JsVisitor[Literal, Literal] {
  val valVisitor = new ValVisitor(pos)

  def visitArray(length: Int, index: Int): ArrVisitor[Literal, Literal] = valVisitor.visitArray(length, index).asInstanceOf[ArrVisitor[Literal, Literal]]

  def visitObject(length: Int, index: Int): ObjVisitor[Literal, Literal] = valVisitor.visitObject(length, index).asInstanceOf[ObjVisitor[Literal, Literal]]

  def visitNull(index: Int): Literal = valVisitor.visitNull(index).asInstanceOf[Literal]

  def visitFalse(index: Int): Literal = valVisitor.visitFalse(index).asInstanceOf[Literal]

  def visitTrue(index: Int): Literal = valVisitor.visitTrue(index).asInstanceOf[Literal]

  def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Literal = valVisitor.visitFloat64StringParts(s, decIndex, expIndex, index).asInstanceOf[Literal]

  def visitString(s: CharSequence, index: Int): Literal = valVisitor.visitString(s, index).asInstanceOf[Literal]
}