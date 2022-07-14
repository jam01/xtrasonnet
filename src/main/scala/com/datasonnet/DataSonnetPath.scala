package com.datasonnet

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

import sjsonnet.{OsPath, Path}

import scala.collection.mutable

/**
 * An implementation of sjsonnet.Path that works with the JVM's ClassPath
 *
 * @param path name of the desired resource
 */
case class DataSonnetPath(path: String) extends Path {
  override def parent(): DataSonnetPath = {
    var parent = path.split('/').dropRight(1).mkString("/")
    if (!"".equals(parent)) parent = parent + "/"
    DataSonnetPath(parent)
  }

  override def segmentCount(): Int = path.split('/').length

  override def last: String = path.split('/').last

  override def /(s: String): Path = DataSonnetPath(path + "/" + s)

  override def renderOffsetStr(offset: Int, loadedFileContents: mutable.HashMap[Path, Array[Int]]): String =
    path + ":" + offset

  override def equals(other: Any): Boolean = other match {
    case other: DataSonnetPath => other.path == path
    case _ => false
  }

  override def hashCode: Int = path.hashCode()

  override def relativeToString(p: Path): String = {
    if (this.parent().equals(p.asInstanceOf[DataSonnetPath].parent())) last else toString
  }

  override def toString: String = path
}
