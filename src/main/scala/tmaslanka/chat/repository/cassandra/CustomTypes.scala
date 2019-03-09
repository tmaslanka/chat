package tmaslanka.chat.repository.cassandra

import com.outworkers.phantom.dsl.Primitive
import tmaslanka.chat.model.domain.{ChatId, UserId, UserName}

private[cassandra] object CustomTypes {
  implicit val userIdPrimitive: Primitive[UserId] = Primitive.derive[UserId, String](_.value)(UserId.apply)
  implicit val userNamePrimitive: Primitive[UserName] = Primitive.derive[UserName, String](_.value)(UserName.apply)
  implicit val chatIdPrimitive: Primitive[ChatId] = Primitive.derive[ChatId, String](_.value)(ChatId.apply)
}
