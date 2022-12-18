package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.Std.builtin
import sjsonnet.{Val, Error}

object Base64 {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("decode", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => new String(java.util.Base64.getDecoder.decode(x.value.toString))
          case x: Val.Str => new String(java.util.Base64.getDecoder.decode(x.value))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("encode", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num =>
            if (x.value % 1 == 0) new String(java.util.Base64.getEncoder.encode(x.value.toInt.toString.getBytes()))
            else new String(java.util.Base64.getEncoder.encode(x.value.toString.getBytes()))
          case x: Val.Str => new String(java.util.Base64.getEncoder.encode(x.value.getBytes()))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    }
  )
}
