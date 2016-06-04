package spatutorial.client.components

import scalacss.Defaults._

object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(70.px))
  )

  val bootstrapStyles = new BootstrapStyles
}
