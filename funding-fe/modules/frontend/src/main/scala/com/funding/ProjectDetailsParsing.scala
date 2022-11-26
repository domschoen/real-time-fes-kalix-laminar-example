package com.funding
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.parser._
import io.circe.ParsingFailure

object ProjectDetailsParsing {
  case class LogEvent(action: String, content: String)

  case class Details(events: Seq[LogEvent])

  implicit val nestedDecoder: io.circe.Decoder[LogEvent] = deriveDecoder[LogEvent]
  implicit val jsonDecoder: io.circe.Decoder[Details]    = deriveDecoder[Details]

  def parseDetails(response: String) = {
    val parseResult: Either[io.circe.Error, Details] = io.circe.parser.decode[Details](response)
    parseResult match {
      case Right(a) =>
        val eventOpts = a.events.map(rawEvent => {
          DataModel.eventWith(rawEvent.action, rawEvent.content) match {
            case Right(event) =>
              Some(event)
            case Left(error) =>
              println("Decoding error " + error.getMessage + " " + error.getCause)
              None
          }
        })
        val events = eventOpts.flatten
        Right(events)
      case Left(error) =>
        Left(s"Error parsing fetched customers: ${error.getMessage}")
    }
  }

}
