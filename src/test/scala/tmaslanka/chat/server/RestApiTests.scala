package tmaslanka.chat.server

import io.restassured.RestAssured._
import io.restassured.module.scala.RestAssuredSupport.AddThenToResponse
import org.hamcrest.Matchers
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import tmaslanka.chat.model.domain.UserId

class RestApiTests extends FlatSpec with BeforeAndAfterAll {

  val `application/json` = "application/json"

  val server = new ServerModule().server
  server.start()

  override protected def afterAll(): Unit = {
    server.stop()
  }

  "PUT v1/users" should "create user" in {
    given()
      .contentType(`application/json`)
      .body(""" {"userName": "John"} """)
      .when()
      .put("v1/users")
    .Then()
      .statusCode(200)
      .body("userId", isNotEmptyString)
  }

  "PUT v1/users" should "not create new user for the same user name" in {
    val userName = "DoubleCreated"
    putUser(userName)
      .Then()
      .statusCode(200)

    putUser(userName)
      .Then()
      .statusCode(400)
  }

  "GET v1/users/userId/chats" should "return list of chat ids for user" in {
    val userId = createUser("Mat")

    given()
      .get(s"v1/users/$userId/chats")
      .Then()
      .body("chats", Matchers.hasSize(0))
  }

  private def createUser(userName: String): UserId = UserId(
    putUser(userName)
      .body()
      .path("userId"))

  private def putUser(userName: String) = {
    given()
      .contentType(`application/json`)
      .body(s""" {"userName": "$userName"} """)
      .when()
      .put("v1/users")
  }

  private def isNotEmptyString = {
    Matchers.not(Matchers.isEmptyString)
  }
}
