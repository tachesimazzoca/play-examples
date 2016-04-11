import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

  scalaVersion := "2.11.8"

  val appName         = "play-overview"
  val appVersion      = "0.1.0-SNAPSHOT"

  val appDependencies = Seq(
    filters,
    ws,
    "org.scalatestplus" %% "play" % "1.1.0" % "test"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies
  )
}
