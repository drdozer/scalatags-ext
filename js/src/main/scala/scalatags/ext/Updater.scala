package scalatags.ext

import org.scalajs.dom
import org.scalajs.dom.Element
import rx._
import rx.core.Obs

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scalatags.JsDom.all._

import scalatags.ext.SeqDiff._

trait Updater[T] {
  /**
   * Run when a data item enters. The returned fragment is grafted in to represent this data item.
   * @param en  the entered data value
   * @return    a fragment that renders the data item
   */
  def onEntered(en: Entered[T]): Frag

  /**
   * Run when a data item exits.
   * If the return value is empty, the existing node will be removed.
   * If it is a frag that returns the existing node, that node will be left as-is.
   * If it is a frag that is new, the old node will be removed and the frag grafted in as a replacement.
   *
   * @param ex        the data item
   * @param existing  the node currently rendering the item
   * @return          optionally a fragment to add representing the exiting item
   */
  def onExited(ex: Exited[T], existing: dom.Node): Option[Frag] = None
  def onModified(mod: Modified[T], existing: dom.Node): Option[Frag] = None
  def onUnchanged(un: Unchanged[T], existing: dom.Node): Option[Frag] = None

  def dataAttribute: String = "__rx_data"
}

object Updater {

  implicit class RxEnhancer[T : Ordering](_rx: Rx[IndexedSeq[T]]) {

    def updateWith(updater: Updater[T]): Modifier = new Modifier {

      val diff = SeqDiff(_rx)
      val Undefined = js.undefined

      def advanceTo(n: dom.Node, t: T): dom.Node = {
        n match {
          case null | Undefined =>
            n
          case e: dom.Element =>
            val da = Dynamic(e).selectDynamic(updater.dataAttribute)
            if (implicitly[Ordering[T]].equiv(da.asInstanceOf[T], t)) {
              e
            } else {
              advanceTo(n.nextSibling, t)
            }
          case _ => advanceTo(n.nextSibling, t)
        }
      }

      override def applyTo(parent: Element) = Obs(diff.updates) {

        def unwind(child: dom.Node, dss: List[Update[T]]): Unit = {

          dss match {
            case (u@Entered(_, _))::us =>
              val fragR = updater.onEntered(u).render.asInstanceOf[dom.Element]
              Dynamic(fragR).updateDynamic(updater.dataAttribute)(Dynamic(u.item))
              parent.insertBefore(fragR, child)
              unwind(child, us)
            case (u@Exited(_, _))::us =>
              val thisChild = advanceTo(child, u.item)
              val nextChild = thisChild.nextSibling
              updater.onExited(u, thisChild) match {
                case Some(frag) =>
                  val fragR = frag.render
                  if(fragR eq thisChild) {
                    Dynamic(child).updateDynamic(updater.dataAttribute)(Undefined)
                  } else {
                    parent.replaceChild(fragR, thisChild)
                  }
                case None =>
                  parent.removeChild(thisChild)
              }
              unwind(nextChild, us)
            case (u@Modified(_, _))::us =>
              val thisChild = advanceTo(child, u.item._1)
              val nextChild = thisChild.nextSibling
              updater.onModified(u, child) match {
                case Some(frag) =>
                  val fragR = frag.render
                  if(fragR eq thisChild) {
                    Dynamic(thisChild).updateDynamic(updater.dataAttribute)(Dynamic(u.item._2))
                  } else {
                    Dynamic(thisChild).updateDynamic(updater.dataAttribute)(js.undefined)
                    Dynamic(fragR).updateDynamic(updater.dataAttribute)(Dynamic(u.item._2))
                    parent.insertBefore(fragR, child)
                    parent.removeChild(child)
                  }
                case None =>
                  Dynamic(thisChild).updateDynamic(updater.dataAttribute)(Dynamic(u.item._2))
              }
              unwind(nextChild, us)
            case (u@Unchanged(_, _))::us =>
              val thisChild = advanceTo(child, u.item._1)
              val nextChild = thisChild.nextSibling
              updater.onUnchanged(u, child) match {
                case Some(fragR) =>
                  val frag = fragR.render
                  if(frag eq child) {
                    Dynamic(thisChild).updateDynamic(updater.dataAttribute)(Dynamic(u.item._2))
                  } else {
                    Dynamic(child).updateDynamic(updater.dataAttribute)(Dynamic(u.item))
                    parent.insertBefore(fragR.render, child)
                    parent.removeChild(child)
                  }
                case None =>
                  Dynamic(thisChild).updateDynamic(updater.dataAttribute)(Dynamic(u.item._2))
              }
              unwind(nextChild, us)
            case Nil =>
              // noop
          }
        }

        unwind(parent.firstChild, diff.updates())
      }
    }
  }

}