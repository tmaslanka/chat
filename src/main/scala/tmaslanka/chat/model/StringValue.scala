package tmaslanka.chat.model

trait StringValue {
  def value: String
  override def toString: String = value
}
