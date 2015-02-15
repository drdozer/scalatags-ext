package scalatags.ext

import scala.language.implicitConversions

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
        case (Match(ia, ib, a, b),    Match(ic, id, c, d))    => intIntTT.compare((ia, ib, a, b), (ic, id, c, d))
        case (Match(_, _, _, _), _) => -1
      }
    }
  }

  type Alignment[T] = List[Move[T]]
}

/**
 * An alignment storing function.
 *
 * @tparam T  the cored type
 */
trait ScoreFunction[T] {
  /** Cost for an indel of the item `t`. */
  def indelCost(t: T): Int

  /** The match cost of `t1` and `t2`. */
  def matchCost(t1: T, t2: T): Int
}

class GreedyAlignment[T : Ordering](val scoreFunction: ScoreFunction[T]) extends PairwiseAlignment[T] {

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
          val sc = scoreFunction.indelCost(iVal) + bestScore
          rest enqueue (sc -> (pos -> (move::bestAlignment)))
        } else {
          rest
        }

        val afterDelete = if(j < js.length) {
          val jVal = js(j)
          val move = PairwiseAlignment.Insert(j, jVal)
          val sc = scoreFunction.indelCost(jVal) + bestScore
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
          val sc = scoreFunction.matchCost(iVal, jVal) + bestScore
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
