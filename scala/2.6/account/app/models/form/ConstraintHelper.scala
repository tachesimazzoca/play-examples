package models.form

import org.apache.commons.lang3.StringUtils
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object ConstraintHelper {

  def passed[T](message: String = "error.passed"): Constraint[Boolean] =
    Constraint[Boolean]("constraint.passed") { x =>
      if (x) Valid else Invalid(message)
    }

  def nonBlank(message: String = "error.nonBlank"): Constraint[String] =
    Constraint[String]("constraint.nonBlank") { x =>
      if (!StringUtils.isBlank(x))
        Valid
      else
        Invalid(ValidationError(message))
    }

  def sameValue[T](message: String = "error.saveValue"): Constraint[(T, T)] =
    Constraint[(T, T)]("constraint.sameValue") { x =>
      if (x._1 == null && x._2 == null)
        Valid
      else if (x._1 != null && x._1.equals(x._2))
        Valid
      else
        Invalid(ValidationError(message))
    }

  private val EMAIL_PATTERN = ("""^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+""" +
    """@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?""" +
    """(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""").r

  def email(message: String = "error.email"): Constraint[String] =
    Constraint[String]("constraint.email") { x =>
      if (x == null) Invalid(ValidationError(message))
      else if (StringUtils.isBlank(x)) Invalid(ValidationError(message))
      else EMAIL_PATTERN.findFirstMatchIn(x)
        .map(_ => Valid)
        .getOrElse(Invalid(ValidationError(message)))
    }

  private val PASSWORD_PATTERN = """^[\u0021-\u007e]{8,255}$""".r

  def password[T](message: String = "error.password"): Constraint[String] =
    Constraint[String]("constraint.password") { x =>
      if (x == null) Invalid(ValidationError(message))
      else if (StringUtils.isBlank(x)) Invalid(ValidationError(message))
      else PASSWORD_PATTERN.findFirstMatchIn(x)
        .map(_ => Valid)
        .getOrElse(Invalid(ValidationError(message)))
    }
}
