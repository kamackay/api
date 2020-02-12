package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.*
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

@Singleton
class LsRuleTask @Inject
internal constructor(db: Database) : Task() {
  private val log = getLogger(this::class)

  private val whitespacePattern = Pattern.compile("\\s")

  private val lsCollection = db.getCollection("lsrules")

  // Offset so that it doesn't always run at the same time as the main tasks
  override fun time(): Long = minutes(46.9)

  override fun run() {
    val start = System.currentTimeMillis()
    log.info("Started Task to calculate Little Snitch Servers")
    val response = khttp.get("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts;showintro=0")
    if (response.statusCode != 200) {
      log.error("HTTP Status ${response.statusCode} from ServerList")
    } else {
      log.debug("Pulled Ad Server File after " +
          millisToReadableTime(System.currentTimeMillis() - start))

      val added = ArrayList<String>()
      for (line in response.text.split("\n")) {
        try{
        val trimmed = line.trim()
          val split = line.split(whitespacePattern)
          if (!trimmed.startsWith("127.0.0.1") || split.size != 2) {
            // This is not a line of the output with a server
            continue
          }
          val server = split[1]
          val filter = doc("server", server)
            val exists = lsCollection.find(filter).count() > 0
            if (!exists) {
              lsCollection.updateOne(filter,
                  set(doc("server", server)
                      .append("time", System.currentTimeMillis())),
                  upsert())
              added.add(server)
            }
        } catch (e: Exception) {
          log.warn(e)
          continue
        }
      }
      if (added.count() > 0) {
        log.info("Successfully Added ${added.count()} new servers " +
            "(${millisToReadableTime(System.currentTimeMillis() - start)})")
      } else {
        log.info("No New Servers found to add to list")
      }
    }
  }
}