package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

@RunWith(classOf[JUnitRunner])
class MappingSuite extends FunSuite {

  case class User(name: String, age: Option[Int], activated: Boolean)

  case class Item(id: Option[Long], code: String, name: String, price: Int)

  case class Blog(id: Option[Long], title: String, body: String, publishedAt: Long, tags: List[String])

  case class Address(street: String, city: String)

  case class Profile(name: String, address: Address)

  test("User") {
    val m: Mapping[User] = mapping(
      "name" -> of[String],
      "age" -> optional(of[Int]),
      "activated" -> default(of[Boolean], true)
    )(User.apply)(User.unapply)

    m.bind(Map.empty) match {
      case Right(_) => fail("It must fail with an empty map.")
      case Left(errors) =>
        assert(1 === errors.size)
        assert("name" === errors(0).key)
        assert("error.required" === errors(0).message)
    }

    val u: User = m.bind(Map("name" -> "Foo", "age" -> "25")).right.get
    assert("Foo" === u.name)
    assert(Some(25) === u.age)
    assert(true === u.activated)
  }

  test("Item") {
    val m: Mapping[Item] = mapping(
      "id" -> optional(longNumber),
      "code" -> text.verifying(pattern( """[A-Z][0-9]{5}""".r)),
      "name" -> nonEmptyText,
      "price" -> number(min = 1, max = 100000)
    )(Item.apply)(Item.unapply)

    m.bind(Map(
      "id" -> "1",
      "code" -> "A12345",
      "name" -> "Play Framework",
      "price" -> "35"
    )) match {
      case Right(item) =>
        assert(Some(1L) === item.id)
        assert("A12345" === item.code)
        assert("Play Framework" === item.name)
        assert(35 === item.price)
      case Left(_) => fail("It must not fail with a valid map.")
    }

    m.bind(Map(
      "code" -> "A1234B",
      "name" -> "      ",
      "price" -> "0"
    )) match {
      case Right(_) => fail("It must fail with an empty map.")
      case Left(errors) =>
        assert(3 === errors.size)
        assert("code" === errors(0).key)
        assert("error.pattern" === errors(0).message)
        assert("name" === errors(1).key)
        assert("error.required" === errors(1).message)
        assert("price" === errors(2).key)
        assert("error.min" === errors(2).message)
    }
  }

  test("Blog") {
    val m = mapping(
      "id" -> optional(longNumber),
      "title" -> text,
      "meta" -> tuple(
        "body" -> text,
        "publishedAt" -> longNumber
      ),
      "tags" -> list(text)
    ) { (id, title, meta, tags) =>
      Blog(id, title, meta._1, meta._2, tags)
    } { (blog) =>
      Some((blog.id, blog.title, (blog.body, blog.publishedAt), blog.tags))
    }

    val obj = Blog(
      Some(12345L),
      "Play Framework",
      "I'm binding into Blog!",
      23456L,
      List("repeated", "tags")
    )
    val params = Map(
      "id" -> obj.id.get.toString,
      "title" -> obj.title,
      "meta.body" -> obj.body,
      "meta.publishedAt" -> obj.publishedAt.toString,
      "tags[0]" -> obj.tags(0),
      "tags[1]" -> obj.tags(1)
    )

    m.bind(params) match {
      case Right(blog) =>
        assert(obj === blog)
      case Left(_) => fail("It must not fail with a valid map.")
    }
    assert(params === m.unbind(obj))
  }

  test("curried apply/unapply") {
    val params = Map("name" -> "foo", "age" -> "15", "activated" -> "false")
    val m = mapping[User, String, Option[Int], Boolean](
      "name" -> of[String],
      "age" -> optional(of[Int]),
      "activated" -> default(of[Boolean], true)) _

    val f = m(_: (String, Option[Int], Boolean) => User)(User.unapply)
    assert(Right(User("foo!", Some(15), false)) ===
      f((name, age, activated) => User(name + "!", age, activated)).bind(params))
    assert(Right(User("foo", Some(25), false)) ===
      f((name, age, activated) => User(name, age.map(_ + 10), activated)).bind(params))

    val g = m(User.apply)(_: (User) => Option[(String, Option[Int], Boolean)])
    assert(params === g((user) => Some((user.name, user.age.map(_ - 10), user.activated)))
      .unbind(User("foo", Some(25), false)))
    assert(Map.empty[String, String] === g((user) => None).unbind(User("foo", Some(15), false)))
  }

  test("single") {
    val m = single("name" -> of[String])

    m.bind(Map("name" -> "Foo")) match {
      case Right(a) =>
        assert("Foo" === a)
      case Left(_) => fail("It must not fail with a valid map.")
    }
  }

  test("tuple") {
    val m = tuple(
      "name" -> of[String],
      "age" -> of[Int])

    m.bind(Map(
      "name" -> "Foo",
      "age" -> "123"
    )) match {
      case Right(a) =>
        assert("Foo" === a._1)
        assert(123 === a._2)
      case Left(_) => fail("It must not fail with a valid map.")
    }
  }

  test("nested value") {
    val m = mapping(
      "name" -> of[String],
      "address" -> mapping(
        "street" -> of[String],
        "city" -> of[String]
      )(Address.apply)(Address.unapply)
    )(Profile.apply)(Profile.unapply)

    m.bind(Map(
      "name" -> "Nested Value",
      "address.street" -> "1-2-3",
      "address.city" -> "Fukuoka"
    )) match {
      case Right(a) =>
        assert("Nested Value" === a.name)
        assert("1-2-3" === a.address.street)
        assert("Fukuoka" === a.address.city)
      case Left(_) => fail("It must not fail with a valid map.")
    }
  }

  test("repeated value") {
    val m = tuple(
      "numbers" -> list(of[Int]),
      "tags" -> seq(of[String])
    )

    m.bind(Map(
      "numbers[0]" -> "123",
      "numbers[1]" -> "456",
      "tags[0]" -> "scala",
      "tags[1]" -> "play",
      "tags[2]" -> "framework"
    )) match {
      case Right(a) =>
        assert(List(123, 456) === a._1)
        assert(Seq("scala", "play", "framework") === a._2)
      case Left(_) => fail("It must not fail with a valid map.")
    }
  }
}
