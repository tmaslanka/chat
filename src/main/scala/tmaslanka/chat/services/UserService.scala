package tmaslanka.chat.services

import java.util.UUID

import tmaslanka.chat.model.commands.{CreateUserCommand, UserCreatedResponse}
import tmaslanka.chat.model.domain.{User, UserId}
import tmaslanka.chat.repository.UsersRepository

import scala.concurrent.{ExecutionContext, Future}

class UserService(repository: UsersRepository)(implicit ex: ExecutionContext) {

  def getUsers: Future[Vector[User]] = repository.findAll()

  def getUser(userId: UserId): Future[Option[User]] = {
    repository.findUser(userId)
  }

  def createUser(request: CreateUserCommand): Future[Option[UserCreatedResponse]] = {
    val userId = UserId(UUID.randomUUID().toString)
    for {
      created <- repository.createUserIfNotExists(User(userId, request.userName))
      response = if (created) Some(UserCreatedResponse(userId)) else None
    } yield response
  }
}
