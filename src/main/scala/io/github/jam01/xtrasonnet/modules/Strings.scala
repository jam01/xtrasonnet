package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.Std.builtin
import sjsonnet.{Val, Error}

import java.text.DecimalFormat
import scala.collection.mutable

object Strings {
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
        "([A-Z])|[\\s-_]+(\\w)".r("head", "tail").replaceAllIn(str, found => {
          if (found.group(2) != null) found.group(2).toUpperCase
          else found.group(1).toLowerCase
        })
    },

    builtin("capitalize", "str") {
      (_, _, str: String) =>
        //regex fo _CHAR
        val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
        val middleRegex = "([a-z])([A-Z])".r("end", "start")

        //Start string at first non underscore, lower case xt
        var temp = str.substring("[0-9A-Za-z]".r.findFirstMatchIn(str).map(_.start).toList.head)
        temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toUpper.toString)

        //replace and uppercase
        temp = regex.replaceAllIn(temp, m => s" ${(m group "two").toUpperCase() + (m group "three").toLowerCase()}")
        temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"} ${(m group "start").toUpperCase()}")

        temp
    },

    builtin("charCode", "str") {
      (_, _, str: String) =>
        str.codePointAt(0)
    },

    builtin("charCodeAt", "str", "num") {
      (_, _, str: String, num: Int) =>
        str.codePointAt(num)
    },

    builtin("toKebabCase", "str") {
      (_, _, str: String) =>
        //regex fo _CHAR
        val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
        val middleRegex = "([a-z])([A-Z])".r("end", "start")

        //Start string at first non underscore, lower case xt
        var temp = str

        //replace and uppercase
        temp = regex.replaceAllIn(temp, m => s"-${(m group "two") + (m group "three").toLowerCase()}")
        temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}-${m group "start"}")

        temp.toLowerCase()
    },

    builtin("ofCharCode", "num") {
      (_, _, num: Int) =>
        String.valueOf(num.asInstanceOf[Char])
    },

    builtin("isAlpha", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => "^[A-Za-z]+$".r.matches(value.value)
          case _: Val.Num => false
          case _: Val.Bool => true
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isAlphanumeric", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => "^[A-Za-z0-9]+$".r.matches(value.value)
          case _: Val.Num => true
          case _: Val.Bool => true
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isLowerCase", "str") {
      (_, _, str: String) => "^[a-z]+$".r.matches(str)
    },

    builtin("isNumeric", "str") {
      (_, _, str: Val) =>
        str match {
          case value: Val.Str => "^[0-9]+$".r.matches(value.value)
          case _: Val.Num => true
          case _: Val.Bool => false
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("isUpperCase", "str") {
      (_, _, str: String) => "^[A-Z]+$".r.matches(str)
    },

    builtin("leftPad", "str", "offset", "pad") {
      (_, _, str: Val, size: Int, pad: String) =>
        str match {
          case str: Val.Str => ("%" + size + "s").format(str.value).replace(" ", pad.substring(0, 1))
          //TODO change to use sjsonnet's Format and DecimalFormat
          case x: Val.Num => ("%" + size + "s").format(new DecimalFormat("0.#").format(x.value)).replace(" ", pad.substring(0, 1))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("numOrdinalOf", "num") {
      (_, _, num: Val) =>
        val str = num match { //convert number value to string
          case value: Val.Str =>
            if ("^[0-9]+$".r.matches(value.value)) value.value
            else Error.fail("Expected Number, got: " + value.value)
          case value: Val.Num => value.value.toInt.toString
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
      (_, _, value: Val, offset: Int, pad: String) =>
        value match {
          case str: Val.Str => str.value.padTo(offset, pad.charAt(0))
          //TODO change to use sjsonnet's Format and DecimalFormat
          case x: Val.Num => new DecimalFormat("0.#").format(x.value).padTo(offset, pad.charAt(0))
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
        s.substring(
          s.indexOf(sep) match {
            case -1 => s.length
            case i => if (sep.equals("")) i else i + 1
          }
        )
    },

    builtin("substringAfterLast", "value", "sep") {
      (_, _, s: String, sep: String) =>
        val split = s.split(sep)
        if (sep.equals("")) ""
        else if (split.length == 1) ""
        else split(split.length - 1)
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
        //regex fo _CHAR
        val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
        val middleRegex = "([a-z])([A-Z])".r("end", "start")

        //Start string at first non underscore, lower case xt
        var temp = str.substring("[0-9A-Za-z]".r.findFirstMatchIn(str).map(_.start).toList.head)
        temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

        //replace and uppercase
        temp = regex.replaceAllIn(temp, m => s"_${(m group "two") + (m group "three")}")
        temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}_${m group "start"}")

        temp.toLowerCase
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

    // todo: and these?
    //      builtin("scan", "str", "regex") {
    //        (pos, ev, str: String, regex: String) =>
    //          new Val.Arr(pos, regex.r.findAllMatchIn(str).map(item => {
    //            new Val.Arr(pos, (0 to item.groupCount).map(i => Val.Str(pos, item.group(i))).toArray)
    //          }).toArray
    //          )
    //      },
    //
    //      builtin("match", "string", "regex") {
    //        (pos, _, string: String, regex: String) =>
    //          val out = new ArrayBuffer[Lazy]
    //          regex.r.findAllMatchIn(string).foreach(
    //            word => (0 to word.groupCount).foreach(index => out += Val.Str(pos, word.group(index)))
    //          )
    //          new Val.Arr(pos, out.toArray)
    //      },
    //
    //      builtin("matches", "string", "regex") {
    //        (pos, ev, string: String, regex: String) =>
    //          regex.r.matches(string);
    //      }
  )
}
