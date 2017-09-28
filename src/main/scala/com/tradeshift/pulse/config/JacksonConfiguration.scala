package com.tradeshift.pulse.config

import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategy, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
@ImportAutoConfiguration(Array(
  classOf[HttpMessageConvertersAutoConfiguration]))
class JacksonConfiguration {

  @Bean
  def jacksonObjectMapper(): ObjectMapper = {
    new ObjectMapper()
      .registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.INDENT_OUTPUT)
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
  }
}
