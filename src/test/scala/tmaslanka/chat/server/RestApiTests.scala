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

  "POST v1/users" should "create user" in {
    given()
      .contentType(`application/json`)
      .body(""" {"userName": "John"} """)
      .when()
      .post("v1/users")
    .Then()
      .statusCode(200)
      .body("userId", isNotEmptyString)
  }

  "GET v1/users/userId/chats" should "return list of chat ids for user" in {
    val userId = createUser()

    given()
      .get(s"v1/users/$userId/chats")
      .Then()
      .body("chats", Matchers.hasSize(0))
  }

  def createUser(): UserId = UserId(
    given()
      .contentType(`application/json`)
      .body(""" {"userName": "John"} """)
      .when()
      .post("v1/users")
      .body()
      .path("userId"))

  private def isNotEmptyString = {
    Matchers.not(Matchers.isEmptyString)
  }
}
