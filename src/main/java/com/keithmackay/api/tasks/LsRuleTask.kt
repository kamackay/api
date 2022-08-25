package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.*
import java.util.concurrent.Executors
import java.util.regex.Pattern


@Singleton
class LsRuleTask @Inject
internal constructor(db: Database) : Task() {
  private val log = getLogger(this::class)

  private val whitespacePattern = Pattern.compile("\\s")

  private val lsCollection = db.getCollection("lsrules")

  private val additionPool = Executors.newFixedThreadPool(16)

  // Offset so that it doesn't always run at the same time as the main tasks
  override fun time(): Long = minutes(9.9)

  data class AdList(val url: String, val name: String)

  override fun run() {
    log.info("Started Task to calculate Little Snitch Servers")
    listOf(
        AdList("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts;showintro=0", "Yoyo"),
        AdList("https://someonewhocares.org/hosts/zero/hosts", "SomeoneWhoCares"),
        AdList("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", "StevenBlack"),
        AdList("https://www.github.developerdan.com/hosts/lists/hate-and-junk-extended.txt", "DevDanHate"),
        AdList("https://www.github.developerdan.com/hosts/lists/ads-and-tracking-extended.txt", "DevDanAds"),
        AdList("https://www.github.developerdan.com/hosts/lists/amp-hosts-extended.txt", "DevDanAmp")
    ).forEach(this::processList)
  }

  private fun processList(list: AdList) {
    val url = list.url
    val start = System.currentTimeMillis()
    val response = httpGet(list.url)
    if (response.code != 200) {
      log.error("HTTP Status ${response.code} from ServerList")
    } else {
      log.debug(
          "Pulled Ad Server File after " +
              millisToReadableTime(System.currentTimeMillis() - start)
      )
      val text = response.body!!.string()
      log.debug(text)

      val added = ArrayList<String>()
      val lines = text.split("\n")
      log.info("Block List $url has ${lines.size} Items")
      for (line in lines) {
        try {
          val trimmed = line.trim()
          val split = line.split(whitespacePattern)
          if (!(trimmed.startsWith("127.0.0.1") || trimmed.startsWith("0.0.0.0")) || split.size != 2) {
            // This is not a line of the output with a server
            continue
          }
          val server = split[1]
          val filter = doc("server", server)
          val exists = lsCollection.find(filter).count() > 0
          if (!exists) {
            additionPool.submit {
              this.addServer(server, list.name)
            }
            added.add(server)
          }
        } catch (e: Exception) {
          log.warn(e)
          continue
        }
      }
      if (added.isNotEmpty()) {
        log.info(
            "Successfully Added ${added.count()} new servers " +
                "(${millisToReadableTime(System.currentTimeMillis() - start)})"
        )
      } else {
        log.info("No New Servers found to add to list")
      }
    }
  }

  private fun addServer(server: String, source: String) {
    try {
      lsCollection.updateOne(
          doc("server", server),
          set(
              doc("server", server)
                  .append("source", source)
                  .append("time", System.currentTimeMillis())
          ),
          upsert()
      )
      log.info("Added $server")
    } catch (e: Exception) {
      log.warn("Unable to add server to Database", e)
    }
  }
}