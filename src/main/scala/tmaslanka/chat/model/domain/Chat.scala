package tmaslanka.chat.model.domain

import tmaslanka.chat.actor.ChatActor.{ChatEvent, MessageAdded}
import tmaslanka.chat.model.StringValue
import tmaslanka.chat.model.commands._

final case class ChatId(value: String) extends StringValue

object ChatId {
  def create(userId: UserId, otherUserId: UserId): ChatId = {
    //some naive method to make chat ids unique
    val generated = Vector(userId, otherUserId).sortBy(_.value).mkString("-")
    ChatId(generated)
  }
}

final case class ChatDescription(chatId: ChatId, userIds: Set[UserId], lastMessage: String)


final case class ChatState(lastMessageId: Long = -1, userLastMessages: Map[UserId, ChatMessage] = Map()) {
  def withMessageAdded(message: ChatMessage): ChatState = copy(
    lastMessageId = lastMessageId + 1,
    userLastMessages = userLastMessages.updated(message.userId, message))

}

sealed trait ChatCommandAction
final case class Save(event: ChatEvent) extends ChatCommandAction
final case class Reply(msg: ChatCommandResponse) extends ChatCommandAction

object ChatLogic {
  def commandToAction(state: ChatState, cmd: ChatCommand): Vector[ChatCommandAction] = cmd match {
    case AddMessageCommand(message) =>
      val lastSeq = state.userLastMessages.get(message.userId) match {
        case Some(previous) =>
          previous.seq
        case None => -1
      }

      val action = if (lastSeq + 1 == message.seq) {
        Save(MessageAdded(message))
      } else if (lastSeq < message.seq) {
        Reply(Reject)
      } else {
        Reply(Confirm)
      }

      Vector(action)
  }

  def updateState(state: ChatState, event: ChatEvent): ChatState = event match {
    case MessageAdded(message) =>
      state.withMessageAdded(message)
  }
}