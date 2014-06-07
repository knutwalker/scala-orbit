package orbit

import java.awt.event.MouseEvent
import physics.Pos


case class Age(a: Int) extends AnyVal {
  def >(n: Int) = a > n
  def dec = Age(a - 1)
}
case class Magnification(m: Double) extends AnyVal {
  def *(f: Double) = Magnification(m * f)
}
case class Delay(d: Int) extends AnyVal {
  def inc = Delay(d + 1)
  def dec = if (d > 0) Delay(d - 1) else this
}
case class Collision(age: Age, pos: Pos)
case class Controls(magnification: Magnification, sunCenter: Pos,
                    delay: Delay, trackSun: Boolean, collision: List[Collision],
                    tickTime: Long, tick: Long, mouseDown: Option[MouseEvent], mouseUp: Option[MouseEvent])
case class Settings(sunMass: Double, objectCount: Int)

object World extends App {
  def parse(as: List[String], settings: Settings): Settings = as match {
    case List() => settings
    case "-sun" :: sunMass :: rest => parse(rest, settings.copy(sunMass = sunMass.toDouble))
    case "-count" :: objectCount :: rest => parse(rest, settings.copy(objectCount = objectCount.toInt))
    case "-help" :: rest =>
      println("scala-orbit [-sun SUN_MASS] [-count OBJECT_COUNT] [-help]")
      System.exit(-1)
      ???
    case x :: rest => parse(rest, settings)
  }
  val settings = parse(args.toList, Settings(1500, 400))

  orbit.runWorld(settings)
}
