name := """scala-orbit"""

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= List(
  "org.scala-lang.modules" %% "scala-swing"    % "1.0.1",
  "org.scalatest"          %% "scalatest"      % "2.2.0"   % "test"
)

scalacOptions := List(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

Revolver.settings

