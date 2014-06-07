package physics

case class Vec(x: Double = 0, y: Double = 0) extends PointLike
object Vec extends PointLikes[Vec] {
  protected def make(nx: Double, ny: Double): Vec = apply(nx, ny)
}
