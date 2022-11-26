package com.funding.entity

object ProjectReplay {

  def moneyInvested(currentState: ProjectState, event: MoneyInvested): ProjectState =
    currentState.copy(funded = currentState.funded + event.amount)

  def projectDetailsChanged(currentState: ProjectState, event: ProjectDetailsChanged): ProjectState =
    currentState.copy(
      projectId = event.projectId,
      goal = event.goal,
      title = event.title,
      description = event.description
    )

}
