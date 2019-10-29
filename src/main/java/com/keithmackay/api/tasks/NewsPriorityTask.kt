package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.db.Database
import javax.inject.Singleton


@Singleton
class NewsPriorityTask @Inject
internal constructor(db: Database) : Task() {
  override fun run() {
    //TODO
  }
}
