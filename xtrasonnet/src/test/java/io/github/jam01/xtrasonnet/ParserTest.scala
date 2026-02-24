package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.ParserTest.fsTest
import org.junit.jupiter.api.Assertions.{assertArrayEquals, assertEquals, assertFalse, assertTrue}
import org.junit.jupiter.api.Test
import sjsonnet.Expr.*
import sjsonnet.Expr.FieldName.*
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.ObjBody.*
import sjsonnet.Val.{Arr, Literal}
import sjsonnet.{Expr, FileScope, Position, Val}

import scala.collection.mutable

object ParserTest {
  val fs = new FileScope(ResourcePath("(memory)")) // TODO: clarify which should be in where
  val fsTest = new FileScope(ResourcePath("(test)"))
}

class ParserTest {

  private def assertParsedAST(input: String, expectedAST: Expr, errorMessage: String, success: Boolean = true): Unit = {
    val resultAST = fastparse.parse(input, new FluentParser(ResourcePath("(test)"), mutable.HashMap.empty, mutable.HashMap.empty).document(_))
    if (success) assertTrue(resultAST.isSuccess, errorMessage)
    else assertFalse(resultAST.isSuccess, errorMessage)

    if (success) ExprComparator.assertExprEquals(expectedAST, resultAST.get.value._1)
  }

  @Test def testParseBoolLiteral(): Unit =
    assertParsedAST("true", Val.True(Position(fsTest, 0)), "The AST for the boolean literal is incorrect.")

  @Test def testParseNullLiteral(): Unit =
    assertParsedAST("null", Val.Null(Position(fsTest, 0)), "The AST for the null literal is incorrect.")

  @Test def testParseFloatLiteral(): Unit =
    assertParsedAST("3.14", Val.Float64(Position(fsTest, 0), 3.14), "The AST for the float literal is incorrect.")

  @Test def testParseIntLiteral(): Unit =
    assertParsedAST("42", Val.Int64(Position(fsTest, 0), 42), "The AST for the integer literal is incorrect.")

  @Test def testParseStringLiteral(): Unit =
    assertParsedAST("\"hello world\"", Val.Str(Position(fsTest, 0), "hello world"), "The AST for the string literal is incorrect.")

  @Test def testParseIdent(): Unit =
    assertParsedAST("myIdent", Id(Position(fsTest, 0), "myIdent"), "The AST for the Ident is incorrect.")

  @Test def testParseArrayLiteral(): Unit =
    assertParsedAST(
      "[1, 2, 3]",
      Arr(Position(fsTest, 0), Array(
        Val.Int64(Position(fsTest, 1), 1),
        Val.Int64(Position(fsTest, 4), 2),
        Val.Int64(Position(fsTest, 7), 3)
      )),
      "The AST for the array literal is incorrect."
    )

  @Test def testParseObjLiteral(): Unit =
    assertParsedAST("{ \"key\": \"value\" }",
//      Val.Obj(Position(fsTest, 0), mutable.Map("key" -> Val.Str(Position(fsTest, 9), "value"))),
      Val.staticObject(Position(fsTest, 0), Array(
        Expr.Member.Field(Position(fsTest, 2), Expr.FieldName.Fixed("key"), false, null, Visibility.Normal, Val.Str(Position(fsTest, 9), "value"))
      ), mutable.HashMap.empty, mutable.HashMap("key" -> "key")),
      "The AST for the map literal is incorrect."
    )

  @Test def testParseParens(): Unit =
    assertParsedAST("(42)", Val.Int64(Position(fsTest, 1), 42), "The AST for the expression within parentheses is incorrect.")

  @Test def testParseSimplePostfixExpression(): Unit =
    assertParsedAST("Ident.field", Select(Position(fsTest, 5), Id(Position(fsTest, 0), "Ident"), "field"), "The AST for the simple postfix expression is incorrect.")

