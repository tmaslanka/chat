package tmaslanka.chat.model.json

import tmaslanka.chat.model.commands._

trait MessagesJsonSupport extends BaseJsonSupport with ModelJsonSupport {
  implicit val createUserCommandFormat = jsonFormat1(CreateUserCommand)
  implicit val userCreatedResponseFormat = jsonFormat1(UserCreatedResponse)
  implicit val chatMessageFormat = jsonFormat3(ChatMessage)
  implicit val createMessageCommandFormat = jsonFormat1(AddMessageCommand)
  implicit val chatsListResponseFormat = jsonFormat1(ChatsListResponse)
}
