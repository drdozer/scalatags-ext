package scalatags.ext

import scalatags.generic.Modifier


case class NamedEvent(name: String) {

  def :=[Builder, T](v: T)(implicit ev: EventValue[Builder, T]) = EventHandlerPair(this, v, ev)

}

/**
 * A [[Modifier]] which wraps up an event listener.
 *
 * Rather than adding an attribute or a child, an [[EventHandlerPair]] will call `addEventListner()` to register the
 * listener.
 *
 * @tparam Builder  the builder type
 */
case class EventHandlerPair[Builder, T](ne: NamedEvent, v: T, ev: EventValue[Builder, T]) extends Modifier[Builder] {
  override def applyTo(t: Builder): Unit = {
    ev.apply(t, ne, v)
  }
}

trait EventValue[Builder, T] {
  def apply(t: Builder, ne: NamedEvent, v: T)
}

trait NamedEventUtil {
  implicit class NamedEvent_String(s: String) {

    def namedEvent = NamedEvent(s)

  }
}

trait DomL3  {
  self : NamedEventUtil =>

  val DOMNodeInsertedIntoDocument = "DOMNodeInsertedIntoDocument".namedEvent

  val DOMNodeInserted = "DOMNodeInserted".namedEvent

  val DOMNodeRemoved = "DOMNodeRemoved".namedEvent

  val DOMNodeRemovedFromDocument = "DOMNodeRemovedFromDocument".namedEvent

  val DOMSubtreeModified = "DOMSubtreeModified".namedEvent

  val DOMAttrModified = "DOMAttrModified".namedEvent
}

trait Events extends MouseEvents with KeyboardEvents with FrameEvents with FormEvents with SVGAnimationEvents {
  self : NamedEventUtil =>
}

// http://www.w3schools.com/jsref/dom_obj_event.asp

trait MouseEvents2 {
  self : NamedEventUtil =>

  val click = "click".namedEvent

  val contextmenu = "contextmenu".namedEvent

  val mousedown = "mousedown".namedEvent

  val mouseenter = "mouseenter".namedEvent

  val mouseLeave = "mouseleave".namedEvent

  val mouseover = "mouseover".namedEvent

  val mouseout = "mouseout".namedEvent

  val mouseup = "mouseup".namedEvent
}

trait MouseEvents extends MouseEvents2 {
  self : NamedEventUtil =>
}

trait KeyboardEvents2 {
  self : NamedEventUtil =>

  val keydown = "keydown".namedEvent

  val keypress = "keypress".namedEvent

  val keyup = "keyup".namedEvent
}

trait KeyboardEvents extends KeyboardEvents2 {
  self : NamedEventUtil =>
}

trait FrameEvents2 {
  self : NamedEventUtil =>

  val abort = "abort".namedEvent

  val beforeunload = "beforeunload".namedEvent

  val error = "error".namedEvent

  val load = "load".namedEvent

  val resize = "resize".namedEvent

  val scroll = "scroll".namedEvent

  val unload = "unload".namedEvent
}

trait FrameEvents3 {
  self : NamedEventUtil =>

  val hashchange = "hashchange".namedEvent

  val pageshow = "pageshow".namedEvent

  val pagehide = "pagehider".namedEvent
}

trait FrameEvents extends FrameEvents2 with FrameEvents3 {
  self : NamedEventUtil =>

}

trait FormEvents2 {
  self : NamedEventUtil =>

  val blur = "blur".namedEvent

  val change = "change".namedEvent

  val focus = "focus".namedEvent

  val focusin = "focusin".namedEvent

  val focusout = "focusout".namedEvent

  val reset = "reset".namedEvent

  val select = "select".namedEvent

  val submit = "submit".namedEvent
}

trait FormEvents3 {
  self : NamedEventUtil =>

  val input = "input".namedEvent

  val invalid = "invalid".namedEvent

  val search = "search".namedEvent
}

trait FormEvents extends FormEvents2 with FormEvents3 {
  self : NamedEventUtil =>
}

trait SVGAnimationEvents {
  self : NamedEventUtil =>

  val beginEvent = "beginEvent".namedEvent

  val endEvent = "endEvent".namedEvent

  val repeatEvent = "repeatEvent".namedEvent
}