package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.{MediaType, MediaTypes}
import sjsonnet.Settings

class TransformerSettings(override val preserveOrder: Boolean = false,
                          override val strict: Boolean = false,
                          override val noStaticErrors: Boolean = false,
                          val defInputMediaType: MediaType = MediaTypes.APPLICATION_JSON,
                          val defOutputMediaType: MediaType = MediaTypes.APPLICATION_JSON
                         ) extends Settings(preserveOrder, strict, noStaticErrors)

object TransformerSettings {
  val default = new TransformerSettings()
}
