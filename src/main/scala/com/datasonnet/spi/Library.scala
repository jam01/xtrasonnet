package com.datasonnet.spi

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright retention, per Apache 2.0-4.c */
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

import com.datasonnet.header.Header
import com.datasonnet.spi.Library.{dummyPosition, memberOf}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.Obj
import sjsonnet.{EvalScope, Expr, FileScope, Importer, Position, Val, ValScope}

import scala.jdk.CollectionConverters._

object Library {
  val dummyPosition = new Position(null, 0)
  val emptyObj: Obj = Val.Obj.mk(dummyPosition, Seq.empty: _*)

  def memberOf(value: Val): Obj.Member = new Obj.ConstMember(false, Visibility.Normal, value)
}

abstract class Library {
  def namespace(): String

  def functions(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Func]

  def modules(dataFormats: DataFormatService, header: Header, importer: Importer): java.util.Map[String, Val.Obj]

  def libsonnets(): java.util.Set[String]

  protected def moduleFrom(functions: (String, Val.Func)*): Val.Obj = {
    Val.Obj.mk(dummyPosition, functions.map { case (k, v) => (k, memberOf(v)) }: _*)
  }

  protected def makeSimpleFunc(params: java.util.List[String], eval: java.util.function.Function[java.util.List[Val], Val]): Val.Func = {
    val paramIndices = params.asScala.indices
    new Val.Func(dummyPosition, ValScope.empty, Params(params.toArray.asInstanceOf[Array[String]], new Array[Expr](params.size))) {
      override def evalRhs(scope: ValScope, ev: EvalScope, fs: FileScope, pos: Position): Val = eval.apply(paramIndices.map(i => scope.bindings(i).force).asJava)
    }
  }
}
