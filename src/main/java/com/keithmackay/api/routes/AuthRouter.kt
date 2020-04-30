package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.AuthUtils
import com.keithmackay.api.auth.AuthUtils.hashPass
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.authSessionAttribute
import com.keithmackay.api.benchmark.Benchmark
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.InvalidAuthenticationResponse
import com.keithmackay.api.model.LoginModel
import com.keithmackay.api.model.User
import com.keithmackay.api.utils.cleanDoc
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.set
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import org.bson.Document

@Singleton
class AuthRouter @Inject
internal constructor(
    private val db: IDatabase,
    private val requestValidator: RequestValidator
) : Router {
  private val log = getLogger(AuthRouter::class)

  override fun routes() {
    path("auth") {
      post("login", this::login)
      post("logout/:username", this::logout)
      requestValidator.secureGet("checkAuth", { ctx, _, user ->
        ctx.json(doc("valid", true).append("username", user.username))
      })
      requestValidator.securePost("/setPassword", this::setPassword)
    }
  }

  private fun setPassword(ctx: Context, body: Document, user: User) {
    val creds = ctx.bodyAsClass(LoginModel::class.java)
    if (user.username == creds.username) {
      db.getCollection("users").updateOne(doc("username", creds.username),
          set(doc("password", hashPass(creds.password))))
      ctx.status(200).result("Updated")
    } else {
      throw InvalidAuthenticationResponse()
    }
  }

  @Benchmark(limit = 15)
  private fun login(ctx: Context) {
    val creds = ctx.bodyAsClass(LoginModel::class.java)
    val documentElective = AuthUtils.login(
        db.getCollection("users"),
        db.getCollection("tokens"),
        creds)
    documentElective
        .map<Document> { cleanDoc(it) }
        .ifPresentOrElse({
          ctx.sessionAttribute(authSessionAttribute(), it.getString("token"))
          ctx.json(it)
        }, { ctx.status(400) })

  }

  @Benchmark(limit = 15)
  private fun logout(ctx: Context) {
    ctx.status(200).result("Successful")
    AuthUtils.logout(db.getCollection("tokens"), ctx.pathParam("username"))
  }

  override fun isHealthy(): Boolean = true
}
