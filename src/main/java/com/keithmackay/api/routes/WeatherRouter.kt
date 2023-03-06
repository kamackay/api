package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.services.WeatherService
import com.keithmackay.api.utils.Cacher
import com.keithmackay.api.utils.getLogger
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import org.bson.Document
import java.time.Duration

@Singleton
class WeatherRouter @Inject
internal constructor(
    private val weatherService: WeatherService
) : Router {
  private val log = getLogger(this::class)

  private val cache = Cacher<WeatherService.Weather>(Duration.ofMinutes(30), "weather")

  override fun routes() {
    path("weather") {
      post("/") { ctx ->
        val location = ctx.bodyAsClass(WeatherService.Location::class.java)

        ctx.json(cache.get(location.name) {
          log.info("Fetching ${location.name} weather")
          weatherService.getWeatherForLocation(location) ?: WeatherService.emptyWeather(location)
        })
      }
    }
  }

  override fun isHealthy() = true
}