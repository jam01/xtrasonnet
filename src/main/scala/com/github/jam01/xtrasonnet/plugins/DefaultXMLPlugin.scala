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
import scala.jdk.CollectionConverters.MapHasAsScala

// See: http://wiki.open311.org/JSON_and_XML_Conversion/#the-badgerfish-convention
// http://www.sklar.com/badgerfish/
// http://dropbox.ashlock.us/open311/json-xml/
object DefaultXMLPlugin extends BasePlugin {
  val PARAM_MODE = "mode"
  val SIMPLIFIED_MODE_VALUE = "simplified"
  val BADGER_MODE_VALUE = "badger"
  val EXTENDED_MODE_VALUE = "extended"

  // convention key and char params
  val PARAM_TEXT_KEY = "textkey"
  val PARAM_ATTRIBUTE_KEY = "attrkey"
  val PARAM_CDATA_KEY = "cdatakey"
  val PARAM_ORDER_KEY = "orderkey"
  val PARAM_XMLNS_KEY = "xmlnskey"
  val PARAM_QNAME_SEP = "qnamesep"

  // default keys and chars
  private val DEFAULT_TEXT_KEY = "_text"
  private val DEFAULT_ATTRIBUTE_KEY = "_attr"
  private val DEFAULT_CDATA_KEY = "_cdata"
  private val DEFAULT_ORDER_KEY = "_pos"
  private val DEFAULT_XMLNS_KEY = "_xmlns"
  private val DEFAULT_QNAME_SEP = ":"
  private val DEFAULT_XML_VERSION = "1.0"
  val DEFAULT_NS_KEY = "_def"

  val PARAM_XMLNS_DECLARATIONS = "xmlns\\..*"

  // parsing / writing instructions
  val PARAM_NAME_FORM = "nameform"
  val NAME_FORM_QNAME_VALUE = "qname"
  val NAME_FORM_LOCAL_VALUE = "local-name"

  val PARAM_EXCLUDE = "exclude"
  val EXCLUDE_ATTRIBUTES_VALUE = "attrs"
  val EXCLUDE_XML_DECLARATION_VALUE = "xml-declaration"

  val PARAM_INCLUDE = "include"
  val INCLUDE_COMMENTS_VALUE = "comments"

  // read only
  val PARAM_ARR_ELEMENTS = "arrelements"
  val PARAM_TRIM_TEXT = "trimtext"
  val PARAM_XMLNS_AWARE = "xmlnsaware"

  // write only
  val PARAM_XML_VERSION = "xmlversion"
  val PARAM_EMPTY_TAGS = "emptytags"

  val EMPTY_TAGS_NULL_VALUE = "null"
  val EMPTY_TAGS_STRING_VALUE = "string"
  val EMPTY_TAGS_OBJECT_VALUE = "object"

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
  writerParams.add(PARAM_QNAME_SEP)
  writerParams.add(PARAM_XMLNS_DECLARATIONS)
  writerParams.add(PARAM_XML_VERSION)
  writerParams.add(PARAM_EMPTY_TAGS)
  writerParams.add(PARAM_EXCLUDE)

  readerParams.add(PARAM_MODE)
  readerParams.add(PARAM_TEXT_KEY)
  readerParams.add(PARAM_ATTRIBUTE_KEY)
  readerParams.add(PARAM_CDATA_KEY)
  readerParams.add(PARAM_ORDER_KEY)
  readerParams.add(PARAM_XMLNS_KEY)
  readerParams.add(PARAM_XMLNS_AWARE)
  readerParams.add(PARAM_QNAME_SEP)
  readerParams.add(PARAM_XMLNS_DECLARATIONS)
  readerParams.add(PARAM_TRIM_TEXT)
  readerParams.add(PARAM_EXCLUDE)

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

    val inputAsObj: mutable.Map[String, Value] = input.obj.asInstanceOf[mutable.Map[String, Value]]

    if (inputAsObj.keys.size > 1) {
      throw new PluginException("Object must have only one root element")
    }