  @Test def testParseChainedPostfixExpression(): Unit =
    assertParsedAST(
      "Ident.field.method",
      Select(Position(fsTest, 11), Select(Position(fsTest, 5), Id(Position(fsTest, 0), "Ident"), "field"), "method"),
      "The AST for the chained postfix expression is incorrect."
    )

  @Test def testParseArrayLookupExpression(): Unit =
    assertParsedAST("array[0]", Lookup(Position(fsTest, 5), Id(Position(fsTest, 0), "array"), Val.Int64(Position(fsTest, 6), 0)), "The AST for the array lookup expression is incorrect.")

  @Test def testParseSafeNavigationExpression(): Unit =
    assertParsedAST("object?.field", Select(Position(fsTest, 6), Id(Position(fsTest, 0), "object"), "field", safe = true), "The AST for the safe navigation expression is incorrect.")

  @Test def testParseMacroCallExpr(): Unit =
    assertParsedAST(
      "receiver.method(arg1, arg2)",
      Apply(Position(fsTest, 15), Select(Position(fsTest, 8), Id(Position(fsTest, 0), "receiver"), "method"), Array(Id(Position(fsTest, 16), "arg1"), Id(Position(fsTest, 22), "arg2")), null, false),
      //Macro(Position(fsTest, 8), Id(Position(fsTest, 0), "receiver"), "method", List(Id(Position(fsTest, 16), "arg1"), Id(Position(fsTest, 22), "arg2"))),
      "The AST for the macro call expression is incorrect."
    )

  @Test def testParseUnaryNotExpression(): Unit =
    assertParsedAST("!true", UnaryOp(Position(fsTest, 0), Expr.UnaryOp.OP_!, Val.True(Position(fsTest, 1))), "The AST for the unary NOT expression is incorrect.")

  @Test def testParseUnaryNegateExpression(): Unit =
    assertParsedAST("-42", UnaryOp(Position(fsTest, 0), Expr.UnaryOp.OP_-, Val.Int64(Position(fsTest, 1), 42)), "The AST for the unary negate expression is incorrect.")

  @Test def testParseAdditiveExpression(): Unit =
    assertParsedAST(
      "1 + 2",
      BinaryOp(Position(fsTest, 2), Val.Int64(Position(fsTest, 0), 1), Expr.BinaryOp.OP_+, Val.Int64(Position(fsTest, 4), 2)),
      "The AST for the additive expression is incorrect."
    )

  @Test def testParseSubtractiveExpression(): Unit =
    assertParsedAST(
      "5 - 3",
      BinaryOp(Position(fsTest, 2),Val.Int64(Position(fsTest, 0), 5), Expr.BinaryOp.OP_-, Val.Int64(Position(fsTest, 4), 3)),
      "The AST for the subtractive expression is incorrect."
    )

  @Test def testParseMultiplicativeExpression(): Unit =
    assertParsedAST(
      "3 * 4",
      BinaryOp(Position(fsTest, 2), Val.Int64(Position(fsTest, 0), 3), Expr.BinaryOp.OP_*, Val.Int64(Position(fsTest, 4), 4)),
      "The AST for the multiplicative expression is incorrect."
    )

  @Test def testParseDivisionExpression(): Unit =
    assertParsedAST(
      "10 / 2",
      BinaryOp(Position(fsTest, 3), Val.Int64(Position(fsTest, 0), 10), Expr.BinaryOp.OP_/, Val.Int64(Position(fsTest, 5), 2)),
      "The AST for the division expression is incorrect."
    )

