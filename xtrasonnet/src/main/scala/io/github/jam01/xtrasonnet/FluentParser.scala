package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import fastparse.*
import fastparse.JsonnetWhitespace.whitespace
import io.github.jam01.xtrasonnet.FluentParser.{applyId, isSpaceOrTab, prependInfix}
import sjsonnet.{Expr, Parser, Path, Position, Settings, Val}

import scala.annotation.switch
import scala.collection.mutable

object FluentParser {
  private val isSpaceOrTab: Char => Boolean = c => c == ' ' || c == '\t'

  private def applyId(root: Expr.Id, selectors: Seq[(Position, String)]): Expr = {
    selectors.foldLeft(root: Expr) { (currentExpr, selector) =>
      Expr.Select(selector._1, currentExpr, selector._2)
    }
  }

  private def prependInfix(i: Position,
                            lhs: Expr,
                            first: Expr,
                            rest: Array[Expr],
                            namedNames: Array[String],
                            tailstrict: Boolean) = {
    val a =
      if (first == null) rest
      else {
        val y = new Array[Expr](rest.length + 1)
        y(0) = first
        System.arraycopy(rest, 0, y, 1, rest.length)
        y
      }

    Expr.Apply(
      i,
      lhs,
      a,
      if (namedNames == null || namedNames.length == 0) null else namedNames,
      tailstrict
    )
  }
}

final class FluentParser(currentFile: Path,
                         internedStrings: mutable.HashMap[String, String],
                         internedStaticFieldSets: mutable.HashMap[
                           Val.StaticObjectFieldSet,
                           java.util.LinkedHashMap[String, java.lang.Boolean]
                         ],
                         settings: Settings = Settings.default)
  extends Parser(currentFile, internedStrings, internedStaticFieldSets, settings) {

  private def ws[$: P]: P[Unit] = P(CharsWhile(isSpaceOrTab, 1)).opaque("whitespace")

  override def expr1[$: P](currentDepth: Int): P[Expr] = {
    P(expr2(currentDepth + 1) ~~ (exprSuffix2(currentDepth + 1) | infix(currentDepth + 1)).rep)
      .map { case (pre, fs) =>
        fs.foldLeft(pre) { case (p, f) => f(p) }
      }
  }

  override def exprSuffix2[$: P](currentDepth: Int): P[Expr => Expr] = {
    P(
      Pass ~ Pos.flatMapX { i =>
        (CharIn(".[({") | StringIn("?."))./.!.map(_(0)).flatMapX { c =>
          (c: @switch) match {
            case '.' =>
              Pass ~ (id.map(x => Expr.Select(i, _: Expr, x)) | string.map(x =>
                Expr.Select(i, _: Expr, x)
              ))
            case '?' =>
              Pass ~ (id.map(x => Expr.Select(i, _: Expr, x, safe = true)) | string.map(x =>
                Expr.Select(i, _: Expr, x, safe = true)
              ))
            case '[' =>
              Pass ~ (expr(currentDepth + 1).? ~ (":" ~ expr(currentDepth + 1).?).rep ~ "]").map {
                case (Some(tree), Seq()) => Expr.Lookup(i, _: Expr, tree)
                case (start, ins) =>
                  Expr.Slice(i, _: Expr, start, ins.headOption.flatten, ins.lift(1).flatten)
              }
            case '(' =>
              Pass ~ (args(currentDepth + 1) ~ ")" ~ "tailstrict".!.?).map {
                case (args, namedNames, tailstrict) =>
                  Expr.Apply(
                    i,
                    _: Expr,
                    args,
                    if (namedNames.length == 0) null else namedNames,
                    tailstrict.nonEmpty
                  )
              }
            case '{' =>
              Pass ~ (objinside(i, currentDepth + 1) ~ "}").map(x => Expr.ObjExtend(i, _: Expr, x))
            case _ => Fail
          }
        }
      }
    )
  }

  def infix[$: P]: P[Expr => Expr] = infix(0)

  def infix[$: P](currentDepth: Int): P[Expr => Expr] = {
    P(
      ws ~ ((Pos ~~ id.!) ~ (Pos ~~ ("." ~ id)).rep).flatMapX { case (pos, x, xs) =>
        val applyLhs = applyId(Expr.Id(pos, x), xs)
        (Pass ~ (Pos ~~ "(").opaque("\"(\"") ~/ args(currentDepth + 1) ~ ")" ~ "tailstrict".!.?).map {
          case (pos1, (args, namedNames), tailstrict) =>
            (left: Expr) =>
              prependInfix(pos1, applyLhs, left, args, namedNames, tailstrict.nonEmpty)
        } |
          (ws ~/ expr(currentDepth + 1)).map { right =>
            val pos1 = Position(
              pos.fileScope,
              xs.lastOption
                .map(lx => lx._1.offset + lx._2.length)
                .getOrElse(pos.offset + x.length)
            )
            (left: Expr) => Expr.Apply(pos1, applyLhs, Array(left, right), null, false)
          }
      }
    )
  }
}
