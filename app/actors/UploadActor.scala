package actors

import actors.AdGroupsActor.AddAdGroups
import actors.CampaignActor.AddCampaign
import actors.CampaignBudgetActor.AddCampaignBudget
import actors.ExpandedTextAdsActor.AddExpandedTextAds
import actors.FileActor.ParseFile
import actors.GoogleAdsActor._
import actors.KeywordsActor.AddKeywords
import actors.RootActor.{GetFileActor, GetGoogleAdsActor}
import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named}
import models.Product
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object UploadActor {

  sealed trait Message

  case class CreateCampaign(uploadedFile: TemporaryFile, fileName: String, refreshToken: String, managerCustomerId: Long, clientCustomerId: Long) extends Message

}

class UploadActor @Inject()(@Named("root-actor") rootActor: ActorRef) extends Actor {

  import UploadActor._

  implicit val timeout: Timeout = 1.minute

  override def receive: Receive = {
    case CreateCampaign(uploadedFile, fileName, refreshToken, managerCustomerId, clientCustomerId) =>
      val productsFuture = (rootActor ? GetFileActor(fileName)).mapTo[ActorRef].flatMap { fileActor =>
        (fileActor ? ParseFile(uploadedFile)).mapTo[List[Product]]
      }
      val googleAdsActorFuture = (rootActor ? GetGoogleAdsActor(refreshToken, managerCustomerId, clientCustomerId)).mapTo[ActorRef]
      googleAdsActorFuture.flatMap { googleAdsActor =>
        val campaignBudgetActorFuture = (googleAdsActor ? GetCampaignBudgetActor).mapTo[ActorRef]
        val campaignActorFuture = (googleAdsActor ? GetCampaignActor).mapTo[ActorRef]
        val adGroupsActorFuture = (googleAdsActor ? GetAdGroupsActor).mapTo[ActorRef]
        val keywordsActorFuture = (googleAdsActor ? GetKeywordsActor).mapTo[ActorRef]
        val expandedTextAdsActorFuture = (googleAdsActor ? GetExpandedTextAdsActor).mapTo[ActorRef]
        for {
          budgetResourceName <- campaignBudgetActorFuture.flatMap { campaignBudgetActor =>
            (campaignBudgetActor ? AddCampaignBudget(500000, s"CampaignBudget #${System.currentTimeMillis()}")).mapTo[String]
          }
          campaignResourceName <- campaignActorFuture.flatMap { campaignActor =>
            (campaignActor ? AddCampaign(budgetResourceName, s"Campaign #${System.currentTimeMillis()}")).mapTo[String]
          }
          products <- productsFuture
          adGroupsResourcesNames <- adGroupsActorFuture.flatMap { adGroupsActor =>
            (adGroupsActor ? AddAdGroups(campaignResourceName, products)).mapTo[List[String]]
          }
          productsWithAdGroups = products.zip(adGroupsResourcesNames)
          _ <- keywordsActorFuture.flatMap { keywordsActor =>
            keywordsActor ? AddKeywords(productsWithAdGroups)
          }
          _ <- expandedTextAdsActorFuture.flatMap { expandedTextAdsActor =>
            expandedTextAdsActor ? AddExpandedTextAds(productsWithAdGroups)
          }
        } yield Created("Campaign created")
      }.fallbackTo(Future.successful(InternalServerError("Error"))).pipeTo(sender())
  }
}