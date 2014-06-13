import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

  val appName         = "play-anorm"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    anorm,
    //"mysql" % "mysql-connector-java" % "5.1.21",
    "com.h2database" % "h2" % "1.4.178",
    "org.scalatestplus" % "play_2.10" % "1.0.0" % "test"
  )

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    libraryDependencies ++= appDependencies
  )
}
