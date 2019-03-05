package tmaslanka.chat.services

import tmaslanka.chat.model.commands.{CreateUserCommand, UserCreatedResponse}
import tmaslanka.chat.model.domain.UserId

import scala.concurrent.Future

class UserService {

  val result = Future.successful("")

  def getUser(userId: UserId) = result

  def createUser(request: CreateUserCommand): Future[UserCreatedResponse] = Future.successful {
    UserCreatedResponse(UserId("abc"))
  }
}
