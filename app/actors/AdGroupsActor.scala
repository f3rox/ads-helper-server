package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import models.Product
import utils.GoogleAds

object AdGroupsActor {

  sealed trait Message

  case class AddAdGroups(campaignResourceName: String, products: List[Product]) extends Message

  trait Factory {
    def apply(googleAdsClient: GoogleAdsClient, customerId: Long): Actor
  }

}

class AdGroupsActor @Inject()(@Assisted implicit val googleAdsClient: GoogleAdsClient, @Assisted implicit val customerId: Long, googleAds: GoogleAds) extends Actor {

  import actors.AdGroupsActor._

  override def receive: Receive = {
    case AddAdGroups(campaignResourceName, products) =>
      sender() ! googleAds.addAdGroups(campaignResourceName, products)
  }
}