package object orbit {

  type Worlds = List[physics.World]

  def × : Nothing = throw new RuntimeException

  def parse(as: List[String], settings: Settings): Settings = as match {
    case List() => settings
    case "-sun" :: sunMass :: rest => parse(rest, settings.copy(sunMass = sunMass.toDouble))
    case "-count" :: objectCount :: rest => parse(rest, settings.copy(objectCount = objectCount.toInt))
    case "-help" :: rest =>
      println("scala-orbit [-sun SUN_MASS] [-count OBJECT_COUNT] [-help]")
      System.exit(-1); ×
    case x :: rest => parse(rest, settings)
  }
}
