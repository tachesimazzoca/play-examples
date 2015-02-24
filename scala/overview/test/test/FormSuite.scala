package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

@RunWith(classOf[JUnitRunner])
class FormSuite extends FunSuite {

  case class User(name: String, age: Option[Int])

  case class Item(id: Option[Long], code: String, name: String, price: Int)

  case class Address(street: String, city: String)

  case class Profile(name: String, address: Address)

  test("bind User") {
    val m: Mapping[User] = mapping(
      "name" -> of[String],
      "age" -> optional(of[Int])
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
  }

  test("bind Item") {
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

  test("curried apply/unapply") {
    val params = Map("name" -> "foo", "age" -> "15")
    val m = mapping[User, String, Option[Int]](
      "name" -> of[String],
      "age" -> optional(of[Int])) _

    val f = m(_: (String, Option[Int]) => User)(User.unapply)
    assert(Right(User("foo!", Some(15))) === f((name, age) => User(name + "!", age)).bind(params))
    assert(Right(User("foo", Some(25))) === f((name, age) => User(name, age.map(_ + 10))).bind(params))

    val g = m(User.apply)(_: (User) => Option[(String, Option[Int])])
    assert(params === g((user) => Some((user.name, user.age.map(_ - 10)))).unbind(User("foo", Some(25))))
    assert(Map.empty[String, String] === g((user) => None).unbind(User("foo", Some(15))))
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
      "name" -> "Nested Tuple",
      "address.street" -> "1-2-3",
      "address.city" -> "Fukuoka"
    )) match {
      case Right(a) =>
        assert("Nested Tuple" === a.name)
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
