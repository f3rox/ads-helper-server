package actors

import akka.actor.Actor

object HelloActor {

  sealed trait Message

  case object SayHello extends Message

}

class HelloActor extends Actor {

  import HelloActor._

  def receive: PartialFunction[Any, Unit] = {
    case SayHello =>
      sender() ! "Hello! Server is running."
  }
}