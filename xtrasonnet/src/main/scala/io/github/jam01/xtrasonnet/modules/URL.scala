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
import java.nio.charset.Charset

object URL extends AbstractFunctionModule {
  override def name: String = "url"

  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("encode", "data") {
      (_, _, data: String) =>
        URLEncoder.encode(data, Charset.defaultCharset())
    },

    builtin("decode", "data") {
      (_, _, data: String) =>
        URLDecoder.decode(data, Charset.defaultCharset())
    },
  )
}
