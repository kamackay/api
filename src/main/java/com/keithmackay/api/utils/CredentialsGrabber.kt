package com.keithmackay.api.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.inject.Inject
import com.google.inject.Singleton

@Singleton
class CredentialsGrabber @Inject
internal constructor() {

  private val secrets =
      JsonParser().parse(fileToString(System.getenv("CREDENTIALS_FILE")).trim())
          .asJsonObject

  fun getSecret(secret: String): JsonElement = secrets.get(secret)
}