    if (targetType.isAssignableFrom(classOf[String])) {
      val writer = new StringWriter()
      XML.writeXML(writer, inputAsObj.head, effectiveParams)

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
    val simplified, extended, badger = Value
  }

  case class EffectiveParams(mode: Mode.Value,
                             excludeAttrs: Boolean, includeComments: Boolean,
                             textKey: String, cdataKey: String, attrKey: String, orderKey: String, xmlnsKey: String,
                             xmlnsAware: Boolean, declarations: Map[String, String], qnameSep: String,
                             omitDeclaration: Boolean, xmlVer: String,
                             emptyTagsStr: Boolean, emptyTagsNull: Boolean, emptyTagsObj: Boolean, arrElements: java.util.List[String],
                             nameform: String, trimText: Boolean)

  object EffectiveParams {
    def apply(mediaType: MediaType): EffectiveParams = {
      val qnameChar = mediaType.getParameter(PARAM_QNAME_SEP, DEFAULT_QNAME_SEP)
      val textKey = mediaType.getParameter(PARAM_TEXT_KEY, DEFAULT_TEXT_KEY)
      val cdataKey = mediaType.getParameter(PARAM_CDATA_KEY, DEFAULT_CDATA_KEY)
      val attrKey = mediaType.getParameter(PARAM_ATTRIBUTE_KEY, DEFAULT_ATTRIBUTE_KEY)
      val orderKey = mediaType.getParameter(PARAM_ORDER_KEY, DEFAULT_ORDER_KEY)
      val xmlVer = mediaType.getParameter(PARAM_XML_VERSION, DEFAULT_XML_VERSION)
      val xmlnsKey = mediaType.getParameter(PARAM_XMLNS_KEY, DEFAULT_XMLNS_KEY)
      val emptyTags = mediaType.getParameterAsList(PARAM_EMPTY_TAGS, Collections.emptyList())
      val declarations: Map[String, String] = mediaType.getParameters.asScala.toList
        .filter(entryVal => entryVal._1.matches(PARAM_XMLNS_DECLARATIONS))
        .map(entryVal => (entryVal._2, entryVal._1.substring(PARAM_XMLNS_DECLARATIONS.length - 3)))
        .map(entry => if (entry._2 == DEFAULT_NS_KEY) (entry._1, "") else entry)
        .toMap
      val mode = Mode.withName(mediaType.getParameter(PARAM_MODE, BADGER_MODE_VALUE))
      val xmlnsAware = mediaType.getParameterAsBoolean(PARAM_XMLNS_AWARE, true)
      val nameForm = if (mediaType.containsParameter(PARAM_NAME_FORM)) mediaType.getParameter(PARAM_NAME_FORM)
        else if (mode == Mode.simplified) NAME_FORM_LOCAL_VALUE else NAME_FORM_QNAME_VALUE

      val exclude = mediaType.getParameterAsList(PARAM_EXCLUDE, Collections.emptyList())
      val include = mediaType.getParameterAsList(PARAM_INCLUDE, Collections.emptyList())

      val includeComments = include.contains(INCLUDE_COMMENTS_VALUE) || mode == Mode.extended
      val excludeAttrs = exclude.contains(EXCLUDE_ATTRIBUTES_VALUE) || mode == Mode.simplified
      val omitDeclaration = exclude.contains(EXCLUDE_XML_DECLARATION_VALUE)

      val trimText = if (mediaType.containsParameter(PARAM_TRIM_TEXT)) mediaType.getParameterAsBoolean(PARAM_TRIM_TEXT, true)
      else if (mode == Mode.extended) false
      else true

      new EffectiveParams(mode,
        excludeAttrs, includeComments,
        textKey, cdataKey, attrKey, orderKey, xmlnsKey,
        xmlnsAware, declarations, qnameChar,
        omitDeclaration, xmlVer,
        emptyTags.contains(EMPTY_TAGS_NULL_VALUE), emptyTags.contains(EMPTY_TAGS_STRING_VALUE), emptyTags.contains(EMPTY_TAGS_OBJECT_VALUE), Collections.emptyList(),
        nameForm, trimText)
    }
  }
}
