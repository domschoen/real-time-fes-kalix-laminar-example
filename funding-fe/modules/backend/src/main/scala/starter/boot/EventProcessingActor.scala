package starter.boot

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import starter.boot.RootActor.broadcastMessage

import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object EventProcessingActor {

  sealed trait Command
  object Command {

    case class UserRequest(msg: Message) extends Command

    case class Connection(actorRef: ActorRef[EventProcessingActor.Outgoing]) extends Command

    case class ConnectionFailure(ex: Throwable) extends Command

    case object Complete extends Command

    case class PushText(s: String) extends Command

  }

  sealed trait Outgoing
  object Outgoing {

    case class MessageToClient(msg: Message) extends Outgoing

    case object Completed extends Outgoing

    case class Failure(ex: Exception) extends Outgoing

  }

  def pending(): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Command.Connection(ref) =>
        connected(ref)
      case Command.ConnectionFailure(ex) =>
        ctx.log.warn("WebSocket failed", ex)
        Behaviors.stopped
      case Command.Complete =>
        ctx.log.info("User closed connection")
        Behaviors.stopped
      case _ => Behaviors.same
    }
  }

  def connected(actorRef: ActorRef[EventProcessingActor.Outgoing]): Behavior[Command] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Command.Connection(_) => // shouldn't happen at this point
          Behaviors.same
        case Command.UserRequest(TextMessage.Strict(txt)) =>
          broadcastMessage(EventProcessingActor.Command.PushText(txt))
          connected(actorRef)
        case Command.UserRequest(uk) =>
          actorRef ! Outgoing.MessageToClient(TextMessage(s"Received unknown: $uk"))
          Behaviors.same
        case Command.PushText(text) =>
          actorRef ! Outgoing.MessageToClient(TextMessage(text))
          Behaviors.same
        case Command.ConnectionFailure(ex) =>
          ctx.log.warn("WebSocket failed", ex)
          Behaviors.stopped
        case Command.Complete =>
          ctx.log.info("User closed connection")
          Behaviors.stopped
      }
    }
}
