package com.tradeshift.pulse.config

import java.net.{ConnectException, SocketTimeoutException}

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.{FlywayAutoConfiguration, FlywayMigrationStrategy}
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.policy.SimpleRetryPolicy.DEFAULT_MAX_ATTEMPTS
import org.springframework.retry.support.RetryTemplate
import org.springframework.retry.{RetryCallback, RetryContext}

import scala.collection.JavaConversions


@Configuration
@ImportAutoConfiguration(Array(
  classOf[FlywayAutoConfiguration]))
class MigrationConfiguration {

  @Bean
  def migrationRetryTemplate(): RetryTemplate = {
    val retryTemplate = new RetryTemplate
    val retryPolicy = new SimpleRetryPolicy(
      DEFAULT_MAX_ATTEMPTS,
      JavaConversions.mapAsJavaMap(Map(
        classOf[SocketTimeoutException] -> true,
        classOf[ConnectException] -> true)),
      true,
      true)
    retryTemplate.setRetryPolicy(retryPolicy)
    val backoffPolicy = new ExponentialBackOffPolicy
    retryTemplate.setBackOffPolicy(backoffPolicy)

    retryTemplate
  }

  @Bean
  def flywayMigrationStrategy(retryTemplate: RetryTemplate): FlywayMigrationStrategy = {
    new FlywayMigrationStrategy {
      override def migrate(flyway: Flyway) = retryTemplate.execute(new RetryCallback[Unit, Exception] {
        override def doWithRetry(context: RetryContext) = flyway.migrate()
      })
    }
  }

}


