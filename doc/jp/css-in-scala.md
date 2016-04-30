# CSS in Scala

Now that it's quite common to generate HTML in code (using Scalatags for example), why not do the same for style sheets! There are certain benefits to
creating style sheets in code instead of using external CSS files. One clear benefit is to get rid of global class names that quite easily clash with
each other if you're not careful. Additionally you get things like type safety, easy refactoring and IDE completion support.

At this writing there are at least two separate libraries for producing CSS in Scala. One is embedded with [Scalatags](https://github.com/lihaoyi/scalatags) and
the other one is a separate library called [ScalaCSS](https://github.com/japgolly/scalacss). They take a bit different approaches, so you might want to check both
out and see which one fits your application better. In this tutorial we are using ScalaCSS, as it integrates nicely with scalajs-react.

## Defining global styles

In our tutorial we are relying on Bootstrap to provide most of the CSS, so the global style definitions are really simple. The original CSS basically
contains only one definition.

```css
body {
    padding-top: 50px;
}
```

To express this in ScalaCSS we will use the `StyleSheet.Inline` class.

```scala
object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(50.px))
  )
}
```

For more extensive examples, please refer to [ScalaCSS documentation](https://japgolly.github.io/scalacss/book).

Each call to `style` registers a new style in the internal registry. To actually generate the CSS we need in the HTML page, we have to call

```scala
GlobalStyles.addToDocument()
```

in our application initialization code. Note that this is [specific initialization to scalajs-react](https://japgolly.github.io/scalacss/book/ext/react.html)
and there are other methods for creating and inserting CSS in other situations.

## Wrapping external CSS

As most of the styles we use are defined in Bootstrap CSS, we want to access those in a more convenient manner. Especially if at some point we would want
to switch from Bootstrap to, say, [MaterializeCSS](http://materializecss.com/), it would be really nice if all the CSS class names would occur only in a single location.

In Bootstrap it's very common to define a style using a base class and a contextual class, for example:

```html
<button class="btn btn-info">Info button</button>
```

So we'll start by defining the contextual options and some helper functions to create the style wrappers.

```scala
class BootstrapStyles(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  val csDomain = Domain.ofValues(default, primary, success, info, warning, danger)
  val contextDomain = Domain.ofValues(success, info, warning, danger)

  def commonStyle[A](domain: Domain[A], base: String) = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-$opt"))
  )

  def styleWrap(classNames: String*) = style(addClassNames(classNames: _*))
```

The values `default`, `primary` etc. come from an enumeration defined in the `Bootstrap.scala` component. The concept of *domain* comes from ScalaCSS 
[functional styles](http://japgolly.github.io/scalacss/book/features/stylef.html) and is a way of listing all possible values for a style that are generated
before being used.

`commonStyle` is a *functional style*, which takes as an input one value from the defined domain and returns the appropriate style. We can define all
possible Bootstrap `button` styles with simply

```scala
  val buttonOpt = commonStyle(csDomain, "btn")
  val button = buttonOpt(default)
```
The default button style is defined as `button` for simple use, but if you'd need an *info* button a simple call `buttonOpt(info)` would give you that.

For more straightforward Bootstrap styles we use the `styleWrap` function, which simply adds all the provided Bootstrap class names to the style. To make the
use of all the various Bootstrap styles more clear, we wrap related styles under separate objects.

```scala
object listGroup {
  val listGroup = styleWrap("list-group")
  val item = styleWrap("list-group-item")
  val itemOpt = commonStyle(contextDomain, "list-group-item")
}
```

## Using styles

To use the defined inline styles in your React components, you need to `import scalacss.ScalaCssReact._` to get the relevant implicit conversions.
After that it's as simple as getting a reference to your stylesheet and using the styles in your tags like below.

```scala
private def bss = GlobalStyles.bootstrapStyles

val style = bss.listGroup
def renderItem(item: TodoItem) = {
  // convert priority into Bootstrap style
  val itemStyle = item.priority match {
    case TodoLow => style.itemOpt(CommonStyle.info)
    case TodoNormal => style.item
    case TodoHigh => style.itemOpt(CommonStyle.danger)
  }
  <.li(itemStyle)(
    <.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
    <.span(" "),
    if (item.completed) <.s(item.content) else <.span(item.content),
    Button(Button.Props(() => P.editItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Edit"),
    Button(Button.Props(() => P.deleteItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Delete")
  )
}
<.ul(style.listGroup)(P.items map renderItem)
```

As the Bootstrap class names are "hidden" behind Scala methods, you have full IDE code completion support and there is no chance of mistyping a class name
without the compiler noticing it. And the output is still identical to what you would expect:

```html
<ul class="scalacss-0029 list-group">
  <li class="scalacss-0034 list-group-item list-group-item-danger">
  .
  .
```
