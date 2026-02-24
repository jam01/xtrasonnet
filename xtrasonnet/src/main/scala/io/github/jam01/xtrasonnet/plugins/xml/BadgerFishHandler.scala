package io.github.jam01.xtrasonnet.plugins.xml

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
/*-
 * Copyright 2019-2020 the original author or authors. *
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
/*
 * Work covered:
 * - 290693116f36cd82f17edc0ac2384cf0f30c6b41: XML namespace support for reading
 * - 12270ae72d5432b916359b3c1747936ffaf26be1: Merge pull request #73 from datasonnet/ordering-keys
 *
 * Changes made:
 * - 1570c045ab8e750305e1d86206f4cddeadabfedd: conformed badgerfish ordering behavior
 * - de7029978b65a012dfdb8dd32b598e99a9c7708a: renamed currentNS to declaredXmlns, only start new xmlns context when
 *    xmlns found, use new NamespaceDeclarations
 */

import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin
import io.github.jam01.xtrasonnet.plugins.DefaultXMLPlugin.{EffectiveParams, Mode}
import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.{Attributes, SAXParseException}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Val.Obj
import sjsonnet.{Position, Val}

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsJava;

// See {@link scala.xml.parsing.FactoryAdapter}
class BadgerFishHandler(params: EffectiveParams) extends DefaultHandler2 {
  private val dummyPos: Position = Position(null, -1)
  def memberOf(value: Val): Obj.Member = new Obj.ConstMember(false, Visibility.Normal, value)

  def result: Val.Obj = freezeObj(badgerStack.top.obj)
  private val COLON = ":"

  val buffer = new mutable.StringBuilder()
  val badgerStack = new mutable.Stack[BadgerFish]
  var capture: Boolean = false // ignore text until after first start tag found

  private var startContext = true // start a new context at the first xmlns declaration in doc
  private val overrides = new NamespaceDeclarations(params.declarations.asJava)
  private var declaredXmlns: util.LinkedHashMap[String, Val.Obj.Member] = util.LinkedHashMap()

  // root
  badgerStack.push(BadgerFish(mutable.LinkedHashMap.empty))

  // called before startElement when xmlns attributes are present
  override def startPrefixMapping(prefix: String, uri: String): Unit = {
    if (startContext) { // should be true at first xmlns declaration found in doc, or after a new start tag
      overrides.pushContext() // start new context
      startContext = false
      if (!declaredXmlns.isEmpty) declaredXmlns = new util.LinkedHashMap() // re-use map of xmlns -- tradeoff of speed for memory(clear map values)
    }

    val newPrefix = if (prefix.equals("xmlns")) DefaultXMLPlugin.DEFAULT_NS_KEY else overrides.prefix(prefix, uri)
    if (!params.excludeXmlns) declaredXmlns.put(newPrefix, memberOf(Val.Str(dummyPos, uri)))
  }

  override def startElement(uri: String,
                            _localName: String,
                            qname: String,
                            attributes: Attributes): Unit = {
    captureText() // capture text before this start tag for the parent element, no-op if this is the first start tag as capture == false
    capture = true // enable capture after we see any start tag; only needed for the first one, but less expensive to always set it
    startContext = true // signal to startPrefixMapping to start a new context

    val current = mutable.LinkedHashMap.empty[String, BFValue] // builder for this element

    if (!declaredXmlns.isEmpty) {
      current.put(params.xmlnsKey, BFSingle(Val.Obj(dummyPos, declaredXmlns, false, null, null)))
      declaredXmlns = new util.LinkedHashMap()
    }

    // add attributes
    if (!params.excludeAttrs && attributes.getLength > 0) {
      val attrs = new util.LinkedHashMap[String, Val.Obj.Member]

      // TODO: optimize with while-loop
      for (i <- 0 until attributes.getLength) {
        var attrName = attributes.getQName(i)
        val value = attributes.getValue(i)

        if (params.nameform.equalsIgnoreCase(DefaultXMLPlugin.NAME_FORM_LOCAL_VALUE)) {
          attrName = if (params.xmlnsAware) _localName else {
            val colonidx = attrName.indexOf(COLON)
            attrName.substring(colonidx + 1)
          }
        } else {
          attrName = if (params.xmlnsAware) overrides.name(attrName, true) else attrName
        }

        attrs.put(attrName, memberOf(Val.Str(dummyPos, value)))
      }
      current.put(params.attrKey, BFSingle(Val.Obj(dummyPos, attrs, false, null, null)))
    }

    badgerStack.push(BadgerFish(current)) // the current Obj to use in other parser events
  }

  override def characters(ch: Array[Char], offset: Int, length: Int): Unit = {
    if (capture) buffer.appendAll(ch, offset, length)
  }

  override def startCDATA(): Unit = {
    captureText() // capture text before this cdata node
  }

