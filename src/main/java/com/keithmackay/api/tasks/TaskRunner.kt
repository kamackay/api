package com.keithmackay.api.tasks

import com.google.inject.Inject

class TaskRunner @Inject
internal constructor(
    private val tasks: TaskList) {
  fun start() {
    tasks.forEach(Task::start)
  }
}