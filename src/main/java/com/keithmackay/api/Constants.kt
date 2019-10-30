package com.keithmackay.api

fun authSessionAttribute() = "authorization"

fun tokenTimeoutDays(): Long = 7

fun minutes(n: Int): Long = 60000L * n
fun megabytes(n: Int): Long = 1000000L * n
fun kilobytes(n: Int): Long = 1000L * n