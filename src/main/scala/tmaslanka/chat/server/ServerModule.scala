package tmaslanka.chat.server

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import tmaslanka.chat.Settings
import tmaslanka.chat.actor._
import tmaslanka.chat.repository.cassandra.CassandraConnector
import tmaslanka.chat.services.{ChatService, UserService}

import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success

class ServerModule(val settings: Settings) {
  implicit val system: ActorSystem = ActorSystemFactory.startActorSystem
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ex: ExecutionContext = system.dispatcher

  val cassandraReadJournal: CassandraReadJournal =
    PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val queries = new ChatQueries(cassandraReadJournal, settings)

  val cassandraConnector = new CassandraConnector(settings.cassandraConfig)
  val database = cassandraConnector.connect()
  val userRepository = database.usersRepository

  val protocol = Sharding.startSharding(settings, () => new ChatActor(queries, userRepository))

  val userService = new UserService(userRepository)
  val chatService = new ChatService(protocol, userRepository)

  val routes = new Routes(settings, userService, chatService)
  val server = new Server(settings, routes)

  def start(onStarted: => Unit = ()): Unit = {
    server.start()
      .andThen {
        case Success(_) => onStarted
      }
  }

  def stop(): Unit = {
    server.stop()
    Await.ready(system.terminate(), settings.`stop-timeout`)
    cassandraConnector.stop()
  }
}
