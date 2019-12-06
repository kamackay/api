package com.keithmackay.api

fun authSessionAttribute() = "authorization"

fun tokenTimeoutDays(): Long = 7

fun minutes(n: Number): Long = (60000L * n.toDouble()).toLong()
fun seconds(n: Number): Long = (1000L * n.toDouble()).toLong()
fun megabytes(n: Number): Long = (1000000L * n.toDouble()).toLong()
fun kilobytes(n: Int): Long = 1000L * n