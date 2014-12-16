package scalatags.ext

import utest._

/**
 *
 *
 * @author Matthew Pocock
 */
object GreedyAlignmentTestSuite extends TestSuite {

  def tests = TestSuite {
    "GreedyAlignment" - {
      "aligns case-sensitive strings" - {

        val csf = new ScoreFunction[Char] {
          /** Cost for an indel of the item `t`. */
          override def indelCost(t: Char) = -2

          /** The match cost of `t1` and `t2`. */
          override def matchCost(t1: Char, t2: Char) =
            if(t1 == t2) 0
            else indelCost(t1)*2-1
        }

        val ga = new GreedyAlignment(csf)

        "identical strings match" - {
          val al = ga.align("abcde", "abcde")

          "has alignment with the same length as the input" - {
            assert(al.length == "abcde".length)
          }

          "match at all positions" - {
            val matches = al.count { case PairwiseAlignment.Match(_, _, _, _) => true; case _ => false }
            assert(matches == al.length)
          }

          al
        }

        "missing leading char in first string" - {
          val al = ga.align("bcde", "abcde")
          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "is inserted at position 0" - assert(inserts.head.j == 0)
          "is an insert of 'a'" - assert(inserts.head.jVal == 'a')

          al
        }

        "missing trailing char in first string" - {
          val al = ga.align("abcd", "abcde")
          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "is inserted at position 4" - assert(inserts.head.j == 4)
          "is an insert of 'e'" - assert(inserts.head.jVal == 'e')

          al
        }

        "missing middle char in first string" - {
          val al = ga.align("abde", "abcde")
          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "is inserted at position 2" - assert(inserts.head.j == 2)
          "is an insert of 'c'" - assert(inserts.head.jVal == 'c')

          al
        }

        "missing leading char in second string" - {
          val al = ga.align("abcde", "bcde")
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one delete" - assert(deletes.length == 1)
          "is deleted at position 0" - assert(deletes.head.i == 0)
          "is a deletion of 'a'" - assert(deletes.head.iVal == 'a')

          al
        }

        "missing trailing char in second string" - {
          val al = ga.align("abcde", "abcd")
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one delete" - assert(deletes.length == 1)
          "is deleted at position 4" - assert(deletes.head.i == 4)
          "is a delete of 'e'" - assert(deletes.head.iVal == 'e')

          al
        }

        "missing middle char in second string" - {
          val al = ga.align("abcde", "abde")
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one delete" - assert(deletes.length == 1)
          "is deleted at position 2" - assert(deletes.head.i == 2)
          "is a delete of 'c'" - assert(deletes.head.iVal == 'c')

          al
        }

        "leading mismatch" - {
          val al = ga.align("Xbcde", "Ybcde")

          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "has exactly one delete" - assert(deletes.length == 1)
          "is insert at position 0" - assert(inserts.head.j == 0)
          "is delete at position 0" - assert(deletes.head.i == 0)
          "is insert of 'Y'" - assert(inserts.head.jVal == 'Y')
          "is insert of 'X'" - assert(deletes.head.iVal == 'X')

          al
        }

        "trailing mismatch" - {
          val al = ga.align("abcdX", "abcdY")

          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "has exactly one delete" - assert(deletes.length == 1)
          "is insert at position 4" - assert(inserts.head.j == 4)
          "is delete at position 4" - assert(deletes.head.i == 4)
          "is insert of 'Y'" - assert(inserts.head.jVal == 'Y')
          "is insert of 'X'" - assert(deletes.head.iVal == 'X')

          al
        }

        "middle mismatch" - {
          val al = ga.align("abXde", "abYde")

          val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }
          val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

          "has exactly one insert" - assert(inserts.length == 1)
          "has exactly one delete" - assert(deletes.length == 1)
          "is insert at position 2" - assert(inserts.head.j == 2)
          "is delete at position 2" - assert(deletes.head.i == 2)
          "is insert of 'Y'" - assert(inserts.head.jVal == 'Y')
          "is delete of 'X'" - assert(deletes.head.iVal == 'X')

          al
        }
      }
    }

    "aligns case-insensitive strings" - {

      val csf = new ScoreFunction[Char] {
        /** Cost for an indel of the item `t`. */
        override def indelCost(t: Char) = -2

        /** The match cost of `t1` and `t2`. */
        override def matchCost(t1: Char, t2: Char) =
          if(t1 == t2) 0
          else if(t1.toLower == t2.toLower) -1
          else indelCost(t1)*2-1
      }

      val ga = new GreedyAlignment(csf)

      "identical strings match" - {
        val al = ga.align("abcde", "abcde")

        "has alignment with the same length as the input" - {
          assert(al.length == "abcde".length)
        }

        "match at all positions" - {
          val matches = al.count { case PairwiseAlignment.Match(_, _, _, _) => true; case _ => false }
          assert(matches == al.length)
        }

        al
      }

      "identical different-case strings match" - {
        val al = ga.align("abcde", "ABCDE")

        "has alignment with the same length as the input" - {
          assert(al.length == "abcde".length)
        }

        "match at all positions" - {
          val matches = al.count { case PairwiseAlignment.Match(_, _, _, _) => true; case _ => false }
          assert(matches == al.length)
        }

        al
      }

      "aligning x to xX" - {
        val al = ga.align("abxde", "abxXde")
        val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }

        "has exactly one insert" - assert(inserts.length == 1)
        "is insert at position 3" - assert(inserts.head.j == 3)
        "is insert of 'X'" - assert(inserts.head.jVal == 'X')

        al
      }

      "aligning x to Xx" - {
        val al = ga.align("abxde", "abXxde")
        val inserts = al collect { case i@PairwiseAlignment.Insert(_, _) => i }

        "has exactly one insert" - assert(inserts.length == 1)
        "is insert at position 2" - assert(inserts.head.j == 2)
        "is insert of 'X'" - assert(inserts.head.jVal == 'X')

        al
      }

      "aligning xX to x" - {
        val al = ga.align("abxXde", "abxde")
        val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

        "has exactly one delete" - assert(deletes.length == 1)
        "is delete at position 3" - assert(deletes.head.i == 3)
        "is delete of 'X'" - assert(deletes.head.iVal == 'X')

        al
      }

      "aligning Xx to x" - {
        val al = ga.align("abXxde", "abxde")
        val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

        "has exactly one delete" - assert(deletes.length == 1)
        "is delete at position 2" - assert(deletes.head.i == 2)
        "is delete of 'X'" - assert(deletes.head.iVal == 'X')

        al
      }

      "aligning Xx to x with other mixed case matches" - {
        val al = ga.align("abXxde", "ABxDE")
        val deletes = al collect { case i@PairwiseAlignment.Delete(_, _) => i }

        "has exactly one delete" - assert(deletes.length == 1)
        "is delete at position 2" - assert(deletes.head.i == 2)
        "is delete of 'X'" - assert(deletes.head.iVal == 'X')

        al
      }
    }
  }

}
