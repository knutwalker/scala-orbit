package physics

case class Pos(x: Double = 0, y: Double = 0) extends PointLike
object Pos extends PointLikes[Pos] {
  private val mean = (a: Double, b: Double) => (a + b) / 2

  protected def make(nx: Double, ny: Double): Pos = apply(nx, ny)

  def average(p: Pos, q: Pos): Pos = {
    val (Pos(x1, y1), Pos(x2, y2)) = (p, q)
    Pos(mean(x1, x2), mean(y1, y2))
  }
}
