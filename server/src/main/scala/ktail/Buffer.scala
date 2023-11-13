package ktail

import zio.{ Queue, UIO, ULayer, URIO, ZIO, ZLayer }

trait Buffer {
  def offer(message: Message): UIO[Unit] // ZIO[Any, Nothing, Unit]
  def poll(): UIO[Option[Message]]       // ZIO[Any, Nothing, Option[Message]]
}

object Buffer {
  def offer(message: Message): URIO[Buffer, Unit] =
    ZIO.serviceWithZIO[Buffer](_.offer(message))

  def poll(): URIO[Buffer, Option[Message]] =
    ZIO.serviceWithZIO[Buffer](_.poll())
}

final case class BufferImpl(queue: Queue[Message]) extends Buffer {
  override def offer(message: Message): UIO[Unit] =
    queue.offer(message).unit *> ZIO.logInfo(s"buffer message ${message.id}")

  override def poll(): UIO[Option[Message]] = queue.poll
}

object BufferImpl {
  val live: ULayer[Buffer] = ZLayer(Queue.unbounded[Message].map(BufferImpl(_)))
}
