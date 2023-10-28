package ktail

import zio.*
import zio.stm.*
import zio.stream.{ ZSink, ZStream }

trait Broadcast {
  def subscribe(topic: String): Task[TDequeue[Message]] // ZIO[Any, Throwable, TDequeue[Message]]
  def unsubscribe(topic: String): UIO[Unit]             // ZIO[Any, Nothing, Unit]
}

case class BroadcastImpl(
    subscribers: TMap[String, Int],
    hubs: TMap[String, THub[Message]]
) extends Broadcast {

  override def subscribe(topic: String): Task[TDequeue[Message]] = {
    val subscribe: USTM[(Int, TDequeue[Message])] = for {
      subscribers <- incrementSubscribers(topic)
      hub         <- resolveHub(topic)
      dequeue     <- hub.subscribe
    } yield (subscribers, dequeue)

    subscribe.commit.flatMap { case (subscribers, dequeue) =>
      ZIO.logInfo(s"topic $topic has $subscribers subscriber(s)").as(dequeue)
    }
  }

  private def incrementSubscribers(topic: String): USTM[Int] =
    subscribers
      .updateWith(topic) {
        case Some(counter) => Some(counter + 1)
        case _             => Some(1)
      }
      .map(_.get)

  private def resolveHub(topic: String): USTM[THub[Message]] =
    hubs.get(topic).flatMap {
      case Some(hub) => STM.succeed(hub)
      case _         => THub.unbounded[Message].tap(hubs.put(topic, _))
    }

  override def unsubscribe(topic: String): UIO[Unit] = {
    val unsubscribe = for {
      subscribers <- decrementSubscribers(topic)
      _           <- if (subscribers.isEmpty) hubs.delete(topic) else STM.unit
    } yield subscribers

    unsubscribe.commit.flatMap(subscribers =>
      ZIO.logInfo(s"topic $topic has ${subscribers.getOrElse(0)} subscriber(s)")
    )
  }

  private def decrementSubscribers(topic: String): USTM[Option[Int]] =
    subscribers.updateWith(topic) {
      case Some(1)       => None
      case Some(counter) => Some(counter - 1)
      case None          => None
    }
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

                publish.commit
              case _ => ZIO.unit
            }
            .run(ZSink.drain)
            .forkDaemon
      } yield BroadcastImpl(subscribers, hubs)
    }
}
