package com.funding

import com.raquo.laminar.api.L._
import io.circe.syntax._
import io.circe.parser._
import scalapb_circe.codec._
import com.funding.entity._
import com.softwaremill.quicklens._
import io.circe.Json
import io.circe.ParsingFailure
import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.funding.config.Variables
import com.funding.entity.ProjectReplay
import com.funding.entity.ProjectState
import io.circe.generic.semiauto.deriveDecoder

import scala.scalajs.js.Date

object DataModel {
  val diffBus                  = new EventBus[Any]
  val barWriter: WriteBus[Any] = diffBus.writer

  case class ProjectData(state: com.funding.entity.ProjectState = ProjectState(), events: Seq[Any] = Seq.empty[Any])

  val $projects: Signal[List[ProjectData]] = diffBus.events.foldLeft(initial = List.empty[ProjectData])((acc, ev) => {
    ev match {
      case evt: com.funding.entity.ProjectState =>
        replayEvent(evt.projectId, evt, true, acc, snapshotReplay)
      case evt: com.funding.entity.ProjectDetailsChanged =>
        replayEvent(evt.projectId, evt, false, acc, ProjectReplay.projectDetailsChanged)
      case evt: com.funding.entity.MoneyInvested =>
        replayEvent(evt.projectId, evt, false, acc, ProjectReplay.moneyInvested)
      case _ =>
        println("Bus no managin event " + ev)
        acc
    }
  })

  def snapshotReplay(currentState: ProjectState, event: ProjectState): ProjectState = event

  def replayEvent[A](id: Int, event: A, isSnapshot: Boolean, acc: List[ProjectData], replay: (ProjectState, A) => ProjectState) = {
    val filter: ProjectData => Boolean = _.state.projectId == id
    val projectDataOpt                 = acc.find(filter)
    val projectData                    = projectDataOpt.getOrElse(ProjectData(state = ProjectState(projectId = id)))
    val newState                       = replay(projectData.state, event)
    val newAcc = projectDataOpt match {
      case Some(pd) => acc
      case None =>
        projectData :: acc
    }
    val newProjectData = if (isSnapshot) {
      projectData.copy(state = newState)
    } else {
      val newEvents = projectData.events :+ event
      ProjectData(newState, newEvents)
    }
    newAcc.modify(_.eachWhere(filter)).setTo(newProjectData)
  }

  def eventReceived(eventClass: String, eventText: String) = {
    eventWith(eventClass, eventText) match {
      case Right(event) =>
        println("WS Received " + event)
        barWriter.onNext(event)
      case Left(error) =>
        println(error.getMessage)
    }
  }

  def eventWith(eventClass: String, eventText: String): Either[io.circe.Error, Any] = {
    eventClass match {
      case "com.funding.entity.ProjectDetailsChanged" =>
        decode[com.funding.entity.ProjectDetailsChanged](eventText)
      case "com.funding.entity.MoneyInvested" =>
        decode[com.funding.entity.MoneyInvested](eventText)
      case _ =>
        val msg = "Web socket received unknown event " + className
        Left(ParsingFailure(msg, new Exception(msg)))
    }
  }

  def projectDetailsChangedWithCommand(command: com.funding.api.ChangeProjectDetailsCommand): ProjectDetailsChanged = {
    val dateMillis = new Date().getTime().toLong
    com.funding.entity.ProjectDetailsChanged(projectId = command.projectId,
                                             goal = command.goal,
                                             title = command.title,
                                             description = command.description,
                                             user = command.user,
                                             date = dateMillis
    )
  }

  def moneyInvestedWithCommand(command: com.funding.api.InvestCommand): MoneyInvested = {
    val dateMillis = new Date().getTime().toLong
    com.funding.entity.MoneyInvested(projectId = command.projectId, amount = command.amount, user = command.user, date = dateMillis)
  }

  def sendChangeProjectDetailsCommand(command: com.funding.api.ChangeProjectDetailsCommand): EventStream[Option[String]] = {
    val $response = sendKalixChangeProjectDetailsCommand(command)
    $response.map(resp =>
      resp match {
        case Left(error) => {
          println("Send to kalix error " + error)
          Some(error)
        }
        case Right(resp) =>
          val eventJSon = broadcastEvent(command, projectDetailsChangedWithCommand)
          None
      }
    )
  }

