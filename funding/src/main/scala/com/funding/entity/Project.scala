package com.funding.entity

import com.funding.api
import com.google.protobuf.empty.Empty
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class Project(context: EventSourcedEntityContext) extends AbstractProject {
  private val log = org.slf4j.LoggerFactory.getLogger(classOf[Project])

  override def emptyState: ProjectState = ProjectState()

  override def invest(currentState: ProjectState, command: api.InvestCommand): EventSourcedEntity.Effect[Empty] =
    if (command.amount <= 0)
      effects.error(s"Amount must be greater than zero.")
    else {
      val event = MoneyInvested(
        projectId = command.projectId,
        amount = command.amount,
        user = command.user,
        date = System.currentTimeMillis()
      )
      effects.emitEvent(event)
        .thenReply(_ => Empty.defaultInstance)
    }


  override def changeProjectDetails(currentState: ProjectState, command: api.ChangeProjectDetailsCommand): EventSourcedEntity.Effect[Empty] = {
      val event = ProjectDetailsChanged(
        projectId = command.projectId,
        goal = command.goal,
        title = command.title,
        description = command.description,
        user = command.user,
        date = System.currentTimeMillis()
      )
      effects.emitEvent(event)
        .thenReply(_ => Empty.defaultInstance)
    }


  override def getProject(state: ProjectState, request: api.GetProjectRequest): EventSourcedEntity.Effect[api.Project] =
    effects.reply(toApi(state))

  private def toApi(state: ProjectState): api.Project = {
    api.Project(
      projectId = state.projectId,
      funding = state.funded,
      goal = state.goal,
      title = state.title,
      description = state.description
    )
  }


  override def moneyInvested(currentState: ProjectState, event: MoneyInvested): ProjectState =
    ProjectReplay.moneyInvested(currentState,event)

  override def projectDetailsChanged(currentState: ProjectState, event: ProjectDetailsChanged): ProjectState =
    ProjectReplay.projectDetailsChanged(currentState,event)




}
