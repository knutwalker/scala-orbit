package orbit

import physics.Pos

// value classes
case class Age(a: Int) extends AnyVal {
  def >(n: Int) = a > n
  def dec = Age(a - 1)
}
case class Magnification(m: Double) extends AnyVal {
  def *(f: Double) = Magnification(m * f)
}
case class Delay(d: Int) extends AnyVal {
  def +(n: Int) = Delay(0 max (n + d))
}
case class Collision(age: Age, pos: Pos)

// orbit interactions
sealed trait OrbitInteraction
case class Magnify(factor: Double) extends OrbitInteraction
case class ChangeSpeed(diff: Int) extends OrbitInteraction
case class ShiftScreen(diff: Pos) extends OrbitInteraction
case class AddObject(start: Pos, end: Pos, duration: Long) extends OrbitInteraction
case object ResetMagnification extends OrbitInteraction
case object TrackSun extends OrbitInteraction
case object CenterScreen extends OrbitInteraction
case object ClearTrails extends OrbitInteraction


// specific mouse interactions
sealed trait MouseInteraction
case class ClickStarted(when: Long, pos: Pos) extends MouseInteraction
case class ClickEnded(when: Long, pos: Pos) extends MouseInteraction

// states
case class Controls(magnification: Magnification, sunCenter: Pos, delay: Delay, trackSun: Boolean, collision: List[Collision], tickTime: Long, tick: Long)

// settings for start parameters
case class Settings(sunMass: Double, objectCount: Int)
