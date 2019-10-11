package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.keithmackay.api.utils.threadSafeList

class TaskRunner @Inject
internal constructor(
    ls: LsRuleTask
) {
  private val tasks = threadSafeList(ls)
  fun start() {
    tasks.forEach(LsRuleTask::start)
  }
}