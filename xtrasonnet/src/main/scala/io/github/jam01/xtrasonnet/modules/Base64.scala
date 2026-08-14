package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.functions.AbstractFunctionModule
import sjsonnet.{Error, NumberMath, RenderUtils, Val}

import java.nio.charset.StandardCharsets

object Base64 extends AbstractFunctionModule {
  override def name: String = "base64"

  // UTF-8 rather than the platform default: base64 is defined over octets, so the host's encoding
  // decided which octets a non-ASCII string produced, and encode/decode did not round-trip between
  // hosts. The base64 alphabet itself is ASCII, but decode's output is arbitrary text.
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("decode", "value") {
      (_, _, value: Val) =>
        value match {
          case x: Val.Num => new String(java.util.Base64.getDecoder.decode(RenderUtils.renderNum(x)), StandardCharsets.UTF_8)
          case x: Val.Str => new String(java.util.Base64.getDecoder.decode(x.str), StandardCharsets.UTF_8)
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    },

    builtin("encode", "value") {
      (pos, ev, value: Val) =>
        value match {
          case x: Val.Num =>
            new String(java.util.Base64.getEncoder.encode(RenderUtils.renderNum(x).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
          case x: Val.Str => new String(java.util.Base64.getEncoder.encode(x.str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
          case x => Error.fail("Expected String, got: " + x.prettyName)
        }
    }
  )
}
