package com.tradeshift.pulse.rest

import javax.ws.rs.{Path, Produces}

@Path("/")
class RootResource {

  @Path("status")
  def status(): Class[StatusResource] = {
    classOf[StatusResource]
  }

  @Path("events")
  def events(): Class[EventsResource] = {
    classOf[EventsResource]
  }

}
