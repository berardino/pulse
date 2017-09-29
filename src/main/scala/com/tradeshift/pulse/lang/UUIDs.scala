package com.tradeshift.pulse.lang

import java.util.UUID

import com.fasterxml.uuid.Generators

object UUIDs {
  private[this] val timeUUIDGenerator = Generators.timeBasedGenerator

  def timeUUID(): UUID = {
    timeUUIDGenerator.generate()
  }

}
