package com.keithmackay.api.model

import com.keithmackay.api.db.Database

class Tuple<A, B>
internal constructor(private val a: A, private val b: B){

  public fun getA() = a
  public fun getB() = b

}