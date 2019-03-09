package tmaslanka.chat

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import tmaslanka.chat.server.ServerModule

object Main extends App with StrictLogging {
  val settings = Settings(ConfigFactory.load())

  if(settings.`start-cassandra-hack`) {
    //dirty hack to start cassandra :P
    startCassandra(settings)
    TimeUnit.HOURS.sleep(1)
  } else {
    startServer(settings)
  }

  private def startServer(settings: Settings): Unit = {
    val module = new ServerModule(settings)

    module.start(logger.info("Server started"))

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      module.server.stop()
    }))
  }

  def startCassandra(settings: Settings): Unit = {
    import akka.persistence.cassandra.testkit.CassandraLauncher
    CassandraLauncher.start(new java.io.File("target/cassandra"),
      CassandraLauncher.DefaultTestConfigResource, clean = true, port = settings.cassandraConfig.port)
  }
}
