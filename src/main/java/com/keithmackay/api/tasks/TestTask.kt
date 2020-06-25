package com.keithmackay.api.tasks

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.CryptoLookupBean
import com.keithmackay.api.services.CryptoService
import com.keithmackay.api.services.NewsService
import com.keithmackay.api.services.WeatherService
import com.keithmackay.api.utils.SecretsGrabber
import com.keithmackay.api.utils.getLogger
import org.quartz.JobExecutionContext

@Singleton
class TestTask
@Inject internal constructor(
    private val emailSender: EmailSender,
    private val secrets: SecretsGrabber,
    private val weatherService: WeatherService,
    private val cryptoService: CryptoService,
    private val newsService: NewsService
) : CronTask() {
  private val log = getLogger(this::class)
  override fun name() = "TestTask"

  override fun cron() = CronTimes.minutes(2)

  override fun execute(ctx: JobExecutionContext?) {
    val secret = secrets.getSecret("keith-coinbase")
    cryptoService.getAccounts(CryptoLookupBean(
        secret.asJsonObject.get("key").asString,
        secret.asJsonObject.get("secret").asString
    )).forEach { coin ->
      log.info("${coin.count} ${coin.name} " +
          "- Worth \$${String.format("%.2f", coin.count * coin.value)}")
    }
  }

  private fun testNewsService() = log.info(GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(newsService.getDaysTopNews()))

  private fun testWeather() = log.info(GsonBuilder()
      .setPrettyPrinting()
      .create()
      .toJson(weatherService.getWeatherForLocation(WeatherService.Location(
          name = "Durm",
          latitude = 36.06,
          longitude = -78.87
      ))))
}