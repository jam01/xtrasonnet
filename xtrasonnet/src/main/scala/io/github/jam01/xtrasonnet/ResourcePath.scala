package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.ResourcePath.root
import sjsonnet.{Importer, Path}

import scala.collection.mutable

object ResourcePath {
  val root = new ResourcePath("")
  val importer: Importer = new Importer {
    override def resolve(docBase: Path, importName: String): Option[Path] = {
      if (!docBase.isInstanceOf[ResourcePath]) return None

      if (root.equals(docBase)) Some(ResourcePath(importName))
      else if (importName.startsWith("http:") || importName.startsWith("https:")
          || importName.startsWith("file:") || importName.startsWith("classpath:")) { // means not relative
          Some(ResourcePath(importName))
        } else
          Some(docBase / importName)
    }

    override def read(path: Path): Option[String] = {
      val p = path.asInstanceOf[ResourcePath].path
      Some(ResourceResolver.asString(p, null))
    }
  }
}

/**
 * An implementation of sjsonnet.Path that works with commons Java "resources", supporting classpath, files, and http(s)
 *
 * @param path string URI of the desired resource
 */
case class ResourcePath(path: String) extends Path {
  override def parent(): ResourcePath = {
    val idx = path.lastIndexOf('/')
    val parent = if (idx > 0) path.substring(0, idx) else ""
    ResourcePath(parent)
  }

  override def segmentCount(): Int = path.split('/').length

  override def last: String = {
    path.split('/').last
  }

  override def /(s: String): Path =
    if (this.equals(root)) ResourcePath(s)
    else ResourcePath(path + "/" + s)

  override def renderOffsetStr(offset: Int, loadedFileContents: mutable.HashMap[Path, Array[Int]]): String =
    path.toString + ":" + offset

  override def equals(other: Any): Boolean = other match {
    case other: ResourcePath => path.equals(other.path)
    case _ => false
  }

  override def hashCode: Int = path.hashCode()

  override def relativeToString(other: Path): String = {
    val opath = other.asInstanceOf[ResourcePath].path
    if (path.equals(Transformer.main)) path
    else if (path.startsWith(opath)) {
      val bool = opath.endsWith("/")
      path.substring(if (bool) opath.length else opath.length + 1)
    }
    else path
  }

  override def toString: String = path
}
