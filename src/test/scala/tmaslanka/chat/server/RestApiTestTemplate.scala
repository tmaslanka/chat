package tmaslanka.chat.server

import java.util.UUID

import akka.persistence.cassandra.testkit.CassandraLauncher
import cats.syntax.option._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import io.restassured.RestAssured._
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import tmaslanka.chat.Settings
import tmaslanka.chat.model.domain.{ChatId, ChatMessage, UserId}

trait RestApiTestTemplate extends FlatSpec with BeforeAndAfterAll with StrictLogging {

  val `application/json` = "application/json"

  val settings = Settings(ConfigFactory.load())

  CassandraLauncher.start(new java.io.File("target/cassandra"),
    CassandraLauncher.DefaultTestConfigResource, clean = true, port = settings.cassandraConfig.port)

  "PUT /v1/users" should "create user" in {
    putUser(unique("John"))
    .Then()
      .statusCode(200)
      .body("userId", isNotEmptyString)
  }

  "PUT /v1/users" should "not create new user for the same user name" in {
    val userName = unique("DoubleCreated")
    putUser(userName)
      .Then()
      .statusCode(200)

    putUser(userName)
      .Then()
      .statusCode(400)
  }

  "GET /v1/users/userId" should "return user" in {
    val userName = unique("Andrzej")
    val userId = createUser(userName)

    given()
      .get(toServerUrl(s"v1/users/$userId"))
      .Then()
      .statusCode(200)
      .body("userName", is(userName))
      .body("userId", is(userId.value))

  }

  "GET /v1/users/userId" should "not return user" in {
    val userId = unique("Robert")
    given()
      .get(toServerUrl(s"v1/users/$userId"))
      .Then()
      .statusCode(404)
  }

  "GET /v1/users/userId/chats" should "return empty list" in {
    val userId = createUser(unique("Mat"))

    getUserChats(userId)
      .Then()
      .body("chats", hasSize(0))
  }

  "GET /v1/users/userId/chats" should "return one chat" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val userIds = Set(bobId, aliceId)

    val chatId = createChatForUsers(aliceId, bobId)

    getUserChats(bobId)
      .Then()
      .statusCode(200)
      .body("chats", hasSize(1))
      .bodyChatDescription(0, chatId, userIds)

    getUserChats(aliceId)
      .Then()
      .statusCode(200)
      .body("chats", hasSize(1))
      .bodyChatDescription(0, chatId, userIds)

    val bob0Message = ChatMessage(0, bobId, "bob-0")

    putMessageToChat(chatId, bob0Message)

    getUserChats(bobId)
      .Then()
      .bodyChatDescription(0, chatId, userIds, lastMessage = bob0Message)

