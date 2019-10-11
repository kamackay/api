package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.lte


@Singleton
class TokenCleanupTask @Inject
internal constructor(db: Database) : Task() {
  private val tokenCollection = db.getCollection("tokens")
  private val log = getLogger(this::class)

  override fun run() {
    log.info("Checking to see if any tokens can be cleaned from the Database")
    val result = tokenCollection.deleteMany(doc("timeout", lte(System.currentTimeMillis())))
    if (result.deletedCount > 0) {
      log.info("Deleted ${result.deletedCount} Expired Tokens from the database")
    } else {
      log.info("No Tokens needed to be deleted")
    }
  }
}