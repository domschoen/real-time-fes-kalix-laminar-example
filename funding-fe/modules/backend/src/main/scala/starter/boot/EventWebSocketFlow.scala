package starter.boot

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.typed.scaladsl.ActorSource

object EventWebSocketFlow {
  def apply(clockActor: ActorRef[EventProcessingActor.Command]): Flow[Message, Message, NotUsed] = {
    val incoming: Sink[Message, NotUsed] = Flow[Message]
      .map(EventProcessingActor.Command.UserRequest)
      .to(
        ActorSink.actorRef[EventProcessingActor.Command](
          clockActor,
          onCompleteMessage = EventProcessingActor.Command.Complete,
          onFailureMessage = { case ex => EventProcessingActor.Command.ConnectionFailure(ex) }
        )
      )
    val outgoing: Source[Message, Unit] = ActorSource
      .actorRef[EventProcessingActor.Outgoing](
        completionMatcher = { case EventProcessingActor.Outgoing.Completed => },
        failureMatcher = { case EventProcessingActor.Outgoing.Failure(ex) => ex },
        bufferSize = 10,
        OverflowStrategy.dropHead
      )
      .mapMaterializedValue(client => clockActor ! EventProcessingActor.Command.Connection(client))
      .map {
        case EventProcessingActor.Outgoing.MessageToClient(msg) => msg
        // These are already handled by completionMatcher and failureMatcher so should never happen
        // added them just to silence exhaustiveness warning
        case EventProcessingActor.Outgoing.Completed | EventProcessingActor.Outgoing.Failure(_) => TextMessage.Strict("")
      }

    Flow.fromSinkAndSource(incoming, outgoing)
  }
}