    getUserChats(aliceId)
      .Then()
      .bodyChatDescription(0, chatId, userIds, lastMessage = bob0Message)
  }

  "PUT /v1/chats/chatId" should "start chat for Bob and Alice" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    putChatForUsers(bobId, aliceId)
      .Then()
      .statusCode(200)
      .body("chatId", isNotEmptyString)
  }

  "PUT /v1/chats/chatId" should "be idempotent" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    putChatForUsers(aliceId, bobId)
      .Then()
      .statusCode(200)
      .body("chatId", is(chatId.value))
  }

  "PUT /v1/chats/chatId" should "be idempotent also" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    putChatForUsers(bobId, aliceId)
      .Then()
      .statusCode(200)
      .body("chatId", is(chatId.value))
  }

  "GET /v1/chats/chatId" should "return chat description" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    given()
      .get(toServerUrl(s"v1/chats/$chatId"))
      .Then()
      .statusCode(200)
      .body("chatId", is(chatId.value))
      .body("userIds", hasItems(bobId.value, aliceId.value))
      .body("$", not(hasKey("lastMessage")))

    putMessageToChat(chatId, ChatMessage(0, bobId, "text-0"))

    given()
      .get(toServerUrl(s"v1/chats/$chatId"))
      .Then()
      .statusCode(200)
      .body("lastMessage.text", is("text-0"))
      .body("lastMessage.userId", is(bobId.value))
      .body("lastMessage.userSeq", is(0))
  }

  "PUT /v1/chats/chatId/messages" should "append message to chat" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    putMessageToChat(chatId, ChatMessage(0, bobId, "text-0"))
      .Then()
      .statusCode(200)
  }

  "PUT /v1/chats/chatId/messages" should "return 404 when chat is not started" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    putMessageToChat(ChatId.create(Set(bobId, aliceId)), ChatMessage(0, bobId, "text-0"))
      .Then()
      .statusCode(404)
  }

  "GET /v1/chats/chatId/messages" should "return chat messages" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    val bob0Message = ChatMessage(0, bobId, "bob-0")
    val alice0Message = ChatMessage(0, aliceId, "alice-0")
    val alice1Message = ChatMessage(1, aliceId, "alice-1")

    putMessageToChat(chatId, bob0Message)
    putMessageToChat(chatId, alice0Message)
    putMessageToChat(chatId, alice1Message)

    getChatMessages(chatId)
      .Then()
      .body("messages", hasSize(3))
      .body("from", is(0))
      .bodyMessages(0, bob0Message)
      .bodyMessages(1, alice0Message)
      .bodyMessages(2, alice1Message)
  }

  "GET /v1/chats/chatId/messages" should "return limited no of messages" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    val bob0Message = ChatMessage(0, bobId, "bob-0")
    val bob1Message = ChatMessage(1, bobId, "bob-1")
    val alice0Message = ChatMessage(0, aliceId, "alice-0")
    val alice1Message = ChatMessage(1, aliceId, "alice-1")
    val alice2Message = ChatMessage(2, aliceId, "alice-2")
    val alice3Message = ChatMessage(3, aliceId, "alice-3")

    putMessageToChat(chatId, bob0Message)
    putMessageToChat(chatId, alice0Message)
    putMessageToChat(chatId, alice1Message)
    putMessageToChat(chatId, alice2Message)
    putMessageToChat(chatId, alice3Message)
    putMessageToChat(chatId, bob1Message)

    getChatMessages(chatId, 3L.some, 2.some)
      .Then()
      .body("messages", hasSize(2))
      .body("from", is(3))
      .bodyMessages(0, alice2Message)
      .bodyMessages(1, alice3Message)

    getChatMessages(chatId, 10L.some, 2.some)
      .Then()
      .body("messages", hasSize(0))
      .body("from", is(10))

    getChatMessages(chatId, 4L.some, 10.some)
      .Then()
      .body("messages", hasSize(2))
      .body("from", is(4))
      .bodyMessages(0, alice3Message)
      .bodyMessages(1, bob1Message)

    getChatMessages(chatId, 0L.some, 3.some)
      .Then()
      .body("messages", hasSize(3))
      .body("from", is(0))
      .bodyMessages(0, bob0Message)
      .bodyMessages(1, alice0Message)
      .bodyMessages(2, alice1Message)

    getChatMessages(chatId, none, 4.some)
      .Then()
      .body("messages", hasSize(4))
      .body("from", is(2))
      .bodyMessages(0, alice1Message)
      .bodyMessages(1, alice2Message)
      .bodyMessages(2, alice3Message)
      .bodyMessages(3, bob1Message)

    getChatMessages(chatId, none, none)
      .Then()
      .body("messages", hasSize(6))
      .body("from", is(0))
      .bodyMessages(0, bob0Message)
      .bodyMessages(1, alice0Message)
      .bodyMessages(2, alice1Message)
      .bodyMessages(3, alice2Message)
      .bodyMessages(4, alice3Message)
      .bodyMessages(5, bob1Message)
  }

  private def createUser(userName: String): UserId = {
    val userId = putUser(userName).body().path[String]("userId")
    UserId(userId)
  }

  private def putUser(userName: String) = {
    given()
      .contentType(`application/json`)
      .body(s""" {"userName": "$userName"} """)
      .when()
      .put(toServerUrl("v1/users"))
  }

  private def createChatForUsers(firstUserId: UserId, secondUserId: UserId) = {
    val chatId = putChatForUsers(firstUserId, secondUserId)
      .body()
      .path[String]("chatId")
    ChatId(chatId)
  }

  private def putChatForUsers(firstUserId: UserId, secondUserId: UserId) = {
    given()
      .contentType(`application/json`)
      .body(
        s"""
           |{
           |  "userIds": ["$firstUserId", "$secondUserId"]
           |}
         """.stripMargin)
      .when()
      .put(toServerUrl("v1/chats"))
  }

  private def putMessageToChat(chatId: ChatId, message: ChatMessage) = {
    given()
      .contentType(`application/json`)
      .body(
        s""" {
           |"message": {
           |    "userSeq": ${message.userSeq},
           |    "userId": "${message.userId}",
           |    "text": "${message.text}"
           |  }
           |} """.stripMargin)
      .put(toServerUrl(s"v1/chats/$chatId/messages"))
  }

  private def getChatMessages(chatId: ChatId) = {
    given()
      .get(toServerUrl(s"v1/chats/$chatId/messages"))
  }

  private def getChatMessages(chatId: ChatId, from: Option[Long], limit: Option[Int]) = {
    def toParams[A](nameValues: (String, Option[A])*) = {
      val params = nameValues.map { case (name, value) =>
        value.map(v => s"$name=$v").getOrElse("")
      }.filterNot(_.isEmpty).mkString("&")

      if (params.isEmpty) {
        params
      } else {
        s"?$params"
      }
    }

    given()
      .get(toServerUrl(s"v1/chats/$chatId/messages${toParams("from" -> from, "limit" -> limit)}"))
  }

  private def getUserChats(bobId: UserId) = {
    given()
      .get(toServerUrl(s"v1/users/$bobId/chats"))
  }

  private def isNotEmptyString = {
    not(isEmptyString)
  }

  private def unique(s: String): String = s"$s-${UUID.randomUUID()}"

  implicit class ValidatableResponseOps(response: ValidatableResponse) {
    def bodyMessages(idx: Int, chatMessage: ChatMessage): ValidatableResponse = {
      response
        .body(s"messages[$idx].userSeq", is(chatMessage.userSeq.toInt))
        .body(s"messages[$idx].text", is(chatMessage.text))
        .body(s"messages[$idx].userId", is(chatMessage.userId.value))
    }

    def bodyChatDescription(idx: Int, chatId: ChatId, userIds: Set[UserId]): ValidatableResponse = {
      response
        .body(s"chats[$idx].chatId", is(chatId.value))
        .body(s"chats[$idx].userIds", hasSize(userIds.size))
        .body(s"chats[$idx].userIds", containsInAnyOrder(userIds.map(_.value).toArray:_*))
        .body("$", not(hasKey("lastMessage")))
    }

    def bodyChatDescription(idx: Int, chatId: ChatId, userIds: Set[UserId], lastMessage: ChatMessage): ValidatableResponse = {
      response
        .body(s"chats[$idx].chatId", is(chatId.value))
        .body(s"chats[$idx].userIds", hasSize(userIds.size))
        .body(s"chats[$idx].userIds", containsInAnyOrder(userIds.map(_.value).toArray:_*))
        .body(s"chats[$idx].lastMessage.userSeq", is(lastMessage.userSeq.toInt))
        .body(s"chats[$idx].lastMessage.text", is(lastMessage.text))
        .body(s"chats[$idx].lastMessage.userId", is(lastMessage.userId.value))
    }
  }

  def toServerUrl(path: String): String = {
    val url = s"http://$restClientHost:$restClientPort/$path"
    logger.info(s"request url: $url")
    url
  }

  def restClientHost: String = settings.host
  def restClientPort: Int = settings.port
}
