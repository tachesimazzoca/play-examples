import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play-forum"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    jdbc,
    anorm,
    "mysql" % "mysql-connector-java" % "5.1.21",
    "org.apache.commons" % "commons-email" % "1.3.2"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    libraryDependencies ++= Seq(
      "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
    )
  )
}
