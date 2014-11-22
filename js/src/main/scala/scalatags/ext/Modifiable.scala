package scalatags.ext

import org.scalajs.dom
import org.scalajs.dom.Element

import scala.annotation.unchecked.uncheckedVariance
import scalatags.JsDom.all._
import scalatags.generic

/**
 *
 *
 * @author Matthew Pocock
 */
trait Modifiable {

  implicit class ModifiableElement[Output <: Element](elem: Output) {
    def modifyWith: ElementModifier[Output] = ElementModifier(elem, Nil)
  }

  case class ElementModifier[Output <: Element](elem: Output,
                                                    modifiers: List[Seq[Modifier]])
    extends generic.TypedTag[Element, Output, dom.Node]
    with scalatags.jsdom.Frag
  {
    // unchecked because Scala 2.10.4 seems to not like this, even though
    // 2.11.1 works just fine. I trust that 2.11.1 is more correct than 2.10.4
    // and so just force this.
    protected[this] type Self = ElementModifier[Output @uncheckedVariance]

    def render: Output = {
      build(elem)
      elem
    }

    /**
     * Trivial override, not strictly necessary, but it makes IntelliJ happy...
     */
    def apply(xs: Modifier*): Self = {
      this.copy(modifiers = xs :: modifiers)
    }

    override def tag = elem.nodeName

    override def toString = render.outerHTML
  }

  def modifyWith(el: Element => Unit): Modifier = new generic.Modifier[Element] {
    override def applyTo(t: Element) = el(t)
  }
}
