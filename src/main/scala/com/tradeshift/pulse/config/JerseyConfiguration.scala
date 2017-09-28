package com.tradeshift.pulse.config

import com.tradeshift.pulse.rest.RootResource
import org.glassfish.jersey.server.ResourceConfig
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration
import org.springframework.context.annotation.{Bean, Configuration, Import}

@Configuration
class ResourceConfigConfiguration {

  @Bean
  def resourceConfig(): ResourceConfig = {
    new ResourceConfig {
      register(classOf[RootResource])
    }
  }
}

@Configuration
@Import(Array(classOf[ResourceConfigConfiguration]))
@ImportAutoConfiguration(Array(
  classOf[JerseyAutoConfiguration]))
class JerseyConfiguration
