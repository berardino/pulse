package com.tradeshift.pulse

import java.io.InputStreamReader
import java.sql.Timestamp
import java.util.Properties
import javax.annotation.PreDestroy

import com.tradeshift.pulse.config._
import com.tradeshift.pulse.dto.{AuditEvent, SSE, TradeshiftDispatch}
import org.apache.spark.sql.SparkSession
import org.mortbay.util.ajax.JSON
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Bean, Configuration, Import}


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
  val logFile = "/tmp/events.txt"
  val spark = SparkSession.builder.appName("Simple Application").master("local[2]").getOrCreate()

  import spark.implicits._

  val logData = spark.read.json(logFile).as[AuditEvent]
  logData.show()
  val result = logData.filter(line => line.channel.map(c => "TRADESHIFT".equals(c.id)).getOrElse(false))
    .map(ProcessEvent.apply)

  val properties = new Properties();
  properties.setProperty("driver", "org.postgresql.Driver")
  properties.setProperty("user", "pulse")
  //result.write.mode("append").jdbc("jdbc:postgresql://192.168.99.100:6432/pulse", "events_per_second", properties);

  result.show()


  @PreDestroy
  def stop(): Unit = {
    spark.stop()
  }
}


object ProcessEvent extends Serializable {

  def apply(event: AuditEvent): SSE = {
    val data = TradeshiftDispatch("US", "FR", "7765543")
    SSE(new Timestamp(event.timestamp), "document-sent", JSON.toString(data))
  }
}

case class Country(iso: String, lat: String, lon: String)

object GEOLookUp {

  import com.github.tototoshi.csv._


  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  val reader = CSVReader.open(new InputStreamReader(Main.getClass.getResourceAsStream("/country_centroids_all.csv")))
  val map = reader.all().map(l => l(12) -> {
    Country(l(12), l(0), l(1))
  }).toMap
  reader.close()


  def lookup(iso: String): Country = {
    map(iso)
  }
}

object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
