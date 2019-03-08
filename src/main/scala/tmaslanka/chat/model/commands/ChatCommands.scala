package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain.{ChatDescription, ChatId, ChatMessage, UserId}


sealed trait ChatCommand
final case class CreateChatCommand(userIds: Set[UserId]) extends ChatCommand
final case class AddMessageCommand(message: ChatMessage) extends ChatCommand

sealed trait ChatCommandResponse
case object Confirm extends ChatCommandResponse
case object Reject extends ChatCommandResponse
case object UnAuthorized extends ChatCommandResponse
case object NotFound extends ChatCommandResponse
case class ChatCreated(chatId: ChatId) extends ChatCommandResponse

sealed trait ChatQuery
sealed trait ChatQueryResponse
final case class GetChatMessages(from: Long = 0L, limit: Long = Long.MaxValue) extends ChatQuery
final case class GetChatMessagesResponse(from: Long, messages: Vector[ChatMessage]) extends ChatQueryResponse

final case class ChatsListResponse(chats: Vector[ChatDescription])
