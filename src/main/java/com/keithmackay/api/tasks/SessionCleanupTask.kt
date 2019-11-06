package com.keithmackay.api.tasks


import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.tokenTimeoutDays
import com.keithmackay.api.utils.*
import java.time.Instant
import java.time.temporal.ChronoUnit


@Singleton
class SessionCleanupTask @Inject
internal constructor(db: Database) : Task() {
  private val tokenCollection = db.getCollection("session_data")
  private val log = getLogger(this::class)

  override fun run() {
    log.info("Checking to see if any sessions can be cleaned from the Database")
    val weekAgo = lte(Instant.now()
        .minus(tokenTimeoutDays(), ChronoUnit.DAYS)
        .toEpochMilli())
    val result = tokenCollection
        .deleteMany(
            or(
                doc("created", lessThan(weekAgo)),
                doc("valid", eq(false))))
    if (result.deletedCount > 0) {
      log.info("Deleted ${result.deletedCount} Expired Sessions from the database")
    } else {
      log.info("No Sessions needed to be deleted")
    }
  }
}