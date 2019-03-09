package tmaslanka.chat.model.domain

import tmaslanka.chat.actor.ChatActor.{ChatCreatedEvent, ChatEvent, MessageAddedEvent}
import tmaslanka.chat.model.StringValue
import tmaslanka.chat.model.commands._

final case class ChatId(value: String) extends StringValue

object ChatId {
  def create(userIds: Set[UserId]): ChatId = {
    //some naive method to make chat ids normalized and unique
    val generated = userIds.toVector.sortBy(_.value).mkString("-")
    ChatId(generated)
  }
}

final case class ChatDescription(chatId: ChatId, userIds: Set[UserId], lastMessage: String)


final case class ChatState(lastMessageId: Long = -1, userIds: Set[UserId] = Set(), userLastMessages: Map[UserId, ChatMessage] = Map()) {
  def withUserIds(userIds: Set[UserId]): ChatState = copy(userIds = userIds)

  def withMessageAdded(message: ChatMessage): ChatState = copy(
    lastMessageId = lastMessageId + 1,
    userLastMessages = userLastMessages.updated(message.userId, message))

  def chatId: ChatId = ChatId.create(userIds)
}

sealed trait ChatAction
sealed trait ChatActionEventAction extends ChatAction
final case class Save(event: ChatEvent) extends ChatAction
final case class Reply(msg: ChatCommandResponse) extends ChatAction with ChatActionEventAction

object ChatLogic {
  def commandToAction(state: ChatState, cmd: ChatCommand): Vector[ChatAction] = cmd match {
    case CreateChatCommand(userIds) =>
      val action = if(state.userIds.isEmpty) {
        Save(ChatCreatedEvent(userIds))
      } else if (state.userIds == userIds) {
        println("commandToAction ChatCreated")
        Reply(ChatCreated(state.chatId))
      } else {
        Reply(Reject)
      }
      Vector(action)

    case AddMessageCommand(message) =>
      val userId = message.userId
      val action = if (!state.userIds(userId)) {
        Reply(UnAuthorized)
      } else {
        val lastSeq = state.userLastMessages.get(userId) match {
          case Some(previous) =>
            previous.seq
          case None => -1
        }

        if (lastSeq + 1 == message.seq) {
          Save(MessageAddedEvent(message))
        } else if (lastSeq < message.seq) {
          Reply(Reject)
        } else {
          Reply(Confirm)
        }
      }
      Vector(action)
  }

  def updateState(state: ChatState, event: ChatEvent): (ChatState, Vector[ChatActionEventAction]) = event match {
    case ChatCreatedEvent(userIds) =>
      val newState = state.withUserIds(userIds)
      println("update state ChatCreated")
      newState -> Vector(Reply(ChatCreated(newState.chatId)))
    case MessageAddedEvent(message) =>
      state.withMessageAdded(message) -> Vector(Reply(Confirm))
  }
}