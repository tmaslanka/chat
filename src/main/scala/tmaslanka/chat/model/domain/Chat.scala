package tmaslanka.chat.model.domain

import tmaslanka.chat.model.StringValue

final case class ChatId(value: String) extends StringValue

object ChatId {
  def create(userId: UserId, otherUserId: UserId): ChatId = {
    //some naive method to make chat ids unique
    val generated = Vector(userId, otherUserId).sortBy(_.value).mkString("-")
    ChatId(generated)
  }
}

final case class ChatDescription(chatId: ChatId, userIds: Set[UserId], lastMessage: String)