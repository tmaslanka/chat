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

final case class ChatDescription(chatId: ChatId, userIds: Set[UserId], lastMessage: Option[ChatMessage], lastSeq: Option[Long])

final case class ChatMessage(userSeq: Long, userId: UserId, text: String)

final case class ChatState(lastSeq: Long = -1,
                           userIds: Set[UserId] = Set(),
                           userLastMessages: Map[UserId, ChatMessage] = Map(),
                           lastMessage: Option[ChatMessage] = None
                          ) {
  def withUserIds(userIds: Set[UserId]): ChatState = copy(userIds = userIds)

  def withMessageAdded(message: ChatMessage): ChatState = copy(
    lastSeq = lastSeq + 1,
    userLastMessages = userLastMessages.updated(message.userId, message),
    lastMessage = Some(message)
  )

  def chatId: ChatId = ChatId.create(userIds)
}

sealed trait ChatAction
sealed trait ChatEventAction extends ChatAction
final case class Save(event: ChatEvent) extends ChatAction
final case class Reply(msg: ChatCommandResponse) extends ChatAction with ChatEventAction
final case class UpdateUserChats(reply: Reply, userIds: Set[UserId], chatId: ChatId) extends ChatEventAction

object ChatLogic {
  def commandToAction(state: ChatState, cmd: ChatCommand): Vector[ChatAction] = cmd match {
    case CreateChatCommand(userIds) =>
      val action = if (state.userIds.isEmpty) {
        Save(ChatCreatedEvent(userIds))
      } else if (state.userIds == userIds) {
        Reply(ChatCreated(state.chatId))
      } else {
        Reply(Reject)
      }
      Vector(action)

    case AddMessageCommand(message) =>
      val userId = message.userId
      val action = if(state.userIds.isEmpty) {
        Reply(NotFound)
      } else if (!state.userIds(userId)) {
        Reply(UnAuthorized)
      } else {
        val lastSeq = state.userLastMessages.get(userId) match {
          case Some(previous) =>
            previous.userSeq
          case None => -1
        }

        if (lastSeq + 1 == message.userSeq) {
          Save(MessageAddedEvent(message))
        } else if (lastSeq < message.userSeq) {
          Reply(Reject)
        } else {
          Reply(Confirm)
        }
      }
      Vector(action)
  }

  def updateState(state: ChatState, event: ChatEvent): (ChatState, Vector[ChatEventAction]) = event match {
    case ChatCreatedEvent(userIds) =>
      val newState = state.withUserIds(userIds)
      val reply = Reply(ChatCreated(newState.chatId))
      newState -> Vector(UpdateUserChats(reply, userIds, newState.chatId))
    case MessageAddedEvent(message) =>
      state.withMessageAdded(message) -> Vector(Reply(Confirm))
  }
}