package com.tradeshift.pulse

import com.tradeshift.pulse.akka.Akka
import com.tradeshift.pulse.config._
import com.tradeshift.pulse.spark.Spark
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Bean, Configuration, Import}
import org.springframework.core.env.Environment


@Configuration
@Import(value = Array(
  classOf[ServerConfiguration],
  classOf[JerseyConfiguration],
  classOf[JacksonConfiguration],
  classOf[PropertiesConfiguration],
  classOf[JdbcConfiguration]
  //classOf[MigrationConfiguration]
))
class Main {

  @Bean
  def spark(env: Environment): Spark = {
    new Spark(env)
  }

  @Bean
  def akka(env: Environment): Akka = {
    new Akka(env)
  }
}


object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
