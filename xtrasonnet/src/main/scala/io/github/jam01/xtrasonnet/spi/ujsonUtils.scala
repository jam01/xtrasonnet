package io.github.jam01.xtrasonnet.spi

/*-
 * Copyright 2022-2023 Jose Montoya.
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
// TODO: state changes

import ujson.{Arr, Bool, Null, Num, Obj, Readable, Str, Value}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import scala.util.control.TailCalls.{TailRec, done, tailcall}

object ujsonUtils {
  def strValueOf(str: String): Str = Str(str)

  def stringValueOf(value: ujson.Value): String = String.valueOf(value.value)

  def parse(jsonData: String): Value = ujson.read(ujson.Readable.fromString(jsonData))

  def read(s: Readable, trace: Boolean = false): Value.Value = ujson.read(s, trace)

  def write(t: Value.Value,
            indent: Int = -1,
            escapeUnicode: Boolean = false): String = ujson.write(t, indent, escapeUnicode)

  def writeTo(t: Value.Value,
              out: java.io.Writer,
              indent: Int = -1,
              escapeUnicode: Boolean = false): Unit = ujson.writeTo(t, out, indent, escapeUnicode)

  def objectOf(node: ujson.Value): java.lang.Object = node match {
    case Null => null
    case Bool(value) => value.asInstanceOf[java.lang.Boolean]
    case Num(value) => value.asInstanceOf[java.lang.Double]
    case Str(value) => value
    case Obj(value) => value.map(keyVal => (keyVal._1, objectOf(keyVal._2))).asJava
    case Arr(value) => value.map(objectOf).asJava
  }

  // Stack safe implementation using Trampolines for deeply nested objects
  // See: https://stackoverflow.com/a/37585818/4814697
  // https://stackoverflow.com/a/55047640/4814697
  def deepObjectOf(node: Value): java.lang.Object = {
    def tailrecObjFrom(node: Value): TailRec[java.lang.Object] = {
      node match {
        case Null => done(null)
        case Bool(value) => done(value.asInstanceOf[java.lang.Boolean])
        case Num(value) => done(value.asInstanceOf[java.lang.Double])
        case Str(value) => done(value)
        case Obj(value) => value.toList.foldRight(done(new ArrayBuffer[(String, java.lang.Object)])) {
          (keyVal, tailrecEntrySet) =>
            for {
              entrySet <- tailrecEntrySet
              javaObj <- tailcall(tailrecObjFrom(keyVal._2))
            } yield entrySet.prepended((keyVal._1, javaObj))
        }.map(list => list.toMap.asJava)
        case Arr(value) => value.foldRight(done(List.empty[java.lang.Object])) {
          (item, tailrecList) =>
            for {
              list <- tailrecList
              javaObj <- tailcall(tailrecObjFrom(item))
            } yield list.::(javaObj)
        }.map(list => list.asJava)
      }
    }

    tailrecObjFrom(node).result
  }
}
