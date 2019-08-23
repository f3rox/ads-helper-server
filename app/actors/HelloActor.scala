package actors

import akka.actor.Actor

object HelloActor {

  case object SayHello

}

class HelloActor extends Actor {

  import HelloActor._

  def receive: PartialFunction[Any, Unit] = {
    case SayHello =>
      sender() ! "Hello! Server is running."
  }
}