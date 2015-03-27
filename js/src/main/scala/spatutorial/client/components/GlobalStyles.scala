package spatutorial.client.components

import japgolly.scalacss.Defaults._

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(50.px))
  )

  style(unsafeRoot(".label-as-badge")(
    borderRadius(1.em))
  )

  val bootstrapStyles = new BootstrapStyles
}
