package physics

trait TestUtilities {

  def square(n: Double) = n * n
  def closeTo(a: Double, b: Double) = square(a - b) < 0.0001
  def vectorCloseTo(a: Vector, b: Vector) = closeTo(a.x, b.x) && closeTo(a.y, b.y)

}
