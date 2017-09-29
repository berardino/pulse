package com.tradeshift.pulse.geo

import java.io.InputStreamReader
import java.util.Locale

import com.tradeshift.pulse.Main

import scala.util.Random

case class Country(iso: String, lat: String, lon: String)

object Geo {

  import com.github.tototoshi.csv._


  implicit object MyFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  val reader = CSVReader.open(new InputStreamReader(Main.getClass.getResourceAsStream("/country_centroids_all.csv")))
  val map = reader.all().map(l => l(12) -> {
    Country(l(12), l(0), l(1))
  }).toMap
  reader.close()

  val locales_source = Random.shuffle(Array(Locale.getISOCountries: _*).toSeq)
  val locales_desc = Random.shuffle(Array(Locale.getISOCountries: _*).toSeq).toBuffer


  def lookup(iso: String): Option[Country] = map.get(iso)

  def toCountry(iso: String): Country = {
    lookup(iso).getOrElse(lookup("US").get)
  }

  def randomCountrySourceIso(): String = {
    locales_source(Random.nextInt(locales_source.length))
  }

  def randomCountryDestIso(): String = {
    locales_desc(Random.nextInt(locales_desc.length))
  }
}
