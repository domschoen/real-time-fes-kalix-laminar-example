package starter.boot

import java.util.concurrent.TimeUnit
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Props
import akka.actor.typed.SpawnProtocol
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server._
import akka.util.Timeout
import starter.config.HttpConfiguration

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe.Import
import scala.util.Failure
import scala.util.Success

object RootActor {
  var eventProcessingActors = List.empty[ActorRef[EventProcessingActor.Command]]

  def broadcastMessage(command: starter.boot.EventProcessingActor.Command) = {
    eventProcessingActors.foreach(a => a ! command)
  }

  def apply(system: ActorSystem[SpawnProtocol.Command], httpConfiguration: HttpConfiguration): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
    implicit val timeout   = Timeout(3, TimeUnit.SECONDS)
    implicit val scheduler = system.scheduler

    val websocketRoute: Route = {
      concat(
        path("hello") {
          get {
            complete {
              println("Hello")
              broadcastMessage(EventProcessingActor.Command.PushText("Hello"))
              "Sent"
            }
          }
        },
        path("ws") {
          val clientF = system.ask[ActorRef[EventProcessingActor.Command]] { ref =>
            Spawn[EventProcessingActor.Command](
              EventProcessingActor.pending(),
              "client",
              props = Props.empty,
              replyTo = ref
            )
          }
          val eventProcessingActor = Await.result(clientF, Duration.Inf)
          eventProcessingActors = eventProcessingActor :: eventProcessingActors
          handleWebSocketMessages(
            EventWebSocketFlow(eventProcessingActor)
          )
        }
      )
    }
    startHttpServer(websocketRoute, httpConfiguration)(system)

    Behaviors.empty
  }

  private def startHttpServer(routes: Route, httpConfiguration: HttpConfiguration)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt(httpConfiguration.interface, httpConfiguration.port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
