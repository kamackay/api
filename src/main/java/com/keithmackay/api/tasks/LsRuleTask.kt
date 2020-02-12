package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.minutes
import com.keithmackay.api.utils.*
import java.util.*
import java.util.regex.Pattern

@Singleton
class LsRuleTask @Inject
internal constructor(db: Database) : Task() {
  private val log = getLogger(this::class)

  private val lsCollection = db.getCollection("lsrules")

  // Offset so that it doesn't always run at the same time as the main tasks
  override fun time(): Long = minutes(58)

  override fun run() {
    val start = System.currentTimeMillis()
    log.info("Started Task to calculate Little Snitch Servers")
    val response = khttp.get("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts;showintro=0")
    if (response.statusCode != 200) {
      log.error("HTTP Status ${response.statusCode} from ServerList")
    } else {
      val added = response.text
          .split("\n")
          .asSequence()
          .map { it.trim() }
          .filter { it.startsWith("127.0.0.1") }
          .map { it.split(Pattern.compile("\\s")) }
          .filter { it.size == 2 }
          .map { it[1] }
          .map {
            val filter = doc("server", it)
            val exists = lsCollection.find(filter).count() > 0
            if (!exists) {
              lsCollection.updateOne(filter,
                  set(doc("server", it)
                      .append("time", System.currentTimeMillis())),
                  upsert())
              it
            } else null
          }
          .filter { Objects.nonNull(it) }
          .mapNotNull { it }
          .toList()
      if (added.count() > 0) {
        log.info("Successfully Added ${added.count()} new servers " +
            "(${millisToReadableTime(System.currentTimeMillis() - start)})")
      } else {
        log.info("No New Servers found to add to list")
      }
    }
  }
}