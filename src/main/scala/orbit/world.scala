package orbit

import physics.Pos

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
case class MouseInteraction(when: Long, pos: Pos)
case class AddObject(start: Option[MouseInteraction], end: Option[MouseInteraction])

sealed trait KeyIntention
case class Magnify(factor: Double) extends KeyIntention
case class ChangeSpeed(diff: Int) extends KeyIntention
case class ShiftScreen(diff: Pos) extends KeyIntention
case object TrackSun extends KeyIntention
case object CenterScreen extends KeyIntention
case object ClearTrails extends KeyIntention

case class Controls(magnification: Magnification, sunCenter: Pos, delay: Delay, trackSun: Boolean, collision: List[Collision],
                    tickTime: Long, tick: Long, addObject: AddObject)
case class Settings(sunMass: Double, objectCount: Int)

object World {
  def parse(as: List[String], settings: Settings): Settings = as match {
    case List() => settings
    case "-sun" :: sunMass :: rest => parse(rest, settings.copy(sunMass = sunMass.toDouble))
    case "-count" :: objectCount :: rest => parse(rest, settings.copy(objectCount = objectCount.toInt))
    case "-help" :: rest =>
      println("scala-orbit [-sun SUN_MASS] [-count OBJECT_COUNT] [-help]")
      System.exit(-1); ???
    case x :: rest => parse(rest, settings)
  }
}
