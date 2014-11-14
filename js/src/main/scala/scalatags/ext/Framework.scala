package scalatags.ext

import scala.scalajs.js
import scalatags.JsDom.all._
import org.scalajs.dom.{Event, Element}


// taken from:
//   https://github.com/lihaoyi/workbench-example-app/blob/todomvc/src/main/scala/example/Framework.scala

/**
 * A minimal binding between Scala.Rx and Scalatags and Scala-Js-Dom
 */
object Framework extends Modifiable with Reactives with Html5 with Webkit with Animations {

  implicit class EnhancedInt(val _int: Int) extends AnyVal {
    def s: String = s"${_int}s"
  }

  implicit class EnhancedDouble(val _double: Double) extends AnyVal {
    def s: String = s"${_double}s"
  }

  object Events extends NamedEventUtil with DomL3 with Events

  val rowspan = "rowspan".attr
  val colspan = "colspan".attr

  implicit def FuncEventValue: EventValue[Element, Event => Unit] = new EventValue[Element, Event => Unit] {
    override def apply(t: Element, ne: NamedEvent, v: (Event) => Unit) = {
      t.addEventListener(ne.name, v)
    }
  }
}


