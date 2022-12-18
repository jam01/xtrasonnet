package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.spi.Library.dummyPosition
import sjsonnet.Std.{builtin, builtinWithDefaults}
import sjsonnet.Val

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.Charset

object URL {
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
