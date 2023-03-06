package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.model.LoginModel
import com.keithmackay.api.services.WeatherService
import com.keithmackay.api.tasks.GoodMorningTask
import com.keithmackay.api.utils.async
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import org.bson.Document

@Singleton
class WeatherRouter @Inject
internal constructor(
    private val weatherService: WeatherService
) : Router {

  override fun routes() {
    path("weather") {
      get("/") { ctx ->
        val location = ctx.bodyAsClass(WeatherService.Location::class.java)

        ctx.json(weatherService.getWeatherForLocation(location) ?: Document())
      }
    }
  }

  override fun isHealthy() = true
}