package com.tradeshift.pulse.config

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.{EmbeddedServletContainerAutoConfiguration, HttpEncodingAutoConfiguration, MultipartAutoConfiguration, ServerPropertiesAutoConfiguration}
import org.springframework.context.annotation.Configuration


@Configuration
@ImportAutoConfiguration(Array(
  classOf[EmbeddedServletContainerAutoConfiguration],
  classOf[HttpEncodingAutoConfiguration],
  classOf[MultipartAutoConfiguration],
  classOf[ServerPropertiesAutoConfiguration]))
class ServerConfiguration
