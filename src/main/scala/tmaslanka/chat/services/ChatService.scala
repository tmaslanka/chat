package tmaslanka.chat.services

import tmaslanka.chat.model.commands.{AddMessageCommand, ChatCommandResponse, ChatsListResponse, Confirm}
import tmaslanka.chat.model.domain.{ChatId, UserId}

import scala.concurrent.Future

class ChatService {

  val response = Future.successful("")

  def getUserChats(userId: UserId): Future[ChatsListResponse] = Future.successful(ChatsListResponse(Vector()))


  def getChat(chatId: ChatId) = response

  def getChatMessages(chatId: ChatId) = response

  def appendMessage(chatId: ChatId, createMessageCommand: AddMessageCommand): Future[ChatCommandResponse] = {
    Future.successful(Confirm)
  }

}
