package io.github.jam01.xtrasonnet.plugins.xml

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin.EffectiveParams
import org.xml.sax.InputSource

import java.io._
import java.nio.charset.Charset
import javax.xml.parsers.SAXParser

object Source {
  def fromFile(file: File) = new InputSource(new FileInputStream(file))

  def fromInputStream(is: InputStream): InputSource = new InputSource(is)

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

  def writeXML(sb: java.io.Writer, root: (String, ujson.Value), effParams: EffectiveParams): Unit = {
    // TODO: get charset from params
    if (!effParams.omitDeclaration) sb.append("<?xml version='" + effParams.xmlVer + "' encoding='" + Charset.defaultCharset().displayName() + "'?>")
    new BadgerFishWriter(effParams).serialize(root._1, root._2, sb).toString
  }
}
