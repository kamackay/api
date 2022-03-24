package com.keithmackay.api.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.inject.Inject
import com.google.inject.Singleton
import java.io.FileNotFoundException

@Singleton
class SecretsGrabber @Inject
internal constructor() {

  private val secrets = init()

  private fun init(): JsonObject {
    return try {
      JsonParser.parseString(fileToString(System.getenv("SECRETS_FILE")).trim())
        .asJsonObject
    } catch (e: FileNotFoundException) {
      JsonObject()
    }
  }

  fun getSecret(secret: String): JsonElement = secrets.get(secret)
}
