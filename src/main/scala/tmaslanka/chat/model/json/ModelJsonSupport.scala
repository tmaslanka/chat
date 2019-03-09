package tmaslanka.chat.model.json

import tmaslanka.chat.model.domain._

trait ModelJsonSupport extends BaseJsonSupport {
  implicit val userIdFormat = valueJsonFormat(UserId)
  implicit val userNameFormat = valueJsonFormat(UserName)
  implicit val userFormat = jsonFormat2(User)
  implicit val chatIdFormat = valueJsonFormat(ChatId.apply)
  implicit val chatMessageFormat = jsonFormat3(ChatMessage)
  implicit val chatDescriptionFormat = jsonFormat4(ChatDescription)
}
