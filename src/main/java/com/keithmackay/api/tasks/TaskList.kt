package com.keithmackay.api.tasks

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.threadSafeList

@Singleton
class TaskList @Inject
internal constructor(
    ls: LsRuleTask,
    token: TokenCleanupTask,
    session: SessionCleanupTask,
    memory: MemoryTask,
    newsPriority: NewsPriorityTask,
    news: NewsTask
) {
  // Be sure to add new Tasks here too!
  private val tasks = threadSafeList(ls, token, session, news)

  fun forEach(f: (Task) -> Unit) = tasks.forEach(f)
}