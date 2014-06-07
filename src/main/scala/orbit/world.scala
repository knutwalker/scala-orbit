package orbit

import physics.Pos


case class Controls(magnification: Double, center: Pos, trails: Boolean, clear: Boolean)

object World extends App {
  orbit.runWorld()
}
