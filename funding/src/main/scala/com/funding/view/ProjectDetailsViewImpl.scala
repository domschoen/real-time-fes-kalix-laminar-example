package com.funding.view

import com.funding.entity.MoneyInvested
import com.funding.entity.ProjectDetailsChanged
import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext

import io.circe.syntax._
import scalapb_circe.codec._

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ProjectDetailsViewImpl(context: ViewContext) extends AbstractProjectDetailsView {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[ProjectDetailsViewImpl])

  override def emptyState: ProjectDetailsResponse = ProjectDetailsResponse.defaultInstance

  override def onMoneyInvested(
    state: ProjectDetailsResponse, event: MoneyInvested): UpdateEffect[ProjectDetailsResponse] =
    effects.updateState(updatedStateWith(event.projectId, state, event))

  override def onProjectDetailsChanged(
    state: ProjectDetailsResponse, event: ProjectDetailsChanged): UpdateEffect[ProjectDetailsResponse] =
    effects.updateState(updatedStateWith(event.projectId, state, event))

  def updatedStateWith(projectId: Int, state: ProjectDetailsResponse, event: scalapb.GeneratedMessage) = {
    val eventClass: String = event.getClass.getName
    val json = event.asJson.noSpaces
    val eventMessage = EventMessage(eventClass, json)
    log.info("New event message: {}", eventMessage)

    state.copy(projectId = projectId, events = state.events :+ eventMessage)
  }

}
