package tmaslanka.chat.model.domain

object ExampleObjects {
  val userId = UserId("some-user-id")
  val otherUserId = UserId("other-user-id")

  val userName = UserName("some-user-name")
  val otherUserName = UserName("other-user-name")

  val user = User(userId, userName)
  val otherUser = User(otherUserId, otherUserName)
}
