package com.funding.view

import com.funding.entity.{MoneyInvested, ProjectDetailsChanged, ProjectReplay, ProjectState}
import com.google.protobuf.empty.Empty
import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class AllProjectsViewImpl(context: ViewContext) extends AbstractAllProjectsView {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[ProjectDetailsViewImpl])

  override def emptyState: ProjectState = ProjectState.defaultInstance

  override def onMoneyInvested(
    state: ProjectState, event: MoneyInvested): UpdateEffect[ProjectState] = {
    val newState = ProjectReplay.moneyInvested(state, event)
    effects.updateState(newState)
  }



  override def onProjectDetailsChanged(
    state: ProjectState, event: ProjectDetailsChanged): UpdateEffect[ProjectState] = {
    val newState = ProjectReplay.projectDetailsChanged(state, event)
    effects.updateState(newState)
  }
  
}
