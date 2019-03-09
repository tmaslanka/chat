package tmaslanka.chat

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import tmaslanka.chat.server.ServerModule

object Main extends App with StrictLogging {
  val module = new ServerModule(Settings(ConfigFactory.load()))

  module.start(logger.info("Server started"))

  Runtime.getRuntime.addShutdownHook(new Thread(() => {
    module.server.stop()
  }))
}
