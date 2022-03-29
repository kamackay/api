package com.keithmackay.api.routes

import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import com.keithmackay.api.auth.RequestValidator
import com.keithmackay.api.tasks.GoodMorningTask
import com.keithmackay.api.utils.async
import io.javalin.apibuilder.ApiBuilder.path

@Singleton
class EmailRouter @Inject
internal constructor(
  private val validator: RequestValidator,
  private val injector: Injector
) : Router {

  override fun routes() {
    path("email") {
      validator.securePost("/goodMorning", { ctx, doc, user ->
        if (user.admin) {
          async(this::sendEmail)
          ctx.result("Sending Good Morning Email")
        }
      })
    }
  }

  private fun sendEmail() {
    injector.getInstance(GoodMorningTask::class.java).run()
  }

  override fun isHealthy() = true
}