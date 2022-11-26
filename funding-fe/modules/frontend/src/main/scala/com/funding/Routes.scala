package com.funding

import com.funding.allprojects.AllProjects
import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import org.scalajs.dom
import io.circe.syntax._
import io.circe.parser._
import com.funding.pages._
import com.funding.project.ProjectDashboardPage

object Routes {
  private val allProjectsRoute = Route.static(AllProjectsPage, root / "projects" / endOfSegments)

  private val projectRoute = Route[ProjectPage, Int](
    encode = projectPage => projectPage.id,
    decode = arg => ProjectPage(id = arg),
    pattern = root / "project" / segment[Int] / endOfSegments
  )

  private val notFoundRoute = Route.static(NotFoundPage, root)

  val router = new Router[Page](
    routes = List(
      allProjectsRoute,
      projectRoute,
      notFoundRoute
    ),
    getPageTitle = _.toString,                                                                      // mock page title (displayed in the browser tab next to favicon)
    serializePage = page => page.asJson.noSpaces,                                                   // serialize page data for storage in History API log
    deserializePage = pageStr => decode[Page](pageStr).fold(e => ErrorPage(e.getMessage), identity) // deserialize the above
  )(
    initialUrl = dom.document.location.href, // must be a valid LoginPage or UserPage url
    owner = unsafeWindowOwner,               // this router will live as long as the window
    origin = dom.document.location.origin.get,
    $popStateEvent = windowEvents.onPopState
  )

  private val splitter =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[ErrorPage] { $errorPage =>
        div(
          div("An unpredicted error has just happened. We think this is truly unfortunate."),
          div(
            child.text <-- $errorPage.map(_.error)
          )
        )
      }
      .collectSignal[ProjectPage] { $projectPage =>
        ProjectDashboardPage.render($projectPage)
      }
      .collectStatic(AllProjectsPage) { AllProjects.render() }

  def pushState(page: Page): Unit = {
    router.pushState(page)
  }

  def replaceState(page: Page): Unit = {
    router.replaceState(page)
  }

  val view: Signal[HtmlElement] = splitter.$view

}
