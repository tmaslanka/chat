package tmaslanka.chat.model.domain

import tmaslanka.chat.model.StringValue

final case class ChatId(value: String) extends StringValue

final case class ChatDescription(chatId: ChatId, userIds: Set[UserId], lastMessage: String)