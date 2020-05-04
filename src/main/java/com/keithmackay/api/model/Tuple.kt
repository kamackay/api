package com.keithmackay.api.model

class Tuple<A, B>
internal constructor(private val a: A, private val b: B) {

  fun getA() = a
  fun getB() = b

}