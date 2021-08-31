package com.keithmackay.api.email

import com.google.inject.Inject
import com.google.inject.Singleton
import com.keithmackay.api.utils.Credentials
import com.keithmackay.api.utils.getLogger
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import java.io.IOException

@Singleton
class EmailSender @Inject
internal constructor(private val creds: Credentials) {
  private val log = getLogger(this::class)

  fun mainUser() = "keith@keithm.io"

  fun send(title: String, body: String, recipient: String) {
    val from = Email("api@keithm.io")
    val to = Email(recipient)
    val content = Content("text/html", body)
    val mail = Mail(from, title, to, content)
    val sg = SendGrid(creds.getString("sendgrid-key"))
    val request = Request()
    try {
      request.method = Method.POST
      request.endpoint = "mail/send"
      request.body = mail.build()
      val response: Response = sg.api(request)
      log.info("Successfully Sent Email to {}", recipient)
    } catch (ex: IOException) {
      log.error("Error Sending Email", ex)
    }
  }
}