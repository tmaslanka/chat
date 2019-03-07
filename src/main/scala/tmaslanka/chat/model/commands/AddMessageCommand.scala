package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain.{ChatDescription, UserId}

final case class ChatMessage(seq: Long, userId: UserId, text: String)

sealed trait ChatCommand
final case class AddMessageCommand(message: ChatMessage) extends ChatCommand

sealed trait ChatCommandResponse
case object Confirm extends ChatCommandResponse
case object Reject extends ChatCommandResponse

final case class ChatsListResponse(chats: Vector[ChatDescription])
