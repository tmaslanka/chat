package tmaslanka.chat.services

import tmaslanka.chat.model.commands.{ChatsListResponse, CreateMessageCommand}
import tmaslanka.chat.model.domain.{ChatDescription, ChatId, UserId}

import scala.concurrent.Future

class ChatService {

  val response = Future.successful("")

  def getUserChats(userId: UserId): Future[ChatsListResponse] = Future.successful(ChatsListResponse(Vector()))


  def getChat(chatId: ChatId) = response

  def getChatMessages(chatId: ChatId) = response

  def appendMessage(chatId: ChatId, createMessageCommand: CreateMessageCommand) = response

}
