package io.github.jam01.xtrasonnet.plugins.xml

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin.EffectiveParams
import org.xml.sax.InputSource
import sjsonnet.{EvalScope, Val}

import java.io.*
import javax.xml.parsers.SAXParser

object Source {
  // no fromFile: it opened a stream nobody owned, so every file read leaked a descriptor.
  // XMLLoader.loadFile opens and closes its own.

  def fromInputStream(is: InputStream, params: EffectiveParams): InputSource = {
    val src = new InputSource(is)
    // only when the caller declared one: setEncoding overrides the document's own declaration, so
    // defaulting it here would misread any document that says otherwise
    params.charset.foreach(cs => src.setEncoding(cs.name()))
    src
  }

  def fromReader(reader: Reader): InputSource = new InputSource(reader)

  def fromString(string: String): InputSource = fromReader(new StringReader(string))
}

// See {@link scala.xml.XML}
object XML extends XMLLoader {

  /** Returns an XMLLoader whose load* methods will use the supplied SAXParser. */
  def withSAXParser(p: SAXParser): XMLLoader =
    new XMLLoader {
      override def parser(params: EffectiveParams): SAXParser = p
    }

  /**
   * @param declaredCharset the charset to name in the declaration. Only the caller knows whether the
   *                        output is bytes it is about to encode, or a String, which carries none.
   */
  def writeXML(sb: java.io.Writer, root: (String, Val), effParams: EffectiveParams,
               declaredCharset: java.nio.charset.Charset)(implicit ev: EvalScope): Unit = {
    // the declaration must name the charset the bytes are actually encoded with
    if (!effParams.omitDeclaration) sb.append("<?xml version='" + effParams.xmlVer + "' encoding='" + declaredCharset.name() + "'?>")
    new BadgerFishVisitor(effParams).serialize(root._1, root._2, sb).toString
  }
}
