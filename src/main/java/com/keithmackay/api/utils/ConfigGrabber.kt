package com.keithmackay.api.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.inject.Inject
import com.google.inject.Singleton

@Singleton
class ConfigGrabber @Inject
internal constructor() {

    private val config =
            JsonParser().parse(fileToString(System.getenv("CONFIG_FILE")).trim())
                    .asJsonObject

    fun getValue(secret: String): JsonElement = config.get(secret)
}