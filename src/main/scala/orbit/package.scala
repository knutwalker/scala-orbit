import java.awt.{Color, Graphics}

import physics.{Obj, Pos, Vec, World}

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.Random


package object orbit {
  import orbit.atom._

  type Worlds = List[World]

  val center = Pos(500, 500)
  val historySize = 70

  def sizeByMass(o: Obj) =
    math.sqrt(o.mass) + 0

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
    controls.swap(c => c.copy(sunCenter = Pos.add(direction, c.sunCenter), trackSun = false))

  def clearTrails(worldHistory: Atom[Worlds]): Unit =
    worldHistory.reset(worldHistory.`@`.head :: Nil)

  def changeSpeed(diff: Int, controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(delay = c.delay + diff))

  def trackSun(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(trackSun = !c.trackSun))

  def handleKey(ki: KeyIntention, worldHistory: Atom[Worlds], controls: Atom[Controls]) = ki match {
    case Magnify(factor) => magnify(factor, controls, worldHistory)
    case ChangeSpeed(diff) => changeSpeed(diff, controls)
    case ShiftScreen(diff) => shiftScreen(diff, controls, worldHistory)
    case TrackSun => trackSun(controls)
    case CenterScreen => centerScreen(controls, worldHistory)
    case ClearTrails => clearTrails(worldHistory)
  }

  def paintAllWorlds(g: Graphics, worldHistory: Atom[Worlds], controls: Atom[Controls]): Unit = {
    worldHistory.`@`.zipWithIndex.foreach(w => drawWorldPanel(g, w._1, w._2, controls))
  }

  def handleMouseDown(controls: Atom[Controls], mi: MouseInteraction): Unit =
    if (controls.`@`.addObject.start.isEmpty)
      controls.swap(_.copy(addObject = AddObject(Some(mi), None)))

  def handleMouseUp(controls: Atom[Controls], mi: MouseInteraction): Unit =
    if (controls.`@`.addObject.end.isEmpty)
      controls.swap(c => c.copy(addObject = c.addObject.copy(end = Some(mi))))

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
    sun +: Vector.tabulate(objectCount)(n => randomObject(sun, objectCount - n))
  }

  def addObjectToWorld(o: Obj, worldHistory: Worlds): Worlds = {
    val currentWorld = worldHistory.head :+ o
    currentWorld :: worldHistory.tail
  }

  def handleMouse(worldHistory: Atom[Worlds], controls: Atom[Controls]) = for {
    c <- controls
    start <- c.addObject.start
    end <- c.addObject.end
    duration = end.when - start.when
    pos = toObjectCoords(start.pos, c.magnification, c.sunCenter)
    v = Vec.scale(Vec(Pos.subtract(end.pos, start.pos)), 0.01 / c.magnification.m)
    obj = Obj(pos, duration / 100.0, v, Vec(), "m")
  } {
    worldHistory.swap(h => addObjectToWorld(obj, h))
    controls.swap(_.copy(addObject = AddObject(None, None)))
  }

  def worldState(objectCount: Int, sunMass: Double): (Atom[List[World]], Atom[Controls]) = {
    val worldHistory = Atom(createWorld(objectCount, sunMass) :: Nil)
    val controls = Atom(Controls(Magnification(1.0), center, Delay(0), trackSun = true, List(), 0, 0, AddObject(None, None)))
    (worldHistory, controls)
  }

  def startWorld(worldHistory: Atom[Worlds], controls: Atom[Controls], repaint: => Unit)(implicit ec: ExecutionContext): Unit =
    Future { blocking { while(true) {
      val startTime = System.currentTimeMillis()
      val delay = controls.`@`.delay.d
      if (delay > 0) Thread.sleep(delay * 2)
      if (controls.`@`.trackSun) centerScreen(controls, worldHistory)
      updateScreen(worldHistory, controls)
      handleMouse(worldHistory, controls)
      controls.swap(c => c.copy(tickTime = System.currentTimeMillis() - startTime, tick = c.tick + 1))
      repaint
    }}}
}
