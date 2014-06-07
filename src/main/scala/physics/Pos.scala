package physics

case class Pos(x: Double = 0, y: Double = 0)
object Pos {
  private val sqr = math.pow(_: Double, 2)
  private val mean = (a: Double, b: Double) => (a + b) / 2

  def isOrigin(p: Pos): Boolean = p.x == 0 && p.y == 0

  def add(p1: Pos, p2: Pos): Pos = Pos(p1.x + p2.x, p1.y + p2.y)

  def add(p1: Pos, p2: Vector): Pos = Pos(p1.x + p2.x, p1.y + p2.y)

  def subtract(p1: Pos, p2: Pos): Pos = Pos(p1.x - p2.x, p1.y - p2.y)

  def distance(p: Pos, q: Pos): Double = math.sqrt(sqr(p.x - q.x) + sqr(p.y - q.y))

  def average(p: Pos, q: Pos): Pos = {
    val (Pos(x1, y1), Pos(x2, y2)) = (p, q)
    Pos(mean(x1, x2), mean(y1, y2))
  }
}
