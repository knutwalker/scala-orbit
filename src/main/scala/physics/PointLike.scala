package physics

trait PointLike {
  val x: Double
  val y: Double
}

trait PointLikes[A <: PointLike] {
  private val sqr = math.pow(_: Double, 2)

  protected def make(nx: Double, ny: Double): A

  def add(p1: PointLike, p2: PointLike): A = make(p1.x + p2.x, p1.y + p2.y)

  def subtract(p1: PointLike, p2: PointLike): A = make(p1.x - p2.x, p1.y - p2.y)

  def scale(p: PointLike, s: Double): A = make(p.x * s, p.y * s)

  def rotate90(p: PointLike): A = make(-p.y, p.x)

  def unit(p: PointLike): A = scale(p, 1 / magnitude(p))

  def magnitude(p: PointLike): Double = math.sqrt(p.x * p.x + p.y * p.y)

  def distance(p: PointLike, q: PointLike): Double = math.sqrt(sqr(p.x - q.x) + sqr(p.y - q.y))

  def isOrigin(p: PointLike): Boolean = p.x == 0 && p.y == 0
}
