package physics

case class Vec(p: Pos) extends AnyVal {
  def x = p.x
  def y = p.y
}
object Vec {
  def apply(x: Double = 0, y: Double = 0): Vec =
    apply(Pos(x, y))

  def add(v1: Vec, v2: Vec): Vec =
    apply(Pos.add(v1.p, v2.p))

  def subtract(v1: Vec, v2: Vec): Vec =
    apply(Pos.subtract(v1.p, v2.p))

  def scale(v: Vec, s: Double): Vec =
    apply(v.x * s, v.y * s)

  def magnitude(v: Vec): Double =
    math.sqrt(v.x * v.x + v.y * v.y)

  def unit(v: Vec): Vec =
    scale(v, 1 / magnitude(v))

  def rotate90(v: Vec): Vec =
    apply(-v.y, v.x)
}
