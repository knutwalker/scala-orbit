package physics

case class Obj(pos: Pos = Pos(), mass: Double = 0, velocity: Vector = Vector(), force: Vector = Vector(), name: String = "TILT")
object Obj {
  def gravity(m1: Double, m2: Double, r: Double): Double = (m1 * m2) / (r * r)

  def forceBetween(o1: Obj, o2: Obj): Vector = {
    val p1 = o1.pos
    val p2 = o2.pos
    val d = Pos.distance(p1, p2)
    val uv = Vector.unit(Vector.subtract(p2, p1))
    val g = gravity(o1.mass, o2.mass, d)
    Vector.scale(uv, g)
  }

  def accumulateForces(o: Obj, os: List[Obj]): Obj = {
    def recur(os: List[Obj], f: Vector): Vector = os match {
      case List() => f
      case `o` :: rest => recur(rest, f)
      case first :: rest => recur(rest, Vector.add(f, forceBetween(o, first)))
    }
    o.copy(force = recur(os, Vector()))
  }

  def calculateForcesOnAll(os: List[Obj]): List[Obj] = os.map(accumulateForces(_, os))

  def accelerate(o: Obj): Obj = {
    val Obj(_, m, v, f, _) = o
    val av = Vector.add(v, Vector.scale(f, 1.0 / m))
    o.copy(velocity = av)
  }

  def accelerateAll(os: List[Obj]): List[Obj] = os.map(accelerate)

  def reposition(o: Obj): Obj = {
    val Obj(p, _, v, _, _) = o
    o.copy(pos = Pos.add(p, v))
  }

  def repositionAll(os: List[Obj]): List[Obj] = os.map(reposition)

  def isCollided(o1: Obj, o2: Obj): Boolean = {
    val p1 = o1.pos
    val p2 = o2.pos
    Pos.distance(p1, p2) <= 3
  }

  private def centerOfMass(o1: Obj, o2: Obj): Pos = {
    val (Obj(p1, m1, _, _, _), Obj(p2, m2, _, _, _)) = (o1, o2)
    val s = m1 / (m1 + m2)
    val uv = Vector.unit(Vector.subtract(p2, p1))
    val d = Vector.scale(uv, s)
    Pos.add(p1, d)
  }

  def merge(o1: Obj, o2: Obj): Obj = {
    val (Obj(_,  m1, v1, f1, n1), Obj(_,  m2, v2, f2, n2)) = (o1, o2)
    val p = centerOfMass(o1, o2)
    val m = m1 + m2
    val mv1 = Vector.scale(v1, m1)
    val mv2 = Vector.scale(v2, m2)
    val v = Vector.scale(Vector.add(mv1, mv2), 1 / m)
    val f = Vector.add(f1, f2)
    val n = if (m1 > m2) s"$n1.$n2" else s"$n2.$n1"
    Obj(p, m, v, f, n)
  }

  def collide(o1: Obj, o2: Obj, os: List[Obj]): List[Obj] = {
    merge(o1, o2) :: os.filterNot(Set(o1, o2))
  }

  def collideAll(os: List[Obj]): List[Obj] = {
    def recur(pairs: Iterator[List[Obj]], cos: List[Obj]): List[Obj] =
      if (!pairs.hasNext) cos
      else {
        val List(o1, o2) = pairs.next()
        if (isCollided(o1, o2)) recur(pairs, collide(o1, o2, cos))
        else recur(pairs, cos)
      }
    recur(os.combinations(2), os)
  }

  def updateAll(os: List[Obj]): List[Obj] = {
    (collideAll _ andThen calculateForcesOnAll andThen accelerateAll andThen repositionAll)(os)
  }
}
