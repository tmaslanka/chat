package tmaslanka.chat.services

import tmaslanka.chat.actor.ShardingProtocol
import tmaslanka.chat.model.commands._
import tmaslanka.chat.model.domain.{ChatDescription, ChatId, UserId}
import tmaslanka.chat.repository.UsersRepository

import scala.concurrent.{ExecutionContext, Future}

class ChatService(protocol: ShardingProtocol, usersRepository: UsersRepository)(implicit ex: ExecutionContext) {

  def getUserChats(userId: UserId): Future[ChatsListResponse] = {
    for {
      chatIds <- usersRepository.findUserChats(userId)
      queries = chatIds.map(getChatDescription)
      queryResult <- Future.sequence(queries)
      descriptions = queryResult.map(_.description)
    } yield ChatsListResponse(descriptions)
  }

  def createChat(command: CreateChatCommand): Future[ChatCommandResponse] = {
    val userIds = command.userIds
    val chatId = ChatId.create(userIds)
    protocol.command(chatId, command).mapTo[ChatCommandResponse]
  }

  def getChat(chatId: ChatId): Future[ChatDescription] = {
    getChatDescription(chatId)
      .map(_.description)
  }

  def getChatMessages(chatId: ChatId, from: Long, limit: Long): Future[GetChatMessagesResponse] = {
    protocol.query(chatId, GetChatMessages(from, limit)).mapTo[GetChatMessagesResponse]
  }

  def appendMessage(chatId: ChatId, createMessageCommand: AddMessageCommand): Future[ChatCommandResponse] = {
    protocol.command(chatId, createMessageCommand)
  }

  private def getChatDescription(chatId: ChatId) = {
    protocol.query(chatId, GetChatDescription)
      .mapTo[GetChatDescriptionResponse]
  }
}
