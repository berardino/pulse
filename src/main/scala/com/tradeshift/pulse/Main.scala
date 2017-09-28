package com.tradeshift.pulse

import com.tradeshift.pulse.config._
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Configuration, Import}


@Configuration
@Import(value = Array(
  classOf[ServerConfiguration],
  classOf[JerseyConfiguration],
  classOf[JacksonConfiguration],
  classOf[PropertiesConfiguration],
))
class Main

object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
