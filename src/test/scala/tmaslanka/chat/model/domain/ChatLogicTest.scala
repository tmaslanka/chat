package tmaslanka.chat.model.domain

import cats.data.State
import org.scalatest.{MustMatchers, WordSpec}
import tmaslanka.chat.actor.ChatActor.MessageAdded
import tmaslanka.chat.model.commands.{ChatCommand, Confirm, Reject}

class ChatLogicTest extends WordSpec with MustMatchers {
  import ExampleObjects._

  type StateM = cats.data.State[ChatState, Vector[ChatCommandAction]]

  "ChatLogic" should {
    "add message" in {
      runTest(ChatState())(for {
        actions <- applyCommand(addMessageCommand)
        state1 <- State.get
      } yield {
        actions mustEqual Vector(Save(MessageAdded(addMessageCommand.message)))

        state1.lastMessageId mustEqual 0
        state1.userLastMessages mustEqual Map(userId -> addMessageCommand.message)
      })
    }

    "not add the same message" in {
      val command = addMessageCommand.copy(message = chatState.userLastMessages(userId))
      ChatLogic.commandToAction(chatState, command) mustEqual Vector(Reply(Confirm))
    }

    "not add previous message" in {
      runTest(ChatState())(for {
        _ <- applyCommand(addMessageCommand)
        _ <- applyCommand(addMessageCommand.copy(message = secondChatMessage))
        previousMessageActions <- applyCommand(addMessageCommand)
        stateN <- State.get
      } yield {
        previousMessageActions mustEqual Vector(Reply(Confirm))

        stateN.lastMessageId mustEqual 1
        stateN.userLastMessages mustEqual Map(userId -> secondChatMessage)
      })
    }

    "not add future message" in {
      val state0 = ChatState()
      runTest(state0)(for {
        actions <- applyCommand(next(addMessageCommand))
        stateN <- State.get
      } yield {
        actions mustEqual Vector(Reply(Reject))
        stateN mustEqual state0
      })
    }

    "add messages from different users" in {
      runTest(ChatState())(for {
        _ <- applyCommand(addMessageCommand)
        _ <- applyCommand(otherUserAddMessage)
        _ <- State.inspect { state: ChatState =>
          state.lastMessageId mustEqual 1
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
        stateN.lastMessageId mustEqual 4
        stateN.userLastMessages mustEqual Map(
          userId -> next(next(addMessageCommand.message)),
          otherUserId -> next(otherUserAddMessage.message))
      })
    }


    def applyCommand(cmd: ChatCommand): StateM = cats.data.State { state =>
      val actions = ChatLogic.commandToAction(state, cmd)
      actions.foldLeft(state)({ (s, action) =>
        action match {
          case Save(event) => ChatLogic.updateState(s, event)
          case Reply(_) => s
        }
      }) -> actions
    }

    def runTest[A](state: ChatState)(stateM: cats.data.State[ChatState, A]) = {
      stateM.run(state).value
    }
  }
}
