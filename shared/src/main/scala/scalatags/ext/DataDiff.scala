package scalatags.ext

class DataDiff[D, K](items: Rx[Seq[D]], byKey: D => K, areEqual: (D, D) => Boolean) {
  var dataMap = Rx {
    (items().zipWithIndex.map { case(d, i) => byKey(d) -> (d, i) }).toMap
  }

  var updates = dataMap.diff({ (_previous, _next) =>
    val previous = _previous.mapValues { case (d, v) => d -> DataDiff.OldIndex(v) }
    val next = _next.mapValues { case (d, v) => d -> DataDiff.NewIndex(v) }
    val enteredKeys = next.keySet &~ previous.keySet
    val exitedKeys = previous.keySet &~ next.keySet
    val commonKeys = previous.keySet & next.keySet
    val (unchangedKeys, updatedKeys) = commonKeys.partition { k =>
      val pk = previous(k)
      val nk = next(k)
      (pk._2.index == nk._2.index) && areEqual(pk._1, nk._1)
    }

    DataDiff.Updates(
      entered = for(k <- enteredKeys.to[Seq]) yield next(k),
      exited = for(k <- exitedKeys.to[Seq]) yield previous(k),
      modified = for(k <- updatedKeys.to[Seq]) yield  {
        val p = previous(k)
        val n = next(k)
        p._1 -> (p._2, n._2)
      },
      unchanged = for(k <- unchangedKeys.to[Seq]) yield  {
        val p = previous(k)
        val n = next(k)
        p._1 -> (p._2, n._2)
      })
  }, { (next) =>
    DataDiff.Updates(
      entered = (next.mapValues { case (d, v) => d -> DataDiff.NewIndex(v) }).values.to[Seq],
      exited = Seq(),
      modified = Seq(),
      unchanged = Seq())
  })
}

/**
 *
 *
 * @author Matthew Pocock
 */
case object DataDiff {

  def apply[D](items: Rx[Seq[D]]): DataDiff[D, D] = new DataDiff(items, identity, _ == _)
  def apply[D, K](items: Rx[Seq[D]], byKey: D => K): DataDiff[D, K] = new DataDiff(items, byKey, _ == _)

  case class OldIndex(index: Int) extends AnyVal
  case class NewIndex(index: Int) extends AnyVal

  case class Updates[D](exited:     Seq[(D, OldIndex)],
                        entered:    Seq[(D, NewIndex)],
                        modified:    Seq[(D, (OldIndex, NewIndex))],
                        unchanged:  Seq[(D, (OldIndex, NewIndex))])

  def unDiff[D](diffs: Rx[Updates[D]]): Rx[Seq[D]] = diffs.fold(List.empty[D]) { (seq, diff) =>
    val Updates(entered, exited, updated, unchanged) = diff

    val exitedIndexes = exited.map(_._2.index).to[Set]
    val enteredByIndex = entered.map { case (d, i) => i.index -> d }.toMap
    val updatedIndexes = updated.map { case (d, (_, i)) => i.index -> d}.toMap

    // delete exited indexes
    val withDeletes = seq.zipWithIndex.filterNot(exitedIndexes contains _._2).unzip._1
    // insert entered items at index
    def doInsert(oldItems: List[D], i: Int): List[D] =
      enteredByIndex get i match {
        case Some(d) => (d: D) +: doInsert(oldItems, i+1)
        case None => oldItems match {
          case Nil => Nil
          case h::t => h +: doInsert(t, i+1)
        }
      }
    val withInserts = doInsert(withDeletes, 0)

    // replace updated items
    val withUpdates = withInserts.zipWithIndex.map { case (d, i) =>
      updatedIndexes get i match {
        case Some(dd) if d != dd => dd
        case _ => d
      }
    }

    withUpdates
  }

}

class SeqDiff[D, K : Ordering](items: Rx[IndexedSeq[D]], byKey: D => K, areEqual: (D, D) => Boolean) {
  import scalatags.ext.SeqDiff._

  private def score(m: PairwiseAlignment.Move[D]): Int = m match {
    case PairwiseAlignment.Delete(_, _) => -1
    case PairwiseAlignment.Insert(_, _) => -1
    case PairwiseAlignment.Match(_, _, a, b) =>
      if(implicitly[Ordering[K]].equiv(byKey(a), byKey(b))) 0 else -100
  }

  private val ga = new GreedyAlignment(score)(Ordering.by(byKey))

  val aligned = items.diff(ga.align, ga.align(IndexedSeq.empty, _))

  val updates = Rx {

    aligned().foldLeft(List.empty[Update[D]]) { (acc, m) =>
      val update = m match {
        case Delete(i, iVal) =>
          Exited(iVal, OldIndex(i))
        case Insert(j, jVal) =>
          Entered(jVal, NewIndex(j))
        case Match(i, j, iVal, jVal) if i == j && areEqual(iVal, jVal) =>
          Unchanged((iVal, jVal), (OldIndex(i), NewIndex(j)))
        case Match(i, j, iVal, jVal) =>
          Modified((iVal, jVal), (OldIndex(i), NewIndex(j)))
      }
      update :: acc
    }
  }

  def map[T](entered: Entered[D] => T, modified: (Modified[D], T) => T) = updates.fold(List.empty[T]) {
    (oldTs, diffs) =>
      def unwind(tss: List[T], dss: List[Update[D]]): List[T] = {
        (tss, dss) match {
          case (th::tt, Unchanged(_, _)::dt) => th::unwind(tt, dt)
          case (th::tt, (m@Modified(_, _))::dt) => modified(m, th)::unwind(tt, dt)
          case (_::tt, Exited(_, _)::dt) => unwind(tt, dt)
          case (ts, (e@Entered(_, _))::dt) => entered(e)::unwind(ts, dt)
          case (Nil, Nil) => Nil
        }
      }

      unwind(oldTs, diffs)
  } map (_.to[IndexedSeq])
}

object SeqDiff {

  def apply[D : Ordering](items: Rx[IndexedSeq[D]]): SeqDiff[D, D] = {
    def deq(d1: D, d2: D): Boolean = implicitly[Ordering[D]].compare(d1, d2) == 0
    new SeqDiff[D, D](items, identity, deq)
  }

  case class OldIndex(index: Int) extends AnyVal
  case class NewIndex(index: Int) extends AnyVal

  sealed trait Update[D] {
    type Val
    type Indx
    def item: Val
    def at: Indx
  }

  case class Exited[D](item: D, at: OldIndex) extends Update[D] {
    type Val = D
    type Indx = OldIndex
  }

  case class Entered[D](item: D, at: NewIndex) extends Update[D] {
    type Val = D
    type Indx = NewIndex
  }

  case class Modified[D](item: (D, D), at: (OldIndex, NewIndex)) extends Update[D] {
    type Val = (D, D)
    type Indx = (OldIndex, NewIndex)
  }

  case class Unchanged[D](item: (D, D), at: (OldIndex, NewIndex)) extends Update[D] {
    type Val = (D, D)
    type Indx = (OldIndex, NewIndex)
  }

}
