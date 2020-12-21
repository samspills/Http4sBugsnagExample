package io.github.samspills.http4sbugsnagexample

import cats.effect.Sync
import cats.implicits._
import io.circe.{Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe._

trait ExampleExceptions[F[_]]{
  def error(code: ExampleExceptions.Code): F[String]
}

case class ForbiddenExample(m: String) extends Exception(m)
case class NotFoundExample(m: String) extends Exception(m)
case class NotImplementedExample(m: String) extends Exception(m)
case class UnavailableExample(m: String) extends Exception(m)

object ExampleExceptions {
  implicit def apply[F[_]](implicit ev: ExampleExceptions[F]): ExampleExceptions[F] = ev
  final case class Code(value: Int) extends AnyVal
  def impl[F[_]](implicit F: Sync[F]): ExampleExceptions[F] = new ExampleExceptions[F]{
    def error(code: ExampleExceptions.Code): F[String] = {
      code.value match {
      case 200 => "all good".pure[F]
      case 403 => F.raiseError(new ForbiddenExample("403 forbidden example"))
      case 501 => F.raiseError(new NotImplementedExample("501 not implemented example"))
      case 503 => F.raiseError(new UnavailableExample("503 unavailable example"))
      case _ => F.raiseError(new NotFoundExample("not found catch all"))
    }
  }
  }
}
