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
import com.sendgrid.helpers.mail.objects.Personalization
import java.io.IOException

@Singleton
class EmailSender @Inject
internal constructor(private val creds: Credentials) {
  private val log = getLogger(this::class)

  fun mainUser() = "keith@keithm.io"

  fun send(title: String, body: String, recipient: String) {
    this.send(title, body, listOf(recipient))
  }

  fun send(title: String, body: String, recipients: List<String>) {
    val from = Email("api@keithm.io")
    val content = Content("text/html", body)
    val mail = Mail()
    mail.setFrom(from)
    mail.setSubject(title)
    mail.addContent(content)
    val personalization = Personalization()
    recipients
      .map(::Email)
      .forEach(personalization::addTo)
    mail.addPersonalization(personalization)
    val sg = SendGrid(creds.getString("sendgrid-key"))
    val request = Request()
    try {
      request.method = Method.POST
      request.endpoint = "mail/send"
      request.body = mail.build()
      val response: Response = sg.api(request)
      log.info("Successfully Sent Email to {}", recipients)
    } catch (ex: IOException) {
      log.error("Error Sending Email", ex)
    }
  }
}