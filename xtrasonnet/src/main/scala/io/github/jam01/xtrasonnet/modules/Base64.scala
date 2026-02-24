package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, NumberMath, Val}

object Base64 extends AbstractFunctionModule {
  override def name: String = "base64"
  
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("decode", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => new String(java.util.Base64.getDecoder.decode(x.toString))
          case x: Val.Str => new String(java.util.Base64.getDecoder.decode(x.value))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("encode", "value") {
      (pos, ev, value: Val) =>
        value match {
          case x: Val.Num =>
            if (NumberMath.mod(pos, x, Val.Num(position, 1))(ev).isZero)
              new String(java.util.Base64.getEncoder.encode(x.toString.getBytes()))
            else new String(java.util.Base64.getEncoder.encode(x.toString.getBytes()))
          case x: Val.Str => new String(java.util.Base64.getEncoder.encode(x.value.getBytes()))
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    }
  )
}
