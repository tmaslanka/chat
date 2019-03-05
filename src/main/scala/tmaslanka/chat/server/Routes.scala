package tmaslanka.chat.server

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import tmaslanka.chat.model.commands.{CreateMessageCommand, CreateUserCommand}
import tmaslanka.chat.model.domain.{ChatId, UserId}
import tmaslanka.chat.model.json.JsonSupport
import tmaslanka.chat.services.{ChatService, UserService}

import scala.concurrent.ExecutionContext

class Routes(userService: UserService,
             chatService: ChatService)
            (implicit system: ActorSystem,
             mat: Materializer,
             ex: ExecutionContext) extends JsonSupport {

  // @formatter:off
  def users: Route = pathPrefix("users") {
    pathEndOrSingleSlash {
      post {
        entity(as[CreateUserCommand]) { request =>
          complete(userService.createUser(request))
        }
      }
    } ~
    pathPrefix(Segment) { userIdParam =>
      val userId = UserId(userIdParam)
      (pathEnd & get) {
        complete(userService.getUser(userId))
      } ~
      path("chats") {
        complete(chatService.getUserChats(userId))
      }
    }
  }

  def chats: Route = pathPrefix("chats") {
    pathPrefix(Segment) { chatIdParam =>
      val chatId = ChatId(chatIdParam)
      (pathEnd & get) {
        complete(chatService.getChat(chatId))
      } ~
      path("messages") {
        get {
          complete(chatService.getChatMessages(chatId))
        } ~
        put {
          entity(as[CreateMessageCommand]) { request =>
            complete(chatService.appendMessage(chatId, request))
          }
        }
      }
    }
  }
  // @formatter:on
}
