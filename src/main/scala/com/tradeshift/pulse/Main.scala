package com.tradeshift.pulse

import java.io.InputStreamReader
import java.sql.Timestamp
import java.time.Instant
import java.util.{Locale, Properties}
import javax.annotation.{PostConstruct, PreDestroy}

import com.tradeshift.pulse.GEOService.randomCountryIso
import com.tradeshift.pulse.config._
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.{Bean, Configuration, Import}
import org.springframework.core.env.Environment

import scala.util.Random


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
  def spark(env: Environment): SparkApp = {
    new SparkApp(env)
  }
}

case class AuditEvent(source: String, dest: String, timestamp: Timestamp)

case class AggregateEvent(source: String, dest: String, start_at: Timestamp, end_at: Timestamp, volume: Long)

object Time extends Serializable {
  def randomTime = {
    if (Random.nextInt(1) == 0) {
      new Timestamp(Instant.now().plusSeconds(Random.nextInt(5)).toEpochMilli)
    } else {
      new Timestamp(Instant.now().minusSeconds(Random.nextInt(5)).toEpochMilli)
    }
  }

}

class SparkApp(val env: Environment) {
  val spark = SparkSession.builder.appName("Pulse").master("local[2]").getOrCreate()

  import org.apache.spark.sql.functions._
  import spark.implicits._

  val numRecords: Int = 50000
  val partitions: Int = 1
  val recordsPerPartition = numRecords / partitions

  val properties = new Properties()
  properties.setProperty("driver", env.getProperty("database.driver"))
  properties.setProperty("user", env.getProperty("database.user"))

  val seedRdd = spark.sparkContext.parallelize(Seq.fill(partitions)(recordsPerPartition), partitions)
  val randomEvents = seedRdd.flatMap(records => Seq.fill(records)(
    AuditEvent(randomCountryIso, randomCountryIso, Time.randomTime))).toDS()


  val toWindowStart = udf { (window: GenericRowWithSchema) =>
    window.getTimestamp(0)
  }

  @PreDestroy
  def stop(): Unit = {
    spark.stop()
  }

  @PostConstruct
  def startProcessing() = {
    randomEvents
      //.filter(line => line.channel.map(c => "TRADESHIFT".equals(c.id)).getOrElse(false))
      .groupBy(col("source"), col("dest"), window($"timestamp", "5 seconds") as ("start_at"))
      .count()
      .map(r => AggregateEvent(
        r.getString(0),
        r.getString(1),
        r.get(2).asInstanceOf[GenericRowWithSchema].getTimestamp(0),
        r.get(2).asInstanceOf[GenericRowWithSchema].getTimestamp(1),
        r.getLong(3)))
      .write
      .mode(SaveMode.Append)
      .jdbc(env.getProperty("database.url"), "aggregated_events", properties);
  }
}

case class Country(iso: String, lat: String, lon: String)

object GEOService {

  import com.github.tototoshi.csv._


  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  val reader = CSVReader.open(new InputStreamReader(Main.getClass.getResourceAsStream("/country_centroids_all.csv")))
  val map = reader.all().map(l => l(12) -> {
    Country(l(12), l(0), l(1))
  }).toMap
  reader.close()

  val locales = Locale.getISOCountries


  def lookup(iso: String): Option[Country] = map.get(iso)

  def toCountry(iso: String): Country = {
    lookup(iso).getOrElse(lookup("US").get)
  }

  def randomCountryIso(): String = {
    locales(Random.nextInt(20))
  }

  def randomCountry(): Country = {
    toCountry(randomCountryIso())
  }
}

object Main {
  def main(args: Array[String]) {
    SpringApplication.run(classOf[Main], args: _*)
  }
}
