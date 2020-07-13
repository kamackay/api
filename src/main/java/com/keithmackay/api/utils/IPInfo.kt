package com.keithmackay.api.utils

import org.bson.Document
import org.json.JSONObject
import java.util.*

data class IPInfo(
    val ip: String,
    val city: String,
    val region: String,
    val regionCode: String,
    val country: String,
    val countryCode: String,
    val countryName: String,
    val postal: String,
    val latitude: Any,
    val longitude: Any,
    val timezone: String,
    val organization: String
) {

  fun toMongo(): Document = doc()
      .append("city", city)
      .append("region", region)
      .append("regionCode", regionCode)
      .append("country", country)
      .append("countryCode", countryCode)
      .append("countryName", countryName)
      .append("postal", postal)
      .append("latitude", latitude)
      .append("longitude", longitude)
      .append("timezone", timezone)
      .append("organization", organization)
}


fun getIpInfo(ip: String): IPInfo? {
  val log = getLogger("IPUtils")
  val response = khttp.get("https://ipapi.co/$ip/json/")
  if (response.statusCode == 200) {
    val json = response.jsonObject
    return IPInfo(
        ip = ip,
        city = get(json, "city", "Unknown"),
        region = get(json, "region", "Unknown"),
        regionCode = get(json, "region_code", "Unknown"),
        country = get(json, "country", "Unknown"),
        countryCode = get(json, "country_code", "Unknown"),
        countryName = get(json, "country_name", "Unknown"),
        postal = get(json, "postal", "Unknown"),
        latitude = get(json, "latitude", 0.0),
        longitude = get(json, "longitude", 0.0),
        timezone = get(json, "timezone", "Unknown"),
        organization = get(json, "org", "Unknown")
    )
  } else {
    log.error("Error Fetching ip info", response.text)
    return null
  }
}

private inline fun <reified T> get(obj: JSONObject, key: String, default: T): T {
  return Optional.of(obj)
      .map {
        try {
          it.get(key)
        } catch (e: Exception) {
          null
        }
      }.map {
        if (it is T) {
          it
        } else {
          default
        }
      }
      .orElse(default) as T
}