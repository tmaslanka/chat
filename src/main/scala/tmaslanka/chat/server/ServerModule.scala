package tmaslanka.chat.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import tmaslanka.chat.Settings
import tmaslanka.chat.services.{ChatService, UserService}

import scala.concurrent.ExecutionContext

class ServerModule {
  implicit val system: ActorSystem = ActorSystem("server")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ex: ExecutionContext = system.dispatcher

  val settings = Settings(ConfigFactory.load())

  val userService = new UserService
  val chatService = new ChatService

  val routes = new Routes(userService, chatService)
  val server = new Server(settings, routes)
}
