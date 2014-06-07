package physics

class ObjSpec extends UnitSpec with TestUtilities {
  val v0 = Vec()
  val v11 = Vec(1, 1)
  val o1 = Obj(Pos(1, 1), 2, v0, v0, "o1")
  val o2 = Obj(Pos(1, 2), 3, v0, v0, "o2")
  val o3 = Obj(Pos(4, 5), 4, v0, v0, "o3")
  val world = Vector(o1, o2, o3)

  def worldMomentum(world: Objs) = world.map(o => Vec.scale(o.velocity, o.mass)).reduce(Vec.add)

  def worldEnergy(world: Objs) = world.map(o => square(Vec.magnitude(o.velocity)) * o.mass * 0.5)

  test("default creation") {
    val o = Obj()
    assert(Pos.isOrigin(o.pos))
    assert(o.mass == 0)
    assert(o.velocity == v0)
    assert(o.force == v0)
    assert(o.name == "TILT")
  }

  test("defined creation") {
    val pos = Pos(1, 1)
    val mass = 2
    val velocity = Vec(2, 2)
    val force = Vec(3, 3)
    val name = "name"
    val o = Obj(pos, mass, velocity, force, name)
    assert(o.pos == pos)
    assert(o.mass == mass)
    assert(o.velocity == velocity)
    assert(o.force == force)
    assert(o.name == name)
  }

  test("gravity") {
    assert(Obj.gravity(2, 3, 4) == 6.0 / 16)
  }

  test("force between") {
    val c3r2 = 3 / math.sqrt(2)
    val o1 = Obj(Pos(1, 1), 2, v0, v0, "o1")
    val o2 = Obj(Pos(2, 2), 3, v0, v0, "o2")
    assert(vectorCloseTo(Vec(c3r2, c3r2), Obj.forceBetween(o1, o2)))
  }

  test("accumulate forces") {
    val accumulatedO1 = Obj.accumulateForces(o1, world)
    val expected = Vec.add(Obj.forceBetween(o1, o2), Obj.forceBetween(o1, o3))
    assert(vectorCloseTo(expected, accumulatedO1.force))
  }

  test("calculate forces on all") {
    val fs = Obj.calculateForcesOnAll(world)
    assert(fs.size == 3)
    assert(fs(0) == Obj.accumulateForces(o1, world))
    assert(fs(1) == Obj.accumulateForces(o2, world))
    assert(fs(2) == Obj.accumulateForces(o3, world))
  }

  test("accelerate") {
    val o = Obj(Pos(), 2, v11, v11, "o1")
    val ao = Obj.accelerate(o)
    assert(vectorCloseTo(Vec(1.5, 1.5), ao.velocity))
  }

  test("accelerate all") {
    val as = Obj.accelerateAll(Obj.calculateForcesOnAll(world))
    val `->` = (Obj.accumulateForces(_: Obj, world)) andThen Obj.accelerate
    assert(as.size == 3)
    assert(as(0) == `->`(o1))
    assert(as(1) == `->`(o2))
    assert(as(2) == `->`(o3))
  }

  test("reposition") {
    val o = Obj(Pos(1, 1), 2, v11, v0, "o1")
    val ro = Obj.reposition(o)
    assert(ro.pos == Pos(2, 2))
  }

  test("reposition all") {
    val rs = (Obj.calculateForcesOnAll _ andThen Obj.accelerateAll andThen Obj.repositionAll)(world)
    val `->` = (Obj.accumulateForces(_: Obj, world)) andThen Obj.accelerate andThen Obj.reposition
    assert(rs.size == 3)
    assert(rs(0) == `->`(o1))
    assert(rs(1) == `->`(o2))
    assert(rs(2) == `->`(o3))
  }

  test("collided") {
    assert(Obj.isCollided(o1, o2))
    assert(!Obj.isCollided(o1, o3))
  }

  test("merge") {
    val o1 = Obj(Pos(1, 1), 2, Vec(1, 0), Vec(1, 1), "o1")
    val o2 = Obj(Pos(1, 2), 3, Vec(-1, 0), Vec(1, 1) ,"o2")
    val om = Obj.merge(o1, o2)
    assert(om.name == "o2.o1")
    assert(om.pos == Pos(1, 1.4))
    assert(om.mass == 5)
    assert(om.velocity == Vec(-1.0 / 5, 0))
    assert(om.force == Vec(2, 2))
  }

  test("collide") {
    val cos = Obj.collide(o1, o2, world)
    assert(cos.size == 2)
    assert(cos.exists(_ == Obj.merge(o1, o2)))
    assert(cos.exists(_ == o3))
  }
  test("collide all") {
    val cos = Obj.collideAll(world)
    assert(cos.size == 2)
    assert(cos.exists(_ == Obj.merge(o1, o2)))
    assert(cos.exists(_ == o3))
  }

}
