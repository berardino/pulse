package com.tradeshift.pulse.rest

import javax.inject.{Inject, Singleton}
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{Context, MediaType}
import javax.ws.rs.{GET, Produces}

import akka.actor.{Actor, ActorLogging, Props}
import com.fasterxml.jackson.databind.ObjectMapper
import com.tradeshift.pulse.akka.Akka
import com.tradeshift.pulse.geo.{Country, Geo}
import org.eclipse.jetty.io.EofException
import org.glassfish.jersey.media.sse.{EventOutput, OutboundEvent, SseFeature}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

case class Event(source: Country, dest: Country, volume: Int)


case object Tick

class EventOutputSSEActor(eventOutput: EventOutput, mapper: ObjectMapper, remoteAddr: String) extends Actor with ActorLogging {

  context.system.scheduler.scheduleOnce(2 seconds, self, Tick)

  override def receive = {
    case Tick => {
      if (eventOutput.isClosed) {
        log.info("Connection from {} is closed, shutting down {}", remoteAddr, self.path)
        context.stop(self)
      }
      val event = Event(Geo.randomCountry(), Geo.randomCountry(), Math.abs(Random.nextInt(1000000)))
      val payload = mapper.writeValueAsString(event)
      val sse = new OutboundEvent.Builder().mediaType(MediaType.APPLICATION_JSON_TYPE).data(classOf[String], payload).build
      try {
        eventOutput.write(sse)
        context.system.scheduler.scheduleOnce(2 seconds, self, Tick)
      } catch {
        case ex: EofException => {
          log.info("Unable to write to {}, shutting down {}", remoteAddr, self.path)
          context.stop(self)
        }
      }
    }
  }
}


@Singleton
class EventsResource @Inject()(akka: Akka, mapper: ObjectMapper) {

  import org.glassfish.jersey.media.sse.SseBroadcaster

  private val broadcaster = new SseBroadcaster

  new Thread() {
    override def run(): Unit = {
      try {
        while (true) {
          val event = Event(Geo.randomCountry(), Geo.randomCountry(), Math.abs(Random.nextInt(1000000)))
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
  def getEvents(@Context request: HttpServletRequest): EventOutput = {
    val out = new EventOutput
    akka.system.actorOf(Props(
      new EventOutputSSEActor(out, mapper, request.getRemoteAddr)))
    out
  }

}
