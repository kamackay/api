package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.AuthUtils.hashPass
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.SuccessResponse
import com.keithmackay.api.utils.doc
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.set
import com.keithmackay.api.utils.upsert
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.UnauthorizedResponse

@Singleton
class UserRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(this::class)

  private val userCollection = db.getCollection("users")

  override fun routes() {
    path("users") {

      validator.securePut("create", { ctx, body, user ->
        if (user.admin) {
          val result = userCollection.updateOne(doc("username", body.getString("username")),
              set(doc("username", body.getString("username"))
                  .append("firstName", body.getString("firstName"))
                  .append("lastName", body.getString("lastName"))
                  .append("email", body.getString("email"))
                  .append("password", hashPass(body.getString("password")))
                  .append("admin", false)), upsert())
          if (result.upsertedId != null) {
            log.info("Successfully created user ${body.getString("username")}")
            throw SuccessResponse()
          } else {
            throw InternalServerErrorResponse("Could not create list")
          }
        } else {
          throw UnauthorizedResponse()
        }
      })
    }
  }

  override fun isHealthy(): Boolean = true
}