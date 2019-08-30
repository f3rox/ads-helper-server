package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import utils.GoogleAds

object CampaignActor {

  sealed trait Message

 case class AddCampaign(budgetResourceName: String, name: String) extends Message

  trait Factory {
    def apply(googleAdsClient: GoogleAdsClient, customerId: Long): Actor
  }

}

class CampaignActor @Inject()(@Assisted implicit val googleAdsClient: GoogleAdsClient, @Assisted implicit val customerId: Long, googleAds: GoogleAds) extends Actor {

  import actors.CampaignActor._

  override def receive: Receive = {
    case AddCampaign(budgetResourceName, name) =>
      sender() ! googleAds.addCampaign(budgetResourceName, name)
  }
}