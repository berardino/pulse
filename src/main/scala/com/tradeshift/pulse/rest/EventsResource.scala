package com.tradeshift.pulse.rest

import javax.inject.{Inject, Singleton}
import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, Produces}

import com.fasterxml.jackson.databind.ObjectMapper
import com.tradeshift.pulse.GEOService.randomCountry
import com.tradeshift.pulse.{Country, GEOService}
import org.glassfish.jersey.media.sse.{EventOutput, OutboundEvent, SseFeature}

import scala.util.Random

case class Event(source: Country, dest: Country, volume: Int)


@Singleton
class EventsResource @Inject()(mapper: ObjectMapper) {

  @volatile var eventOutput = new EventOutput

  import org.glassfish.jersey.media.sse.SseBroadcaster

  private val broadcaster = new SseBroadcaster

  new Thread() {
    override def run(): Unit = {
      try {
        while (true) {
          val event = Event(randomCountry(), randomCountry(), Math.abs(Random.nextInt(1000000)))
          val payload = mapper.writeValueAsString(event)
          val sse = new OutboundEvent.Builder().mediaType(MediaType.APPLICATION_JSON_TYPE).data(classOf[String], payload).build
          broadcaster.broadcast(sse)
          Thread.sleep(2000)
        }
      } catch {
        case e@(_: InterruptedException) =>
          e.printStackTrace
      }
    }
  }.start()

  @GET
  @Produces(Array(SseFeature.SERVER_SENT_EVENTS))
  def getEvents(): EventOutput = {
    val out = new EventOutput
    broadcaster.add(out)
    out
  }

}
