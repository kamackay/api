package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.AuthUtils
import com.keithmackay.api.auth.AuthUtils.hashPass
import com.keithmackay.api.authSessionAttribute
import com.keithmackay.api.benchmark.Benchmark
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.LoginModel
import com.keithmackay.api.utils.cleanDoc
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.set
import com.mongodb.client.MongoCollection
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import org.bson.Document

@Singleton
class AuthRouter @Inject
internal constructor(db: IDatabase) : Router {
  private val log = getLogger(AuthRouter::class)
  private val tokenCollection: MongoCollection<Document> = db.getCollection("tokens")
  private val userCollection: MongoCollection<Document> = db.getCollection("users")

  override fun routes() {
    path("auth") {
      post("login", this::login)
      post("logout/:username", this::logout)
      post("/setPassword", this::setPassword)
    }
  }

  private fun setPassword(ctx: Context) {
    val creds = ctx.bodyAsClass(LoginModel::class.java)
    this.userCollection.updateOne(doc("username", creds.username),
        set(doc("password", hashPass(creds.password))))
    ctx.status(200).result("Updated")

  }

  @Benchmark(limit = 15)
  private fun login(ctx: Context) {
    val creds = ctx.bodyAsClass(LoginModel::class.java)
    val documentElective = AuthUtils.login(this.userCollection, this.tokenCollection, creds)
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
    AuthUtils.logout(this.tokenCollection, ctx.pathParam("username"))
  }
}