  @Test def testParseComplexExpression(): Unit = {
    val input = "(a + b * (c - d / e) || !f) && ((if p then q else r) && (g?.h[i] == j.k(\"l\", m) % n))"
    val expectedAST = Expr.And(
      Position(fsTest, 28),
      Expr.Or(
        Position(fsTest, 21),
        BinaryOp(
          Position(fsTest, 3),
          Id(Position(fsTest, 1), "a"),
          Expr.BinaryOp.OP_+,
          BinaryOp(
            Position(fsTest, 7),
            Id(Position(fsTest, 5), "b"),
            Expr.BinaryOp.OP_*,
            BinaryOp(
              Position(fsTest, 12),
              Id(Position(fsTest, 10), "c"),
              Expr.BinaryOp.OP_-,
              BinaryOp(Position(fsTest, 16), Id(Position(fsTest, 14), "d"), Expr.BinaryOp.OP_/, Id(Position(fsTest, 18), "e"))
            )
          )
        ),
        UnaryOp(Position(fsTest, 24), Expr.UnaryOp.OP_!, Id(Position(fsTest, 25), "f"))
      ),
      Expr.And(
        Position(fsTest, 53),
        Expr.IfElse(
          Position(fsTest, 33),
          Id(Position(fsTest, 36), "p"),
          Id(Position(fsTest, 43), "q"),
          Id(Position(fsTest, 50), "r")
        ),
        BinaryOp(
          Position(fsTest, 65),
          Lookup(Position(fsTest, 61), Select(Position(fsTest, 58), Id(Position(fsTest, 57), "g"), "h", safe = true), Id(Position(fsTest, 62), "i")),
          Expr.BinaryOp.OP_==,
          BinaryOp(
            Position(fsTest, 80),
            Apply(Position(fsTest, 71), Select(Position(fsTest, 69),Id(Position(fsTest, 68), "j"), "k"), Array(Val.Str(Position(fsTest, 72), "l"), Id(Position(fsTest, 77), "m")), null, false),
            Expr.BinaryOp.OP_%,
            Id(Position(fsTest, 82), "n")
          )
        )
      )
    )

    assertParsedAST(input, expectedAST, "The AST for the complex expression is incorrect.")
  }

  @Test def testParseMapFunction(): Unit = {
    assertParsedAST(
      "var map(function(x) x + 2)",
      Apply(
        Position(fsTest, 7),
        Id(Position(fsTest, 4), "map"),
        Array(Id(Position(fsTest, 0), "var"), Function(
            Position(fsTest, 8),
            Expr.Params(Array("x"), Array[Expr](null)),
            BinaryOp(Position(fsTest, 22), Id(Position(fsTest, 20), "x"), Expr.BinaryOp.OP_+, Val.Int64(Position(fsTest, 24), 2)))
          ),
        null,
        false
      ),
      "The AST for the map function application is incorrect."
    )
  }

  @Test def testParseLibMapFunction(): Unit = {
    assertParsedAST(
      "var lib.map(function(x) x + 2)",
      Apply(
        Position(fsTest, 11),
        Select(Position(fsTest, 7), Id(Position(fsTest, 4), "lib"), "map"),
        Array(Id(Position(fsTest, 0), "var"), Function(
            Position(fsTest, 12),
            Expr.Params(Array("x"), Array[Expr](null)),
            BinaryOp(Position(fsTest, 26), Id(Position(fsTest, 24), "x"), Expr.BinaryOp.OP_+, Val.Int64(Position(fsTest, 30), 2)))
          ),
        null,
        false
      ),
      "The AST for the map function application is incorrect."
    )
  }

  @Test def testInfixMacro(): Unit = {
    val expected = Apply(Position(fsTest, 5), Id(Position(fsTest, 2), "add"), Array(Id(Position(fsTest, 0), "a"), Id(Position(fsTest, 6), "b")), null, false)
    assertParsedAST("a add b", expected, "The AST for the infix macro expression is incorrect.")
  }

  @Test def testInfixMacroWithMultipleArgs(): Unit = {
    val expected = Apply(Position(fsTest, 6), Id(Position(fsTest, 2), "sum"), Array(Id(Position(fsTest, 0), "a"), Id(Position(fsTest, 7), "b"), Id(Position(fsTest, 10), "c")), null, false)
    assertParsedAST("a sum (b, c)", expected, "The AST for the infix macro with multiple args is incorrect.")
  }

