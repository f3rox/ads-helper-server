package actors

import akka.actor.{Actor, ActorRef}
import javax.inject.Inject
import play.api.libs.concurrent.InjectedActorSupport

object RootActor {

  sealed trait RootActorMessage

  case class GetFileActor(fileName: String) extends RootActorMessage

  case class GetGoogleAdsActor(refreshToken: String, managerCustomerId: Long, clientCustomerId: Long) extends RootActorMessage

}

class RootActor @Inject()(fileActorFactory: FileActor.Factory, googleAdsActorFactory: GoogleAdsActor.Factory) extends Actor with InjectedActorSupport {

  import RootActor._

  override def receive: Receive = {
    case GetFileActor(fileName) =>
      val fileActor: ActorRef = injectedChild(fileActorFactory(fileName), s"file-actor-$fileName-${System.currentTimeMillis()}")
      sender() ! fileActor
    case GetGoogleAdsActor(refreshToken, managerCustomerId, clientCustomerId) =>
      val googleAdsActor: ActorRef = injectedChild(googleAdsActorFactory(refreshToken, managerCustomerId, clientCustomerId), s"google-ads-actor-$clientCustomerId-${System.currentTimeMillis()}")
      sender() ! googleAdsActor
  }
}