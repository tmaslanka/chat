package tmaslanka.chat.repository.cassandra

import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import cats.syntax.option._
import tmaslanka.chat.model.domain.{ChatId, User}
import scala.concurrent.ExecutionContext.Implicits.global

class CassandraUsersRepositoryTest extends WordSpec with WithCassandra with MustMatchers with ScalaFutures {

  "CassandraUsersRepository" should {
    "create user" in new Context {
      whenReady (repository.createUserIfNotExists(someUser)) ( _ mustEqual true)
      whenReady (repository.findUser(someUser.userId)) (_ mustEqual someUser.some)
    }

    "not create user when userName already exists" in new Context {
      whenReady (repository.createUserIfNotExists(someUser)) ( _ mustEqual true)
      whenReady (repository.findUser(someUser.userId)) (_ mustEqual someUser.some)

      whenReady (repository.createUserIfNotExists(someUser)) ( _ mustEqual false)
    }

    "add chat for user" in new Context {
      private def addAndFindUserChats(user: User, chatId: ChatId) = for {
        _ <- repository.saveUserChat(user.userId, chatId)
        result <- repository.findUserChats(user.userId)
      } yield result


      whenReady (repository.findUserChats(someUser.userId)) (_ mustBe empty)
      whenReady (repository.findUserChats(otherUser.userId)) (_ mustBe empty)

      whenReady (addAndFindUserChats(someUser, chatId)) (_ must contain only chatId)

      whenReady (addAndFindUserChats(someUser, otherChatId)) (_ must contain only(chatId, otherChatId))

      whenReady (repository.findUserChats(otherUser.userId)) (_ mustBe empty)

      whenReady (addAndFindUserChats(otherUser, chatId)) (_ must contain only chatId)
      whenReady (repository.findUserChats(someUser.userId)) (_ must contain only(chatId, otherChatId))
    }
  }

  trait Context {
    import tmaslanka.chat.model.domain.ExampleObjects

    val id = UUID.randomUUID().toString

    val someUser = unique(ExampleObjects.user)
    val otherUser = unique(ExampleObjects.otherUser)

    val chatId = unique(ExampleObjects.chatId)
    val otherChatId = unique(ChatId("other-chat-id"))

    private def unique(user: User): User = {
      val userId = user.userId.copy(s"${user.userId.value}-$id")
      val userName = user.userName.copy(s"${user.userName.value}-$id")
      User(userId, userName)
    }

    private def unique(chatId: ChatId) = {
      chatId.copy(value = s"${chatId.value}-$id")
    }
  }
}
