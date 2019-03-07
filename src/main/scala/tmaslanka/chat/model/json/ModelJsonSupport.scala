package tmaslanka.chat.model.json

import tmaslanka.chat.model.domain.{ChatDescription, ChatId, UserId, UserName}

trait ModelJsonSupport extends BaseJsonSupport {
  implicit val userIdFormat = valueJsonFormat(UserId)
  implicit val userNameFormat = valueJsonFormat(UserName)
  implicit val chatIdFormat = valueJsonFormat(ChatId.apply)
  implicit val chatDescriptionFormat = jsonFormat3(ChatDescription)
}
