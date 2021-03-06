package coop.rchain.rspace

import coop.rchain.rspace.examples.StringExamples.{StringsCaptor, Wildcard}
import coop.rchain.rspace.internal._

trait JoinOperationsTests extends StorageActionsBase {

  "joins" should "remove joins if no PsK" in withTestStore { store =>
    store.withTxn(store.createTxnWrite()) { txn =>
      store.putA(txn, List("ch1"), Datum("datum1", persist = false))
      store.putA(txn, List("ch2"), Datum("datum2", persist = false))
      store.addJoin(txn, "ch1", List("ch1", "ch2"))
      store.addJoin(txn, "ch2", List("ch1", "ch2"))

      //ensure that doubled addJoin creates only one entry
      store.addJoin(txn, "ch1", List("ch1", "ch2"))
      store.addJoin(txn, "ch2", List("ch1", "ch2"))

      store.putA(txn, List("ch1", "ch2"), Datum("datum_ch1_ch2", persist = false))
    }

    store.withTxn(store.createTxnRead()) { txn =>
      store.getJoin(txn, "ch1") shouldBe List(List("ch1", "ch2"))
      store.getJoin(txn, "ch2") shouldBe List(List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnWrite()) { txn =>
      store.removeJoin(txn, "ch1", List("ch1", "ch2"))
      store.removeJoin(txn, "ch2", List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnRead()) { txn =>
      store.getJoin(txn, "ch1") shouldBe List.empty[List[String]]
      store.getJoin(txn, "ch2") shouldBe List.empty[List[String]]
    }

    store.isEmpty shouldBe false

    //now ensure that garbage-collection works and all joins
    //are removed when we remove As
    store.withTxn(store.createTxnWrite()) { txn =>
      store.removeA(txn, "ch1", 0)
      store.removeA(txn, "ch2", 0)
      store.removeA(txn, List("ch1", "ch2"), 0)
    }

    store.isEmpty shouldBe true
  }

  "removeAllJoins" should "should not clear joins if PsK exists" in withTestStore { store =>
    store.withTxn(store.createTxnWrite()) { txn =>
      store.putK(txn,
                 List("ch1"),
                 WaitingContinuation(List(Wildcard), new StringsCaptor, persist = false))
      store.putK(txn,
                 List("ch2"),
                 WaitingContinuation(List(Wildcard), new StringsCaptor, persist = false))
      store.putK(txn,
                 List("ch1", "ch2"),
                 WaitingContinuation(List(Wildcard), new StringsCaptor, persist = false))
      store.addJoin(txn, "ch1", List("ch1", "ch2"))
      store.addJoin(txn, "ch2", List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnRead()) { txn =>
      store.getJoin(txn, "ch1") shouldBe List(List("ch1", "ch2"))
      store.getJoin(txn, "ch2") shouldBe List(List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnWrite()) { txn =>
      store.removeJoin(txn, "ch1", List("ch1", "ch2"))
      store.removeJoin(txn, "ch2", List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnRead()) { txn =>
      store.getJoin(txn, "ch1") shouldBe List(List("ch1", "ch2"))
      store.getJoin(txn, "ch2") shouldBe List(List("ch1", "ch2"))
    }

    store.withTxn(store.createTxnWrite()) { txn =>
      store.removeAllJoins(txn, "ch1")
      store.removeAllJoins(txn, "ch2")
    }

    store.withTxn(store.createTxnRead()) { txn =>
      store.getJoin(txn, "ch1") shouldBe List.empty[List[String]]
      store.getJoin(txn, "ch2") shouldBe List.empty[List[String]]
    }

    //now ensure that garbage-collection works and all joins
    //are removed when we remove PsK
    store.withTxn(store.createTxnWrite()) { txn =>
      store.removePsK(txn, List("ch1", "ch2"), 0)
      store.removePsK(txn, List("ch1"), 0)
      store.removePsK(txn, List("ch2"), 0)
    }

    store.isEmpty shouldBe true
  }
}
