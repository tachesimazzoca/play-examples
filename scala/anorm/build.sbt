name := "play-anorm"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "com.h2database" % "h2" % "1.4.178",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test"
)

lazy val main = (project in file(".")).enablePlugins(PlayScala)
