package com.funding

import org.scalajs.dom
import org.scalajs.dom._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.parser._
import io.circe._
// google: scala.js websocket send java.nio.ByteBuffer
// ->
// Could be the solution:
// https://github.com/kiritsuku/amora/blob/master/web-ui/src/main/scala/amora/frontend/webui/Connection.scala
import scala.scalajs.js.timers._

object WebSocketClient {
  val url                       = "ws://localhost:" + config.FrontEndConfig.config.websocketPort + "/ws"
  var socketOpt: Option[Socket] = None

  def setSocket() = {
    socketOpt = Some(
      Socket(url)((event: MessageEvent) =>
        event.data match {
          case text: String =>
            val parseResult: Either[ParsingFailure, Json] = parse(text)
            parseResult match {
              case Right(json) =>
                val obj       = json.asObject.get
                val className = obj.keys.toList.head
                val eventJson = obj.values.toList.head
                DataModel.eventReceived(className, eventJson.noSpaces)
              case Left(error) =>
                println("Decoding error " + error)
            }

          case blob: Blob =>
            println("Socket received blob " + blob)
          case _ => println("Error on receive, should be a blob.")
        }
      )
    )

  }

  def send(msg: String): Unit = {
    if (socketOpt.isEmpty) {
      setSocket()
    }
    socketOpt.get.send(msg)
  }

  case class Socket(url: String)(onMessage: (MessageEvent) => _) {

    private val socket: WebSocket = new dom.WebSocket(url = url)

    def send(msg: String): Unit = {
      if (socket.readyState == 1) {
        socket.send(msg)
      } else {
        setTimeout(500) {
          send(msg)
        }
      }
    };

    socket.onopen = (e: Event) => {}
    socket.onclose = (e: CloseEvent) => {
      println(s"Socket closed. Reason: ${e.reason} (${e.code})")
      setSocket()

    }
    socket.onerror = (e: Event) => {
      println(s"Socket error! ${e}")
    }
    socket.onmessage = onMessage
  }

}
