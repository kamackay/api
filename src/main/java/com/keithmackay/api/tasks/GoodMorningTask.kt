package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.CoinHolding
import com.keithmackay.api.model.CryptoLookupBean
import com.keithmackay.api.services.CryptoService
import com.keithmackay.api.services.NewsService
import com.keithmackay.api.services.NewsService.NewsItem
import com.keithmackay.api.services.WeatherService
import com.keithmackay.api.services.WeatherService.Companion.printSpeed
import com.keithmackay.api.services.WeatherService.Companion.printTemperature
import com.keithmackay.api.services.WeatherService.Location
import com.keithmackay.api.utils.ConfigGrabber
import com.keithmackay.api.utils.SecretsGrabber
import com.keithmackay.api.utils.getLogger
import io.keithm.domn8.DOMn8
import io.keithm.domn8.nodes.DomNode
import io.keithm.domn8.nodes.HtmlBody
import io.keithm.domn8.nodes.HtmlBody.BodyConfig
import io.keithm.domn8.nodes.elements.BreakEl.breakEl
import io.keithm.domn8.nodes.elements.DivEl
import io.keithm.domn8.nodes.elements.DivEl.divConfig
import io.keithm.domn8.nodes.elements.DivEl.divEl
import io.keithm.domn8.nodes.elements.HeaderEl.headerConfig
import io.keithm.domn8.nodes.elements.HeaderEl.headerEl
import io.keithm.domn8.nodes.elements.ImgNode
import io.keithm.domn8.nodes.elements.ImgNode.imgNode
import io.keithm.domn8.nodes.elements.LinkEl.LinkConfig
import io.keithm.domn8.nodes.elements.LinkEl.linkEl
import io.keithm.domn8.nodes.elements.TextNode.TextConfig
import io.keithm.domn8.nodes.elements.TextNode.textNode
import io.keithm.domn8.styles.CSS.css
import org.quartz.CronScheduleBuilder
import org.quartz.JobExecutionContext
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class GoodMorningTask
@Inject internal constructor(
    private val emailSender: EmailSender,
    private val config: ConfigGrabber,
    private val secrets: SecretsGrabber,
    private val cryptoService: CryptoService,
    private val weatherService: WeatherService,
    private val newsService: NewsService
) : CronTask() {
  private val log = getLogger(this::class)

  override fun name(): String = "GoodMorning"

  //    override fun cron() = CronTimes.minutes(10)
  override fun cron() = CronTimes.CRON_EVERY_MORNING

  override fun schedule(): CronScheduleBuilder =
      CronScheduleBuilder
          .cronSchedule(this.cron())
          .inTimeZone(TimeZone.getTimeZone("America/New_York"))

  override fun execute(context: JobExecutionContext?) {
    log.info("Good Morning")
    config.getValue("goodMorningEmails").asJsonArray
        .map { it.asJsonObject }
        .forEach { el ->
          val timezone = el.get("timezone").asString
          val title = "Good Morning: ${today(timezone)}"
          val locationObj = el.get("location").asJsonObject
          val location = Location(
              name = locationObj.get("name").asString,
              latitude = locationObj.get("latitude").asDouble,
              longitude = locationObj.get("longitude").asDouble
          )
          val coinSecret = secrets.getSecret(el.get("coinbaseSecret").asString)
          emailSender.send(title,
              emailRenderer.renderIntoString(GoodMorningEmail(
                  title = title,
                  location = location,
                  name = el.get("name").asString,
                  timezone = timezone,
                  weather = weatherService.getWeatherForLocation(location),
                  news = newsService.getDaysTopNews(),
                  coins = cryptoService.getAccounts(CryptoLookupBean(
                      coinSecret.asJsonObject.get("key").asString,
                      coinSecret.asJsonObject.get("secret").asString
                  ))
              )),
              el.get("email").asString)
        }
  }

  private val emailRenderer = DOMn8.generic(GoodMorningEmail::class.java,
      { model: GoodMorningEmail ->
        HtmlBody.body(BodyConfig(),
            listOf(
                headerEl(headerConfig()
                    .level(2)
                    .text(model.title)),
                divEl(DivEl.DivConfig(), emailContents(model))
            ) as List<DomNode<*>>?)
      }, "Good Morning!")

  private fun emailContents(model: GoodMorningEmail): List<DomNode<*>>? {
    return listOf(
        headerEl(headerConfig().level(5).text("Hi, ${model.name}")),
        cryptoContents(model),
        weatherContents(model),
        newsContents(model)
    )
  }

  private fun weatherContents(model: GoodMorningEmail): DomNode<*> {
    if (model.weather == null) {
      return divEl(divConfig(), listOf())
    }
    val weather = model.weather
    return divEl(divConfig(), listOf(
        headerEl(headerConfig()
            .level(3)
            .text("Weather for ${model.location.name}")),
        *weather.current.conditions.map {
          imgNode(ImgNode.ImgConfig()
              .src(it.iconUrl)
              .height(50)
              .preRendered(true)
              .alt(it.description))
        }.toTypedArray(),
        row("Currently ${printTemperature(weather.current.temp)}"),
        row("${weather.current.clouds}% Cloudy"),
        row("Wind: ${printSpeed(weather.current.windSpeed)}"),
        row("${weather.current.humidity}% Humidity"),
        headerEl(headerConfig()
            .level(5)
            .text("High Today: ${printTemperature(weather.highTemp())}")),
        headerEl(headerConfig()
            .level(5)
            .text("Low Today: ${printTemperature(weather.lowTemp())}")),
        divEl(divConfig().styles(css().setValue("display", "inline")),
            weather.hourly.map { hourly ->
              divEl(divConfig().styles(css()
                  .set("margin", "1px")
                  .set("padding", "5px")
                  .set("text-align", "center")
                  .set("border-radius", "4px")
                  .set("display", "inline-block")
                  .set("border", "thin solid black")),
                  listOf(headerEl(headerConfig()
                      .level(5)
                      .text(printHour(hourly.dt, model.timezone))),
                      *hourly.conditions.map { condition ->
                        imgNode(ImgNode.ImgConfig()
                            .styles(css()
                                .set("margin-left", "auto")
                                .set("margin-right", "auto")
                                .set("display", "block"))
                            .src(condition.iconUrl)
                            .height(50)
                            .preRendered(true)
                            .alt(condition.description))
                      }.toTypedArray(),
                      textNode(TextConfig()
                          .text(printTemperature(hourly.temp))
                          .styles(css()
                              .setValue("display", "block"))),
                      textNode(TextConfig()
                          .text("Wind: ${printSpeed(hourly.windSpeed)}")
                          .styles(css().set("display", "block"))),
                      textNode(TextConfig().text("Clouds: ${hourly.clouds}%")
                          .styles(css().set("display", "block")))))
            })
    ))
  }

  private fun cryptoContents(model: GoodMorningEmail): DomNode<*> {
    return if (model.coins.isEmpty()) {
      divEl(divConfig(), listOf())
    } else {
      divEl(divConfig().styles(css().set("margin-top", "10px")), listOf(
          headerEl(headerConfig().level(3).text("Cryptocurrencies")),
          *model.coins.map { coin ->
            divEl(divConfig()
                .styles(css().setValue("display", "block")),
                listOf(textNode(TextConfig()
                    .styles(css().set("color", coin.color))
                    .text("${coin.count} ${coin.name} " +
                        "- Worth \$${String.format("%.2f", coin.count * coin.value)}")),
                    breakEl()))
          }.toTypedArray()
      ))
    }
  }

  private fun newsContents(model: GoodMorningEmail): DomNode<*> {
    return if (model.news.isEmpty()) {
      divEl(divConfig(), listOf())
    } else {
      divEl(divConfig().styles(css().set("margin-top", "10px")), listOf(
          headerEl(headerConfig().level(3).text("Today's News")),
          *model.news.map(this::renderNewsItem).toTypedArray()
      ))
    }
  }

  private fun renderNewsItem(item: NewsItem): DomNode<*> {
    return linkEl(LinkConfig()
        .url(item.link)
        .setNewTab(true)
        .text("${item.source.site}: ${item.title}")
        .fontSize(12)
        .styles(css()
            .set("display", "block")
            .set("border-bottom", "thin solid black")
            .set("margin-bottom", "1rem")))
  }

  private fun dateFormatter(format: String, timezone: String): SimpleDateFormat {
    val formatter = SimpleDateFormat(format)
    formatter.timeZone = TimeZone.getTimeZone(timezone)
    return formatter
  }

  private fun printHour(time: Long, timezone: String) = dateFormatter("MM/dd hha", timezone).format(Date(time))

  private fun row(line: String) =
      divEl(divConfig()
          .styles(css().setValue("display", "block")),
          listOf(textNode(line), breakEl()))

  private data class GoodMorningEmail(
      val title: String,
      val name: String,
      val location: Location,
      val weather: WeatherService.Weather?,
      val timezone: String,
      val news: List<NewsItem>,
      val coins: List<CoinHolding>
  )

  private fun today(timezone: String): String =
      dateFormatter("yyyy-MM-dd", timezone).format(Date())
}