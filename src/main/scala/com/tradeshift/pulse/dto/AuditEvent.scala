package com.tradeshift.pulse.dto

case class Channel(id: String)
case class Connection(connectionType: String, id: String, state: String, companyAccountId: String);

case class AuditEvent(eventType: String,
                      channel: Option[Channel],
                      connection: Option[Connection],
                      tenantId: String,
                      documentId: Option[String],
                      timestamp: Long)


case class TradeshiftDispatch(source: String, dest: String, volume: String)

case class SSE(timestamp: java.sql.Timestamp, event: String, data: String)