package physics

class PosSpec extends UnitSpec with TestUtilities {

  test("pos creation") {
    assert(Pos.isOrigin(Pos()))
    assert(Pos(1, 1) == Pos(1, 1))
    assert(Pos(1, 0).x == 1)
    assert(Pos(1, 0).y == 0)
  }

  test("pos addition") {
    assert(Pos(2, 2) == Pos.add(Pos(1, 1), Pos(1, 1)))
  }

  test("pos subtraction") {
    assert(Pos(1, 2) == Pos.subtract(Pos(3, 4), Pos(2, 2)))
  }

  test("distance") {
    assert(Pos.distance(Pos(0, 0), Pos(0, 1)) == 1)
    assert(Pos.distance(Pos(1, 0), Pos(2, 0)) == 1)
    assert(Pos.distance(Pos(0, 0), Pos(3, 4)) == 5)
  }

  test("average") {
    assert(Pos.average(Pos(0, 0), Pos(2, 2)) == Pos(1, 1))
    assert(Pos.average(Pos(1, 2), Pos(3, 2)) == Pos(2, 2))
  }
}
