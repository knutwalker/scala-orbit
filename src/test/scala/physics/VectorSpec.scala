package physics


class VectorSpec extends UnitSpec with TestUtilities {

  test("vector creation") {
    assert(Vector().x == 0)
    assert(Vector().y == 0)
    assert(Vector(1, 1) == Vector(1, 1))
    assert(Vector(1, 0).x == 1)
    assert(Vector(1, 0).y == 0)
  }

  test("vector addition") {
    assert(Vector(2, 2) == Vector.add(Vector(1, 1), Vector(1, 1)))
  }

  test("vector subtraction") {
    assert(Vector(1, 2) == Vector.subtract(Vector(3, 4), Vector(2, 2)))
  }

  test("vector scaling") {
    assert(Vector(3, 3) == Vector.scale(Vector(1, 1), 3))
  }

  test("magnitude") {
    assert(Vector.magnitude(Vector(0, 3)) == 3)
    assert(Vector.magnitude(Vector(3, 0)) == 3)
    assert(Vector.magnitude(Vector(3, 4)) == 5)
  }

  test("unit vector") {
    assert(Vector(0, 1) == Vector.unit(Vector(0, 99)))
    assert(Vector(1, 0) == Vector.unit(Vector(99, 0)))

    val r2 = math.sqrt(1 / 2.0)
    assert(vectorCloseTo(Vector(r2, r2), Vector.unit(Vector(1, 1))))
  }

  test("rotate90") {
    assert(Vector(-1, 2) == Vector.rotate90(Vector(2, 1)))
  }
}
