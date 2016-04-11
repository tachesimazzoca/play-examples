package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.{Mode, Environment}

@RunWith(classOf[JUnitRunner])
class EnvironmentSuite extends FunSuite {

  test("simple") {
    val env = Environment.simple()
    assert(env.rootPath.getPath === ".")
    assert(env.classLoader === Environment.getClass.getClassLoader)
    assert(env.mode === Mode.Test)
  }

  test("resource") {
    val env = Environment.simple()
    assert(env.resource("test/a.txt").isDefined)
    assert(env.resource("/test/a.txt").isDefined)
  }

  test("resourceAsStream") {
    val env = Environment.simple()
    val input = env.resourceAsStream("test/a.txt").get
    val bytes = Iterator.continually(input.read).takeWhile(_ != -1).map(_.toByte).toArray
    assert("Are you looking for this?" === new String(bytes, "UTF-8").trim)
  }
}
