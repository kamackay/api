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
class EmailSender @Inject
internal constructor(secretGrabber: SecretGrabber) {

  private val log = getLogger(this::class)
  private val sg = SendGrid(secretGrabber.getSecret("sendgrid-key").asString)

  fun send(to: String,
           subject: String,
           content: Content,
           from: String = "api@keithmackay.com") {
    val email = Mail(Email(from), "[KeithMacKay.com]: $subject", Email(to), content)
    val request = Request()
    try {
      request.method = Method.POST
      request.endpoint = "mail/send"
      request.body = email.build()
      val response = sg.api(request)
      log.info(response.statusCode)
      log.info(response.body)
      log.info(response.headers)
    } catch (ex: IOException) {
      log.error("Could not send email", ex)
    }
  }

  fun send(to: String,
           subject: String,
           content: String,
           from: String = "api@keithmackay.com") =
      this.send(to = to, subject = subject,
          content = Content("text/plain", content),
          from = from)

}