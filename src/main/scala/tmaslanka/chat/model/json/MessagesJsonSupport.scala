package tmaslanka.chat.model.json

import tmaslanka.chat.model.commands._

trait MessagesJsonSupport extends BaseJsonSupport with ModelJsonSupport {
  implicit val createUserCommandFormat = jsonFormat1(CreateUserCommand)
  implicit val userCreatedResponseFormat = jsonFormat1(UserCreatedResponse)
  implicit val createChatCommandFormat = jsonFormat1(CreateChatCommand)
  implicit val chatCreatedResponseFormat = jsonFormat1(ChatCreated)
  implicit val createMessageCommandFormat = jsonFormat1(AddMessageCommand)
  implicit val chatsListResponseFormat = jsonFormat1(ChatsListResponse)
  implicit val getChatMessagesResponseFormat = jsonFormat2(GetChatMessagesResponse)
}
