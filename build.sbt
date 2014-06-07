name := """orbit"""

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= List(
  "org.scalatest"      %% "scalatest"                     % "2.2.0"   % "test",
  "org.scalacheck"     %% "scalacheck"                    % "1.11.4"  % "test"
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

