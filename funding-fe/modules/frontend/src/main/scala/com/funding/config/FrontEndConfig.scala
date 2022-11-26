package com.funding.config

import io.circe.parser._
import com.funding.config

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSImport
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.semiauto.deriveDecoder

final case class FrontEndConfig(
  useAuthentication: Boolean,
  websocketPort: Int,
  defaultUser: String,
  msal: MSALConfig,
  kalix: KalixConfig
)

//@JsonCodec
final case class MSALConfig(
  authority: String,
  authRedirectUrl: String,
  clientId: String
)

//@JsonCodec
final case class KalixConfig(
  useCloud: Boolean,
  cloudHostURL: String,
  localHostURL: String
)

object FrontEndConfig {
  implicit val msalConfigDecoder: io.circe.Decoder[MSALConfig]         = deriveDecoder[MSALConfig]
  implicit val kalixConfigDecoder: io.circe.Decoder[KalixConfig]       = deriveDecoder[KalixConfig]
  implicit val frontEndConfigDecoder: io.circe.Decoder[FrontEndConfig] = deriveDecoder[FrontEndConfig]

  @js.native
  @JSImport("frontend-config", JSImport.Namespace)
  private object ConfigGlobalScope extends js.Object {

    val config: js.Object = js.native

  }

  lazy val config: FrontEndConfig = decode[FrontEndConfig](JSON.stringify(ConfigGlobalScope.config)).fold(throw _, identity)

}

object Variables {
  val cloudHostURL = config.FrontEndConfig.config.kalix.cloudHostURL
  val localHostURL = config.FrontEndConfig.config.kalix.localHostURL
  val hostURL      = if (config.FrontEndConfig.config.kalix.useCloud) cloudHostURL else localHostURL
}
