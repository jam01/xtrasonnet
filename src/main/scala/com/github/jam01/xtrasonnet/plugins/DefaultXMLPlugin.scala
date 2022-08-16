package com.github.jam01.xtrasonnet.plugins

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.document.Document.BasicDocument

import java.io._
import java.net.URL
import java.nio.charset.Charset
import com.github.jam01.xtrasonnet.document.{Document, MediaType, MediaTypes}
import com.github.jam01.xtrasonnet.plugins.xml.XML
import com.github.jam01.xtrasonnet.spi.{BasePlugin, PluginException}
import ujson.Value

import java.util.Collections
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsScala}

// See: http://wiki.open311.org/JSON_and_XML_Conversion/#the-badgerfish-convention
// http://www.sklar.com/badgerfish/
// http://dropbox.ashlock.us/open311/json-xml/
object DefaultXMLPlugin extends BasePlugin {
  val SIMPLE_MODE = "simple"
  val BASIC_MODE = "basic"
  val EXTENDED_MODE = "extended"
  val DEFAULT_NS_KEY = "def"
  private val DEFAULT_TEXT_KEY = "_text"
  private val DEFAULT_ATTRIBUTE_KEY = "_attr"
  private val DEFAULT_CDATA_KEY = "_cdata"
  private val DEFAULT_ORDER_KEY = "_idx"
  private val DEFAULT_XMLNS_KEY = "_xmlns"
  private val DEFAULT_QNAME_CHAR = ':'
  private val DEFAULT_XML_VERSION = "1.0"

  val PARAM_MODE = "mode"
  val PARAM_TEXT_KEY = "textkey"
  val PARAM_ATTRIBUTE_KEY = "attributechar"
  val PARAM_CDATA_KEY = "cdatakey"
  val PARAM_ORDER_KEY = "orderkey"
  val PARAM_XMLNS_KEY = "xmlnskey"
  val PARAM_QNAME_CHAR = "qnamechar"
  val PARAM_NAMESPACE_QNAME = "xmlns\\..*"
  val PARAM_OMIT_XML_DECLARATION = "omitdeclaration"
  val PARAM_XML_VERSION = "xmlversion"
  val PARAM_EMPTY_TAGS = "emptytags"

  supportedTypes.add(MediaTypes.APPLICATION_XML)
  supportedTypes.add(MediaTypes.TEXT_XML)
  supportedTypes.add(new MediaType("application", "*+xml"))

  writerParams.add(BasePlugin.PARAM_FORMAT)
  writerParams.add(PARAM_MODE)
  writerParams.add(PARAM_TEXT_KEY)
  writerParams.add(PARAM_ATTRIBUTE_KEY)
  writerParams.add(PARAM_CDATA_KEY)
  writerParams.add(PARAM_ORDER_KEY)
  writerParams.add(PARAM_XMLNS_KEY)
  writerParams.add(PARAM_QNAME_CHAR)
  writerParams.add(PARAM_NAMESPACE_QNAME)
  writerParams.add(PARAM_OMIT_XML_DECLARATION)
  writerParams.add(PARAM_XML_VERSION)
  writerParams.add(PARAM_EMPTY_TAGS)

  readerParams.add(PARAM_MODE)
  readerParams.add(PARAM_TEXT_KEY)
  readerParams.add(PARAM_ATTRIBUTE_KEY)
  readerParams.add(PARAM_CDATA_KEY)
  readerParams.add(PARAM_ORDER_KEY)
  readerParams.add(PARAM_XMLNS_KEY)
  readerParams.add(PARAM_QNAME_CHAR)
  readerParams.add(PARAM_NAMESPACE_QNAME)

