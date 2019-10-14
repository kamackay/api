package com.keithmackay.api.utils

import com.google.inject.Inject
import com.google.inject.Singleton
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import java.io.IOException


@Singleton
class TaskList @Inject
internal constructor() {

  private val log = getLogger(this::class)

  fun send() {
    val from = Email("api@keithmackay.com")
    val subject = "Hello"
    val to = Email("keith@keithmackay.com")
    val content = Content("text/plain",
        "This email is from Keith MacKay's API")
    val mail = Mail(from, subject, to, content)

    val sg = SendGrid(System.getenv("SENDGRID_API_KEY"))
    val request = Request()
    try {
      request.method = Method.POST
      request.endpoint = "mail/send"
      request.body = mail.build()
      val response = sg.api(request)
      log.info(response.statusCode)
      log.info(response.body)
      log.info(response.headers)
    } catch (ex: IOException) {
      throw ex
    }

  }

}