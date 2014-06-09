package orbit

import physics.Pos

import scala.concurrent._
import scala.swing._
import scala.swing.event._

object swing {
  import orbit.atom._
  import orbit.world.{ageWorld, centerScreen, handleInteraction, paintAllWorlds, screenUpdate, worldState}

  trait WorldInteraction {
    def worldHistory: Atom[Worlds]
    def controls: Atom[Controls]

    def startWorld(repaint: => Unit)(implicit ec: ExecutionContext): Unit = {
      Future { blocking { while(true) {
        val startTime = System.currentTimeMillis()
        val delay = controls.`@`.delay.d
        if (delay > 0) Thread.sleep(delay * 2)
        if (controls.`@`.trackSun) centerScreen(controls, worldHistory)
        screenUpdate(controls, worldHistory)
        controls.swap(c => c.copy(tickTime = System.currentTimeMillis() - startTime, tick = c.tick + 1))
        repaint
      }}}
    }
  }

  class SwingOrbit(settings: Settings) extends SimpleSwingApplication with WorldInteraction {
    val (worldHistory, controls) = worldState(settings.objectCount, settings.sunMass)
    private val click: Atom[Option[ClickStarted]] = Atom(None)

    private val worldPanel = new Panel {
      focusable = true
      override protected def paintComponent(g: Graphics2D) = {
        super.paintComponent(g)
        paintAllWorlds(g, worldHistory.`@`, controls.`@`)
        ageWorld(controls)
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
            case Key.Key0 => ResetMagnification
            case Key.Space => CenterScreen
            case Key.S => ChangeSpeed(1)
            case Key.F => ChangeSpeed(-1)
            case Key.T => TrackSun
            case Key.R => ClearTrails
            case Key.Q => SwingOrbit.this.quit(); ×
          }
          intention.foreach(handleInteraction(_, controls, worldHistory))

        case e: MousePressed => click.swap {
          case c@Some(x) => c
          case None      => Some(ClickStarted(e.when, Pos(e.point.x, e.point.y)))
        }

        case e: MouseReleased => click.`@`.foreach { start =>
          handleInteraction(AddObject(start.pos, Pos(e.point.x, e.point.y), e.when - start.when), controls, worldHistory)
          click reset None
        }
      }
    }

    val top = new MainFrame {
      title = "Orbit"
      preferredSize = new Dimension(1000, 1000)
      contents = worldPanel
    }

    override def startup(args: Array[String]): Unit = {
      super.startup(args)
      startWorld(worldPanel.repaint())(ExecutionContext.global)
    }
  }

  def main(args: Array[String]) {
    val settings = parse(args.toList, Settings(1500, 400))
    val world = new SwingOrbit(settings)
    world.main(args)
  }
}
