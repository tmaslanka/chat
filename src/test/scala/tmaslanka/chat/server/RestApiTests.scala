package tmaslanka.chat.server

import java.util.UUID

import akka.persistence.cassandra.testkit.CassandraLauncher
import io.restassured.RestAssured._
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.Matchers
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import tmaslanka.chat.model.commands.ChatMessage
import tmaslanka.chat.model.domain.{ChatId, UserId}

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
      .body("userName", Matchers.is(userName))
      .body("userId", Matchers.is(userId.value))

  }

  "GET /v1/users/userId" should "not return user" in {
    val userId = unique("Robert")
    given()
      .get(s"v1/users/$userId")
      .Then()
      .statusCode(404)
  }

  "GET /v1/users/userId/chats" should "return empty list of chat ids for user" in {
    val userId = createUser(unique("Mat"))

    given()
      .get(s"v1/users/$userId/chats")
      .Then()
      .body("chats", Matchers.hasSize(0))
  }

  //todo more tests /v1/users/userId/chats

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
      .body("chatId", Matchers.is(chatId.value))
  }

  "PUT /v1/chats/chatId" should "be idempotent also" in {
    val bobId = createUser(unique("Bob"))
    val aliceId = createUser(unique("Alice"))

    val chatId = createChatForUsers(bobId, aliceId)

    putChatForUsers(bobId, aliceId)
      .Then()
      .statusCode(200)
      .body("chatId", Matchers.is(chatId.value))
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
           |    "seq": ${message.seq},
           |    "userId": "${message.userId}",
           |    "text": "${message.text}"
           |  }
           |} """.stripMargin)
      .put(s"v1/chats/$chatId/messages")
  }

  private def isNotEmptyString = {
    Matchers.not(Matchers.isEmptyString)
  }

  private def unique(s: String): String = s"$s-${UUID.randomUUID()}"
}
