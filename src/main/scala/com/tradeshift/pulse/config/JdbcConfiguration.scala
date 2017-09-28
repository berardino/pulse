package com.tradeshift.pulse.config

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.{DataSourceAutoConfiguration, DataSourceTransactionManagerAutoConfiguration}
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
import org.springframework.context.annotation.Configuration


@Configuration
@ImportAutoConfiguration(Array(
  classOf[DataSourceAutoConfiguration],
  classOf[DataSourceTransactionManagerAutoConfiguration],
  classOf[JooqAutoConfiguration]))
class JdbcConfiguration
