package tmaslanka.chat.model.json

import tmaslanka.chat.model.commands.{ChatsListResponse, CreateMessageCommand, CreateUserCommand, UserCreatedResponse}

trait MessagesJsonSupport extends BaseJsonSupport with ModelJsonSupport {
  implicit val createUserCommandFormat = jsonFormat1(CreateUserCommand)
  implicit val userCreatedResponseFormat = jsonFormat1(UserCreatedResponse)
  implicit val createMessageCommandFormat = jsonFormat1(CreateMessageCommand)
  implicit val chatsListResponseFormat = jsonFormat1(ChatsListResponse)
}
