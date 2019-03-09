package tmaslanka.chat.model.domain

import tmaslanka.chat.model.commands.{AddMessageCommand, ChatMessage, CreateChatCommand}

object ExampleObjects {
  val userId = UserId("some-user-id")
  val otherUserId = UserId("other-user-id")
  val thirdUserId = UserId("third-user-id")

  val userName = UserName("some-user-name")
  val otherUserName = UserName("other-user-name")

  val user = User(userId, userName)
  val otherUser = User(otherUserId, otherUserName)

  val createChatCommand = CreateChatCommand(Set(userId, otherUserId))

  val chatMessage = ChatMessage(0, userId, s"text1 from $userId")
  val secondChatMessage = next(chatMessage)
  val chatState = ChatState(userIds = Set(userId, otherUserId))
  val addMessageCommand = AddMessageCommand(chatMessage)

  val otherUserMessage = chatMessage.copy(userId = otherUserId)
  val otherUserAddMessage = addMessageCommand.copy(message = otherUserMessage)

  def next(message: ChatMessage): ChatMessage = {
    val nextSeq = message.seq + 1
    message.copy(seq = nextSeq, text = s"text-$nextSeq")
  }

  def next(addMessageCommand: AddMessageCommand): AddMessageCommand = {
    addMessageCommand.copy(message = next(addMessageCommand.message))
  }
}
