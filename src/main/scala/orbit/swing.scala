package orbit

import physics.Pos
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.ComputationScheduler
import rx.lang.scala.subjects.PublishSubject

import scala.concurrent._
import scala.concurrent.duration._
import scala.swing._
import scala.swing.event._

object swing {
  import orbit.atom._
  import orbit.world.{worldState, paintAllWorlds, handleKey, handleMouse, centerScreen, screenUpdate, ageWorld}

  trait WorldInteraction {
    def worldHistory: Atom[Worlds]
    def controls: Atom[Controls]

    def handleInteractions(keys: Observable[KeyInteraction], mouse: Observable[AddObject]): Unit = {
      keys.subscribe(handleKey(_, controls, worldHistory))
      mouse.subscribe(handleMouse(_, controls, worldHistory))
    }

    def startWorld(implicit ec: ExecutionContext): Observable[Repaint.type] = {
      Future { blocking { while(true) {
        val startTime = System.currentTimeMillis()
        val delay = controls.`@`.delay.d
        if (delay > 0) Thread.sleep(delay * 2)
        if (controls.`@`.trackSun) centerScreen(controls, worldHistory)
        screenUpdate(controls, worldHistory)
        controls.swap(c => c.copy(tickTime = System.currentTimeMillis() - startTime, tick = c.tick + 1))
      }}}
      Observable.interval(10.millis, ComputationScheduler()).map(_ => Repaint)
    }
  }

  class SwingOrbit(settings: Settings) extends SimpleSwingApplication with WorldInteraction {
    val (worldHistory, controls) = worldState(settings.objectCount, settings.sunMass)

    private val keyChannel = PublishSubject[KeyInteraction]()
    private val mouseChannel = PublishSubject[MouseInteraction]()

    def observeKeys: Observable[KeyInteraction] = keyChannel
    def observeMouse: Observable[MouseInteraction] = mouseChannel

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
            case Key.Q =>
              keyChannel.onCompleted()
              mouseChannel.onCompleted()
              SwingOrbit.this.quit(); ×
          }
          intention foreach keyChannel.onNext

        case e: MousePressed =>
          mouseChannel onNext ClickStarted(e.when, Pos(e.point.x, e.point.y))

        case e: MouseReleased =>
          mouseChannel onNext ClickEnded(e.when, Pos(e.point.x, e.point.y))
      }
    }

    val top = new MainFrame {
      title = "Orbit"
      preferredSize = new Dimension(1000, 1000)
      contents = worldPanel
    }

    private def mergeMouseInteractions(obs: Observable[MouseInteraction]): Observable[AddObject] = {
      val chan = PublishSubject[AddObject]()
      // TODO: does Rx isolate good enough to replace that with a regular var?
      val click: Atom[Option[ClickStarted]] = Atom(None)

      obs.subscribe(
        onNext = {
          case start: ClickStarted =>
            click.swap {
              case c@Some(x) => c
              case None      => Some(start)
            }
          case ClickEnded(when, pos) =>
            click.`@`.foreach { start =>
              chan onNext AddObject(start.pos, pos, when - start.when)
              click reset None
            }
        },
        onError = chan.onError,
        onCompleted = chan.onCompleted
      )

      chan
    }

    override def startup(args: Array[String]): Unit = {
      super.startup(args)

      handleInteractions(this.observeKeys, mergeMouseInteractions(this.observeMouse))
      startWorld(ExecutionContext.global).subscribe(_ => worldPanel.repaint())
    }
  }

  def main(args: Array[String]) {
    val settings = parse(args.toList, Settings(1500, 400))
    val world = new SwingOrbit(settings)
    world.main(args)
  }
}
