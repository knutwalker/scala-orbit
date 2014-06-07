package orbit

import physics.Pos


case class Controls(magnification: Double, center: Pos, trails: Boolean, clear: Boolean)

object World {
  def main(args: Array[String]) {
    orbit.runWorld()
  }
}
