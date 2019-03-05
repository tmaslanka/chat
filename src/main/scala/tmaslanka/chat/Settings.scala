package tmaslanka.chat

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

case class Settings(config: Config) {
  val host: String = config.getString("chat.server.host")
  val port: Int = config.getInt("chat.server.port")
  val `stop-timeout`: FiniteDuration = config.getDuration("chat.server.stop-timeout")

  private implicit def toScalaDuration(d: java.time.Duration): FiniteDuration = FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
}