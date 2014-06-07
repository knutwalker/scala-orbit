import java.awt.{Dimension, Graphics, Color}
import java.awt.event._
import javax.swing.{JFrame, JPanel}

import scala.collection.breakOut
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Random

import physics.{World, Pos, Obj, Vec}


package object orbit {
  import atom._

  type Worlds = List[World]

  val center = Pos(500, 500)
  val historySize = 70

  def sizeByMass(o: Obj) =
    math.sqrt(o.mass)

  def opacity(epoch: Int): Int =
    255 - (epoch * 255.0 / historySize).toInt

  def colorByMass(o: Obj, epoch: Int) = o.mass match {
    case m if m < 1  => new Color(0, 0, 0, opacity(epoch))
    case m if m < 2  => new Color(210, 105, 30, opacity(epoch))
    case m if m < 5  => new Color(255, 0, 0, opacity(epoch))
    case m if m < 10 => new Color(107, 142, 35, opacity(epoch))
    case m if m < 20 => new Color(255, 0, 255, opacity(epoch))
    case m if m < 40 => new Color(0, 0, 255, opacity(epoch))
    case _           => new Color(255, 215, 0, opacity(epoch))
  }

  def toObjectCoords(screenCoords: Pos, mag: Magnification, sunCenter: Pos): Pos = {
    val xOffset = center.x - (mag.m * sunCenter.x)
    val yOffset = center.y - (mag.m * sunCenter.y)
    val x = (screenCoords.x - xOffset) / mag.m
    val y = (screenCoords.y - yOffset) / mag.m
    Pos(x, y)
  }

  def toScreenCoords(objectCoords: Pos, mag: Magnification, sunCenter: Pos): Pos = {
    val xOffset = center.x - (mag.m * sunCenter.x)
    val yOffset = center.y - (mag.m * sunCenter.y)
    val x = xOffset + (mag.m * objectCoords.x)
    val y = yOffset + (mag.m * objectCoords.y)
    Pos(x, y)
  }

  def toGraphicsCoord(pos: Pos, size: Double, mag: Magnification, sunCenter: Pos): (Int, Int) = {
    val screen = toScreenCoords(pos, mag, sunCenter)
    val halfS = size / 2
    val x = (screen.x - halfS).toInt
    val y = (screen.y - halfS).toInt
    (x, y)
  }

  def drawObject(g: Graphics, obj: Obj, epoch: Int, mag: Magnification, sunCenter: Pos): Unit = {
    val s = (mag.m * sizeByMass(obj)) max 2
    val (x, y) = toGraphicsCoord(obj.pos, s, mag, sunCenter)
    val c = colorByMass(obj, epoch)
    g.setColor(c)
    g.fillOval(x, y, s.toInt, s.toInt)
  }

  def drawWorld(g: Graphics, world: World, epoch: Int, controls: Controls): Unit =
    world.foreach(drawObject(g, _, epoch, controls.magnification, controls.sunCenter))

  def drawStatus(g: Graphics, world: World, controls: Controls): Unit = { import g._
    val text = f"Objects: ${world.size}%d, Magnification ${controls.magnification.m}%4.3g, Delay: ${controls.delay.d}%d, Tick: ${controls.tick}%d, Tick Time: ${controls.tickTime}%dms ${if (controls.trackSun) "Tracking" else ""}%s"
    setColor(Color.black)
    clearRect(0, 0, 1000, 20)
    drawString(text, 20, 20)
  }

  def drawCollision(g: Graphics, collision: Collision, mag: Magnification, sunCenter: Pos): Unit = {
    val size = collision.age.a * 1.5
    val (x, y) = toGraphicsCoord(collision.pos, size, mag, sunCenter)
    g.setColor(Color.red)
    g.fillOval(x, y, size.toInt, size.toInt)
  }

  def drawCollisions(g: Graphics, controls: Controls): Unit =
    controls.collision.foreach(c => drawCollision(g, c, controls.magnification, controls.sunCenter))

  def ageCollisions(collisions: List[Collision]): List[Collision] =
    collisions.withFilter(_.age > 1).map(c => c.copy(age = c.age.dec))

  def drawWorldPanel(g: Graphics, world: World, epoch: Int, controls: Atom[Controls]): Unit = {
    val c = controls.`@`
    drawWorld(g, world, epoch, c)
    drawCollisions(g, c)
    drawStatus(g, world, c)
    controls.swap(c => c.copy(collision = ageCollisions(c.collision)))
  }

  def pruneHistory(worldHistory: Worlds): Worlds =
    worldHistory.take(historySize)

  def updateWorldHistory(worldHistory: Worlds): (List[Pos], Worlds) = {
    val (collisions, updatedWorld) = Obj.updateAll(worldHistory.head)
    val newWorldHistory = updatedWorld :: worldHistory
    (collisions, pruneHistory(newWorldHistory))
  }

  def addCollisions(newCollisions: List[Pos], currentCollision: List[Collision]) =
    currentCollision ::: newCollisions.map(p => Collision(Age(10), p))

  def updateScreen(worldHistory: Atom[Worlds], controls: Atom[Controls]) = {
    val (collisions, newWorldHistory) = updateWorldHistory(worldHistory.`@`)
    worldHistory reset newWorldHistory
    controls.swap(c => c.copy(collision = addCollisions(collisions, c.collision)))
  }

  def findSun(world: World) =
    world.find(_.name.contains("sun")).get

  def centerScreen(controls: Atom[Controls], worldHistory: Atom[Worlds]) = {
    val sunPosition = findSun(worldHistory.`@`.head).pos
    controls.reset(controls.`@`.copy(sunCenter = sunPosition))
  }

  def magnify(factor: Double, controls: Atom[Controls], worldHistory: Atom[Worlds]): Unit =
    controls.swap(c => c.copy(magnification = c.magnification * factor))

  def shiftScreen(direction: Pos, controls: Atom[Controls], worldHistory: Atom[Worlds]): Unit =
    controls.swap(c => c.copy(sunCenter = Pos.add(direction, c.sunCenter)))

  def clearTrails(worldHistory: Atom[Worlds]): Unit =
    worldHistory.reset(worldHistory.`@`.head :: Nil)

  def slowDown(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(delay = c.delay.inc))

  def speedUp(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(delay = c.delay.dec))

  def trackSun(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(trackSun = !c.trackSun))

  def handleKey(c: Int, shift: Boolean, worldHistory: Atom[Worlds], controls: Atom[Controls]): Unit = c match {
    case KeyEvent.VK_LEFT => shiftScreen(Pos(-10 * (if (shift) 10 else 1), 0), controls, worldHistory)
    case KeyEvent.VK_UP => shiftScreen(Pos(0, -10 * (if (shift) 10 else 1)), controls, worldHistory)
    case KeyEvent.VK_RIGHT => shiftScreen(Pos(10 * (if (shift) 10 else 1), 0), controls, worldHistory)
    case KeyEvent.VK_DOWN => shiftScreen(Pos(0, 10 * (if (shift) 10 else 1)), controls, worldHistory)
    case KeyEvent.VK_PLUS | KeyEvent.VK_EQUALS => magnify(if (shift) 1.3 else 1.1, controls, worldHistory)
    case KeyEvent.VK_MINUS => magnify(if (shift) 0.7 else 0.9, controls, worldHistory)
    case KeyEvent.VK_SPACE => centerScreen(controls, worldHistory)
    case KeyEvent.VK_S => slowDown(controls)
    case KeyEvent.VK_F => speedUp(controls)
    case KeyEvent.VK_T => trackSun(controls)
    case KeyEvent.VK_R => clearTrails(worldHistory)
    case KeyEvent.VK_Q => System.exit(1)
    case _ =>
  }

  def worldPanel(frame: JFrame, worldHistory: Atom[Worlds], controls: Atom[Controls]) = {
    new JPanel() with KeyListener with MouseListener {
      override def paintComponent(g: Graphics): Unit = {
        super.paintComponent(g)
        worldHistory.`@`.zipWithIndex.foreach(w => drawWorldPanel(g, w._1, w._2, controls))
      }

      def keyPressed(e: KeyEvent) =
        handleKey(e.getKeyCode, e.isShiftDown, worldHistory, controls)

      def mousePressed(e: MouseEvent) =
        if (controls.`@`.mouseDown.isEmpty)
          controls.swap(_.copy(mouseDown = Some(e), mouseUp = None))

      def mouseReleased(e: MouseEvent) =
        if (controls.`@`.mouseUp.isEmpty)
          controls.swap(_.copy(mouseUp = Some(e)))

      override def getPreferredSize: Dimension = new Dimension(1000, 1000)

      def keyReleased(e: KeyEvent) = ()
      def keyTyped(e: KeyEvent) = ()
      def mouseEntered(e: MouseEvent) = ()
      def mouseClicked(e: MouseEvent) = ()
      def mouseExited(e: MouseEvent) = ()
    }
  }

  def randomVelocity(p: Pos, sun: Obj): Vec = {
    val sp = sun.pos
    val sd = Pos.distance(p, sp)
    val v = math.sqrt(1 / sd)
    val direction = Vec.rotate90(Vec.unit(Vec(Pos.subtract(p, sp))))
    Vec.scale(direction, Random.nextDouble() * 0.01 + (v * 13.5 * 3))
  }

  def randomPosition(sunPos: Pos): Pos = {
    val r = Random.nextInt(300) + 30
    val theta = Random.nextDouble() * 2 * math.Pi
    Pos.add(sunPos, Pos(r * math.cos(theta), r * math.sin(theta)))
  }

  def randomObject(sun: Obj, n: Int): Obj = {
    val sp = sun.pos
    val p = randomPosition(sp)
    Obj(p, Random.nextDouble() * 0.2, randomVelocity(p, sun), Vec(), s"r$n")
  }

  def createWorld(objectCount: Int, sunMass: Double): World = {
    val v0 = Vec()
    val sun = Obj(center, sunMass, Vec(0, 0), v0, "sun")
    val world: World = (objectCount to 1 by -1).map(randomObject(sun, _))(breakOut)
    sun +: world
  }

  def addObjectToWorld(o: Obj, worldHistory: Worlds): Worlds = {
    val currentWorld = worldHistory.head :+ o
    currentWorld :: worldHistory.tail
  }

  def handleMouse(worldHistory: Atom[Worlds], controls: Atom[Controls]) = for {
    c <- controls
    ue <- c.mouseUp
    de <- c.mouseDown
    downPos = Pos(de.getX, de.getY)
    upPos = Pos(ue.getX, ue.getY)
    duration = ue.getWhen - de.getWhen
    pos = toObjectCoords(downPos, c.magnification, c.sunCenter)
    v = Vec.scale(Vec(Pos.subtract(upPos, downPos)), 0.01 / c.magnification.m)
    obj = Obj(pos, duration / 100.0, v, Vec(), "m")
  } {
    worldHistory.swap(h => addObjectToWorld(obj, h))
    controls.swap(_.copy(mouseDown = None, mouseUp = None))
  }

  def worldFrame(objectCount: Int, sunMass: Double)(implicit ec: ExecutionContext) = {
    val controls = Atom(Controls(Magnification(1.0), center, Delay(0), trackSun = true, List(), 0, 0, None, None))
    val worldHistory = Atom(createWorld(objectCount, sunMass) :: Nil)
    val frame = new JFrame("Orbit")
    val panel = worldPanel(frame, worldHistory, controls)

    { import panel._
      setFocusable(true)
      addKeyListener(panel)
      addMouseListener(panel) }
    { import frame._
      add(panel)
      pack()
      setVisible(true)
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) }

    Future { blocking {
      while(true) {
        val startTime = System.currentTimeMillis()
        val sleepTime = controls.`@`.delay.d * 2
        Thread.sleep(sleepTime max 1)
        if (controls.`@`.trackSun) centerScreen(controls, worldHistory)
        updateScreen(worldHistory, controls)
        handleMouse(worldHistory, controls)
        controls.swap(c => c.copy(tickTime = System.currentTimeMillis() - startTime, tick = c.tick + 1))
        panel.repaint()
      }
    }}
  }

  def runWorld(settings: Settings) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    worldFrame(settings.objectCount, settings.sunMass)
  }
}
