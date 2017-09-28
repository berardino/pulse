package com.tradeshift.pulse.rest

import javax.ws.rs.Path

@Path("/")
class RootResource {

  @Path("status")
  def status(): Class[StatusResource] = {
    classOf[StatusResource]
  }

}
