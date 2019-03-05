package tmaslanka.chat

import com.typesafe.scalalogging.StrictLogging
import tmaslanka.chat.server.ServerModule

object Main extends App with StrictLogging {
  logger.info("Server started")
  val module = new ServerModule

  module.server.start()

  Runtime.getRuntime.addShutdownHook(new Thread(() => {
    module.server.stop()
  }))
}
