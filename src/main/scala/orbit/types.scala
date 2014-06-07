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

// specific mouse interactions
sealed trait MouseInteraction
case class ClickStarted(when: Long, pos: Pos) extends MouseInteraction
case class ClickEnded(when: Long, pos: Pos) extends MouseInteraction
case class AddObject(start: Pos, end: Pos, duration: Long)

// specific keyboard interactions
sealed trait KeyInteraction
case class Magnify(factor: Double) extends KeyInteraction
case class ChangeSpeed(diff: Int) extends KeyInteraction
case class ShiftScreen(diff: Pos) extends KeyInteraction
case object ResetMagnification extends KeyInteraction
case object TrackSun extends KeyInteraction
case object CenterScreen extends KeyInteraction
case object ClearTrails extends KeyInteraction

// repaint events
case object Repaint

// states
case class Controls(magnification: Magnification, sunCenter: Pos, delay: Delay, trackSun: Boolean, collision: List[Collision], tickTime: Long, tick: Long)

// settings for start parameters
case class Settings(sunMass: Double, objectCount: Int)
