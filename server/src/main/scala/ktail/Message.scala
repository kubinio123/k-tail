package ktail

import zio.json.*

case class Message(topic: String, partition: Int, offset: Long, key: Array[Byte], value: Array[Byte]) {
  lazy val id: String = s"(topic $topic | partition $partition | offset $offset)"
}

object Message {
  implicit val encoder: JsonEncoder[Message] = DeriveJsonEncoder.gen[Message]
}
