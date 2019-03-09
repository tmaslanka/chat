package tmaslanka.chat.repository.cassandra

import com.datastax.driver.core.SocketOptions
import com.outworkers.phantom.dsl._
import tmaslanka.chat.CassandraConfig

import scala.language.reflectiveCalls

class CassandraConnector(cassandraConfig: CassandraConfig) {
  private var _connection: Option[(CassandraConnection, CassandraDatabase)] = None

  def connect(): CassandraDatabase = {
    _connection.getOrElse {
      val keyspace = CassandraKeySpace.keyspace
      val con = ContactPoint(cassandraConfig.host, cassandraConfig.port)
        .withClusterBuilder(_.withSocketOptions(
          new SocketOptions()
            .setConnectTimeoutMillis(20000)
            .setReadTimeoutMillis(20000)))
        .noHeartbeat()
        .keySpace(
          keyspace.ifNotExists().`with`(
            replication eqs SimpleStrategy.replication_factor(1)))


      val database = new CassandraDatabase(con)
      database.create()
      val res = con -> database
      _connection = Some(res)
      res
    }._2
  }

  def stop(): Unit = {
    _connection.foreach {case (connection, _) => connection.session.getCluster.close()}
  }
}
