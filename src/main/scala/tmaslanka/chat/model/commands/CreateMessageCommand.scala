package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain.{ChatDescription, ChatId}

final case class CreateMessageCommand(message: String)

final case class ChatsListResponse(chats: Vector[ChatDescription])
