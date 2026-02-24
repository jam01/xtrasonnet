package io.github.jam01.xtrasonnet.spi

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.*
import sjsonnet.Expr.Params
import sjsonnet.Val.Obj
import sjsonnet.functions.FunctionModule

import java.util.Collections
import scala.jdk.CollectionConverters.MapHasAsScala

object Library {
  val emptyObj: Obj = Val.Obj.mk(Position(null, 0), Seq.empty: _*)

  def keyFrom(value: Val): String = {
    value match {
      case x: Val.Num => x.toString
      case x: Val.Str => x.value
      case Val.Null(_) => "null"
      case _: Val.True => "true"
      case _: Val.False => "false"
      case x => Error.fail("function expected to return Number, String, Null, or Boolean, got: " + x.prettyName)
    }
  }

  def jbuiltin(params: Array[String], func: TriFunction[Array[Val], Position, EvalScope, Val]): Val.Func = {
    val paramIndices = params.indices
    new Val.Func(null, ValScope.empty, Params(params, null)) {
      override def evalRhs(scope: ValScope, ev: EvalScope, fs: FileScope, pos: Position): Val = {
        func.apply(paramIndices.map(i => scope.bindings(i).force).toArray, pos, ev)
      }
    }
  }
}

abstract class Library extends FunctionModule
abstract class JLibrary extends Library {
  def functions(): java.util.Map[String, Val.Func] =
    Collections.emptyMap()

  override def module: Val.Obj = {
    moduleFromFunctions(functions().asScala.toSeq: _*)
  }
}

@FunctionalInterface
trait TriFunction[T, U, V, R] {
  def apply(t: T, u: U, v: V): R
}
