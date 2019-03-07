package tmaslanka.chat.actor

import akka.persistence.PersistentActor
import tmaslanka.chat.model.commands.{ChatCommand, ChatMessage, Confirm}
import tmaslanka.chat.model.domain._

object ChatActor {
  sealed trait ChatEvent
  case class MessageAdded(message: ChatMessage) extends ChatEvent
}

class ChatActor extends PersistentActor {
  import ChatActor._

  override def persistenceId: String = self.path.name

  var state = ChatState()

  def updateState(event: ChatEvent): Unit = {
    state = ChatLogic.updateState(state, event)
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
        sender() ! Confirm
      }
      case Reply(msg) => sender() ! msg
    }
  }
}
