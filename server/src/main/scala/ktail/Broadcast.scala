package ktail

import zio.*
import zio.stm.{ STM, TDequeue, THub, TMap }
import zio.stream.{ ZSink, ZStream }

trait Broadcast {
  def subscribe(topic: String): Task[TDequeue[Message]]
  def unsubscribe(topic: String): UIO[Unit]
}

case class BroadcastImpl(subscribers: TMap[String, Int], hubs: TMap[String, THub[Message]]) extends Broadcast {
  override def subscribe(topic: String): Task[TDequeue[Message]] =
    (for {
      counter <- subscribers.updateWith(topic) {
        case Some(counter) => Some(counter + 1)
        case _             => Some(1)
      }
      (hub, message) <- hubs.get(topic).flatMap {
        case Some(hub) => STM.succeed((hub, s"subscribe to existing $topic topic hub, ${counter.get} subscriber(s)"))
        case _ =>
          THub.unbounded[Message].tap(hubs.put(topic, _)).map((_, s"subscribe to new topic $topic, hub created"))
      }
      dequeue <- hub.subscribe
    } yield (dequeue, message)).commit.flatMap { case (dequeue, message) => ZIO.logInfo(message).as(dequeue) }

  override def unsubscribe(topic: String): UIO[Unit] =
    (for {
      counter <- subscribers.updateWith(topic) {
        case Some(1)       => None
        case Some(counter) => Some(counter - 1)
        case None          => None
      }
      message <- counter match {
        case Some(counter) =>
          STM.unit.map(_ => s"unsubscribe from topic $topic, hub is still active, $counter subscriber(s)")
        case _ =>
          hubs.delete(topic).map(_ => s"unsubscribe from topic $topic, no more subscribers, hub closed")
      }
    } yield message).commit.flatMap(ZIO.logInfo(_))
}

object BroadcastImpl {
  val live: URLayer[Buffer, BroadcastImpl] =
    ZLayer {
      for {
        subscribers <- TMap.empty[String, Int].commit
        hubs        <- TMap.empty[String, THub[Message]].commit
        _ <-
          ZStream
            .repeatZIO(Buffer.poll())
            .mapZIO {
              case Some(message) =>
                val publish = for {
                  hub <- hubs.get(message.topic)
                  _ <- hub match {
                    case Some(hub) => hub.publish(message).unit
                    case _         => STM.unit
                  }
                } yield ()

                ZIO.logTrace(s"publishing message ${message.id}") *> publish.commit
              case _ => ZIO.unit
            }
            .run(ZSink.drain)
            .forkDaemon
      } yield BroadcastImpl(subscribers, hubs)
    }
}
