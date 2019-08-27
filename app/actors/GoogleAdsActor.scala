package actors

import akka.actor.Actor
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import play.api.libs.concurrent.InjectedActorSupport
import utils.GoogleAds

object GoogleAdsActor {

  sealed trait GoogleAdsActorMessage

  case object GetCampaignBudgetActor extends GoogleAdsActorMessage

  case object GetCampaignActor extends GoogleAdsActorMessage

  case object GetAdGroupsActor extends GoogleAdsActorMessage

  case object GetKeywordsActor extends GoogleAdsActorMessage

  case object GetExpandedTextAdsActor extends GoogleAdsActorMessage

  trait Factory {
    def apply(refreshToken: String, @Assisted("manager") managerCustomerId: Long, @Assisted("client") clientCustomerId: Long): Actor
  }

}

class GoogleAdsActor @Inject()(
                                @Assisted refreshToken: String,
                                @Assisted("manager") managerCustomerId: Long,
                                @Assisted("client") clientCustomerId: Long,
                                campaignBudgetActorFactory: CampaignBudgetActor.Factory,
                                campaignActorFactory: CampaignActor.Factory,
                                adGroupsActorFactory: AdGroupsActor.Factory,
                                keywordsActorFactory: KeywordsActor.Factory,
                                expandedTextAdsActorFactory: ExpandedTextAdsActor.Factory,
                                googleAds: GoogleAds
                              ) extends Actor with InjectedActorSupport {

  import GoogleAdsActor._

  lazy val googleAdsClient: GoogleAdsClient = googleAds.getGoogleAdsClient(refreshToken, managerCustomerId)

  override def receive: Receive = {
    case GetCampaignBudgetActor =>
      sender() ! injectedChild(campaignBudgetActorFactory(googleAdsClient, clientCustomerId), s"campaign-budget-actor-${clientCustomerId.toString}")
    case GetCampaignActor =>
      sender() ! injectedChild(campaignActorFactory(googleAdsClient, clientCustomerId), s"campaign-actor-${clientCustomerId.toString}")
    case GetAdGroupsActor =>
      sender() ! injectedChild(adGroupsActorFactory(googleAdsClient, clientCustomerId), s"ad-groups-actor-${clientCustomerId.toString}")
    case GetKeywordsActor =>
      sender() ! injectedChild(keywordsActorFactory(googleAdsClient, clientCustomerId), s"keywords-actor-${clientCustomerId.toString}")
    case GetExpandedTextAdsActor =>
      sender() ! injectedChild(expandedTextAdsActorFactory(googleAdsClient, clientCustomerId), s"expanded-text-ads-actor-${clientCustomerId.toString}")
  }
}