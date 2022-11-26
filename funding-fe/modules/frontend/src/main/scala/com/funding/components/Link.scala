package com.funding.components

import com.raquo.laminar.api.L._
import com.funding.pages.Page
import com.funding.Routes

object Link {

  def apply(page: Page, mods: Modifier[HtmlElement]*): HtmlElement = {
    a(styleAttr := "color: #007bff",
      cls := "font-light",
      href := page.path,
      onClick.preventDefault --> { _ =>
        Routes.pushState(page)
      },
      mods
    )
  }

}
