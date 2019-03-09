package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain._


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

final case class GetChatMessages(from: Option[Long], limit: Option[Int]) extends ChatQuery
final case class GetChatMessagesResponse(from: Long, messages: Vector[ChatMessage]) extends ChatQueryResponse

case object GetChatDescription extends ChatQuery
final case class GetChatDescriptionResponse(description: ChatDescription) extends ChatQueryResponse

final case class ChatsListResponse(chats: Vector[ChatDescription])
