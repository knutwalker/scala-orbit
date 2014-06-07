package physics

case class Obj(pos: Pos = Pos(), mass: Double = 0, velocity: Vec = Vec(), force: Vec = Vec(), name: String = "TILT")
object Obj {

  def gravity(m1: Double, m2: Double, r: Double): Double =
    (m1 * m2) / (r * r)

  def forceBetween(o1: Obj, o2: Obj): Vec = {
    val p1 = o1.pos
    val p2 = o2.pos
    val d = Pos.distance(p1, p2)
    val uv = Vec.unit(Vec(Pos.subtract(p2, p1)))
    val g = gravity(o1.mass, o2.mass, d)
    Vec.scale(uv, g)
  }

  def accumulateForces(o: Obj, world: World): Obj = {
    val newForce = world.foldLeft(Vec()) { (f, obj) =>
      if (obj == o) f else Vec.add(f, forceBetween(o, obj))
    }
    o.copy(force = newForce)
  }

  def calculateForcesOnAll(world: World): World =
    world.map(accumulateForces(_, world))

  def accelerate(o: Obj): Obj = {
    val m = o.mass
    val v = o.velocity
    val f = o.force
    val av = Vec.add(v, Vec.scale(f, 1.0 / m))
    o.copy(velocity = av)
  }

  def accelerateAll(world: World): World =
    world.map(accelerate)

  def reposition(o: Obj): Obj =
    o.copy(pos = Pos.add(o.pos, o.velocity.p))

  def repositionAll(world: World): World =
    world.map(reposition)

  def isCollided(o1: Obj, o2: Obj): Boolean = {
    val distance = Pos.distance(o1.pos, o2.pos)
    val radius = math.sqrt(o1.mass + o2.mass)
    distance <= (radius max 3)
  }

  def centerOfMass(o1: Obj, o2: Obj): Pos = {
    val (Obj(p1, m1, _, _, _), Obj(p2, m2, _, _, _)) = (o1, o2)
    val s = m1 / (m1 + m2)
    val uv = Vec.unit(Vec(Pos.subtract(p2, p1)))
    val d = Vec.scale(uv, s)
    Pos.add(p1, d.p)
  }

  def merge(o1: Obj, o2: Obj): Obj = {
    val (Obj(_,  m1, v1, f1, n1), Obj(_,  m2, v2, f2, n2)) = (o1, o2)
    val p = centerOfMass(o1, o2)
    val m = m1 + m2
    val mv1 = Vec.scale(v1, m1)
    val mv2 = Vec.scale(v2, m2)
    val v = Vec.scale(Vec.add(mv1, mv2), 1 / m)
    val f = Vec.add(f1, f2)
    val n = if (m1 > m2) s"$n1.$n2" else s"$n2.$n1"
    Obj(p, m, v, f, n)
  }

  def remove(o: Obj, world: World) =
    world.filterNot(_ == o)

  def differenceList(w1: World, w2: World) =
    w2.foldLeft(w1)((w, o) => remove(o, w))

  def collideAll(world: World): (List[Pos], World) = {
    def recur(collidingWorld: World, collidedWorld: World, collisions: List[Pos]): (List[Pos], World) = collidingWorld match {
      case Vector() => (collisions, collidedWorld)
      case _ =>
        val impactor = collidingWorld.head
        val targets = collidingWorld.tail
        val colliders = targets.filter(isCollided(impactor, _))
        val merger = colliders.foldLeft(impactor)(merge)
        val survivors = differenceList(targets, colliders)
        val newCollisions = if (colliders.isEmpty) collisions else merger.pos :: collisions
        recur(survivors, collidedWorld :+ merger, newCollisions)
    }
    recur(world, Vector(), List())
  }

  def updateAll(world: World): (List[Pos], World) = {
    val (collisions, collidedWorld) = collideAll(world)
    (collisions, (calculateForcesOnAll _ andThen accelerateAll andThen repositionAll)(collidedWorld))
  }
}
