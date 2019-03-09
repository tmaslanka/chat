package tmaslanka.chat.repository

import tmaslanka.chat.model.domain.{ChatId, User, UserId}

import scala.concurrent.Future

trait UsersRepository {
  def createUserIfNotExists(user: User): Future[Boolean]

  def findUser(userId: UserId): Future[Option[User]]

  def saveUserChat(userId: UserId, chatId: ChatId): Future[Unit]

  def findUserChats(userId: UserId): Future[Vector[ChatId]]
}