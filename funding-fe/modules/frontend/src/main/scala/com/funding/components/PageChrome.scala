package com.funding.components

import com.raquo.laminar.api.L._
import com.funding.App
import com.funding.WebSocketClient
import com.funding.App.userVar
import com.funding.pages._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSGlobalScope
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@js.native
@JSGlobalScope
object DOMGlobalScope extends js.Object {
  def signIn(): Unit = js.native
}

object PageChrome {

  val authenticated = Var(initial = Option.empty[String])

  WebSocketClient.setSocket()

  def showPage($child: Signal[HtmlElement]) = {
    div(
      cls := "bg-white",
      div(
        cls := "block-container w-full bg-top bg-cover -mt-0 -mr-0 -mb-0 -ml-0 pt-0 pr-0 pb-0 pl-0",
        div(
          cls := "px-2 pt-2 pb-3 flex items-baseline",
          backgroundColor := "#007bff",
          div(
            cls := "max-w-screen-2xl flex-col md:flex-row md:justify-between flex w-full bg-top bg-cover justify-center mt-auto mr-auto mb-auto ml-auto pt-1 pr-1 pb-1 pl-8",
            div(
              cls := "flex bg-top bg-cover flex-row justify-between items-center " +
                "mt-0 mr-0 mb-0 ml-0 pt-0 pr-0 pb-0 pl-0 text-white text-xl",
              "CAS Customer Repository"
            ),
            div(
              inContext { thisNode =>
                val $userUI = userVar.signal.map(userOpt =>
                  userOpt match {
                    case Some(user) => div(cls := "text-white", user)
                    case None =>
                      div(
                        button(
                          cls := "h-9 w-24 rounded-lg hover:bg-blue-900 hover:border-blue-900 " +
                            "mt-0 mr-0 mb-0 ml-0 pt-0 pr-0 pb-0 pl-0 text-center text-base font-normal flex items-center justify-center text-white border-blue-700",
                          styleAttr := "font-family: Arial;",
                          onClick --> (_ => {
                            com.funding.App.myMSALObj.loginRedirect(App.loginRequest).toFuture.foreach(_ => println(""))
                          }),
                          "Sign In"
                        )
                      )
                  }
                )
                child <-- $userUI
              }
            )
          )
        )
      ),
      div(
        cls := "block-container bg-white bg-top bg-cover -mt-0 -mr-0 -mb-0 -ml-0 pt-0 pr-0 pb-0 pl-0",
        userVar.now() match {
          case Some(user) => {
            println("user ha " + user)
            child <-- $child
          }
          case None => div(cls := "px-80 py-40 text-4xl", "Please sign-in to continue...")
        }
      )
    )
  }

  def apply($child: Signal[HtmlElement]): HtmlElement =
    showPage($child)

}
