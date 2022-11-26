package com.funding.pages

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait Page {
  def path: String
}

case class ProjectPage(id: Int) extends Page {
  def path: String = s"/project/$id"
}
case object AllProjectsPage extends Page {
  def path: String = "/projects"
}

case class ErrorPage(error: String) extends Page {
  def path: String = "/"
}
case object NotFoundPage extends Page {
  def path: String = "/"
}

object Page {
  implicit val codePage: Codec.AsObject[Page] = deriveCodec[Page]
}
