package com.tradeshift.pulse.rest

import java.util.UUID
import java.util.function.Consumer
import javax.inject.{Inject, Singleton}
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{Context, MediaType}
import javax.ws.rs.{GET, Produces}

import akka.actor.{Actor, ActorLogging, Props}
import com.fasterxml.jackson.databind.ObjectMapper
import com.tradeshift.pulse.akka.Akka
import com.tradeshift.pulse.geo.Country
import org.eclipse.jetty.io.EofException
import org.glassfish.jersey.media.sse.{EventOutput, OutboundEvent, SseFeature}
import org.jooq.{DSLContext, Record}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class Event(id: UUID, source: Country, dest: Country, volume: Long)


case object Tick

class EventOutputSSEActor(eventOutput: EventOutput,
                          mapper: ObjectMapper,
                          remoteAddr: String,
                          dSLContext: DSLContext) extends Actor with ActorLogging {

  context.system.scheduler.scheduleOnce(2 seconds, self, Tick)

  log.info("Created connection from {}", remoteAddr)

  var lastSeenEvent: Option[Event] = None

  def toEvent(t: Record): Event = {
    val source = Country(
      t.get("source").asInstanceOf[String],
      t.get("source_lat").asInstanceOf[String],
      t.get("source_lon").asInstanceOf[String])
    val dest = Country(
      t.get("dest").asInstanceOf[String],
      t.get("dest_lat").asInstanceOf[String],
      t.get("dest_lon").asInstanceOf[String])
    Event(
      t.get("id_uuid").asInstanceOf[UUID],
      source,
      dest,
      t.get("volume").asInstanceOf[Long])
  }

  override def receive = {
    case Tick => {
      if (eventOutput.isClosed) {
        log.info("Connection from {} is closed, shutting down {}", remoteAddr, self.path)
        context.stop(self)
      }

      val query = if (lastSeenEvent.isEmpty) {
        dSLContext.resultQuery(
          """select id::uuid as id_uuid,
            |source, source_lat, source_lon,
            |dest, dest_lat, dest_lon, volume from aggregated_events order by start_at asc limit 50""".stripMargin)
      } else {
        dSLContext.resultQuery(
          """select id::uuid as id_uuid,
            |source, source_lat, source_lon,
            |dest, dest_lat, dest_lon, volume from aggregated_events
            |where id::uuid > ?::uuid order by start_at asc limit 50""".stripMargin, lastSeenEvent.get.id)
      }

      query.forEach(new Consumer[Record] {
        override def accept(t: Record): Unit = {
          val event = toEvent(t)
          lastSeenEvent = Some(event)
          val payload = mapper.writeValueAsString(event)
          val sse = new OutboundEvent.Builder().mediaType(MediaType.APPLICATION_JSON_TYPE).data(classOf[String], payload).build
          try {
            eventOutput.write(sse)
            Thread.sleep(500)
          } catch {
            case ex: EofException => {
              log.info("Unable to write to {}, shutting down {}", remoteAddr, self.path)
              context.stop(self)
            }
          }
        }
      })

      context.system.scheduler.scheduleOnce(2 seconds, self, Tick)
    }
  }
}


@Singleton
class EventsResource @Inject()(akka: Akka, mapper: ObjectMapper, dSLContext: DSLContext) {

  @GET
  @Produces(Array(SseFeature.SERVER_SENT_EVENTS))
  def getEvents(@Context request: HttpServletRequest): EventOutput = {
    val eventOutput = new EventOutput
    akka.system.actorOf(Props(
      new EventOutputSSEActor(
        eventOutput,
        mapper,
        request.getRemoteAddr,
        dSLContext)))
    eventOutput
  }

}
