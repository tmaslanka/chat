package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain.{ChatDescription, ChatId, UserId}

final case class ChatMessage(seq: Long, userId: UserId, text: String)

sealed trait ChatCommand
final case class CreateChatCommand(userIds: Set[UserId]) extends ChatCommand
final case class AddMessageCommand(message: ChatMessage) extends ChatCommand

sealed trait ChatCommandResponse
case object Confirm extends ChatCommandResponse
case object Reject extends ChatCommandResponse
case object UnAuthorized extends ChatCommandResponse
case class ChatCreated(chatId: ChatId) extends ChatCommandResponse

final case class ChatsListResponse(chats: Vector[ChatDescription])
