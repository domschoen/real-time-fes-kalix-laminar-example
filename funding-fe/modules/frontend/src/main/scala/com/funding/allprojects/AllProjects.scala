package com.funding.allprojects

import com.funding.DataModel
import com.funding.Routes
import com.funding.WebSocketClient
import com.funding.DataModel.$projects
import com.funding.DataModel.ProjectData
import com.funding.DataModel.diffBus
import com.funding.components.Link
import com.funding.pages.ProjectPage
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object AllProjects extends Owner {
  case class SearchBox private (node: Element, signal: Signal[String])

  object SearchBox {
    def create = {
      val node = input(
        `type` := "text",
        idAttr := "search-filter"
      )

      val stream =
        node
          .events(onInput).mapTo {
            println("typed " + node.ref.value)
            node.ref.value
          }.startWith("")

      new SearchBox(node, stream)
    }
  }

  val fetchError = Var(Option.empty[String])
  var fetched    = false

  def render() = {
    WebSocketClient.setSocket()
    if (fetched) {
      println("Customers Already fetched")
      renderPage()
    } else {
      div(
        inContext { thisNode =>
          println("Fetching customers")
          val $s = DataModel
            .fetchProjets().map(_ match {
              case Right(fetchedData) =>
                val stream = EventStream.fromSeq(fetchedData)
                stream.addObserver(diffBus.toObserver)(this)
                fetched = true
                renderPage()

              case Left(errorMessage) =>
                renderError(errorMessage)
            })
          div(
            child <-- $s
          )
        }
      )
    }
  }

  def renderError(errorMessage: String): ReactiveHtmlElement[html.Div] = {
    div(cls := "text-purple-600", s"error $errorMessage")
  }
  val searchBox = SearchBox.create

  def sortProjects(cds: List[ProjectData]): List[ProjectData] = {
    cds.sortWith((a, b) => a.state.title.toLowerCase() < b.state.title.toLowerCase())
  }

  def isTitleMatchingCriteria(text: String, criteria: String) = text.toLowerCase().startsWith(criteria.toLowerCase())

  def projectIsKeptAccordingToCriteria(c: ProjectData, criteria: String) = {
    if (criteria.isEmpty) {
      true
    } else {
      isTitleMatchingCriteria(c.state.title, criteria)
    }
  }

  def renderPage(): ReactiveHtmlElement[html.Div] = {

    val $filteredProjects = $projects.combineWith(searchBox.signal)
      .map(tuple => {
        println("Current value " + tuple)
        val projects = tuple._1
        val criteria = tuple._2
        sortProjects(projects.filter(c => projectIsKeptAccordingToCriteria(c, criteria)))
      })
    div(
      cls := "sm:max-w-xl md:max-w-full md:px-24 lg:px-8 lg:py-10 w-full bg-top bg-cover mt-0 mr-auto mb-0 ml-auto pt-16 pr-4 pb-16 pl-4 component-selected",
      h1(cls := "text-4xl font-normal", "Projects "),
      div(
        div("Search with title: ", searchBox.node),
        div(child.text <-- DataModel.$projects.combineWith($filteredProjects).map(tuple => {
          s"${tuple._2.size.toString} / ${tuple._1.size.toString} records"
        })),
        table(
          cls := "border-collapse",
          styleAttr := "border: 1px solid #dee2e6; width: 100%; margin-bottom: 1rem; color: #212529; border-spacing: 2px;",
          thead(
            cls := "",
            styleAttr := "background-color: rgba(0, 0, 0, 0.075); border-spacing: 2px;",
            tr(
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", ""),
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Project ID"),
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Title"),
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Description"),
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Goal"),
              th(styleAttr := "border-bottom-width: 2px; padding: 12px; display: table-cell; border: 1px solid #dee2e6;", "Funded")
            )
          ),
          tbody(
            children <-- $filteredProjects.split(_.state.projectId)(renderProject)
          )
        )
      )
    )
  }

  def inspect = Observer[Int] { projectId =>
    Routes.pushState(ProjectPage(projectId))
  }

  private def renderProject(projectId: Int, initialProject: ProjectData, $item: Signal[ProjectData]): HtmlElement = {
    tr(
      td(
        svg.svg(
          svg.xmlns := "http://www.w3.org/2000/svg",
          svg.cls := "h-5 w-5",
          svg.fill := "currentColor",
          svg.viewBox := "0 0 20 20",
          svg.stroke := "currentColor",
          svg.path(
            svg.d := "M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
          ),
          onClick.mapTo(projectId) --> inspect
        )
      ),
      td(child.text <-- $item.map(_.state.projectId)),
      td(
        child <-- $item.combineWith(searchBox.signal)
          .map(tuple => {
            println("Current value " + tuple)
            val projectData  = tuple._1
            val criteria     = tuple._2
            val title        = projectData.state.title
            val index        = if (criteria.length > title.length) title.length else criteria.length
            val startOfTitle = title.substring(0, index)
            val endOfTitle   = title.substring(index)
            div(span(cls := "text-gray-500 font-bold", startOfTitle), span(cls := "text-gray-400", endOfTitle))
          })
      ),
      td(child.text <-- $item.map(_.state.description)),
      td(child.text <-- $item.map(_.state.goal)),
      td(child.text <-- $item.map(_.state.funded))
    )
  }

}
