package monads

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class FutureEither[A](value: Future[Either[Throwable, A]]) {
  def map[B](f: A => B): FutureEither[B] = FutureEither(value.map(_.map(f)))

  def flatMap[B](f: A => FutureEither[B]) =
    FutureEither(value.flatMap {
      case Right(eitherValue) => f(eitherValue).value
      case Left(exception) => Future.successful(Left(exception))
    })
}