package com.tradeshift.pulse.rest

import java.time.Instant
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.core.Response

case class Status(serviceName: String, timestamp: Instant)

@Singleton
class StatusResource {

  @GET
  def getStatus(): Response = {
    Response.ok(Status("Tradeshift Pulse", Instant.now())).build()
  }
}
