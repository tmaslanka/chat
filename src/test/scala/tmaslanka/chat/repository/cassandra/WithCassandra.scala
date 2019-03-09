package tmaslanka.chat.repository.cassandra

import akka.persistence.cassandra.testkit.CassandraLauncher
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import tmaslanka.chat.Settings
import tmaslanka.chat.repository.UsersRepository

trait WithCassandra extends BeforeAndAfterAll {
  this: Suite =>

  val port = 9042

  val settings = Settings(ConfigFactory.load())

  private var database: CassandraDatabase = _

  def repository: UsersRepository = database.usersRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    CassandraLauncher.start(new java.io.File("target/cassandra"),
      CassandraLauncher.DefaultTestConfigResource, clean = true, port = settings.cassandraConfig.port)

    database = new CassandraConnector(settings.cassandraConfig).connect()
  }
}
