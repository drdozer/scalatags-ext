package scalatags.ext

import org.scalajs.dom.{Element, Node, document}
import rx._
import rx.core.Obs
import rx.ops._

import scala.language.implicitConversions
import scala.util.{Failure, Success}
import scalatags.{generic, JsDom}
import scalatags.generic.Attr
import JsDom.all._

/**
 *
 *
 * @author Matthew Pocock
 */
trait Reactives {

   implicit def RxAttrValueFactory[T: AttrValue]: AttrValue[Element => T] = new AttrValue[Element => T] {
     override def apply(t: Element, a: generic.Attr, v: (Element) => T) = {
       implicitly[AttrValue[T]].apply(t, a, v(t))
     }
   }

   /**
    * Wraps reactive values in spans, so they can be referenced/replaced
    * when the Rx changes.
    */
   implicit def RxT[T](r: Rx[T])(implicit f: T => Frag): Frag = {
     rxModT(Rx(span(r())))
   }

   /**
    * Wrap a reactive string.
    */
   implicit def RxStr[T](r: Rx[String]): Frag = {
     rxModNode(Rx(document.createTextNode(r())))
   }

   /**
    * Wrap a reactive string.
    */
   implicit def RxOStr[T](r: Rx[Option[String]]): Frag = {
     RxStr(r map (_ getOrElse ""))
   }

   /**
    * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
    * to propagate changes into the DOM via the element's ID. Monkey-patches
    * the Obs onto the element itself so we have a reference to kill it when
    * the element leaves the DOM (e.g. it gets deleted).
    */
   implicit def rxModT[T <: Element](r: Rx[JsDom.TypedTag[T]]): Frag = {
     def rSafe = r.toTry match {
       case Success(v) => v.render
       case Failure(e) => span(e.toString, backgroundColor := "red").render
     }
     var last = rSafe
     Obs(r, skipInitial = true){
       val newLast = rSafe
       last.parentNode.replaceChild(newLast, last)
       last = newLast
     }
     bindNode(last)
   }

   /**
    * Sticks some Rx into a Scalatags fragment, which means hooking up an Obs
    * to propagate changes into the DOM via the element's ID. Monkey-patches
    * the Obs onto the element itself so we have a reference to kill it when
    * the element leaves the DOM (e.g. it gets deleted).
    */
   implicit def rxModNode(r: Rx[Node]): Frag = {
     def rSafe = r.toTry match {
       case Success(v) => v.render
       case Failure(e) => span(e.toString, backgroundColor := "red").render
     }
     var last = rSafe
     Obs(r, skipInitial = true){
       val newLast = rSafe
       last.parentNode.replaceChild(newLast, last)
       last = newLast
     }
     bindNode(last)
   }

   implicit def RxAttrValue[T](implicit tAttr: AttrValue[T]): AttrValue[Rx[T]] = new AttrValue[Rx[T]]{
     def apply(t: Element, a: Attr, r: Rx[T]): Unit = {
       Obs(r){ implicitly[AttrValue[T]].apply(t, a, r())}
     }
   }

   implicit def RxStyleValue[T](implicit tSty: StyleValue[T]): StyleValue[Rx[T]] = new StyleValue[Rx[T]]{
     def apply(t: Element, s: Style, r: Rx[T]): Unit = {
       Obs(r){ implicitly[StyleValue[T]].apply(t, s, r())}
     }
   }

 }
