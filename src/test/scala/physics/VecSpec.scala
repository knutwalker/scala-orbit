package physics


class VecSpec extends UnitSpec with TestUtilities {

  test("vector creation") {
    assert(Vec().x == 0)
    assert(Vec().y == 0)
    assert(Vec(1, 1) == Vec(1, 1))
    assert(Vec(1, 0).x == 1)
    assert(Vec(1, 0).y == 0)
  }

  test("vector addition") {
    assert(Vec(2, 2) == Vec.add(Vec(1, 1), Vec(1, 1)))
  }

  test("vector subtraction") {
    assert(Vec(1, 2) == Vec.subtract(Vec(3, 4), Vec(2, 2)))
  }

  test("vector scaling") {
    assert(Vec(3, 3) == Vec.scale(Vec(1, 1), 3))
  }

  test("magnitude") {
    assert(Vec.magnitude(Vec(0, 3)) == 3)
    assert(Vec.magnitude(Vec(3, 0)) == 3)
    assert(Vec.magnitude(Vec(3, 4)) == 5)
  }

  test("unit vector") {
    assert(Vec(0, 1) == Vec.unit(Vec(0, 99)))
    assert(Vec(1, 0) == Vec.unit(Vec(99, 0)))

    val r2 = math.sqrt(1 / 2.0)
    assert(vectorCloseTo(Vec(r2, r2), Vec.unit(Vec(1, 1))))
  }

  test("rotate90") {
    assert(Vec(-1, 2) == Vec.rotate90(Vec(2, 1)))
  }
}
