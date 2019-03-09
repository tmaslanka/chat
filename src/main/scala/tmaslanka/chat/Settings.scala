package tmaslanka.chat

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

case class Settings(config: Config) {

  val host: String = config.getString("chat.server.host")
  val port: Int = config.getInt("chat.server.port")
  val `stop-timeout`: FiniteDuration = config.getDuration("chat.server.stop-timeout")

  val numberOfShards: Int = config.getInt("chat.akka.entities.number-of-shards")

  val askTimeout = Timeout(config.getDuration("chat.akka.ask-timeout"))

  val `chat-messages-query-limit`: Int = config.getInt("chat.messages.query-limit")

  val cassandraConfig = CassandraConfig.fromConfig(config.getConfig("cassandra"))

  private implicit def toScalaDuration(d: java.time.Duration): FiniteDuration = FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
}

object CassandraConfig {
  def fromConfig(config: Config): CassandraConfig = CassandraConfig(
    config.getString("host"),
    config.getInt("port")
  )
}
final case class CassandraConfig(host: String, port: Int)