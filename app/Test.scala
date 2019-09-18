object Test extends App {

  import cats.instances.either._
  import cats.instances.list._
  import cats.syntax.traverse._

  val seqCorrect: List[Either[String, Int]] = List(Right(1), Right(2), Right(3))

  val result1 = seqCorrect.sequence
  println(result1)
}