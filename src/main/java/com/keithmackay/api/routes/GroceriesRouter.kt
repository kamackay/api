package com.keithmackay.api.routes


import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.ConflictResponse
import io.javalin.http.InternalServerErrorResponse

@Singleton
class GroceriesRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(this::class)
  private val groceriesCollection = db.getCollection("groceries")
  private val groceriesListsCollection = db.getCollection("groceries_lists")

  override fun routes() {
    path("groceries") {

      validator.securePut("createList") { ctx, body, user ->
        val name = body.getString("name")
        log.info("${user.username} is requesting to create a Groceries List named $name")
        val exists = groceriesListsCollection.find(doc("name", name)).count() > 0
        if (exists) {
          throw ConflictResponse("List With that name already Exists")
        } else {
          val result = groceriesListsCollection.updateOne(doc("name", name),
              set(doc("name", name)
                  .append("timeCreated", System.currentTimeMillis())
                  .append("createdBy", user.username)
                  .append("users", threadSafeList(user.username))),
              upsert())
          if (result.upsertedId != null) {
            ctx.status(200).result("Successfully Created $name")
          } else {
            throw InternalServerErrorResponse("Could not create the list")
          }
        }
      }

      validator.secureGet("lists") { ctx, _, user ->
        log.info("${user.username} wants a list of groceries")
        ctx.json(groceriesListsCollection.distinct("name", String::class.java)
            .into(threadSafeList()))
      }

      validator.securePost("list/:listname") { ctx, body, user ->
        val list = ctx.pathParam("list")

        // TODO
      }

    }
  }

}