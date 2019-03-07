package tmaslanka.chat.repository

import tmaslanka.chat.model.domain.{User, UserId}

import scala.concurrent.Future

trait UsersRepository {

  def createIfNotExists(user: User): Future[Boolean]

  def getUser(userId: UserId): Future[Option[User]]
}

class InMemoryUserRepository extends UsersRepository {
  import scala.collection.JavaConverters._
  val users: scala.collection.concurrent.Map[UserId, User] =
    new java.util.concurrent.ConcurrentHashMap[UserId, User]().asScala

  override def createIfNotExists(user: User): Future[Boolean] = Future.successful {
    users.putIfAbsent(user.userId, user).isEmpty
  }

  override def getUser(userId: UserId): Future[Option[User]] = Future.successful {
    users.get(userId)
  }
}