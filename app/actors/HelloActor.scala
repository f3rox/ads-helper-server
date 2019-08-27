package actors

import akka.actor.Actor

object HelloActor {

  sealed trait HelloActorMessage

  case object SayHello extends HelloActorMessage

}

class HelloActor extends Actor {

  import HelloActor._

  def receive: PartialFunction[Any, Unit] = {
    case SayHello =>
      sender() ! "Hello! Server is running."
  }
}