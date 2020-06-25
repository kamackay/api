package com.keithmackay.api.model

data class CoinHolding(
    val name: String,
    val code: String,
    val color: String,
    val count: Double,
    val value: Double
)