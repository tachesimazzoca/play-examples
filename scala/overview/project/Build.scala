import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

  scalaVersion := "2.10.4"

  val appName         = "play-overview"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    filters,
    ws,
    "org.scalatestplus" % "play_2.10" % "1.0.0" % "test"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies
  )
}
