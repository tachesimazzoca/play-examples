import sbt._
import Keys._
import play.twirl.sbt.Import._
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

  scalaVersion := "2.11.8"

  val appName         = "play-forum"
  val appVersion      = "0.1.0-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    anorm,
    "com.h2database" % "h2" % "1.4.178",
    "org.apache.commons" % "commons-email" % "1.3.2",
    "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    libraryDependencies ++= appDependencies,
    TwirlKeys.templateImports += "views._helpers.tags._"
  )
}
