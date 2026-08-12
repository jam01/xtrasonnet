package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import sjsonnet.Val
import sjsonnet.functions.AbstractFunctionModule

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

object URL extends AbstractFunctionModule {
  override def name: String = "url"

  // percent-encoding is defined over UTF-8 octets (RFC 3986 s2.5), so the platform default would
  // make the same script emit different escapes on different hosts
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("encode", "data") {
      (_, _, data: String) =>
        URLEncoder.encode(data, StandardCharsets.UTF_8)
    },

    builtin("decode", "data") {
      (_, _, data: String) =>
        URLDecoder.decode(data, StandardCharsets.UTF_8)
    },
  )
}
