package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import models.Product
import utils.GoogleAds

object ExpandedTextAdsActor {

  sealed trait Message

  case class AddExpandedTextAds(productsWithAdGroups: List[(Product, String)]) extends Message

  trait Factory {
    def apply(googleAdsClient: GoogleAdsClient, customerId: Long): Actor
  }

}

class ExpandedTextAdsActor @Inject()(@Assisted implicit val googleAdsClient: GoogleAdsClient, @Assisted implicit val customerId: Long, googleAds: GoogleAds) extends Actor {

  import actors.ExpandedTextAdsActor._

  override def receive: Receive = {
    case AddExpandedTextAds(productsWithAdGroups) =>
      sender() ! googleAds.addExpandedTextAds(productsWithAdGroups)
  }
}