package com.keithmackay.api

fun authSessionAttribute() = "authorization"

fun tokenTimeoutDays(): Long = 7

fun hours(n: Number): Long = minutes(60L * n.toDouble())
fun minutes(n: Number): Long = (60000L * n.toDouble()).toLong()
fun seconds(n: Number): Long = (1000L * n.toDouble()).toLong()
fun megabytes(n: Number): Long = (1000000L * n.toDouble()).toLong()
fun kilobytes(n: Int): Long = 1000L * n