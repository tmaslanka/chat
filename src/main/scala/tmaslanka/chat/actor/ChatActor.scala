package tmaslanka.chat.actor

import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.stream.Materializer
import tmaslanka.chat.actor.ChatActor.MessageAddedEvent
import tmaslanka.chat.model.commands.{ChatCommand, ChatQuery, GetChatMessages, GetChatMessagesResponse}
import tmaslanka.chat.model.domain._

import scala.concurrent.{ExecutionContext, Future}

object ChatActor {
  sealed trait ChatEvent
  case class MessageAddedEvent(message: ChatMessage) extends ChatEvent
  case class ChatCreatedEvent(userIds: Set[UserId]) extends ChatEvent

  val chatMessageTag = "chatMessage"
}

class ChatActor(journalQueries: JournalQueries)(implicit ex: ExecutionContext) extends PersistentActor {
  import ChatActor._
  import akka.pattern.pipe

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
    case query: ChatQuery =>
      journalQueries.handleChatQuery(persistenceId, query, state)
        .pipeTo(sender())
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

class JournalQueries(val queries: CassandraReadJournal)(implicit ex: ExecutionContext, mat: Materializer) {

  def handleChatQuery(persistenceId: String, query: ChatQuery, chatState: ChatState): Future[GetChatMessagesResponse] = query match {
    case GetChatMessages(from, limit) =>
      val to = from + limit-1
      queries.eventsByPersistenceId(persistenceId, toSequenceNr(from), toSequenceNr(math.min(to, chatState.lastSeq)))
        .map { env =>
          println(s"processing envelope $env")
          env.event
        }
        .collect { case event: MessageAddedEvent => event }
        .runFold(Vector.empty[ChatMessage])((acc, event) => acc :+ event.message)
        .map(messages => GetChatMessagesResponse(from, messages))
  }

  private def toSequenceNr(from: Long) = from + 2
}
