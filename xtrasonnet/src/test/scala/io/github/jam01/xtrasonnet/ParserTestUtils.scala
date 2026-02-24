package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.{DefaultParseCache, Error, Evaluator, Importer, Interpreter, Settings, Val}
import ujson.Value

object ParserTestUtils {
  def eval0(s: String): Either[String, Value] = {
    interpreter.interpret0(s, ResourcePath("(test)"), ujson.Value)
  }

  def eval(s: String): Value = {
    eval0(s) match {
      case Right(x) => x
      case Left(e) => throw new Exception(e)
    }
  }

  def evalErr(s: String): String = {
    eval0(s) match{
      case Left(err) => err.split('\n').map(_.trim).mkString("\n")  // normalize inconsistent indenation on JVM vs JS
      case Right(r) => throw new Exception(s"Expected exception, got result: $r")
    }
  }

  def plainEval0(s: String): Either[String, Val] = {
    val int = interpreter
    int.evaluate(s, ResourcePath("(test)")).left.map(Error.formatError)
  }

  def plainEval(s: String): Val = {
    plainEval0(s) match {
      case Right(x) => x
      case Left(e) => throw new Exception(e)
    }
  }

  def plainEvalErr(s: String): String = {
    plainEval0(s) match{
      case Left(err) => err.split('\n').map(_.trim).mkString("\n")  // normalize inconsistent indenation on JVM vs JS
      case Right(r) => throw new Exception(s"Expected exception, got result: $r")
    }
  }

  def interpreter: Interpreter = new FluentInterpreter(
    ResourcePath("(test)"),
    Importer.empty,
    new DefaultParseCache,
    Settings.default,
    sjsonnet.stdlib.StdLibModule.Default.module,
    variableResolver = v => {
      if (v == "xtr") Some(Xtr.Default.asInstanceOf)
      else None
    }
  )

  def evaluator: Evaluator = interpreter.evaluator
}
