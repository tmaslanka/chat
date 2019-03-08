package tmaslanka.chat.model.domain

import cats.data.State
import org.scalatest.{MustMatchers, WordSpec}
import tmaslanka.chat.actor.ChatActor.{ChatCreatedEvent, MessageAddedEvent}
import tmaslanka.chat.model.commands._

class ChatLogicTest extends WordSpec with MustMatchers {
  import ExampleObjects._

  type StateM = cats.data.State[ChatState, Vector[ChatAction]]

  "ChatLogic" should {
    "add chatUsers" in {
      runTest(ChatState())(for {
        actions <- applyCommand(createChatCommand)
        stateN <- State.get
      } yield {
        val userIds = createChatCommand.userIds
        actions must have size 2
        actions must contain allOf (Save(ChatCreatedEvent(userIds)), Reply(ChatCreated(ChatId.create(createChatCommand.userIds))))
        stateN mustEqual ChatState().copy(userIds = userIds)
      })
    }

    "confirm duplicated createChat" in {
      runTest(ChatState())(for {
        actions0 <- applyCommand(createChatCommand)
        actions <- applyCommand(createChatCommand)
        stateN <- State.get
      } yield {
        val userIds = createChatCommand.userIds
        println(actions0)
        actions mustEqual Vector(Reply(ChatCreated(ChatId.create(createChatCommand.userIds))))
        stateN mustEqual ChatState().copy(userIds = userIds)
      })
    }

    "reject modification to chat participants" in {
      runTest(ChatState())(for {
        _ <- applyCommand(createChatCommand)
        actions <- applyCommand(createChatCommand.copy(userIds = createChatCommand.userIds.map(_.value + "-modified").map(UserId)))
        stateN <- State.get
      } yield {
        actions mustEqual Vector(Reply(Reject))
        stateN mustEqual ChatState().copy(userIds = createChatCommand.userIds)
      })
    }

    "return not found when chat is not initialized" in {
      val state0 = ChatState()
      runTest(state0)(for{
        actions <- applyCommand(addMessageCommand)
        state1 <- State.get
      } yield {
        actions mustEqual Vector(Reply(NotFound))
        state1 mustEqual state0
      })
    }

    "reject message if not from chat participant" in {
      val state0 = ChatState()
      runTest(state0)(for{
        _ <- applyCommand(createChatCommand)
        actions <- applyCommand(addMessageCommand.copy(message = chatMessage.copy(userId = thirdUserId)))
        state1 <- State.get
      } yield {
        actions mustEqual Vector(Reply(UnAuthorized))

        state1.userLastMessages mustBe empty
        state1.lastSeq mustEqual -1
      })
    }

    "add message" in {
      runTest(chatState)(for {
        actions <- applyCommand(addMessageCommand)
        state1 <- State.get
      } yield {
        actions mustEqual Vector(Save(MessageAddedEvent(addMessageCommand.message)), Reply(Confirm))

        state1.lastSeq mustEqual 0
        state1.userLastMessages mustEqual Map(userId -> addMessageCommand.message)
      })
    }

    "not add the same message twice" in {
      runTest(chatState)(for {
        _ <- applyCommand(addMessageCommand)
        state1 <- State.get
        actions <- applyCommand(addMessageCommand)
        stateN <- State.get
      } yield {
        actions mustEqual Vector(Reply(Confirm))
        stateN mustEqual state1
      })
    }

    "not add previous message" in {
      runTest(chatState)(for {
        _ <- applyCommand(addMessageCommand)
        _ <- applyCommand(addMessageCommand.copy(message = secondChatMessage))
        previousMessageActions <- applyCommand(addMessageCommand)
        stateN <- State.get
      } yield {
        previousMessageActions mustEqual Vector(Reply(Confirm))

        stateN.lastSeq mustEqual 1
        stateN.userLastMessages mustEqual Map(userId -> secondChatMessage)
      })
    }

    "not add future message" in {
      val state0 = chatState
      runTest(state0)(for {
        actions <- applyCommand(next(addMessageCommand))
        stateN <- State.get
      } yield {
        actions mustEqual Vector(Reply(Reject))
        stateN mustEqual state0
      })
    }

    "add messages from many chat participants" in {
      runTest(chatState)(for {
        _ <- applyCommand(addMessageCommand)
        _ <- applyCommand(otherUserAddMessage)
        _ <- State.inspect { state: ChatState =>
          state.lastSeq mustEqual 1
          state.userLastMessages mustEqual Map(
            userId -> addMessageCommand.message,
            otherUserId -> otherUserMessage
          )
        }
        _ <- applyCommand(next(addMessageCommand))
        _ <- applyCommand(next(next(addMessageCommand)))
        _ <- applyCommand(next(otherUserAddMessage))
        stateN <- State.get
      } yield {
        stateN.lastSeq mustEqual 4
        stateN.userLastMessages mustEqual Map(
          userId -> next(next(addMessageCommand.message)),
          otherUserId -> next(otherUserAddMessage.message))
      })
    }


    def applyCommand(cmd: ChatCommand): StateM = cats.data.State { state =>

      def handleChatEventAction(action: ChatActionEventAction): Unit = action match {
        case Reply(msg) => // nop
      }

      val actions = ChatLogic.commandToAction(state, cmd)
      val (newState, eventActions) = actions.foldLeft((state, Vector.empty[ChatActionEventAction]))({ case ((stateAcc, actionsAcc), action) =>
        action match {
          case Save(event) =>
            val (newState, eventActions) = ChatLogic.updateState(stateAcc, event)
            eventActions.foreach(handleChatEventAction)
            newState -> (actionsAcc ++ eventActions)
          case eventAction: ChatActionEventAction =>
            handleChatEventAction(eventAction)
            stateAcc -> actionsAcc
        }
      })
      newState -> (actions ++ eventActions)
    }

    def runTest[A](state: ChatState)(stateM: cats.data.State[ChatState, A]) = {
      stateM.run(state).value
    }
  }
}