  @Test def testDanglingMacroCall(): Unit = {
    val expected = Apply(Position(fsTest, 9), Id(Position(fsTest, 0), "increment"), Array(Id(Position(fsTest, 10), "a")), null, false)
    assertParsedAST("increment(a)", expected, "The AST for the dangling macro call is incorrect.")
  }

  @Test def testBadInfixMacro(): Unit = {
    assertParsedAST("a imvalidSuffix", null, "Invalid infix.", false)
  }

  @Test def testBadBinaryOp(): Unit = {
    assertParsedAST("a &&", null, "The AST for the suffix macro call is incorrect.", false)
  }

  @Test def testSuffixMacro(): Unit = {
    val expected = Apply(Position(fsTest, 10), Select(Position(fsTest, 1), Id(Position(fsTest, 0), "a"), "suffixOp"), Array(), null, false)
    assertParsedAST("a.suffixOp()", expected, "The AST for the suffix macro call is incorrect.")
  }

  @Test def testMacroWithNamedArguments(): Unit = {
    val expected = Apply(Position(fsTest, 10), Id(Position(fsTest, 0), "namedMacro"), Array(Id(Position(fsTest, 18), "a")), Array("arg1"), false)
    assertParsedAST("namedMacro(arg1 = a)", expected, "The AST for the macro with named arguments is incorrect.")
  }

  @Test def testInfixMacroWithNamedArguments(): Unit = {
    val expected = Apply(Position(fsTest, 12), Id(Position(fsTest, 2), "namedMacro"), Array(Id(Position(fsTest, 0), "a"), Id(Position(fsTest, 20), "b")), Array("arg1"), false)
    assertParsedAST("a namedMacro(arg1 = b)", expected, "The AST for the macro with named arguments is incorrect.")
  }

  @Test def testInfixMacroWithNamedAndPosArguments(): Unit = {
    val expected = Apply(Position(fsTest, 12), Id(Position(fsTest, 2), "namedMacro"), Array(Id(Position(fsTest, 0), "a"), Id(Position(fsTest, 13), "b"), Id(Position(fsTest, 23), "c")), Array("arg2"), false)
    assertParsedAST("a namedMacro(b, arg2 = c)", expected, "The AST for the macro with named arguments is incorrect.")
  }

  @Test def fail(): Unit = {
    assertParsedAST("a id.id[0].id(a)[1]", null, "Apply on non Id or Select chain is not allowed", false)
  }

  @Test def testSelf(): Unit = {
    val expected = MemberList(Position(fsTest, 0), null,
      Array(Expr.Member.Field(Position(fsTest, 2), Fixed("f"), false, null, Visibility.Normal, Val.Int64(Position(fsTest, 5), 1)),
        Expr.Member.Field(Position(fsTest, 8), Fixed("g"), false, null, Visibility.Normal, Select(Position(fsTest, 15), Self(Position(fsTest, 11)), "f"))),
      null
    )
    assertParsedAST("{ f: 1, g: self.f }", expected, "The AST for object with self is incorrect.")
  }

  @Test def test$(): Unit = {
    val expected = MemberList(Position(fsTest, 0), null,
      Array(Expr.Member.Field(Position(fsTest, 2), Fixed("f"), false, null, Visibility.Normal, Val.Int64(Position(fsTest, 5), 1)),
        Expr.Member.Field(Position(fsTest, 8), Fixed("n"), false, null, Visibility.Normal, MemberList(Position(fsTest, 11), null,
          Array(Expr.Member.Field(Position(fsTest, 13), Fixed("g"), false, null, Visibility.Normal, Select(Position(fsTest, 17), $(Position(fsTest, 16)), "f"))), null)),
      ),
      null
    )
    assertParsedAST("{ f: 1, n: { g: $.f } }", expected, "The AST for object with $ is incorrect.")
  }

