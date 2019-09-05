package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import services.GoogleAds

object CampaignBudgetActor {

  sealed trait Message

  case class AddCampaignBudget(amount: Int, name: String) extends Message

  trait Factory {
    def apply(googleAdsClient: GoogleAdsClient, customerId: Long): Actor
  }

}

class CampaignBudgetActor @Inject()(@Assisted implicit val googleAdsClient: GoogleAdsClient, @Assisted implicit val customerId: Long, googleAds: GoogleAds) extends Actor {

  import actors.CampaignBudgetActor._

  override def receive: Receive = {
    case AddCampaignBudget(amount, name) =>
      sender() ! googleAds.addCampaignBudget(amount, name)
  }
}