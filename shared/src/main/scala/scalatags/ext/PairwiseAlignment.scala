package scalatags.ext

/**
 *
 *
 * @author Matthew Pocock
 */
trait PairwiseAlignment[T] {
  def align(is: IndexedSeq[T], js: IndexedSeq[T]): PairwiseAlignment.Alignment[T]
}

object PairwiseAlignment {
  sealed trait Move[T]

  case class Delete[T](i: Int, iVal: T) extends Move[T]
  case class Insert[T](j: Int, jVal: T) extends Move[T]
  case class Match[T](i: Int, j: Int, iVal: T, jVal: T) extends Move[T]

  object Move {
    implicit def moveOrdering[T](implicit ordT: Ordering[T]): Ordering[Move[T]] = new Ordering[Move[T]] {
      val reverseInt = implicitly[Ordering[Int]].reverse
      val intInt = Ordering.Tuple2(reverseInt, reverseInt)
      val intT = Ordering.Tuple2(reverseInt, ordT)
      val intIntTT = Ordering.Tuple4(reverseInt, reverseInt, ordT, ordT)
      override def compare(x: Move[T], y: Move[T]) = (x, y) match {
        case (Delete(ia, a), Delete(ib, b)) => intT.compare((ia, a), (ib, b))
        case (Delete(_, _), _) => -1
        case (Insert(ia, a), Insert(ib, b)) => intT.compare((ia, a), (ib, b))
        case (Insert(_, _), _) => -1
        case (Match(ia, ib, a, b), Match(ic, id, c, d)) => intIntTT.compare((ia, ib, a, b), (ic, id, c, d))
        case (Match(_, _, _, _), _) => -1
      }
    }
  }

  type Alignment[T] = List[Move[T]]
}

class GreedyAlignment[T : Ordering](val score: PairwiseAlignment.Move[T] => Int) extends PairwiseAlignment[T] {

  private implicit def listOrd[TT](implicit tO: Ordering[TT]): Ordering[List[TT]] = new Ordering[List[TT]] {
    override def compare(x: List[TT], y: List[TT]) = (x, y) match {
      case (h1::t1, h2::t2) if tO.compare(h1, h2) == 0 =>
        compare(t1, t2)
      case (h1::t1, h2::t2) =>
        tO.compare(h1, h2)
      case (h1::t1, _) => -1
      case (Nil, Nil) => 0
      case (Nil, h2::t2) => +1
    }
  }

  override def align(is: IndexedSeq[T], js: IndexedSeq[T]) = {

    def doAlign(alignments: PriorityQueue[Int, ((Int, Int), PairwiseAlignment.Alignment[T])]): PairwiseAlignment.Alignment[T] = {
      val ((bestScore, ((i, j), bestAlignment)), rest) = alignments.dequeue

      if(i == is.length && j == js.length) {
        bestAlignment
      } else {

        val afterInsert = if(i < is.length) {
          val iVal = is(i)
          val move = PairwiseAlignment.Delete(i, iVal)
          val pos = (i + 1, j)
          val sc = score(move) + bestScore
          rest enqueue (sc -> (pos -> (move::bestAlignment)))
        } else {
          rest
        }

        val afterDelete = if(j < js.length) {
          val jVal = js(j)
          val move = PairwiseAlignment.Insert(j, jVal)
          val sc = score(move) + bestScore
          val pos = (i, j + 1)
          afterInsert enqueue (sc -> (pos -> (move::bestAlignment)))
        } else {
          afterInsert
        }

        val afterMatch = if(i < is.length && j < js.length) {
          val iVal = is(i)
          val jVal = js(j)
          val move = PairwiseAlignment.Match(i, j, iVal, jVal)
          val pos = (i + 1, j + 1)
          val sc = score(move) + bestScore
          afterDelete enqueue (sc -> (pos -> (move::bestAlignment)))
        } else {
          afterDelete
        }

        doAlign(afterMatch)
      }
    }

    doAlign(PriorityQueue(0 -> ((0, 0) -> (Nil: PairwiseAlignment.Alignment[T])))(implicitly, Ordering.by(_._1)))
  }
}

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