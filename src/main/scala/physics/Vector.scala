package physics

case class Vector(x: Double = 0, y: Double = 0)
object Vector {

  def add(v1: Vector, v2: Vector): Vector = Vector(v1.x + v2.x, v1.y + v2.y)

  def subtract(v1: Vector, v2: Vector): Vector = Vector(v1.x - v2.x, v1.y - v2.y)

  def subtract(v1: Pos, v2: Pos): Vector = Vector(v1.x - v2.x, v1.y - v2.y)

  def scale(v: Vector, s: Double): Vector = Vector(v.x * s, v.y * s)

  def magnitude(v: Vector): Double = math.sqrt(v.x * v.x + v.y * v.y)

  def unit(v: Vector): Vector = scale(v, 1 / magnitude(v))

  def rotate90(v: Vector): Vector = Vector(-v.y, v.x)
}
