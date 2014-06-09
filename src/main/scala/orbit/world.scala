package orbit

import physics._
import java.awt.{Graphics2D, Color}
import scala.util.Random

object world {
  import orbit.atom._

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

  def drawObject(g: Graphics2D, obj: Obj, epoch: Int, mag: Magnification, sunCenter: Pos): Unit = {
    val s = (mag.m * sizeByMass(obj)) max 2
    val (x, y) = toGraphicsCoord(obj.pos, s, mag, sunCenter)
    val c = colorByMass(obj, epoch)
    g.setColor(c)
    g.fillOval(x, y, s.toInt, s.toInt)
  }

  def drawWorld(g: Graphics2D, world: World, epoch: Int, controls: Controls): Unit =
    world.foreach(drawObject(g, _, epoch, controls.magnification, controls.sunCenter))

  def drawStatus(g: Graphics2D, world: World, controls: Controls): Unit = {
    val text = f"Objects: ${world.size}%d, Magnification ${controls.magnification.m}%4.3g, Delay: ${controls.delay.d}%d, Tick: ${controls.tick}%d, Tick Time: ${controls.tickTime}%dms ${if (controls.trackSun) "Tracking" else ""}%s"
    g.setColor(Color.black)
    g.clearRect(0, 0, 1000, 20)
    g.drawString(text, 20, 20)
  }

  def drawCollision(g: Graphics2D, collision: Collision, mag: Magnification, sunCenter: Pos): Unit = {
    val size = collision.age.a * 1.5
    val (x, y) = toGraphicsCoord(collision.pos, size, mag, sunCenter)
    g.setColor(Color.red)
    g.fillOval(x, y, size.toInt, size.toInt)
  }

  def drawCollisions(g: Graphics2D, controls: Controls): Unit =
    controls.collision.foreach(drawCollision(g, _, controls.magnification, controls.sunCenter))

  def ageCollisions(collisions: List[Collision]): List[Collision] =
    collisions.withFilter(_.age > 1).map(c => c.copy(age = c.age.dec))

  def drawWorldPanel(g: Graphics2D, world: World, epoch: Int, controls: Controls): Unit =
    { drawWorld(g, world, epoch, controls); drawCollisions(g, controls); drawStatus(g, world, controls) }

  def ageWorld(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(collision = ageCollisions(c.collision)))

  def pruneHistory(worldHistory: Worlds): Worlds =
    worldHistory.take(historySize)

  def updateWorldHistory(worldHistory: Worlds): (List[Pos], Worlds) = {
    val (collisions, updatedWorld) = Obj.updateAll(worldHistory.head)
    val newWorldHistory = updatedWorld :: worldHistory
    (collisions, pruneHistory(newWorldHistory))
  }

  def addCollisions(newCollisions: List[Pos], currentCollision: List[Collision]) =
    currentCollision ::: newCollisions.map(p => Collision(Age(10), p))

  def screenUpdate(controls: Atom[Controls], worlds: Atom[Worlds]): Unit = {
    val (collisions, newWorldHistory) = updateWorldHistory(worlds.`@`)
    controls.swap(c => c.copy(collision = addCollisions(collisions, c.collision)))
    worlds reset newWorldHistory
  }

  def findSun(world: World) =
    world.find(_.name.contains("sun")).get

  def magnify(controls: Atom[Controls], factor: Double): Unit =
    controls.swap(c => c.copy(magnification = c.magnification * factor))

  def shiftScreen(controls: Atom[Controls], direction: Pos): Unit =
    controls.swap(c => c.copy(sunCenter = Pos.add(direction, c.sunCenter), trackSun = false))

  def changeSpeed(controls: Atom[Controls], diff: Int): Unit =
    controls.swap(c => c.copy(delay = c.delay + diff))

  def trackSun(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(trackSun = !c.trackSun))

  def resetMagnification(controls: Atom[Controls]): Unit =
    controls.swap(c => c.copy(magnification = Magnification(1.0)))

  def centerScreen(controls: Atom[Controls], worlds: Atom[Worlds]): Unit =
    controls swap { c =>
      val w = worlds.`@`
      val sunPosition = findSun(w.head).pos
      c.copy(sunCenter = sunPosition)
    }

  def clearTrails(worlds: Atom[Worlds]): Unit =
    worlds.swap(_.head :: Nil)

  def paintAllWorlds(g: Graphics2D, worldHistory: Worlds, controls: Controls): Unit =
    worldHistory.zipWithIndex.foreach(w => drawWorldPanel(g, w._1, w._2, controls))

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

  def addObject(start: Pos, end: Pos, duration: Long, controls: Controls) = {
    val pos = toObjectCoords(start, controls.magnification, controls.sunCenter)
    val v = Vec.scale(Vec(Pos.subtract(end, start)), 0.01 / controls.magnification.m)
    Obj(pos, duration / 100.0, v, Vec(), "m")
  }

  def insertObject(start: Pos, end: Pos, duration: Long, controls: Atom[Controls], worlds: Atom[Worlds]): Unit =
    worlds swap { w =>
      val obj = addObject(start, end, duration, controls.`@`)
      addObjectToWorld(obj, w)
    }

  def handleInteraction(interaction: OrbitInteraction, controls: Atom[Controls], worlds: Atom[Worlds]) = interaction match {
    case Magnify(factor) => magnify(controls, factor)
    case ChangeSpeed(diff) => changeSpeed(controls, diff)
    case ShiftScreen(diff) => shiftScreen(controls, diff)
    case ResetMagnification => resetMagnification(controls)
    case TrackSun => trackSun(controls)
    case CenterScreen => centerScreen(controls, worlds)
    case ClearTrails => clearTrails(worlds)
    case AddObject(start, end, duration) => insertObject(start, end, duration, controls, worlds)
  }

  def worldState(objectCount: Int, sunMass: Double): (Atom[List[World]], Atom[Controls]) = {
    val worldHistory = Atom(createWorld(objectCount, sunMass) :: Nil)
    val controls = Atom(Controls(Magnification(1.0), center, Delay(0), trackSun = true, List(), 0, 0))
    (worldHistory, controls)
  }
}