  readerSupportedClasses.add(classOf[String].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.net.URL].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.io.File].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.io.InputStream].asInstanceOf[java.lang.Class[_]])

  writerSupportedClasses.add(classOf[String].asInstanceOf[java.lang.Class[_]])
  writerSupportedClasses.add(classOf[OutputStream].asInstanceOf[java.lang.Class[_]])

  @throws[PluginException]
  override def read(doc: Document[_]): Value = {
    if (doc.getContent == null) return ujson.Null

    val effectiveParams = EffectiveParams(doc.getMediaType)

    doc.getContent.getClass match {
      case cls if classOf[String].isAssignableFrom(cls) => XML.loadString(doc.getContent.asInstanceOf[String], effectiveParams)
      case cls if classOf[URL].isAssignableFrom(cls) => XML.load(doc.getContent.asInstanceOf[URL], effectiveParams)
      case cls if classOf[File].isAssignableFrom(cls) => XML.loadFile(doc.getContent.asInstanceOf[File], effectiveParams)
      case cls if classOf[InputStream].isAssignableFrom(cls) => XML.load(doc.getContent.asInstanceOf[InputStream], effectiveParams)
      case _ => throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"))
    }
  }

  @throws[PluginException]
  override def write[T](input: Value, mediaType: MediaType, targetType: Class[T]): Document[T] = {
    if (!input.isInstanceOf[ujson.Obj]) {
      throw new PluginException("Input for XML writer must be an Object, got " + input.getClass)
    }

    val effectiveParams = EffectiveParams(mediaType)
    var charset = mediaType.getCharset
    if (charset == null) {
      charset = Charset.defaultCharset
    }

    var inputAsObj: mutable.Map[String, Value] = input.obj.asInstanceOf[mutable.Map[String, Value]]

    if (inputAsObj.keys.size > 1) {
      throw new PluginException("Object must have only one root element")
    }

    if (targetType.isAssignableFrom(classOf[String])) {
      val writer = new StringWriter()
      XML.writeXML(writer, inputAsObj.head.asInstanceOf[(String, ujson.Obj)], effectiveParams)

      new BasicDocument(writer.toString, MediaTypes.APPLICATION_XML).asInstanceOf[Document[T]]
    }

    else if (targetType.isAssignableFrom(classOf[OutputStream])) {
      val out = new BufferedOutputStream(new ByteArrayOutputStream)
      XML.writeXML(new OutputStreamWriter(out, charset), inputAsObj.head.asInstanceOf[(String, ujson.Obj)], effectiveParams)

      new BasicDocument(out, MediaTypes.APPLICATION_XML).asInstanceOf[Document[T]]
    }

    else {
      throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"))
    }
  }

  object Mode extends Enumeration {
    val simple, extended, basic = Value
  }

  case class EffectiveParams(qnameChar: Char, textKey: String,
                             cdataKey: String, attrKey: String,
                             orderKey: String, omitDeclaration: Boolean,
                             xmlVer: String, xmlnsKey: String, emptyTags: Boolean,
                             declarations: Map[String, String], mode: Mode.Value)

  object EffectiveParams {
    def apply(mediaType: MediaType): EffectiveParams = {
      val qnameChar = DefaultCSVPlugin.paramAsChar(mediaType, PARAM_QNAME_CHAR, DEFAULT_QNAME_CHAR)
      val textKey = DefaultCSVPlugin.paramOr(mediaType, PARAM_TEXT_KEY, DEFAULT_TEXT_KEY)
      val cdataKey = DefaultCSVPlugin.paramOr(mediaType, PARAM_CDATA_KEY, DEFAULT_CDATA_KEY)
      val attrKey = DefaultCSVPlugin.paramOr(mediaType, PARAM_ATTRIBUTE_KEY, DEFAULT_ATTRIBUTE_KEY)
      val orderKey = DefaultCSVPlugin.paramOr(mediaType, PARAM_ORDER_KEY, DEFAULT_ORDER_KEY)
      val omitDeclaration = DefaultCSVPlugin.paramAsBoolean(mediaType, PARAM_OMIT_XML_DECLARATION, false)
      val xmlVer = DefaultCSVPlugin.paramOr(mediaType, PARAM_XML_VERSION, DEFAULT_XML_VERSION)
      val xmlnsKey = DefaultCSVPlugin.paramOr(mediaType, PARAM_XMLNS_KEY, DEFAULT_XMLNS_KEY)
      val emptyTag = DefaultCSVPlugin.paramAsBoolean(mediaType, PARAM_EMPTY_TAGS, false)
      val declarations: Map[String, String] = mediaType.getParameters.asScala.toList
        .filter(entryVal => entryVal._1.matches(PARAM_NAMESPACE_QNAME))
        .map(entryVal => (entryVal._2, entryVal._1.substring(PARAM_NAMESPACE_QNAME.length - 3)))
        .map(entry => if (entry._2 == DEFAULT_NS_KEY) (entry._1, "") else entry)
        .toMap
      val mode = Mode.withName(DefaultCSVPlugin.paramOr(mediaType, PARAM_MODE, SIMPLE_MODE))

      EffectiveParams(qnameChar, textKey, cdataKey, attrKey, orderKey, omitDeclaration, xmlVer, xmlnsKey, emptyTag, declarations, mode)
    }
  }
}
