package com.funding.project
import com.funding.DataModel
import com.funding.WebSocketClient
import com.funding.DataModel.$projects
import com.funding.DataModel.ProjectData
import com.funding.DataModel.diffBus
import com.funding.api.ChangeProjectDetailsCommand
import com.funding.api.InvestCommand
import com.raquo.laminar.api.L._
import org.scalajs.dom
import com.funding.pages.ProjectPage
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.funding.components.Link
import com.funding.entity.MoneyInvested
import com.funding.entity.ProjectDetailsChanged
import com.funding.entity.ProjectState
import org.scalajs.dom.html
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ProjectDashboardPage extends Owner {
  var fetched         = Set.empty[Int]
  val stateVar        = Var(FormState())
  val investAmountVar = Var(InvestState())

  def formStateWithProjectData(p: ProjectData) = FormState(projectId = p.state.projectId, title = p.state.title, description = p.state.description, goal = p.state.goal)

  val projectStateWriter =
    stateVar.updater[ProjectData]((_, p) => formStateWithProjectData(p))

  def render($page: Signal[ProjectPage]): HtmlElement = {
    WebSocketClient.setSocket()
    val $g = $page.map(page => {
      val projectId = page.id
      if (fetched.contains(projectId)) {
        renderPage($page)
      } else {
        div(
          inContext { thisNode =>
            val $s = DataModel
              .fetchProjectDetails(projectId).map(
                _ match {
                  case Right(details) =>
                    val detailsWithDefault = if (details.isEmpty) {
                      List(ProjectState(projectId = projectId))
                    } else {
                      fetched += projectId
                      details
                    }
                    val stream = EventStream.fromSeq(detailsWithDefault)
                    stream.addObserver(diffBus.toObserver)(this)
                    renderPage($page)
                  case Left(errorMessage) =>
                    println("Fetched details errir " + errorMessage)
                    $errorVar.set(Some(errorMessage))
                    renderError($errorVar)
                }
              )
            div(
              child <-- $s
            )
          }
        )
      }
    })
    div(
      child <-- $g
    )
  }

  val backendProjectObs = Observer[Option[ProjectData]] { pd =>
    if (pd.isDefined) {
      stateVar.update(_ => formStateWithProjectData(pd.get))
      investAmountVar.update(_ => InvestState(projectId = pd.get.state.projectId))
    }
  }
  def renderPage($page: Signal[ProjectPage]): HtmlElement = {
    val $opt = $projects.withCurrentValueOf($page)
      .map(tuple => {
        val projectId = tuple._2.id
        tuple._1.find(c => c.state.projectId.equals(projectId))
      })
    $opt.addObserver(backendProjectObs)(this)
    div(
      inContext { thisNode =>
        val $s = $opt.map { projectOpt =>
          renderProject(projectOpt)
        }
        child <-- $s
      }
    )
  }

  def renderError(errorVar: Var[Option[String]]): ReactiveHtmlElement[html.Div] = {
    div(cls := "text-purple-600", child <-- errorVar.signal.map(e => s"error ${e}"))
  }

  case class InvestState(
    projectId: Int = 0,
    amount: Double = 0.0
  )

  case class FormState(
    projectId: Int = 0,
    title: String = "",
    description: String = "",
    goal: Double = 0.0,
    funded: String = "0",
    showErrors: Boolean = false
  ) {

    def hasErrors: Boolean = titleError.nonEmpty || goalError.nonEmpty

    def titleError: Option[String] = {
      if (title.nonEmpty) {
        None
      } else {
        Some("Title must not be empty.")
      }
    }

    def descriptionError: Option[String] = None

    def goalError: Option[String] = {
      if (goal > 0.0) {
        None
      } else {
        Some("Goal must greater than 0.")
      }
    }

    def displayError(error: FormState => Option[String]): Option[String] = {
      error(this).filter(_ => showErrors)
    }
  }

  val senderObs = Observer[Option[String]] { errOpt =>
    $errorVar.set(errOpt)
  }

  val investObs = Observer[InvestState] { investState =>
    DataModel
      .sendInvestCommand(
        InvestCommand(
          projectId = investState.projectId,
          amount = investState.amount,
          user = "dschoen"
        )
      ).addObserver(senderObs)(this)
  }

  val submitter = Observer[FormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showErrors = true))
    } else {
      DataModel
        .sendChangeProjectDetailsCommand(
          ChangeProjectDetailsCommand(
            projectId = state.projectId,
            goal = state.goal.toDouble,
            title = state.title,
            description = state.description,
            user = "dschoen"
          )
        ).addObserver(senderObs)(this)
    }
  }

  def renderInputRow(error: FormState => Option[String])(mods: Modifier[HtmlElement]*): HtmlElement = {
    val $error = stateVar.signal.map(_.displayError(error))
    div(
      cls("-inputRow"),
      cls.toggle("x-hasError") <-- $error.map(_.nonEmpty),
      p(mods),
      child.maybe <-- $error.map(_.map(err => div(cls("-error"), err)))
    )
  }

  val descriptionWriter  = stateVar.updater[String]((state, desc) => state.copy(description = desc))
  val titleWriter        = stateVar.updater[String]((state, text) => state.copy(title = text))
  val goalWriter         = stateVar.updater[String]((state, text) => state.copy(goal = text.toDouble))
  val investAmountWriter = investAmountVar.updater[String]((state, text) => state.copy(amount = text.toDouble))
  val $errorVar          = Var(Option.empty[String])

  def renderProject(projectOpt: Option[ProjectData]) = {
    div(
      div(h1(cls("text-4xl"), child <-- stateVar.signal.map("Project #" + _.projectId))),
      form(
        onSubmit.preventDefault
          .mapTo(stateVar.now()) --> submitter,
        renderInputRow(_.descriptionError)(
          label("Title: "),
          input(
            cls(
              "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
            ),
            value <-- stateVar.signal.map(_.title),
            onInput.mapToValue --> titleWriter
          )
        ),
        renderInputRow(_.descriptionError)(
          label("Description: "),
          input(
            cls(
              "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
            ),
            controlled(
              value <-- stateVar.signal.map(_.description),
              onInput.mapToValue --> descriptionWriter
            )
          )
        ),
        renderInputRow(_.goalError)(
          label("Goal: "),
          input(
            typ("number"),
            stepAttr("0.5"),
            minAttr("0"),
            cls(
              "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
            ),
            controlled(
              value <-- stateVar.signal.map(_.goal.toString),
              onInput.mapToValue --> goalWriter
            )
          )
        ),
        p(
          button(
            cls(
              "text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm w-full sm:w-auto px-5 py-2.5 text-center dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
            ),
            typ("submit"),
            "Update project info"
          )
        ),
        hr(),
        p(
          renderInputRow(_.goalError)(
            label("How much do you want to invest ? "),
            input(
              typ("number"),
              cls(
                "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
              ),
              controlled(
                value <-- investAmountVar.signal.map(_.amount.toString),
                onInput.mapToValue --> investAmountWriter
              )
            )
          ),
          button(
            cls(
              "text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm w-full sm:w-auto px-5 py-2.5 text-center dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
            ),
            onClick.mapTo(investAmountVar.now()) --> investObs,
            "Invest"
          )
        ),
        projectOpt match {
          case Some(pd) => renderHistory(pd.events)
          case None     => div("")
        }
      )
    )
  }

  case class HistoryEvent(action: String, date: String, user: String, description: String)

  val format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def formattedDate(time: Long): String = {
    val instant = Instant.ofEpochMilli(time)
    val date    = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(1))
    date.format(format1)
  }

  def eventToHistoryEvent(event: Any) = {
    event match {
      case projectDetailsChanged: ProjectDetailsChanged =>
        HistoryEvent(
          "Details changed",
          formattedDate(projectDetailsChanged.date),
          projectDetailsChanged.user,
          s"title: ${projectDetailsChanged.title} description:${projectDetailsChanged.description} goal: ${projectDetailsChanged.goal}"
        )
      case moneyInvested: MoneyInvested =>
        HistoryEvent("Invested", formattedDate(moneyInvested.date), moneyInvested.user, s"Invest ${moneyInvested.amount}")
    }
  }

  def renderHistory(events: Seq[Any]): ReactiveHtmlElement[html.Div] = {
    val historyEvents = events.map(eventToHistoryEvent(_))

    div(
      h1(cls("text-3xl"), "Project History"),
      table(
        cls := "border-collapse",
        styleAttr := "border: 1px solid #dee2e6; width: 100%; margin-bottom: 1rem; color: #212529; border-spacing: 2px;",
        thead(
          cls := "",
          styleAttr := "background-color: rgba(0, 0, 0, 0.075); border-spacing: 2px;",
          tr(
            th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Action"),
            th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Date"),
            th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "User"),
            th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Description")
          )
        ),
        tbody(
          historyEvents.map(event => renderEvent(event))
        )
      )
    )

  }

  private def renderEvent(event: HistoryEvent) = {
    tr(
      td(event.action),
      td(event.date),
      td(event.user),
      td(event.description)
    )
  }

}
