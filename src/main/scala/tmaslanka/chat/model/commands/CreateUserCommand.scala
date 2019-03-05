package tmaslanka.chat.model.commands

import tmaslanka.chat.model.domain.{UserId, UserName}

final case class CreateUserCommand(userName: UserName)
final case class UserCreatedResponse(userId: UserId)
