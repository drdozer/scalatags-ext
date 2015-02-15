package scalatags.ext

import rx.core.Var
import utest._


/**
 *
 *
 * @author Matthew Pocock
 */
object SeqDiffTestSuite extends TestSuite {

  def tests = TestSuite {
    "seq diff" - {
      implicit val csf = new ScoreFunction[Char] {
        /** Cost for an indel of the item `t`. */
        override def indelCost(t: Char) = -2

        /** The match cost of `t1` and `t2`. */
        override def matchCost(t1: Char, t2: Char) =
          if(t1 == t2) 0
          else if(t1.toLower == t2.toLower) -1
          else indelCost(t1)*2-1
      }

      val strings = Var(IndexedSeq[Char]())
      val rxDiff = SeqDiff(strings)

      "strings starts with" - strings()
      "diff starts with" - rxDiff.updates()

      val u1 = rxDiff.updates()
      "diff starts empty" - assert(u1 == List())

      strings() = "abcde"

      val u2 = rxDiff.updates()
      val s2 = strings()
      val e2 = u2 collect { case e@SeqDiff.Entered(_, _) => e }
      "diff contains entered for each of `abcde`" - {
        assert(u2.length == s2.length,
               u2.length == e2.length,
               u2 == e2,
               e2.map(_.item).to[Set] == Set('a', 'b', 'c', 'd', 'e'),
               e2.map(_.at.index).to[Set] == Set(0, 1, 2, 3, 4),
               e2 == u2 )
      }

      strings() = "abDe"

      val u3 = rxDiff.updates()
      val s3 = strings()
      val x3 = u3 collect { case e@SeqDiff.Exited(_, _) => e }
      val m3 = u3 collect { case m@SeqDiff.Modified(_, _) => m }
      "new diff" - u3
      "diff now contains an exit for 'c'" - {
        assert(x3.size == 1,
               x3.head.item == 'c')
      }
      "diff now contains modifications" - {
        "two modifications" - assert(m3.size == 2)
        "modification for 'd'->'D'" - assert(m3.count {
          case m@SeqDiff.Modified(('d', 'D'), _) => true
          case _ => false
        } == 1)
        "modification for 'e'->'e' at new indexes" - assert(m3.count {
          case m@SeqDiff.Modified(('e', 'e'), (SeqDiff.OldIndex(i), SeqDiff.NewIndex(j))) if i != j => true
          case _ => false
        } == 1)
      }

      strings() = "abde"

      val u4 = rxDiff.updates()
      val s4 = strings()
      val m4 = u4 collect { case m@SeqDiff.Modified(_, _) => m }
      "new diff" - u4
      "diff now contains a modification for 'D' -> 'd'" - {
        assert(m4.size == 1,
               m4.head.item == 'D' -> 'd')
      }
    }
  }

}
