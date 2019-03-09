package tmaslanka.chat.server

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import tmaslanka.chat.Settings
import tmaslanka.chat.actor._
import tmaslanka.chat.repository.InMemoryUserRepository
import tmaslanka.chat.services.{ChatService, UserService}

import scala.concurrent.ExecutionContext

class ServerModule {
  implicit val system: ActorSystem = ActorSystemFactory.startActorSystem
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ex: ExecutionContext = system.dispatcher

  val settings = Settings(ConfigFactory.load())

  val cassandraReadJournal: CassandraReadJournal =
    PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val queries = new ChatQueries(cassandraReadJournal, settings)

  //todo chage to Cassandra repository
  val userRepository = new InMemoryUserRepository()

  val protocol = Sharding.startSharding(settings, () => new ChatActor(queries, userRepository))

  val userService = new UserService(userRepository)
  val chatService = new ChatService(protocol, userRepository)

  val routes = new Routes(settings, userService, chatService)
  val server = new Server(settings, routes)
}
