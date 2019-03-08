package tmaslanka.chat.server

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import tmaslanka.chat.model.commands._
import tmaslanka.chat.model.domain.{ChatId, UserId}
import tmaslanka.chat.model.json.JsonSupport
import tmaslanka.chat.services.{ChatService, UserService}

import scala.concurrent.ExecutionContext

class Routes(userService: UserService,
             chatService: ChatService)
            (implicit ex: ExecutionContext) extends JsonSupport {

  // @formatter:off
  def users: Route = pathPrefix("users") {
    pathEndOrSingleSlash {
      put {
        entity(as[CreateUserCommand]) { request =>
          complete(
            userService.createUser(request)
              .map(createResourceToResponse))
        }
      }
    } ~
    pathPrefix(Segment) { userIdParam =>
      val userId = UserId(userIdParam)
      pathEndOrSingleSlash {
        get {
          complete(userService.getUser(userId)
            .map(resourceGetToResponse))
          }
      } ~
      path("chats") {
        complete(chatService.getUserChats(userId))
      }
    }
  }

  def chats: Route = pathPrefix("chats") {
    pathEndOrSingleSlash {
      put {
        entity(as[CreateChatCommand]) { command =>
          onSuccess(chatService.createChat(command))(completeChatCommandResponse)
        }
      }
    } ~
    pathPrefix(Segment) { chatIdParam =>
      val chatId = ChatId(chatIdParam)
      pathEndOrSingleSlash {
        get {
          complete(chatService.getChat(chatId))
        }
      } ~
      path("messages") {
        get {
          complete(chatService.getChatMessages(chatId))
        } ~
        put {
          entity(as[AddMessageCommand]) { command =>
            onSuccess(chatService.appendMessage(chatId, command))(completeChatCommandResponse)
          }
        }
      }
    }
  }
  // @formatter:on

  private def createResourceToResponse[A](maybeData: Option[A]): (StatusCode, Option[A]) = maybeData match {
    case Some(data) => StatusCodes.OK -> Some(data)
    case None => StatusCodes.BadRequest -> None
  }

  private def resourceGetToResponse[A](maybeData: Option[A]): (StatusCode, Option[A]) = maybeData match {
    case Some(data) => StatusCodes.OK -> Some(data)
    case None => StatusCodes.NotFound -> None
  }

  private def completeChatCommandResponse(response: ChatCommandResponse) = response match {
    case Confirm => complete(StatusCodes.OK)
    case NotFound => complete(StatusCodes.NotFound)
    case Reject => complete(StatusCodes.BadRequest)
    case UnAuthorized => complete(StatusCodes.Unauthorized)
    case response: ChatCreated => complete(response)
  }
}
