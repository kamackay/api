package com.keithmackay.api.auth

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.authSessionAttribute
import com.keithmackay.api.db.Database
import com.keithmackay.api.model.InvalidAuthenticationResponse
import com.keithmackay.api.model.User
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse
import org.bson.Document
import java.util.*

typealias RequestHandler = (Context, Document, User) -> Unit


@Singleton
class RequestValidator @Inject
internal constructor(db: Database) {
  private val tokenCollection = db.getCollection("tokens")
  private val userCollection = db.getCollection("users")

  private val log = getLogger(RequestValidator::class)

  fun validateThen(post: (Document) -> Unit): Handler {
    return Handler {
      val user = lookup(it)
      if (user != null) {
        log.info("Valid request for ${user.getString("username")}")
        post.invoke(user)
      } else {
        log.warn("Invalid Authorization on request")
        throw InvalidAuthenticationResponse()
      }
    }
  }

  fun secureGet(path: String, handler: RequestHandler) {
    return ApiBuilder.get(path) {
      secureRequest(it, handler)
    }
  }

  fun securePost(path: String, handler: RequestHandler) {
    return ApiBuilder.post(path) {
      secureRequest(it, handler)
    }
  }

  fun securePut(path: String, handler: RequestHandler) {
    return ApiBuilder.put(path) {
      secureRequest(it, handler)
    }
  }

  fun secureDelete(path: String, handler: RequestHandler) {
    return ApiBuilder.delete(path) {
      secureRequest(it, handler)
    }
  }

  private fun secureRequest(ctx: Context, handler: RequestHandler) {
    log.info("Validating ${ctx.method()} Request on ${ctx.path()}")
    val token = lookup(ctx)
    if (token != null) {
      val user = userCollection
          .find(doc("username", token.getString("username")))
          .first()
      if (user == null) {
        log.error("Validated token $token, but could not find user data")
        throw InvalidAuthenticationResponse()
      } else {
        log.debug("Valid Request, triggering handler")
        handler.invoke(ctx,
            if ("GET" == ctx.method()) doc()
            else Document.parse(ctx.body()), User().fromJson(user))
      }
    } else {
      log.warn("Invalid Authorization on request")
      throw InvalidAuthenticationResponse()
    }
  }

  private fun lookup(ctx: Context): Document? {
    val token = Optional.ofNullable(ctx.header("Authorization"))
        .orElseGet { ctx.sessionAttribute(authSessionAttribute()) }
    val user = tokenCollection
        .find(doc("token", token))
        .first()
    log.debug("Checking Validity of $token")
    if (Optional.ofNullable(user)
            .map { it.getLong("timeout") }
            .orElse(0L) > System.currentTimeMillis()) {
      log.debug("Valid Request")
      return user
    }
    log.info("Invalid ${ctx.method()} Request on ${ctx.path()}")
    return null
  }
}
