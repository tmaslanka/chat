package tmaslanka.chat.server

import java.util.UUID

import akka.persistence.cassandra.testkit.CassandraLauncher
import io.restassured.RestAssured._
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers
import org.hamcrest.Matchers.{hasItems, hasKey, is, not}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import tmaslanka.chat.model.domain.{ChatId, ChatMessage, UserId}

class RestApiTests extends FlatSpec with BeforeAndAfterAll {

  val `application/json` = "application/json"

  CassandraLauncher.start(new java.io.File("target/cassandra"),
    CassandraLauncher.DefaultTestConfigResource, clean = true, port = 9042)


  val server = new ServerModule().server
  server.start()

  override protected def afterAll(): Unit = {
    server.stop()
  }

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
      .get(s"v1/users/$userId")
      .Then()
      .statusCode(200)
      .body("userName", is(userName))
      .body("userId", is(userId.value))

  }

  "GET /v1/users/userId" should "not return user" in {
    val userId = unique("Robert")
    given()
      .get(s"v1/users/$userId")
      .Then()
      .statusCode(404)
  }

  "GET /v1/users/userId/chats" should "return empty list" in {
    val userId = createUser(unique("Mat"))

    given()
      .get(s"v1/users/$userId/chats")
      .Then()
      .body("chats", Matchers.hasSize(0))
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
      .get(s"v1/chats/$chatId")
      .Then()
      .statusCode(200)
      .body("chatId", is(chatId.value))
      .body("userIds", hasItems(bobId.value, aliceId.value))
      .body("$", not(hasKey("lastMessage")))

    putMessageToChat(chatId, ChatMessage(0, bobId, "text-0"))

    given()
      .get(s"v1/chats/$chatId")
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
      .body("messages", Matchers.hasSize(3))
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

    getChatMessages(chatId, 3, 2)
      .Then()
      .body("messages", Matchers.hasSize(2))
      .body("from", is(3))
      .bodyMessages(0, alice2Message)
      .bodyMessages(1, alice3Message)

    getChatMessages(chatId, 10, 2)
      .Then()
      .body("messages", Matchers.hasSize(0))
      .body("from", is(10))

    getChatMessages(chatId, 4, 10)
      .Then()
      .body("messages", Matchers.hasSize(2))
      .body("from", is(4))
      .bodyMessages(0, alice3Message)
      .bodyMessages(1, bob1Message)

    getChatMessages(chatId, 0, 3)
      .Then()
      .body("messages", Matchers.hasSize(3))
      .body("from", is(0))
      .bodyMessages(0, bob0Message)
      .bodyMessages(1, alice0Message)
      .bodyMessages(2, alice1Message)
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
      .put("v1/users")
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
      .put("v1/chats")
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
      .put(s"v1/chats/$chatId/messages")
  }

  private def getChatMessages(chatId: ChatId) = {
    given()
      .get(s"v1/chats/$chatId/messages")
  }

  private def getChatMessages(chatId: ChatId, from: Long, limit: Long) = {
    given()
      .get(s"v1/chats/$chatId/messages?from=$from&limit=$limit")
  }

  private def isNotEmptyString = {
    not(Matchers.isEmptyString)
  }

  private def unique(s: String): String = s"$s-${UUID.randomUUID()}"

  implicit class ValidatableResponseOps(response: ValidatableResponse) {
    def bodyMessages(idx: Int, chatMessage: ChatMessage): ValidatableResponse = {
      response
        .body(s"messages[$idx].userSeq", is(chatMessage.userSeq.toInt))
        .body(s"messages[$idx].text", is(chatMessage.text))
        .body(s"messages[$idx].userId", is(chatMessage.userId.value))
    }
  }
}
