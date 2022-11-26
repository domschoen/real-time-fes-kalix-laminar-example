package starter.boot

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Props
import akka.actor.typed.SpawnProtocol
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.Askable
import starter.config.HttpConfiguration

import java.util.concurrent.TimeUnit

object Boot {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[SpawnProtocol.Command](SpawnProtocol(), "WebSocketServer")

    implicit val timeout   = Timeout(3, TimeUnit.SECONDS)
    implicit val scheduler = system.scheduler
    println(timeout)
    println(scheduler)
    system.ask[ActorRef[Nothing]](ref => Spawn[Nothing](RootActor(system, httpConfiguration(system)), "root", Props.empty, ref))

    println("Starting")
  }

  def httpConfiguration(system: ActorSystem[SpawnProtocol.Command]) = {
    val interface = system.settings.config.getString("starter.http.interface")
    val port      = system.settings.config.getInt("starter.http.port")
    HttpConfiguration(interface, port)
  }

}
