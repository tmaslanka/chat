package tmaslanka.chat.repository

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

import tmaslanka.chat.model.domain.{ChatId, User, UserId, UserName}

import scala.collection._
import scala.concurrent.Future

trait UsersRepository {
  def createIfNotExists(user: User): Future[Boolean]

  def findUser(userId: UserId): Future[Option[User]]

  def saveUserChat(userId: UserId, chatId: ChatId): Future[Unit]

  def findUserChats(userId: UserId): Future[Vector[ChatId]]
}

class InMemoryUserRepository extends UsersRepository {
  import scala.collection.JavaConverters._
  val users: concurrent.Map[UserId, User] = new ConcurrentHashMap[UserId, User]().asScala

  val userNamesToUserId: concurrent.Map[UserName, UserId] = new ConcurrentHashMap[UserName, UserId]().asScala

  val userIdToChatId: concurrent.Map[UserId, AtomicReference[Vector[ChatId]]] =
    new ConcurrentHashMap[UserId, AtomicReference[Vector[ChatId]]]().asScala

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

  override def findUser(userId: UserId): Future[Option[User]] = Future.successful {
    users.get(userId)
  }

  override def saveUserChat(userId: UserId, chatId: ChatId): Future[Unit] = Future.successful {
    userIdToChatId.putIfAbsent(userId, new AtomicReference(Vector.empty[ChatId]))
    userIdToChatId.get(userId).foreach(_.accumulateAndGet(Vector(chatId), (v1, v2) => v1 ++ v2))
  }

  override def findUserChats(userId: UserId): Future[Vector[ChatId]] = Future.successful {
    userIdToChatId.get(userId).map(_.get()).getOrElse(Vector.empty)
  }
}