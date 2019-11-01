package com.keithmackay.api.tasks

abstract class Task {
  abstract fun run()

  open fun log(): Boolean = true

  open fun time(): Long = 1000 * 60 * 60 // Once an hour
}