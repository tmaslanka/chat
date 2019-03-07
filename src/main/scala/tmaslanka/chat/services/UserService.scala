package tmaslanka.chat.services

import tmaslanka.chat.model.commands.{CreateUserCommand, UserCreatedResponse}
import tmaslanka.chat.model.domain.{User, UserId}
import tmaslanka.chat.repository.UsersRepository

import scala.concurrent.{ExecutionContext, Future}

class UserService(repository: UsersRepository)(implicit ex: ExecutionContext) {

  val result = Future.successful("")

  def getUser(userId: UserId) = result

  def createUser(request: CreateUserCommand): Future[Option[UserCreatedResponse]] = {
    val userId = UserId(request.userName.value)
    for {
      created <- repository.createIfNotExists(User(userId, request.userName))
      response = if (created) Some(UserCreatedResponse(userId)) else None
    } yield response
  }
}
