package com.tradeshift.pulse

import javax.annotation.PreDestroy

import com.tradeshift.pulse.config._
import com.tradeshift.pulse.dto.{AuditEvent, SSE, TradeshiftDispatch}
import org.apache.spark.sql.SparkSession
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Bean, Configuration, Import}
import java.sql.Timestamp
import java.util.Properties

import com.tradeshift.users.service.config.MigrationConfiguration
import org.mortbay.util.ajax.JSON


@Configuration
@Import(value = Array(
  classOf[ServerConfiguration],
  classOf[JerseyConfiguration],
  classOf[JacksonConfiguration],
  classOf[PropertiesConfiguration],
  classOf[JdbcConfiguration],
  classOf[MigrationConfiguration]
))
class Main {
  @Bean
  def spark(): SparkApp = {
    new SparkApp()
  }
}

class SparkApp {
  val logFile = "/Users/ivadermendjieva/auditEvents.txt"
  val spark = SparkSession.builder.appName("Simple Application").master("local[2]").getOrCreate()

  import spark.implicits._

  val logData = spark.read.json(logFile).as[AuditEvent]
  logData.show()
  val result = logData.filter(line => line.channel.map(c => "TRADESHIFT".equals(c.id)).getOrElse(false))
                      .map(ProcessEvent.apply)

  val properties = new Properties();
  properties.setProperty("driver", "org.postgresql.Driver")
  properties.setProperty("user", "pulse")
  result.write.mode("append").jdbc("jdbc:postgresql://127.0.0.1:6432/pulse", "events_per_second", properties);

  result.show()



  @PreDestroy
  def stop(): Unit = {
    spark.stop()
  }
}

object ProcessEvent extends Serializable {

  def apply(event: AuditEvent): SSE = {
    val data = TradeshiftDispatch("US", "FR", "7765543")
    SSE(new Timestamp(event.timestamp), "document-sent", JSON.toString(data));
  }
}

object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
