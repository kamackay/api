package com.keithmackay.api.routes


import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.db.IDatabase
import com.keithmackay.api.utils.getLogger
import com.keithmackay.api.utils.threadSafeList
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

@Singleton
class GroceriesRouter @Inject
internal constructor(private val validator: RequestValidator, db: IDatabase) : Router {
  private val log = getLogger(GroceriesRouter::class)
  private val groceriesCollection = db.getCollection("groceries")

  override fun routes() {
    path("groceries") {
      get("lists") { ctx ->
        validator.validateThen {
          log.info("$it wants a list of groceries")
          ctx.json(groceriesCollection.distinct("list", String::class.java)
              .into(threadSafeList()))
        }
      }
    }
  }

}