  @Test def assert(): Unit = {
    val expected = MemberList(Position(fsTest, 0), null,
      Array(Expr.Member.Field(Position(fsTest, 23), Fixed("f"), false, null, Visibility.Normal, Val.Int64(Position(fsTest, 26), 1))),
      Array(Expr.Member.AssertStmt(Val.True(Position(fsTest, 9)), Val.Str(Position(fsTest, 16), "wat"))))
    assertParsedAST("{ assert true : 'wat', f: 1 }", expected, "The AST for object assert is incorrect")
  }

//  @Test def badMonovariateInfix(): Unit = {
//    assertParsedAST("a blop()", null, "invalid infix, false", false)
//  }
}


object ExprComparator {
  def assertFieldEquals(f1: Member.Field, f2: Member.Field): Unit =
    assertEquals(f1.pos, f2.pos, "Positions do no match")
    assertEquals(f1.fieldName, f2.fieldName, "Field names do no match")
    assertExprEquals(f1.rhs, f2.rhs)

  def assertParamsEquals(p1: Params, p2: Params): Unit = {
    assertArrayEquals(p1.names.asInstanceOf[Array[AnyRef]], p2.names.asInstanceOf[Array[AnyRef]])
    assertEquals(p1.defaultExprs.length, p2.defaultExprs.length)
    p1.defaultExprs.zip(p2.defaultExprs).foreach((e1, e2) => assertExprEquals(e1, e2))
  }

  def assertExprEquals(expr1: Expr, expr2: Expr): Unit = (expr1, expr2) match {
    case (Expr.Arr(pos1, values1), Expr.Arr(pos2, values2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertArrayEquals(values1.asInstanceOf[Array[AnyRef]], values2.asInstanceOf[Array[AnyRef]], "Arrays do not match")

    case (Expr.Apply(pos1, value1, args1, namedNames1, _), Expr.Apply(pos2, value2, args2, namedNames2, _)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertEquals(value1, value2, "Values do not match")
      assertEquals(args1.length, args2.length, "Args do not match")
      args1.zip(args2).foreach((f1, f2) => assertExprEquals(f1, f2))
      assertArrayEquals(namedNames1.asInstanceOf[Array[AnyRef]], namedNames2.asInstanceOf[Array[AnyRef]], "Named names arrays do not match")

    case (Expr.BinaryOp(pos1, lhs1, op1, rhs1), Expr.BinaryOp(pos2, lhs2, op2, rhs2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertEquals(op1, op2, "Ops do not match")
      assertExprEquals(lhs1, lhs2)
      assertExprEquals(rhs1, rhs2)

    case (Expr.And(pos1, lhs1, rhs1), Expr.And(pos2, lhs2, rhs2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertExprEquals(lhs1, lhs2)
      assertExprEquals(rhs1, rhs2)

    case (Expr.Or(pos1, lhs1, rhs1), Expr.Or(pos2, lhs2, rhs2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertExprEquals(lhs1, lhs2)
      assertExprEquals(rhs1, rhs2)

    case (Expr.ObjBody.MemberList(pos1, _, fields1, asserts1), Expr.ObjBody.MemberList(pos2, _, fields2, asserts2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertEquals(fields1.length, fields2.length, "Fields do not match")
      fields1.zip(fields2).foreach((f1, f2) => assertFieldEquals(f1, f2))
      assertArrayEquals(asserts1.asInstanceOf[Array[AnyRef]], asserts2.asInstanceOf[Array[AnyRef]], "AssertStmts arrays do not match")

    case (Val.Arr(pos1, values1), Val.Arr(pos2, values2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertArrayEquals(values1.asInstanceOf[Array[AnyRef]], values2.asInstanceOf[Array[AnyRef]], "Arrays do not match")

    case (Expr.Function(pos1, params1, body1), Expr.Function(pos2, params2, body2)) =>
      assertEquals(pos1, pos2, "Positions do not match")
      assertParamsEquals(params1, params2)
      assertExprEquals(body1, body2)

    case (x: Literal, y: Literal) => ParserTestUtils.evaluator.equal(x, y)

    // Fallback case for other types of expressions
    case _ =>
      assertEquals(expr1, expr2, "Expressions do not match")
  }
}
