package com.funding

import com.raquo.laminar.api.L._
import org.scalajs.dom.document
import com.funding.components.PageChrome

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.concurrent.ExecutionContext.Implicits.global

@js.native
trait AccountInfo extends js.Object {
  val environment: String      = js.native
  val homeAccountId: String    = js.native
  val idTokenClaims: js.Object = js.native
  val localAccountId: String   = js.native
  val name: String             = js.native
  val tenantId: String         = js.native
  val username: String         = js.native
}
@js.native
trait AuthenticationResult extends js.Object {
  val accessToken: String        = js.native
  val account: AccountInfo       = js.native
  val authority: String          = js.native
  val cloudGraphHostName: String = js.native
  val expiresOn: js.Object       = js.native
  val extExpiresOn: js.Object    = js.native
  val familyId: String           = js.native
  val fromCache: Boolean         = js.native
  val idToken: String            = js.native
  val idTokenClaims: js.Object   = js.native
  val msGraphHost: String        = js.native
  val scopes: js.Array[String]   = js.native
  val state: String              = js.native
  val tenantId: String           = js.native
  val tokenType: String          = js.native
  val uniqueId: String           = js.native
}

// see https://azuread.github.io/microsoft-authentication-library-for-js/ref/classes/_azure_msal_browser.publicclientapplication.html
@js.native
@JSGlobal("msal.PublicClientApplication")
class PublicClientApplication(config: js.Dynamic) extends js.Object {
  def handleRedirectPromise(): js.Promise[AuthenticationResult] = js.native
  def loginRedirect(request: js.Object): js.Promise[Unit]       = js.native
  def getAllAccounts(): js.Array[AccountInfo]                   = js.native
}

object App {
  val userVar = Var(Option.empty[String])

  val loginRequest = js.Dynamic.literal(scopes = js.Array("User.Read"))
  val msalConfig = js.Dynamic.literal(
    auth = js.Dynamic.literal(
      clientId = config.FrontEndConfig.config.msal.clientId,
      authority = config.FrontEndConfig.config.msal.authority,
      redirectUri = config.FrontEndConfig.config.msal.authRedirectUrl
    ),
    cache = js.Dynamic.literal(
      cacheLocation = "sessionStorage", // This configures where your cache will be stored
      storeAuthStateInCookie = false    // Set this to "true" if you are having issues on IE11 or Edge
    )
  )
  val myMSALObj = new PublicClientApplication(msalConfig)

  def main(args: Array[String]): Unit = {
    renderApp()
  }

  def renderApp() = {
    val container = document.getElementById("app-container") // This div, its id and contents are defined in index-fastopt.html/index-fullopt.html files
    val _ =
      render(
        container,
        div(child <-- Routes.view)
      )
  }

}
