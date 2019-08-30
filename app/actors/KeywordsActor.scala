package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import models.Product
import utils.GoogleAds

object KeywordsActor {

  sealed trait Message

  case class AddKeywords(productsWithAdGroups: List[(Product, String)]) extends Message

  trait Factory {
    def apply(googleAdsClient: GoogleAdsClient, customerId: Long): Actor
  }

}

class KeywordsActor @Inject()(@Assisted implicit val googleAdsClient: GoogleAdsClient, @Assisted implicit val customerId: Long, googleAds: GoogleAds) extends Actor {

  import actors.KeywordsActor._

  override def receive: Receive = {
    case AddKeywords(productsWithAdGroups) =>
      sender() ! googleAds.addKeywords(productsWithAdGroups)
  }
}