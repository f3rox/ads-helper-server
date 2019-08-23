package actors

import akka.actor.Actor
import com.google.auth.oauth2.UserAuthorizer

import scala.collection.mutable

object AuthActor {

  case class NewAuth(state: String, userAuthorizer: UserAuthorizer)

  case class GetUserAuthorizer(state: String)

}

class AuthActor extends Actor {

  import AuthActor._

  val authList: mutable.HashMap[String, UserAuthorizer] = mutable.HashMap()

  override def receive = {
    case NewAuth(state: String, userAuthorizer: UserAuthorizer) => authList += (state -> userAuthorizer)
    case GetUserAuthorizer(state: String) => sender() ! authList.remove(state).get
  }
}