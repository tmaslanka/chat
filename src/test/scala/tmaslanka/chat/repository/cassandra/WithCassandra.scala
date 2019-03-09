package tmaslanka.chat.repository.cassandra

import akka.persistence.cassandra.testkit.CassandraLauncher
import com.datastax.driver.core.SocketOptions
import com.outworkers.phantom.dsl._
import org.scalatest.{BeforeAndAfterAll, Suite}
import tmaslanka.chat.repository.UsersRepository

import scala.language.reflectiveCalls

trait WithCassandra extends BeforeAndAfterAll {
  this: Suite =>

  val port = 9042

  private var connector: CassandraConnection = _

  private var database: CassandraDatabase = _

  def repository: UsersRepository = database.usersRepository

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    CassandraLauncher.start(new java.io.File("target/cassandra"),
      CassandraLauncher.DefaultTestConfigResource, clean = true, port = port)

    val keyspace = CassandraKeySpace.keyspace

    connector = ContactPoint.apply("localhost", port)
      .withClusterBuilder(_.withSocketOptions(
        new SocketOptions()
          .setConnectTimeoutMillis(20000)
          .setReadTimeoutMillis(20000)))
      .noHeartbeat()
      .keySpace(
        keyspace.ifNotExists().`with`(
          replication eqs SimpleStrategy.replication_factor(1)))


    database = new CassandraDatabase(connector)
    database.create()
  }
}
