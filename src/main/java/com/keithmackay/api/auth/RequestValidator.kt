package com.keithmackay.api.auth

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.db.Database
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import io.javalin.http.Context
import io.javalin.http.Handler
import org.bson.Document
import java.util.*


@Singleton
class RequestValidator @Inject
internal constructor(db: Database) {
  private val tokenCollection = db.getCollection("tokens")
  private val log = getLogger(RequestValidator::class)
  
  fun validateThen(post: (Document) -> Unit): Handler {
    return Handler {
      val user = lookup(it)
      if (user != null) {
        log.info("Valid request for ${user.getString("username")}")
        post.apply {  }
      } else {
        log.warn("Invalid Authorization on request")
        it.status(401).result("Invalid Authorization")
      }
    }
  }

  fun lookup(ctx: Context): Document? {
    val token = ctx.header("Authorization")
    val user = tokenCollection
        .find(doc("token", token))
        .first()
    log.info("Checking Validity of $token")
    if (Optional.ofNullable(user)
            .map { it.getLong("timeout") }
            .orElse(0L) > System.currentTimeMillis()) {
      log.info("Valid Request")
      return user
    }
    log.info("Invalid Request")
    return null
  }
}
