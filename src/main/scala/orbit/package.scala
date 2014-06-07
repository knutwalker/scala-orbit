import java.awt.{Dimension, Graphics, Color}
import java.awt.event.{KeyEvent, ActionEvent, KeyListener, ActionListener}
import javax.swing.{Timer, JFrame, JPanel}

import scala.collection.breakOut

import physics.{Objs, Pos, Obj, Vec}


package object orbit {
  import atom._

  private def rand(n: Double) = math.random * n

  val objectSize = 500
  val updateInterval = 1

  val center = Pos(500, 500)

  def sizeByMass(o: Obj) = math.sqrt(o.mass) + 0
  def colorByMass(o: Obj) = o.mass match {
    case m if m < 1  => Color.black
    case m if m < 2  => new Color(210, 105, 30)
    case m if m < 5  => Color.red
    case m if m < 10 => new Color(107, 142, 35)
    case m if m < 20 => Color.magenta
    case m if m < 40 => Color.blue
    case _           => new Color(215, 215, 0)
  }

  def drawObject(g: Graphics, obj: Obj, controls: Controls): Unit = {
    val mag = controls.magnification
    val sunCenter = controls.center
    val xOffset = center.x - (mag * sunCenter.x)
    val yOffset = center.y - (mag * sunCenter.y)
    val x = (xOffset + (mag * obj.pos.x)).toInt
    val y = (yOffset + (mag * obj.pos.y)).toInt
    val s = math.max(2, mag * sizeByMass(obj)).toInt
    val halfS = s / 2
    val c = colorByMass(obj)
    g.setColor(c)
    g.fillOval(x - halfS, y - halfS, s, s)
  }

  def findSun(world: Objs) = world.find(_.name.contains("sun")).get

  def drawWorld(g: Graphics, world: Objs, controls: Controls): Unit = {
    world.foreach(drawObject(g, _, controls))
    g.clearRect(0, 0, 1000, 20)
    g.drawString(f"Objects: ${world.size}, Magnification ${controls.magnification}%4.3g", 20, 20)
  }

  def updateWorld(world: Atom[Objs]) = world.alter(Obj.updateAll)

  def magnify(factor: Double, controls: Atom[Controls], world: Atom[Objs]) = {
    val sunPosition = findSun(world.get()).pos
    controls.alter { ctrl =>
      val newMag = factor * ctrl.magnification
      ctrl.copy(magnification = newMag, center = sunPosition, clear = true)
    }
  }

  def resetScreenState(controls: Atom[Controls]) = controls.alter(_.copy(clear = false))

  def toggleTrail(controls: Atom[Controls]) = controls.alter(c => c.copy(trails = !c.trails))

  def handleKey(c: Char, world: Atom[Objs], controls: Atom[Controls]) = c match {
    case 'q' => System.exit(1)
    case '+' | '=' => magnify(1.1, controls, world)
    case '-' | '_' => magnify(0.9, controls, world)
    case ' ' => magnify(1.0, controls, world)
    case 't' => toggleTrail(controls)
    case _ =>
  }

  def worldPanel(frame: JFrame, world: Atom[Objs], controls: Atom[Controls]) = {
    new JPanel() with ActionListener with KeyListener {
      def keyTyped(e: KeyEvent) = ()
      def keyReleased(e: KeyEvent) = ()
      def keyPressed(e: KeyEvent) = {
        handleKey(e.getKeyChar, world, controls)
      }
      def actionPerformed(e: ActionEvent) = {
        updateWorld(world)
        this.repaint()
      }

      override def paintComponent(g: Graphics): Unit = {
        val w = world.get()
        val c = controls.get()
        if (c.clear || !c.trails) super.paintComponent(g)
        drawWorld(g, w, c)
        resetScreenState(controls)
      }

      override def getPreferredSize: Dimension = new Dimension(1000, 1000)
    }
  }

  def randomVelocity(p: Pos, sun: Obj) = {
    val sp = sun.pos
    val sd = Pos.distance(p, sp)
    val v = math.sqrt(1 / sd)
    val direction = Vec.rotate90(Vec.unit(Vec(Pos.subtract(p, sp))))
    Vec.scale(direction, rand(0.01) + (13.5 * v))
  }

  def randomPosition(sunPos: Pos) = {
    val r = rand(300) + 30
    val theta = rand(2 * math.Pi)
    Pos.add(sunPos, Pos(r * math.cos(theta), r * math.sin(theta)))
  }

  def randomObject(sun: Obj, n: Int) = {
    val sp = sun.pos
    val p = randomPosition(sp)
    Obj(p, rand(0.2), randomVelocity(p, sun), Vec(), s"r$n")
  }

  def createWorld(): Objs = {
    val v0 = Vec()
    val sun = Obj(center, 750, Vec(0, 0), v0, "sun")
    val world: Objs = (500 to 1 by -1).map(randomObject(sun, _))(breakOut)
    sun +: world
  }

  def worldFrame() = {
    val controls = Controls(1.0, center, trails = false, clear = false).`@`
    val world = createWorld().`@`
    val frame = new JFrame("Orbit")
    val panel = worldPanel(frame, world, controls)
    val timer = new Timer(updateInterval, panel)
    { import panel._
      setFocusable(true)
      addKeyListener(panel) }
    { import frame._
      add(panel)
      pack()
      setVisible(true)
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) }
    timer.start()
  }

  def runWorld() = worldFrame()
}
