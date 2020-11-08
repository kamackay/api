package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.email.EmailSender
import com.keithmackay.api.model.CoinHolding
import com.keithmackay.api.model.CryptoLookupBean
import com.keithmackay.api.services.CryptoService
import com.keithmackay.api.utils.SecretsGrabber
import com.keithmackay.api.utils.getLogger
import io.keithm.domn8.DOMn8
import io.keithm.domn8.nodes.DomNode
import io.keithm.domn8.nodes.HtmlBody
import io.keithm.domn8.nodes.elements.BreakEl.breakEl
import io.keithm.domn8.nodes.elements.TextNode
import io.keithm.domn8.nodes.elements.TextNode.textNode
import io.keithm.domn8.styles.CSS.css
import org.quartz.JobExecutionContext
import kotlin.math.abs
import kotlin.math.roundToInt

fun Double.format(digits: Int): String {
  val num = "%.${digits}f".format(abs(this))

  return "${if (this >= 0) "" else "-"}$num%"
}

fun Double.currency(): String = "\$${String.format("%.2f", this)}"

fun minutes(time: Long): String {
  val minutes = (time.toDouble() / 60_000).roundToInt()
  return if (minutes == 60) "1 hour" else "$minutes minutes"
}


@Singleton
class CryptoTask
@Inject internal constructor(
    private val emailSender: EmailSender,
    private val secrets: SecretsGrabber,
    private val cryptoService: CryptoService
) : CronTask() {

  private val log = getLogger(this::class)
  override fun name() = "TestTask"

  override fun cron() = CronTimes.minutes(1)

  private val history = HashMap<String, MutableList<CoinHolding>>()

  private val emailRenderer = DOMn8.generic(EmailData::class.java,
      { model: EmailData ->
        HtmlBody.body(HtmlBody.BodyConfig(),
            listOf(
                textNode(TextNode.TextConfig()
                    .styles(css().set("display", "block"))
                    .text("${model.newCoin.name} is now at ${model.newCoin.value.currency()}, " +
                        "making your share worth ${(model.newCoin.count * model.newCoin.value).currency()}")),
                breakEl(),
                textNode("Old Value: ${(model.oldCoin.count * model.oldCoin.value).currency()} " +
                    "(${model.oldCoin.value.currency()} Per)")
            ) as List<DomNode<*>>?)
      }, "Crypto Value Change")

  override fun execute(ctx: JobExecutionContext?) {
    history.entries.forEach { log.debug("${it.key} -> ${it.value.size}") }
    this.calculatePrices()
        .forEach { coin ->
          val list = history.getOrDefault(coin.code, ArrayList())
          log.info("Processing Results for ${coin.name} (${list.size} historical results)")
          this.findPriceToCompare(list)?.run {
            compareAndSend(this, coin)
          }
          list.add(coin)
          history[coin.code] = list
        }
  }

  private fun clearOldMetrics(keep: CoinHolding) {
    val l = this.history[keep.code]
    l?.clear()
    l?.add(keep)
    log.info("Clearing all records except ${this.history[keep.code]?.size ?: 0} most recent")
  }

  private fun compareAndSend(old: CoinHolding,
                             coin: CoinHolding,
                             changeTrigger: Double = 1.0): Boolean {
    val timeDiffString = "over ${minutes(coin.timeCalculated - old.timeCalculated)}"
    val change = calculateChange(old, coin)
    log.info("${coin.name} has changed by ${change.format(2)} " +
        "(${old.value.currency()} -> ${coin.value.currency()}) $timeDiffString")
    if (abs(change) > changeTrigger) {
      emailSender.send(
          "Crypto Value ${coin.name} has changed by ${change.format(2)} $timeDiffString",
          emailRenderer.renderIntoString(EmailData(old, coin)),
          emailSender.mainUser())
      this.clearOldMetrics(coin)
      return true
    }
    return false
  }

  private fun findPriceToCompare(holdings: List<CoinHolding>): CoinHolding? {
    val now = System.currentTimeMillis()
    val filtered = holdings.filter {
      it.timeCalculated > now - 1000 * 60 * 60
    }
    return if (filtered.isEmpty()) {
      null
    } else {
      filtered.sortedBy { it.timeCalculated }[0]
    }
  }

  private fun calculatePrices(): MutableList<CoinHolding> {
    val secret = secrets.getSecret("keith-coinbase")
    return cryptoService.getAccounts(CryptoLookupBean(
        secret.asJsonObject.get("key").asString,
        secret.asJsonObject.get("secret").asString
    ))
  }

  private fun calculateChange(old: CoinHolding, new: CoinHolding): Double {
    val y1 = old.value
    val y2 = new.value
    return ((y2 - y1) / y1) * 100
  }

  private data class EmailData(
      val oldCoin: CoinHolding,
      val newCoin: CoinHolding
  )
}