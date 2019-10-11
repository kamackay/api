package com.keithmackay.api.model

import org.eclipse.jetty.client.HttpResponseException
import org.eclipse.jetty.http.HttpStatus

class SuccessResponse(message: String = "Success")
  : io.javalin.http.HttpResponseException(HttpStatus.OK_200, message)
