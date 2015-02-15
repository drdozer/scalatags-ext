package scalatags.ext

import scalatags.JsDom.attrs._

/**
 *
 *
 * @author Matthew Pocock
 */
trait Html5 {

  val min = "min".attr

  val max = "max".attr

}

trait Webkit {
  val webkitTransform = "-webkit-transform".style
}