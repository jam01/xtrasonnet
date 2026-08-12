package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, Lazy, Materializer, Position, Val}

import java.util.regex.{Matcher, Pattern, PatternSyntaxException}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Strings extends AbstractFunctionModule {
  override def name: String = "strings"

  // hoisted so they compile once rather than on every call
  private val alphanumeric = "[0-9A-Za-z]".r
  private val wordBoundary = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
  private val caseBoundary = "([a-z])([A-Z])".r("end", "start")
  private val camelBoundary = "([A-Z])|[\\s-_]+(\\w)".r("head", "tail")
  private val isAlphaPattern = "^[A-Za-z]+$".r
  private val isAlphanumericPattern = "^[A-Za-z0-9]+$".r
  private val isLowerCasePattern = "^[a-z]+$".r
  private val isNumericPattern = "^[0-9]+$".r
  private val isUpperCasePattern = "^[A-Z]+$".r

  /**
   * The character a pad argument stands for: both pad functions use only its first character.
   */
  private def padChar(pad: String): Char = {
    if (pad.isEmpty) Error.fail("Expected a non-empty pad")
    pad.charAt(0)
  }

  /**
   * Prepends `size - value.length` pad characters to value, or returns value unchanged when it is already
   * that long.
   *
   * The value itself is never altered, and a size it already meets returns it unchanged -- including a
   * negative or zero size.
   */
  private def padStart(value: String, size: Int, pad: String): String = {
    val fill = padChar(pad)
    if (value.length >= size) value
    else {
      val builder = new java.lang.StringBuilder(size)
      var i = value.length
      while (i < size) {
        builder.append(fill)
        i = i + 1
      }
      builder.append(value).toString
    }
  }

  val functions: Seq[(String, Val.Func)] = Seq(

    builtin("appendIfMissing", "str1", "str2") {
      (_, _, str: String, append: String) =>
        var ret = str
        if (!ret.endsWith(append)) {
          ret = ret + append
        }
        ret
    },

    builtin("toCamelCase", "str") {
      (_, _, str: String) =>
        //regex fo _CHAR
        camelBoundary.replaceAllIn(str, found => {
          if (found.group(2) != null) found.group(2).toUpperCase
          else found.group(1).toLowerCase
        })
    },

    builtin("capitalize", "str") {
      (_, _, str: String) =>
        //Start string at first non underscore, lower case xt
        // a str with no alphanumeric character has nothing to capitalize, so it is returned as is
        alphanumeric.findFirstMatchIn(str).map(_.start) match {
          case None => str
          case Some(start) =>
            var temp = str.substring(start)
            temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toUpper.toString)

            //replace and uppercase
            temp = wordBoundary.replaceAllIn(temp, m => s" ${(m group "two").toUpperCase() + (m group "three").toLowerCase()}")
            temp = caseBoundary.replaceAllIn(temp, m => s"${m group "end"} ${(m group "start").toUpperCase()}")

            temp
        }
    },

    builtin("charCode", "str") {
      (_, _, str: String) =>
        if (str.isEmpty) Error.fail("Expected a non-empty String")
        str.codePointAt(0)
    },

    builtin("charCodeAt", "str", "num") {
      (_, _, str: String, num: Int) =>
        if (num < 0 || num >= str.length) Error.fail("Expected an index within [0, " + str.length + "), got: " + num)
        str.codePointAt(num)
    },

    builtin("toKebabCase", "str") {
      (_, _, str: String) =>
        //Start string at first non underscore, lower case xt
        var temp = str

        //replace and uppercase
        temp = wordBoundary.replaceAllIn(temp, m => s"-${(m group "two") + (m group "three").toLowerCase()}")
        temp = caseBoundary.replaceAllIn(temp, m => s"${m group "end"}-${m group "start"}")

        temp.toLowerCase()
    },

    builtin("ofCharCode", "num") {
      (_, _, num: Int) =>
        String.valueOf(num.asInstanceOf[Char])
    },

    builtin("isAlpha", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => isAlphaPattern.matches(value.value)
          case _: Val.Num => false
          case _: Val.Bool => true
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isAlphanumeric", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => isAlphanumericPattern.matches(value.value)
          case _: Val.Num => true
          case _: Val.Bool => true
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isLowerCase", "str") {
      (_, _, str: String) => isLowerCasePattern.matches(str)
    },

    builtin("isNumeric", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => isNumericPattern.matches(value.value)
          case _: Val.Num => true
          case _: Val.Bool => false
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isUpperCase", "str") {
      (_, _, str: String) => isUpperCasePattern.matches(str)
    },

    builtin("leftPad", "str", "offset", "pad") {
      (_, ev, str: Val, size: Int, pad: String) =>
        str match {
          case str: Val.Str => padStart(str.value, size, pad)
          case x: Val.Num => padStart(Materializer.stringify(x)(ev), size, pad)
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("numOrdinalOf", "num") {
      (_, _, num: Val) =>
        val str = num match { //convert number value to string
          case value: Val.Str =>
            if (isNumericPattern.matches(value.value)) value.value
            else Error.fail("Expected Number, got: " + value.value)
          case value: Val.Num => value.asInt.toString
          case _ => Error.fail("Expected Number, got: " + num.prettyName)
        }
        if (str.endsWith("11") || str.endsWith("12") || str.endsWith("13")) str + "th"
        else {
          if (str.endsWith("1")) str + "st"
          else if (str.endsWith("2")) str + "nd"
          else if (str.endsWith("3")) str + "rd"
          else str + "th"
        }
    },

    builtin("pluralize", "value") {
      (_, _, str: String) =>
        val comparator = str.toLowerCase()
        val specialSList = List("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        if (specialSList.contains(comparator)) {
          str + "s"
        }
        else if (comparator.isEmpty) ""
        else {
          if (comparator.endsWith("y")) str.substring(0, str.length - 1) + "ies"
          else if (comparator.endsWith("x")) str + "es"
          else str + "s"
        }
    },

    builtin("prependIfMissing", "str1", "str2") {
      (_, _, str: String, append: String) =>
        var ret = str
        if (!ret.startsWith(append)) {
          ret = append + ret
        }
        ret
    },

    builtin("repeat", "str", "num") {
      (_, _, str: String, num: Int) =>
        var i = 0
        val builder = new mutable.StringBuilder("")
        while (i < num) {
          builder.append(str)
          i = i + 1
        }
        builder.toString
    },

    builtin("rightPad", "str", "offset", "pad") {
      (_, ev, value: Val, offset: Int, pad: String) =>
        value match {
          case str: Val.Str => str.value.padTo(offset, padChar(pad))
          case x: Val.Num => Materializer.stringify(x)(ev).padTo(offset, padChar(pad))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("singularize", "value") {
      (_, _, s: String) =>
        if (s.endsWith("ies"))
          s.substring(0, s.length - 3) + "y"
        else if (s.endsWith("es"))
          s.substring(0, s.length - 2)
        else
          s.substring(0, s.length - 1)
    },

    builtin("substringAfter", "value", "sep") {
      (_, _, s: String, sep: String) =>
        // sep.length, not 1: a multi character separator left its tail in the result
        s.indexOf(sep) match {
          case -1 => ""
          case i => s.substring(i + sep.length)
        }
    },

    builtin("substringAfterLast", "value", "sep") {
      (_, _, s: String, sep: String) =>
        // lastIndexOf rather than split: split treats sep as a regex, unlike every sibling here
        s.lastIndexOf(sep) match {
          case -1 => ""
          case i => s.substring(i + sep.length)
        }
    },

    builtin("substringBefore", "value", "sep") {
      (_, _, s: String, sep: String) =>
        s.substring(0,
          s.indexOf(sep) match {
            case -1 => 0
            case x => x
          }
        )
    },

    builtin("substringBeforeLast", "value", "sep") {
      (_, _, s: String, sep: String) =>
        s.substring(0,
          s.lastIndexOf(sep) match {
            case -1 => 0
            case x => x
          }
        )
    },

    builtin("toSnakeCase", "str") {
      (_, _, str: String) =>
        //Start string at first non underscore, lower case xt
        // as in capitalize, a str with no alphanumeric character is returned as is
        alphanumeric.findFirstMatchIn(str).map(_.start) match {
          case None => str
          case Some(start) =>
            var temp = str.substring(start)
            temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

            //replace and uppercase
            temp = wordBoundary.replaceAllIn(temp, m => s"_${(m group "two") + (m group "three")}")
            temp = caseBoundary.replaceAllIn(temp, m => s"${m group "end"}_${m group "start"}")

            temp.toLowerCase
        }
    },

    builtin("unwrap", "value", "wrapper") {
      (_, _, str: String, wrapper: String) =>
        val starts = str.startsWith(wrapper)
        val ends = str.endsWith(wrapper)
        if (starts && ends) str.substring(0 + wrapper.length, str.length - wrapper.length)
        else if (starts) str.substring(0 + wrapper.length, str.length) + wrapper
        else if (ends) wrapper + str.substring(0, str.length - wrapper.length)
        else str
    },

    builtin("wrapIfMissing", "value", "wrapper") {
      (_, _, str: String, wrapper: String) =>
        val ret = new mutable.StringBuilder(str)
        if (!str.startsWith(wrapper)) ret.insert(0, wrapper)
        if (!str.endsWith(wrapper)) ret.append(wrapper)
        ret.toString()
    },

    builtin("wrap", "value", "wrapper") {
      (_, _, str: String, wrapper: String) => wrapper + str + wrapper
    },

    builtin("match", "str", "regex") {
      (pos, _, str: String, regex: String) =>
        val matcher = compile(regex).matcher(str)
        if (!matcher.matches()) Val.Null(pos)
        else Val.Arr(pos, groupsOf(matcher, pos))
    },

    builtin("matches", "str", "regex") {
      (_, _, str: String, regex: String) => compile(regex).matcher(str).matches()
    },

    builtin("scan", "str", "regex") {
      (pos, _, str: String, regex: String) =>
        val matcher = compile(regex).matcher(str)
        val out = new ArrayBuffer[Lazy]()
        while (matcher.find()) out.append(Val.Arr(pos, groupsOf(matcher, pos)))
        Val.Arr(pos, out.toArray)
    }
  )

  private def compile(regex: String): Pattern =
    try Pattern.compile(regex) catch {
      case e: PatternSyntaxException => Error.fail("Invalid regular expression: " + e.getMessage)
    }

  /** The current match as [entire match, capture groups...], null for groups that didn't participate. */
  private def groupsOf(matcher: Matcher, pos: Position): Array[Lazy] = {
    val groups = new Array[Lazy](matcher.groupCount() + 1)
    var i = 0
    while (i <= matcher.groupCount()) {
      val group = matcher.group(i)
      groups(i) = if (group == null) Val.Null(pos) else Val.Str(pos, group)
      i = i + 1
    }
    groups
  }
}
