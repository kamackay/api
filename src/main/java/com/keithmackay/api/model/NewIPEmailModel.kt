package com.keithmackay.api.model

import com.keithmackay.api.utils.IPInfo

data class NewIPEmailModel(
    val info: IPInfo,
    val application: String,
    val additional: Map<String, String>
) {
  fun getTitle(): String {
    return "New Page Load on $application in ${info.city}, ${info.countryName}"
  }

  override fun toString(): String {
    return info.toString()
  }
}

