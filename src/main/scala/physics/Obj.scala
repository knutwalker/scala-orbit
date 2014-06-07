package physics

case class Obj(pos: Pos = Pos(), mass: Double = 0, velocity: Vec = Vec(), force: Vec = Vec(), name: String = "TILT")
object Obj {

  type Objs = Vector[Obj]

  def gravity(m1: Double, m2: Double, r: Double): Double = (m1 * m2) / (r * r)

  def forceBetween(o1: Obj, o2: Obj): Vec = {
    val p1 = o1.pos
    val p2 = o2.pos
    val d = Pos.distance(p1, p2)
    val uv = Vec.unit(Vec.subtract(p2, p1))
    val g = gravity(o1.mass, o2.mass, d)
    Vec.scale(uv, g)
  }

  // List would be better
  def accumulateForces(o: Obj, os: Objs): Obj = {
    def recur(os: Objs, f: Vec): Vec = os match {
      case Vector() => f
      case nonempty =>
        val first = nonempty.head
        val rest = nonempty.tail
        if (first == o) recur(rest, f)
        else recur(rest, Vec.add(f, forceBetween(o, first)))
    }
    o.copy(force = recur(os, Vec()))
  }

  def calculateForcesOnAll(os: Objs): Objs = os.map(accumulateForces(_, os))

  def accelerate(o: Obj): Obj = {
    val Obj(_, m, v, f, _) = o
    val av = Vec.add(v, Vec.scale(f, 1.0 / m))
    o.copy(velocity = av)
  }

  def accelerateAll(os: Objs): Objs = os.map(accelerate)

  def reposition(o: Obj): Obj = {
    val Obj(p, _, v, _, _) = o
    o.copy(pos = Pos.add(p, v))
  }

  def repositionAll(os: Objs): Objs = os.map(reposition)

  def isCollided(o1: Obj, o2: Obj): Boolean = {
    val p1 = o1.pos
    val p2 = o2.pos
    Pos.distance(p1, p2) <= 3
  }

  private def centerOfMass(o1: Obj, o2: Obj): Pos = {
    val (Obj(p1, m1, _, _, _), Obj(p2, m2, _, _, _)) = (o1, o2)
    val s = m1 / (m1 + m2)
    val uv = Vec.unit(Vec.subtract(p2, p1))
    val d = Vec.scale(uv, s)
    Pos.add(p1, d)
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

  def collide(o1: Obj, o2: Obj, os: Objs): Objs = {
    os.filterNot(Set(o1, o2)) :+ merge(o1, o2)
  }

  def collideAll(os: Objs): Objs = {
    def recur(pairs: Iterator[Objs], cos: Objs): Objs =
      if (!pairs.hasNext) cos
      else {
        val Vector(o1, o2) = pairs.next()
        if (isCollided(o1, o2)) recur(pairs, collide(o1, o2, cos))
        else recur(pairs, cos)
      }
    recur(os.combinations(2), os)
  }

  def updateAll(os: Objs): Objs = {
    (collideAll _ andThen calculateForcesOnAll andThen accelerateAll andThen repositionAll)(os)
  }
}
