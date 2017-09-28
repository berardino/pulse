package com.tradeshift.pulse.config

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.context.{ConfigurationPropertiesAutoConfiguration, PropertyPlaceholderAutoConfiguration}
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
import org.springframework.context.annotation.Configuration


@Configuration
@ImportAutoConfiguration(Array(
  classOf[ConfigurationPropertiesAutoConfiguration],
  classOf[PropertyPlaceholderAutoConfiguration],
  classOf[ProjectInfoAutoConfiguration]))
class PropertiesConfiguration
