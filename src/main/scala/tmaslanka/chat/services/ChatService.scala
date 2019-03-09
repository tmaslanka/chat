package tmaslanka.chat.services

import tmaslanka.chat.actor.ShardingProtocol
import tmaslanka.chat.model.commands._
import tmaslanka.chat.model.domain.{ChatId, UserId}

import scala.concurrent.Future

class ChatService(protocol: ShardingProtocol) {

  val response = Future.successful("")

  def getUserChats(userId: UserId): Future[ChatsListResponse] = Future.successful(ChatsListResponse(Vector()))

  def createChat(command: CreateChatCommand): Future[ChatCommandResponse] = {
    val chatId = ChatId.create(command.userIds)
    protocol.command(chatId, command).mapTo[ChatCommandResponse]
  }

  def getChat(chatId: ChatId) = response

  def getChatMessages(chatId: ChatId, from: Long, limit: Long): Future[GetChatMessagesResponse] = {
    protocol.query(chatId, GetChatMessages(from, limit)).mapTo[GetChatMessagesResponse]
  }

  def appendMessage(chatId: ChatId, createMessageCommand: AddMessageCommand): Future[ChatCommandResponse] = {
    protocol.command(chatId, createMessageCommand)
  }

}
