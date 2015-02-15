package scalatags.ext

import scala.collection.immutable.SortedSet

/**
 * A priority queue, that maintains a number of unique items in priority order and supports efficient removal of the
 * highest priority item and of enqueueing items. In the event of a priority draw, the order in which items are dequeued
 * depends upon the implementation. Each enqueued item appears at most once in the queue.
 *
 * @author Matthew Pocock
 */

trait PriorityQueue[P, A] {

  def dequeue: ((P, A), PriorityQueue[P, A])

  def enqueue(ap: (P, A)): PriorityQueue[P, A]

  def enqueue(p: P, a: A): PriorityQueue[P, A] = enqueue(p -> a)

  def peek: (P, A) = dequeue._1

  def isEmpty: Boolean

}

object PriorityQueue {

  def apply[P, A](pas: (P, A)*)(implicit pOrd: Ordering[P], aOrd: Ordering[A]): PriorityQueue[P, A] =
    pas.foldLeft(new PQ[P, A](SortedSet(), Map()) : PriorityQueue[P, A])(_ enqueue _)

  private case class PQ[P, A](pa: SortedSet[(P, A)], ap: Map[A, P])(implicit pOrd: Ordering[P], aOrd: Ordering[A])
    extends PriorityQueue[P, A]
  {
    def dequeue = {
      val last = pa.lastKey
      (last, copy(pa = pa filterNot (x => aOrd.compare(x._2, last._2) == 0), ap = ap - last._2))
    }

    def enqueue(pair: (P, A)) = {
      val a = pair._2
      ap get a match {
        case Some(p) if pOrd.gt(pair._1, p) =>
          copy(pa = pa - (p -> a) + pair, ap = ap + pair.swap)
        case Some(_) =>
          this
        case None =>
          copy(pa = pa + pair, ap = ap + pair.swap)
      }
    }

    def isEmpty = pa.isEmpty

    override def toString: String = {
      s"PriorityQueue(${
        if(pa.size < 5)
          pa.mkString(", ")
        else
          "..., " + pa.drop(pa.size - 5).mkString(", ")
      }})"
    }
  }
}