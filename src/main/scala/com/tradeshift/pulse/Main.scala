package com.tradeshift.pulse

import javax.annotation.PreDestroy

import com.tradeshift.pulse.config._
import org.apache.spark.sql.SparkSession
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Bean, Configuration, Import}


@Configuration
@Import(value = Array(
  classOf[ServerConfiguration],
  classOf[JerseyConfiguration],
  classOf[JacksonConfiguration],
  classOf[PropertiesConfiguration]
))
class Main {
  @Bean
  def spark(): SparkApp = {
    new SparkApp()
  }
}

class SparkApp {
  val logFile = "/tmp/RandomFile.txt"
  val spark = SparkSession.builder.appName("Simple Application").master("local[2]").getOrCreate()


  val logData = spark.read.textFile(logFile).cache()
  val numAs = logData.filter(line => line.contains("a")).count()
  val numBs = logData.filter(line => line.contains("b")).count()
  println(s"Lines with a: $numAs, Lines with b: $numBs")


  @PreDestroy
  def stop(): Unit = {
    spark.stop()
  }
}

object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
