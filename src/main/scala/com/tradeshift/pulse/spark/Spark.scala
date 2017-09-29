package com.tradeshift.pulse.spark

import java.sql.Timestamp
import java.time.Instant
import java.util.{Properties, UUID}
import javax.annotation.{PostConstruct, PreDestroy}

import com.tradeshift.pulse.geo.Geo
import com.tradeshift.pulse.lang.UUIDs
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.springframework.core.env.Environment

import scala.util.Random


case class AuditEvent(source: String, dest: String, timestamp: Timestamp)

case class AggregateEvent(id: String, //UUID
                          source: String,
                          source_lat: String,
                          source_lon: String,
                          dest: String,
                          dest_lat: String,
                          dest_lon: String,
                          start_at: Timestamp,
                          end_at: Timestamp,
                          volume: Long)

object Time extends Serializable {
  def randomTime = {
    if (Random.nextInt(1) == 0) {
      new Timestamp(Instant.now().plusSeconds(Random.nextInt(5)).toEpochMilli)
    } else {
      new Timestamp(Instant.now().minusSeconds(Random.nextInt(5)).toEpochMilli)
    }
  }
}

class Spark(val env: Environment) {
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
    AuditEvent(Geo.randomCountryIso, Geo.randomCountryIso, Time.randomTime))).toDS()


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
      .groupBy(col("source"), col("dest"), window($"timestamp", "5 seconds"))
      .count()
      .map(r => {
        val source = r.getString(0)
        val source_country = Geo.toCountry(source)
        val dest = r.getString(1)
        val dest_country = Geo.toCountry(dest)
        val window = r.get(2).asInstanceOf[GenericRowWithSchema]
        val start_at = window.getTimestamp(0)
        val end_at = window.getTimestamp(1)
        AggregateEvent(
          UUIDs.timeUUID().toString,
          source,
          source_country.lat,
          source_country.lon,
          dest,
          dest_country.lat,
          dest_country.lon,
          start_at,
          end_at,
          r.getLong(3))
      })
      .write
      .mode(SaveMode.Append)
      .jdbc(env.getProperty("database.url"), "aggregated_events", properties);
  }
}
