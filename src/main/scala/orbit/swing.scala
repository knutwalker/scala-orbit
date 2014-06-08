package orbit

import orbit.World._
import physics.Pos

import scala.swing._
import scala.swing.event.{MouseReleased, MousePressed, Key, KeyPressed}

object swing {
  class SwingOrbit(settings: Settings) extends SimpleSwingApplication { also: Reactor =>
    val (worldHistory, controls) = worldState(settings.objectCount, settings.sunMass)

    val worldPanel = new Panel {
      focusable = true
      override protected def paintComponent(g: Graphics2D): Unit = {
        super.paintComponent(g)
        paintAllWorlds(g, worldHistory, controls)
      }

      listenTo(mouse.clicks, keys)
      reactions += {
        case e @ KeyPressed(_, key, modifiers, _) =>
          val shift = (modifiers & Key.Modifier.Shift) != 0
          val intention = Some(key).collect {
            case Key.Left => ShiftScreen(Pos(-10 * (if (shift) 10 else 1), 0))
            case Key.Up => ShiftScreen(Pos(0, -10 * (if (shift) 10 else 1)))
            case Key.Right => ShiftScreen(Pos(10 * (if (shift) 10 else 1), 0))
            case Key.Down => ShiftScreen(Pos(0, 10 * (if (shift) 10 else 1)))
            case Key.Plus | Key.Equals => Magnify(if (shift) 1.3 else 1.1)
            case Key.Minus => Magnify(if (shift) 0.7 else 0.9)
            case Key.Space => CenterScreen
            case Key.S => ChangeSpeed(1)
            case Key.F => ChangeSpeed(-1)
            case Key.T => TrackSun
            case Key.R => ClearTrails
            case Key.Q => also.quit(); ???
          }
          intention.foreach(handleKey(_, worldHistory, controls))

        case e: MousePressed =>
          handleMouseDown(controls, MouseInteraction(e.when, Pos(e.point.x, e.point.y)))

        case e: MouseReleased =>
          handleMouseUp(controls, MouseInteraction(e.when, Pos(e.point.x, e.point.y)))
      }
    }

    val top = new MainFrame {
      title = "Orbit"
      preferredSize = new Dimension(1000, 1000)
      contents = worldPanel
    }

    override def startup(args: Array[String]): Unit = {
      super.startup(args)
      startWorld(worldHistory, controls, worldPanel.repaint())(scala.concurrent.ExecutionContext.global)
    }
  }

  def main(args: Array[String]) {
    val settings = parse(args.toList, Settings(1500, 400))
    val world = new SwingOrbit(settings)
    world.main(args)
  }
}
