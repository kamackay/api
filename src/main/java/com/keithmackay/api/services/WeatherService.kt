package com.keithmackay.api.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.*
import org.json.JSONArray
import org.json.JSONObject

typealias Temperature = Double
typealias Precipitation = Map<String, Double>

@Singleton
class WeatherService @Inject
internal constructor(
    private val secrets: CredentialsGrabber,
    private val config: ConfigGrabber
) {

  private val log = getLogger(this::class)

  fun getWeatherForLocation(location: Location): Weather? {
    try {
      val response = httpGet(
          url = config.getValue("openWeatherUrl").asString,
          params = mapOf(
              "lat" to location.latitude.toString(),
              "lon" to location.longitude.toString(),
              "appid" to secrets.getSecret("open-weather-key").asString
          ))
      val jsonObject = JSONObject(response.body!!.string())
      val current = jsonObject.getJSONObject("current")
      val hourly = jsonObject.getJSONArray("hourly")
      val daily = jsonObject.getJSONArray("daily")

      return Weather(
          location = location,
          current = CurrentWeather(
              dt = current.optLong("dt") * 1000,
              sunrise = current.optLong("sunrise"),
              sunset = current.optLong("sunset"),
              temp = current.optDouble("temp"),
              feelsLike = current.optDouble("feels_like"),
              pressure = current.optLong("pressure"),
              humidity = current.optDouble("humidity"),
              dewPoint = current.optDouble("dew_point"),
              uvi = current.optDouble("uvi"),
              clouds = current.optDouble("clouds"),
              windSpeed = current.optDouble("wind_speed"),
              windDegree = current.optDouble("wind_deg"),
              windGust = current.optDouble("wind_gust", 0.0),
              conditions = getWeatherConditions(current.optJSONArray("weather")),
              rain = null,
              snow = null
          ),
          hourly = hourly.iterateObjects(24).map {
            HourlyWeather(
                dt = it.optLong("dt", 0) * 1000,
                temp = it.optDouble("temp"),
                feelsLike = it.optDouble("feels_like"),
                pressure = it.optLong("pressure"),
                humidity = it.optDouble("humidity"),
                dewPoint = it.optDouble("dew_point"),
                clouds = current.optDouble("clouds"),
                windSpeed = current.optDouble("wind_speed"),
                windDegree = current.optDouble("wind_deg"),
                conditions = getWeatherConditions(it.optJSONArray("weather"))
            )
          },
          daily = listOf()
      )
    } catch (e: Exception) {
      log.error("Error Getting Weather", e)
      return null
    }
  }

  private fun getWeatherConditions(obj: JSONArray): List<WeatherCondition> {
    try {
      return obj.iterateObjects().map {
        val icon = it.getString("icon")
        WeatherCondition(
            id = it.optInt("id"),
            main = it.optString("main"),
            description = it.optString("description"),
            icon = icon,
            iconUrl = config.getValue("openWeatherIconUrl")
                .asString
                .replace("{}", icon)
        )
      }
    } catch (e: Exception) {
      log.warn("Could not parse for weather conditions", e)
      return listOf()
    }
  }

  companion object {
    fun printTemperature(temp: Double): String =
        "%.1fF".format(((9.0 / 5.0) * (temp - 273.15)) + 32.0)

    fun printSpeed(speed: Double): String =
        "%.2fMPH".format(speed * 2.2369)
  }

  data class Weather(
      val location: Location,
      val current: CurrentWeather,
      val hourly: List<HourlyWeather>,
      val daily: List<DailyWeather>
  ) {
    fun highTemp(): Double =
        hourly.map { it.temp }.toDoubleArray().max() ?: current.temp

    fun lowTemp(): Double =
        hourly.map { it.temp }.toDoubleArray().min() ?: current.temp
  }

  data class CurrentWeather(
      val dt: Long,
      val sunrise: Long,
      val sunset: Long,
      val temp: Temperature,
      val feelsLike: Temperature,
      val pressure: Long,
      val humidity: Double,
      val dewPoint: Temperature,
      val uvi: Double,
      val clouds: Double,
      val windSpeed: Double,
      val windDegree: Double,
      val windGust: Double,
      val conditions: List<WeatherCondition>,
      val rain: Precipitation?,
      val snow: Precipitation?
  )

  data class WeatherCondition(
      val id: Int,
      val main: String,
      val description: String,
      val icon: String,
      val iconUrl: String
  )

  data class HourlyWeather(
      val dt: Long,
      val temp: Temperature,
      val feelsLike: Temperature,
      val pressure: Long,
      val humidity: Double,
      val dewPoint: Temperature,
      val clouds: Double,
      val windSpeed: Double,
      val windDegree: Double,
      val conditions: List<WeatherCondition>
  )

  data class DailyWeather(
      val dt: Long
  )

  data class Location(
      val name: String,
      val latitude: Double,
      val longitude: Double
  )
}