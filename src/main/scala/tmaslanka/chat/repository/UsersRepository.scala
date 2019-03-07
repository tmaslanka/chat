package tmaslanka.chat.repository

import java.util.concurrent.ConcurrentHashMap

import tmaslanka.chat.model.domain.{User, UserId, UserName}

import scala.collection._
import scala.concurrent.Future

trait UsersRepository {

  def createIfNotExists(user: User): Future[Boolean]

  def getUser(userId: UserId): Future[Option[User]]
}

class InMemoryUserRepository extends UsersRepository {
  import scala.collection.JavaConverters._
  val users: concurrent.Map[UserId, User] = new ConcurrentHashMap[UserId, User]().asScala

  val userNamesToUserId: concurrent.Map[UserName, UserId] = new ConcurrentHashMap[UserName, UserId]().asScala

  override def createIfNotExists(user: User): Future[Boolean] = Future.successful {
    users.get(user.userId) match {
      case Some(_) =>
        false
      case None =>
        val userId = userNamesToUserId.putIfAbsent(user.userName, user.userId) match {
          case Some(oldUserId) => oldUserId
          case None => user.userId
        }
        users.putIfAbsent(userId, user).isEmpty
    }
  }

  override def getUser(userId: UserId): Future[Option[User]] = Future.successful {
    users.get(userId)
  }
}