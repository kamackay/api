package com.keithmackay.api.utils

import java.time.Duration
import java.time.Instant

class Cacher<T: Any> constructor(private val ttl: Duration, private val name: String) {
  private val log = getLogger("keith.${this::class.simpleName}[$name]")
  private val map = HashMap<String, CachedItem<T>>()

  public fun get(key: String, provider: () -> T): T {
    if (map.containsKey(key)) {
      val obj = map[key]
      if (obj != null && obj.isValid()) {
        log.info("Returning Cached Value for $key")
        return obj.item
      } else {
        // Item in map is invalid, remove it
        map.remove(key)
      }
    }
    // Get new Value and save it
    log.info("Fetching new value for $key")
    val newVal = provider()
    map[key] = CachedItem(newVal, Instant.now().plus(ttl))
    return newVal
  }

  public fun clear() {
    map.clear()
  }

  class CachedItem<T> constructor(val item: T, private val expiration: Instant) {
    public fun isValid(): Boolean {
      return Instant.now().isBefore(expiration)
    }
  }
}