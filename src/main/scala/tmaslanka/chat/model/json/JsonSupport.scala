package tmaslanka.chat.model.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}
import tmaslanka.chat.model.StringValue

import scala.reflect.ClassTag

trait JsonSupport
  extends ModelJsonSupport
  with MessagesJsonSupport




trait BaseJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  protected def valueJsonFormat[T <: Product with StringValue : ClassTag](create: String => T): JsonFormat[T] = new JsonFormat[T] {
    override def write(obj: T): JsValue = {
      JsString(obj.value)
    }
    override def read(json: JsValue): T = json match {
      case JsString(v) => create(v)
      case _ => deserializationError("String expected")
    }
  }
}