  def sendInvestCommand(command: com.funding.api.InvestCommand): EventStream[Option[String]] = {
    val $response = sendKalixInvestCommand(command)
    $response.map(resp =>
      resp match {
        case Left(error) => {
          println("Send to kalix error " + error)
          Some(error)
        }
        case Right(resp) =>
          val eventJSon = broadcastEvent(command, moneyInvestedWithCommand)
          None
      }
    )
  }

  def broadcastEvent[A, B](command: A, commandConverter: (A) => B)(implicit encoder: io.circe.Encoder[B]): Unit = {
    val event = commandConverter(command)

    val className = event.getClass.getName
    val eventJson = event.asJson

    //val eventJson = event.asJson.noSpaces
    //val text = s"""{"$className" : $eventJson }"""

    val fieldList            = List((className, eventJson))
    val jsonFromFields: Json = Json.fromFields(fieldList)
    WebSocketClient.send(jsonFromFields.noSpaces)
  }

  def kalixChangeProjectDetailsCommandUrl(project_id: Int) = s"${Variables.hostURL}/project/${project_id}/change"
  def kalixMoneyInvestedCommandUrl(project_id: Int)        = s"${Variables.hostURL}/project/${project_id}/invest"

  def kalixGetProjectDetailsCommandUrl(project_id: Int) = s"${Variables.hostURL}/project/${project_id}/details"
  val ALL_URL                                           = Variables.hostURL + "/projects"

  def sendKalixChangeProjectDetailsCommand(command: com.funding.api.ChangeProjectDetailsCommand) = {
    val url  = kalixChangeProjectDetailsCommandUrl(command.projectId)
    val json = command.asJson.noSpaces

    AjaxEventStream
      .put(
        url = url,
        data = json,
        headers = Map(
          "content-type" -> "application/json"
        )
      )
      .map(req => {
        println("response " + req)
        Right(req.responseText)
      })
      .recover { case err: AjaxStreamError => Some(Left(err.getMessage)) }
  }

  def sendKalixInvestCommand(command: com.funding.api.InvestCommand) = {
    val url  = kalixMoneyInvestedCommandUrl(command.projectId)
    val json = command.asJson.noSpaces

    AjaxEventStream
      .post(
        url = url,
        data = json,
        headers = Map(
          "content-type" -> "application/json"
        )
      )
      .map(req => {
        println("response " + req)
        Right(req.responseText)
      })
      .recover { case err: AjaxStreamError => Some(Left(err.getMessage)) }
  }

  def fetchProjectDetails(projectId: Int): EventStream[Either[String, Seq[Any]]] = {
    AjaxEventStream
      .get(
        url = kalixGetProjectDetailsCommandUrl(projectId),
        headers = Map(
          "content-type" -> "application/json"
        )
      )
      .map(r => ProjectDetailsParsing.parseDetails(r.responseText))
      .recover {
        case err: AjaxStreamError => {
          println("Err " + err.xhr.status)
          if (err.xhr.status == 404) {
            Some(Right(Seq.empty[Any]))
          } else
            Some(Left(err.getMessage))
        }
      }
  }

  // will create snapshot
  def fetchProjets(): EventStream[Either[String, List[ProjectState]]] = {
    AjaxEventStream
      .get(
        ALL_URL,
        data = "{}",
        headers = Map(
          "content-type" -> "application/json"
        )
      )
      .map(r => {
        parseFetchedCustomers(r.responseText)
      })
      .recover { case err: AjaxStreamError => Some(Left(err.getMessage)) }
  }

  case class AllProjectsData(projects: List[ProjectState])

  implicit val allJsonDecoder: io.circe.Decoder[AllProjectsData] = deriveDecoder[AllProjectsData]

  def parseFetchedCustomers(text: String): Either[String, List[ProjectState]] = {
    io.circe.parser.decode[AllProjectsData](text) match {
      case Right(a) =>
        val projects = a.projects
        Right(projects)
      case Left(error) =>
        Left(s"Error parsing fetched customers: ${error.getMessage}")
    }

  }

}
