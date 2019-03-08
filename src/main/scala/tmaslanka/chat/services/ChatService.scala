package tmaslanka.chat.services

import tmaslanka.chat.actor.ShardingProtocol
import tmaslanka.chat.model.commands._
import tmaslanka.chat.model.domain.{ChatDescription, ChatId, UserId}

import scala.concurrent.{ExecutionContext, Future}

class ChatService(protocol: ShardingProtocol)(implicit ex: ExecutionContext) {

  def getUserChats(userId: UserId): Future[ChatsListResponse] = Future.successful(ChatsListResponse(Vector()))

  def createChat(command: CreateChatCommand): Future[ChatCommandResponse] = {
    val userIds = command.userIds
    val chatId = ChatId.create(userIds)
    protocol.command(chatId, command).mapTo[ChatCommandResponse]
  }

  def getChat(chatId: ChatId): Future[ChatDescription] = {
    protocol.query(chatId, GetChatDescription)
      .mapTo[GetChatDescriptionResponse]
      .map(_.description)
  }

  def getChatMessages(chatId: ChatId, from: Long, limit: Long): Future[GetChatMessagesResponse] = {
    protocol.query(chatId, GetChatMessages(from, limit)).mapTo[GetChatMessagesResponse]
  }

  def appendMessage(chatId: ChatId, createMessageCommand: AddMessageCommand): Future[ChatCommandResponse] = {
    protocol.command(chatId, createMessageCommand)
  }

}
