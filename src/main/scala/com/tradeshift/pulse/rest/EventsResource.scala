package com.tradeshift.pulse.rest

import java.util.Locale
import javax.inject.{Inject, Singleton}
import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, Produces}

import com.fasterxml.jackson.databind.ObjectMapper
import com.tradeshift.pulse.{Country, GEOLookUp}
import org.glassfish.jersey.media.sse.{EventOutput, OutboundEvent, SseFeature}

import scala.util.Random

case class Event(source: Country, dest: Country, volume: Int)


@Singleton
class EventsResource @Inject()(mapper: ObjectMapper) {


  val locales = Locale.getISOCountries

  @volatile var eventOutput = new EventOutput

  def randomCountry(): Country = {
    GEOLookUp.lookup(locales(Random.nextInt(locales.length)))
  }

  @GET
  @Produces(Array(SseFeature.SERVER_SENT_EVENTS))
  def getEvents(): EventOutput = {
    val seq = new EventOutput

    new Thread() {
      override def run(): Unit = {
        try {
          (1 to 100).foreach { id =>
            val event = Event(randomCountry, randomCountry, Math.abs(Random.nextInt(1000000)))
            val payload = mapper.writeValueAsString(event)
            val sse = new OutboundEvent.Builder().mediaType(MediaType.APPLICATION_JSON_TYPE).data(classOf[String], payload).build
            seq.write(sse)
            Thread.sleep(2000)
          }
          seq.close()
        } catch {
          case e@(_: InterruptedException) =>
            e.printStackTrace
        }
      }
    }.start()
    seq
  }

}
