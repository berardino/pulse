package com.tradeshift.pulse.akka

import akka.actor.ActorSystem
import org.springframework.core.env.Environment

class Akka(env: Environment) {
  val system = ActorSystem("pulse")
}