  override def endCDATA(): Unit = { // like captureText, but uses cdata key
    if (buffer.nonEmpty) {
      val idx = badgerStack.top.nextPos
      val string = buffer.toString
      if (params.mode == Mode.extended) { // only extended keeps individual text elements
        badgerStack.top.obj.put(params.cdataKey + idx, BFSingle(Val.Str(dummyPos, string)))
        badgerStack.top.nextPos = idx + 1
      } else {
        val existingText = badgerStack.top.obj.get(params.textKey).map(_.finalizeVal)
        val merged = existingText match {
          case Some(Val.Str(pos, s)) => Val.Str(pos, s + string)
          case _                => Val.Str(dummyPos, string)
        }
        badgerStack.top.obj.put(params.textKey, BFSingle(merged))
      }
      badgerStack.top.hasText = true
    }

    buffer.clear()
  }

  override def endElement(uri: String, _localName: String, qname: String): Unit = {
    captureText() // capture text for the ending element

    var currName = qname
    if (params.nameform.equalsIgnoreCase(DefaultXMLPlugin.NAME_FORM_LOCAL_VALUE)) {
      currName = if (params.xmlnsAware) _localName else {
        val colonidx = qname.indexOf(COLON)
        qname.substring(colonidx + 1)
      }
    } else {
      currName = if (params.xmlnsAware) overrides.name(qname, false) else qname
    }

    val current = badgerStack.pop
    val parent = badgerStack.top

    val pos = parent.nextPos
    if (params.mode == Mode.extended) current.obj.put(params.posKey, BFSingle(Val.Num(dummyPos, pos))) // only extended supports positions
    parent.nextPos = pos + 1

    val currVal: Val = {
      if (Mode.simplified == params.mode) {
        if (current.obj.isEmpty) Val.Null(dummyPos)
        else if (current.obj.size == 1 && current.hasText) current.obj(params.textKey).finalizeVal
        else {
          current.obj.remove(params.textKey)
          freezeObj(current.obj)
        }
      } else freezeObj(current.obj)
    }

    parent.obj.get(currName) match {
      case Some(existing) => parent.obj.put(currName, existing.add(currVal)) // no Val.Arr created yet
      case None => parent.obj.put(currName, BFSingle(currVal))
    }

    capture = badgerStack.size != 1 // stop capturing at root level
    if (!declaredXmlns.isEmpty) { // some xmlns were found
      overrides.popContext()
    }
  }

  def captureText(): Unit = {
    if (capture && buffer.nonEmpty) {
      val idx = badgerStack.top.nextPos
      var string = buffer.toString
      val trimmed = string.trim
      if (trimmed.nonEmpty) {
        string = if (params.trimText) trimmed else string
        if (params.mode == Mode.extended) { // only extended keeps individual text elements
          badgerStack.top.obj.put(params.textKey + idx, BFSingle(Val.Str(dummyPos, string)))
          badgerStack.top.nextPos = idx + 1
        } else {
          val existingText = badgerStack.top.obj.get(params.textKey).map(_.finalizeVal)
          val merged = existingText match {
            case Some(Val.Str(pos, s)) => Val.Str(pos, s + string)
            case _                => Val.Str(dummyPos, string)
          }
          badgerStack.top.obj.put(params.textKey, BFSingle(merged))
        }
        badgerStack.top.hasText = true
      }
    }

    buffer.clear()
  }

  override def warning(ex: SAXParseException): Unit = {}

  override def error(ex: SAXParseException): Unit = printError("Error", ex)

  override def fatalError(ex: SAXParseException): Unit = printError("Fatal Error", ex)

  // TODO: use slf4j
  protected def printError(errtype: String, ex: SAXParseException): Unit =
    Console.withOut(Console.err) {
      val s = "[%s]" + COLON + "%d" + COLON + "%d" + COLON + " %s".format(
        errtype, ex.getLineNumber, ex.getColumnNumber, ex.getMessage)
      Console.println(s)
      Console.flush()
    }

  private def freezeObj(fields: mutable.LinkedHashMap[String, BFValue]): Val.Obj = {
    Val.Obj.mk(dummyPos, fields.iterator.map { case (k, v) => (k, memberOf(v.finalizeVal)) }.toArray)
  }
}

private sealed trait BFValue {
  def add(v: Val): BFValue
  def finalizeVal: Val
}

private final case class BFSingle(v: Val) extends BFValue {
  override def add(v2: Val): BFValue = BFMany(v.pos, mutable.ArrayBuffer(v, v2))
  override def finalizeVal: Val = v
}

private final case class BFMany(pos: Position, buf: mutable.ArrayBuffer[Val]) extends BFValue {
  override def add(v: Val): BFValue = {
    buf.addOne(v)
    this
  }
  override def finalizeVal: Val = Val.Arr(pos, buf.toArray)
}

case class BadgerFish(obj: mutable.LinkedHashMap[String, BFValue], var hasText: Boolean = false, var nextPos: Int = 1)
