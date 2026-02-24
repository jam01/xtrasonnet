package io.github.jam01.xtrasonnet

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.{MediaType, MediaTypes}
import sjsonnet.Settings

class TransformerSettings(val sjsSettings: Settings = Settings.default,
                          val defInputMediaType: MediaType = MediaTypes.APPLICATION_JSON,
                          val defOutputMediaType: MediaType = MediaTypes.APPLICATION_JSON)

object TransformerSettings {
  val Default = new TransformerSettings()
}
