package tmaslanka.chat.actor

import akka.persistence.PersistentActor
import tmaslanka.chat.model.commands.{ChatCommand, ChatMessage}
import tmaslanka.chat.model.domain._

object ChatActor {
  sealed trait ChatEvent
  case class MessageAddedEvent(message: ChatMessage) extends ChatEvent
  case class ChatCreatedEvent(userIds: Set[UserId]) extends ChatEvent
}

class ChatActor extends PersistentActor {
  import ChatActor._

  override def persistenceId: String = self.path.name

  var state = ChatState()

  def updateState(event: ChatEvent): Vector[ChatActionEventAction] = {
    val (newState, actions) = ChatLogic.updateState(state, event)
    state = newState
    actions
  }

  override def receiveRecover: Receive = {
    case event: ChatEvent => updateState(event)
  }

  override def receiveCommand: Receive = {
    case cmd: ChatCommand =>
      handleChatCommand(cmd)
  }

  def handleChatCommand(cmd: ChatCommand): Unit = {
    ChatLogic.commandToAction(state, cmd).foreach {
      case Save(event) => persist(event) { event =>
        updateState(event)
          .foreach(handleChatEventAction)
      }
      case eventAction: ChatActionEventAction =>
        handleChatEventAction(eventAction)
    }
  }

  def handleChatEventAction(action: ChatActionEventAction): Unit = action match {
    case Reply(msg) => sender() ! msg
  }
}
