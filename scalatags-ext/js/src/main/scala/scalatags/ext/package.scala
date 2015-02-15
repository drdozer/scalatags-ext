package scalatags

import scala.scalajs.js

/**
 *
 *
 * @author Matthew Pocock
 */
package object ext {

  implicit class DynamicApply(val _dynamic: js.Dynamic.type) extends AnyVal {
    def apply(x: Any): js.Dynamic = x.asInstanceOf[js.Dynamic]
  }

}
