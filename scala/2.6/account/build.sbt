name := "play-examples-account"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "org.playframework.anorm" %% "anorm" % "2.6.2",
  "com.h2database" % "h2" % "1.4.178",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "commons-codec" % "commons-codec" % "1.10",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % "test"
)

javaOptions in Test += "-Dconfig.file=conf/test/application.conf"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
