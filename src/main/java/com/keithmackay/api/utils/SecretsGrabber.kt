package com.keithmackay.api.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.inject.Inject
import com.google.inject.Singleton

@Singleton
class SecretsGrabber @Inject
internal constructor() {

  private val secrets =
      JsonParser.parseString(fileToString(System.getenv("SECRETS_FILE")).trim())
          .asJsonObject

  fun getSecret(secret: String): JsonElement = secrets.get(secret)
}