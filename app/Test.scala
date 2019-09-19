import cats.data.EitherT
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object Test extends App {
  def parseInt(s: String): Future[Either[Throwable, Int]] =
    Future {
      Thread.sleep(1000)
      Try(s.toInt).toEither
    }

  val x = List("1", "2", "3").traverse(s => EitherT(parseInt(s)))
  x.map(_.foreach(println))
  Thread.sleep(3000)
}