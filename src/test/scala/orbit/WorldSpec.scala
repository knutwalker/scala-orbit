package orbit

import physics.{UnitSpec, Pos}

class WorldSpec extends UnitSpec {

  test("collision aging") {
    val p0 = Pos()
    val p1 = Pos(1, 1)
    val a5 = Age(5)
    val a4 = Age(4)
    val a2 = Age(2)
    val a1 = Age(1)
    assert(world.ageCollisions(List(Collision(a5, p0))) == List(Collision(a4, p0)))
    assert(world.ageCollisions(List(Collision(a1, p0), Collision(a2, p1))) == List(Collision(a1, p1)))
  }

}
