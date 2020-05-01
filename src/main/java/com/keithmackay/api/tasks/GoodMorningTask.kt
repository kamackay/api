package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.domn8.DOMn8
import com.keithmackay.api.domn8.nodes.DomNode
import com.keithmackay.api.domn8.nodes.HtmlBody
import com.keithmackay.api.domn8.nodes.HtmlBody.BodyConfig
import com.keithmackay.api.domn8.nodes.elements.BreakEl
import com.keithmackay.api.domn8.nodes.elements.DivEl
import com.keithmackay.api.domn8.nodes.elements.DivEl.divConfig
import com.keithmackay.api.domn8.nodes.elements.DivEl.divEl
import com.keithmackay.api.domn8.nodes.elements.HeaderEl.headerConfig
import com.keithmackay.api.domn8.nodes.elements.HeaderEl.headerEl
import com.keithmackay.api.domn8.nodes.elements.ImgNode
import com.keithmackay.api.domn8.nodes.elements.ImgNode.imgNode
import com.keithmackay.api.domn8.nodes.elements.TextNode.textNode
import com.keithmackay.api.domn8.styles.CSS
import com.keithmackay.api.domn8.styles.CSS.css
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.services.WeatherService
import com.keithmackay.api.services.WeatherService.Companion.printTemperature
import com.keithmackay.api.services.WeatherService.Location
import com.keithmackay.api.utils.ConfigGrabber
import com.keithmackay.api.utils.getLogger
import org.quartz.CronScheduleBuilder
import org.quartz.JobExecutionContext
import java.text.SimpleDateFormat
import java.util.*

@Singleton
class GoodMorningTask
@Inject internal constructor(
        private val emailSender: EmailSender,
        private val config: ConfigGrabber,
        private val weatherService: WeatherService
) : CronTask() {
    private val log = getLogger(this::class)

    override fun name(): String = "GoodMorning"

//    override fun cron() = CronTimes.minutes(6)
    override fun cron() = CronTimes.CRON_EVERY_MORNING

    override fun schedule(): CronScheduleBuilder =
            CronScheduleBuilder
                    .cronSchedule(this.cron())
                    .inTimeZone(TimeZone.getTimeZone("America/New_York"))

    override fun execute(context: JobExecutionContext?) {
        log.info("Good Morning")
        val title = "Good Morning: ${today()}"
        config.getValue("goodMorningEmails").asJsonArray
                .map { it.asJsonObject }
                .forEach { el ->
                    val locationObj = el.get("location").asJsonObject
                    val location = Location(
                            name = locationObj.get("name").asString,
                            latitude = locationObj.get("latitude").asDouble,
                            longitude = locationObj.get("longitude").asDouble
                    )
                    emailSender.send(title,
                            emailRenderer.renderIntoString(GoodMorningEmail(
                                    title = title,
                                    location = location,
                                    name = el.get("name").asString,
                                    weather = weatherService.getWeatherForLocation(location)
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
                                divEl(DivEl.DivConfig(),
                                        emailContents(model)
                                )
                        ) as List<DomNode<*>>?)
            }, "Good Morning!")

    private fun emailContents(model: GoodMorningEmail): List<DomNode<*>>? {
        return listOf(
                textNode("Hi, ${model.name}"),
                weatherContents(model))
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
                row("Wind: ${WeatherService.printSpeed(weather.current.windSpeed)}"),
                row("${weather.current.humidity}% Humidity"),
                headerEl(headerConfig()
                        .level(5)
                        .text("High Today: ${printTemperature(weather.highTemp())}")),
                headerEl(headerConfig()
                        .level(5)
                        .text("Low Today: ${printTemperature(weather.lowTemp())}")),
                divEl(divConfig(),
                        weather.hourly.map {
                            divEl(divConfig().styles(css().setValue("display", "inline")),
                                    it.conditions.map { condition ->
                                        imgNode(ImgNode.ImgConfig()
                                                .styles(css().setValue("display", "inline-block"))
                                                .src(condition.iconUrl)
                                                .height(50)
                                                .preRendered(true)
                                                .alt(condition.description))
                                    })
                        })
        ))
    }

    private fun row(line: String) =
            divEl(divConfig()
                    .styles(CSS.css().setValue("display", "block")),
                    listOf(textNode(line), BreakEl.breakEl()))

    private data class GoodMorningEmail(
            val title: String,
            val name: String,
            val location: Location,
            val weather: WeatherService.Weather?
    )

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd").format(Date())
    }
}