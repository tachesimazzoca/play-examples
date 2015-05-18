package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.HeaderNames._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._
import play.filters.gzip._

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class FilterSuite extends FunSuite with ScalaFutures with OneAppPerSuite {
  private val bytesConsumer = Iteratee.consume[Array[Byte]]()

  private val GUNZIPPED_BYTES = ("a" * 100).getBytes
  private val GZIPPED_BYTES = Array(
    0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x4b, 0x4c, 0xa4, 0x3d,
    0x00, 0x00, 0x64, 0x7a,
    0x70, 0xaf, 0x64, 0x00,
    0x00, 0x00).map(_.toByte)

  private def bytesToStr(bytes: Array[Byte]): String = bytes.map(_.toChar).mkString("")

  test("Filter") {
    val sessionKey = "SESSION_ID"

    val filter = Filter { (f, rh) =>
      f(rh).map { result =>
        if (rh.cookies.get(sessionKey).isEmpty) {
          result.withCookies(Cookie(sessionKey, java.util.UUID.randomUUID().toString))
        }
        else result
      }
    }

    val action = Action { request =>
      val sessionId = request.cookies.get(sessionKey).map(_.value).getOrElse("")
      Ok("sessionId: " ++ sessionId)
    }

    whenReady(for {
      result <- filter(action)(FakeRequest()).run
      bytes <- result.body |>>> bytesConsumer
    } yield (result, bytesToStr(bytes))) { t =>
      assert(t._1.header.headers.get("Set-Cookie").isDefined)
      assert("sessionId: " === t._2)
    }

    val req = FakeRequest().withCookies(Cookie(sessionKey, "deadbeef"))
    whenReady(for {
      result <- filter(action)(req).run
      bytes <- result.body |>>> bytesConsumer
    } yield (result, bytesToStr(bytes))) { t =>
      assert(t._1.header.headers.get("Set-Cookie").isEmpty)
      assert("sessionId: deadbeef" === t._2)
    }
  }

  test("GzipFilter") {
    val filter = new GzipFilter(512)
    val action = Action {
      Ok(bytesToStr(GUNZIPPED_BYTES))
    }

    val req = FakeRequest().withHeaders(ACCEPT_ENCODING -> "gzip")
    whenReady(for {
      result <- filter(action)(req).run
      bytes <- result.body |>>> bytesConsumer
    } yield (result, bytes)) { t =>
      assert(Some("gzip") === t._1.header.headers.get(CONTENT_ENCODING))
      assert(GZIPPED_BYTES === t._2)
    }
  }

  test("EssentialFilter") {
    val filter = new EssentialFilter {
      override def apply(next: EssentialAction) = new EssentialAction {
        override def apply(rh: RequestHeader): Iteratee[Array[Byte], Result] = {
          if (rh.headers.get(CONTENT_ENCODING).exists(_ === "gzip")) {
            Gzip.gunzip() &>> next(rh)
          } else next(rh)
        }
      }
    }

    val action = Action(parse.text) { request =>
      Ok(request.body)
    }

    val textReq = FakeRequest().withHeaders(CONTENT_TYPE -> "text/plain")
    whenReady(for {
      result <- Enumerator(GUNZIPPED_BYTES) |>>> filter(action)(textReq)
      bytes <- result.body |>>> bytesConsumer
    } yield (result, bytes)) { t =>
      assert(GUNZIPPED_BYTES === t._2)
    }

    val gzippedReq = FakeRequest().withHeaders(
      CONTENT_ENCODING -> "gzip",
      CONTENT_TYPE -> "text/plain"
    )
    whenReady(for {
      result <- Enumerator(GZIPPED_BYTES) |>>> filter(action)(gzippedReq)
      bytes <- result.body |>>> bytesConsumer
    } yield (result, bytes)) { t =>
      assert(GUNZIPPED_BYTES === t._2)
    }
  }
}
