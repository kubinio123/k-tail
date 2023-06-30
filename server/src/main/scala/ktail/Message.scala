package ktail

import zio.json.*

import java.util.Base64

case class Message(topic: String, partition: Int, offset: Long, key: Array[Byte], value: Array[Byte]) {
  lazy val id: String = s"(topic $topic | partition $partition | offset $offset)"
}

object Message {
  implicit val bytesEncoder: JsonEncoder[Array[Byte]] =
    JsonEncoder.string.contramap(Base64.getEncoder.encodeToString)
  implicit val encoder: JsonEncoder[Message] =
    DeriveJsonEncoder.gen[Message]
}
