package com.keithmackay.api.model

import com.keithmackay.api.utils.IPInfo

data class NewIPEmailModel(
    val info: IPInfo,
    val application: String,
    val additional: Map<String, String>
) {
  fun getTitle(returning: Boolean = false): String {
    return if (returning) "Returning Page Load on $application from ${info.city}, ${info.region}, ${info.countryName}"
    else "New Page Load on $application in ${info.city}, ${info.region}, ${info.countryName}"
  }

  override fun toString(): String {
    return info.toString()
  }
}

