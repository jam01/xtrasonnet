package io.github.jam01.xtrasonnet.spi

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*-
 * Adopted:
 * - e6698ae51aed518a9c671ae39682d7068fa10deb: Merge pull request #62 from datasonnet/java-friendly-libraries
 * moved makeSimpleFunc to companion object
 */

import io.github.jam01.xtrasonnet.DataFormatService
import io.github.jam01.xtrasonnet.header.Header
import io.github.jam01.xtrasonnet.spi.Library.{dummyPosition, memberOf}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.{Builtin, Obj}
import sjsonnet._

import java.util.Collections
import scala.jdk.CollectionConverters._

object Library {
  val dummyPosition = new Position(null, 0)
  val emptyObj: Obj = Val.Obj.mk(dummyPosition, Seq.empty: _*)

  def memberOf(value: Val): Obj.Member = new Obj.ConstMember(false, Visibility.Normal, value)

  def keyFrom(value: Val): String = {
    value match {
      case x: Val.Num => x.asDouble.toString
      case x: Val.Str => x.value
      case Val.Null(_) => "null"
      case _: Val.True => "true"
      case _: Val.False => "false"
      case x => Error.fail("function expected to return Number, String, Null, or Boolean, got: " + x.prettyName)
    }
  }

  def makeSimpleFunc(params: java.util.List[String], eval: java.util.function.Function[java.util.List[Val], Val]): Val.Func = {
    val paramIndices = params.asScala.indices
    new Val.Func(dummyPosition, ValScope.empty, Params(params.toArray(new Array[String](0)), new Array[Expr](params.size))) {
      override def evalRhs(scope: ValScope, ev: EvalScope, fs: FileScope, pos: Position): Val =
        eval.apply(paramIndices.map(i => scope.bindings(i).force).asJava)
    }
  }
}

abstract class Library {
  def namespace(): String

  def functions(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Func] =
    Collections.emptyMap()

  def modules(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Obj] =
    Collections.emptyMap()

  def libsonnets(): java.util.Set[String] =
    Collections.emptySet()

  final def moduleFrom(functions: (String, Val.Func)*): Val.Obj = {
    Val.Obj.mk(dummyPosition, functions.map { case (k, v) => (k, memberOf(v)) }: _*)
  }

  final def builtin0[R: ReadWriter](name: String)
                             (eval: (Position, EvalScope) => R): (String, Val.Func) = {
    (name, new Builtin0() {
      def evalRhs(ev: EvalScope, outerPos: Position): Val = {
        //println("--- calling builtin: "+name)
        implicitly[ReadWriter[R]].write(outerPos, eval(outerPos, ev))
      }
    })
  }

  final def builtin4[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String)
                                                                                             (eval: (Position, EvalScope, T1, T2, T3, T4) => R): (String, Val.Func) = {
    (name, new Builtin4(p1, p2, p3, p4) {
      def evalRhs(arg1: Val, arg2: Val, arg3: Val, arg4: Val, ev: EvalScope, outerPos: Position): Val = {
        //println("--- calling builtin: "+name)
        val v1: T1 = implicitly[ReadWriter[T1]].apply(arg1)
        val v2: T2 = implicitly[ReadWriter[T2]].apply(arg2)
        val v3: T3 = implicitly[ReadWriter[T3]].apply(arg3)
        val v4: T4 = implicitly[ReadWriter[T4]].apply(arg4)
        implicitly[ReadWriter[R]].write(outerPos, eval(outerPos, ev, v1, v2, v3, v4))
      }
    })
  }
}

// this assumes that we're comparing Vals of the same type
object ValOrdering extends Ordering[Val] {
  def compare(x: Val, y: Val): Int =
    x match {
      case value: Val.Num => Ordering.Double.TotalOrdering.compare(value.value, y.asDouble)
      case value: Val.Str => Ordering.String.compare(value.value, y.asString)
      case bool: Val.Bool => Ordering.Boolean.compare(bool.asBoolean, y.asBoolean)
      case unsupported: Val => Error.fail("Expected embedded function to return a String, Number, or Boolean, received: " + unsupported.prettyName)
    }
}

abstract class Builtin4(pn1: String, pn2: String, pn3: String, pn4: String, defs: Array[Expr] = null) extends Builtin(Array(pn1, pn2, pn3, pn4), defs) {
  final def evalRhs(args: Array[Val], ev: EvalScope, pos: Position): Val =
    evalRhs(args(0), args(1), args(2), args(3), ev, pos)

  def evalRhs(arg1: Val, arg2: Val, arg3: Val, arg4: Val, ev: EvalScope, pos: Position): Val

  override def apply(argVals: Array[_ <: Lazy], namedNames: Array[String], outerPos: Position)(implicit ev: EvalScope): Val =
    if (namedNames == null && argVals.length == 4)
      evalRhs(argVals(0).force, argVals(1).force, argVals(2).force, argVals(3).force, ev, outerPos)
    else super.apply(argVals, namedNames, outerPos)
}

abstract class Builtin0() extends Builtin(Array.empty, null) {
  final def evalRhs(args: Array[Val], ev: EvalScope, pos: Position): Val =
    evalRhs(ev, pos)

  def evalRhs(ev: EvalScope, pos: Position): Val

  override def apply(argVals: Array[_ <: Lazy], namedNames: Array[String], outerPos: Position)(implicit ev: EvalScope): Val =
    if (namedNames == null && argVals.length == 1) evalRhs(ev, outerPos)
    else super.apply(argVals, namedNames, outerPos)

  override def apply1(argVal: Lazy, outerPos: Position)(implicit ev: EvalScope): Val =
    if (params.names.length == 1) evalRhs(ev, outerPos)
    else super.apply(Array(argVal), null, outerPos)
}