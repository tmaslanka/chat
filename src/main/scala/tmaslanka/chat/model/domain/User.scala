package tmaslanka.chat.model.domain

import tmaslanka.chat.model.StringValue

final case class UserId(value: String) extends StringValue

final case class UserName(value: String) extends StringValue

final case class User(userId: UserId, userName: UserName)