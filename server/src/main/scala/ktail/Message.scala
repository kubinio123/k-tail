package ktail

case class Message(topic: String, partition: Int, offset: Long, key: Array[Byte], value: Array[Byte]) {
  lazy val id: String = s"(topic $topic | partition $partition | offset $offset)"
}
