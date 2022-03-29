package com.keithmackay.api.routes


import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.model.SuccessResponse
import com.keithmackay.api.utils.*
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.NotFoundResponse
import org.bson.Document
import org.bson.types.ObjectId

@Singleton
class GroceriesRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(this::class)
  private val groceriesCollection = db.getCollection("groceries")
  private val groceriesListsCollection = db.getCollection("groceries_lists")

  override fun routes() {
    path("groceries") {

      validator.securePut("createList", { ctx, body, user ->
        val name = body.getString("name")
        log.info("${user.username} is requesting to create a Groceries List named $name")
        val exists = groceriesListsCollection.find(doc("name", name)).count() > 0
        if (exists) {
          throw ConflictResponse("List With that name already Exists")
        } else {
          val result = groceriesListsCollection.updateOne(
            doc("name", name),
            set(
              doc("name", name)
                .append("timeCreated", System.currentTimeMillis())
                .append("createdBy", user.username)
                .append("users", threadSafeList(user.username))
            ),
            upsert()
          )
          if (result.upsertedId != null) {
            ctx.status(200).result("Successfully Created $name")
          } else {
            throw InternalServerErrorResponse("Could not create the list")
          }
        }
      })

      validator.secureGet("lists", { ctx, _, user ->
        log.info("${user.username} wants a list of grocery lists")
        ctx.json(groceriesListsCollection.find()
          .map {
            doc("id", it.getObjectId("_id").toString())
              .append("name", it.getString("name"))
          }
          .into(threadSafeList()))
      })

      validator.secureGet("list/:listId", { ctx, _, user ->
        val listId = ctx.pathParam("listId")
        val list = groceriesListsCollection
          .find(doc("_id", ObjectId(listId)))
          .first()
        if (list != null) {
          val users = list.getList("users", String::class.java)
          if (users.contains(user.username)) {
            ctx.json(
              groceriesCollection
                .find(doc("list", listId))
                .into(threadSafeList<Document>())
                .mapNotNull(::cleanDoc)
            )
          } else {
            throw BadRequestResponse("User is not a member of the list")
          }
        } else {
          throw NotFoundResponse("Could not find List")
        }
      })

      validator.securePost("list/:listName", { ctx, body, user ->
        val listName = ctx.pathParam("listName")
        val list = groceriesListsCollection.find(doc("_id", ObjectId(listName)))
          .first()
        if (list != null) {
          val users = list.getList("users", String::class.java)
          if (users.contains(user.username)) {
            groceriesCollection.insertOne(
              doc("name", body.getString("name"))
                .append("list", list.getObjectId("_id").toString())
                .append("addedBy", user.username)
                .append("addedAt", System.currentTimeMillis())
                .append("count", body.getInteger("count"))
                .append("done", false)
                .append("removed", false)
            )
            throw SuccessResponse("Added")
          } else {
            throw BadRequestResponse("User is not a member of the list")
          }
        } else {
          throw NotFoundResponse("Could not find List")
        }
      })

    }
  }

  override fun isHealthy(): Boolean = true
}