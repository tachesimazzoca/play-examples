package models

import models.test._
import org.scalatest.FunSuite

class IdSequenceDaoSuite extends FunSuite {

  test("nextId(SequenceType.Account)") {
    withTestDatabase() { db =>
      db.withTransaction { implicit conn =>
        val idSequenceDao = new IdSequenceDao()
        val id1 = idSequenceDao.nextId(IdSequence.SequenceType.Account)
        val id2 = idSequenceDao.nextId(IdSequence.SequenceType.Account)
        assert(id1 === id2 - 1)
      }
    }
  }
}
