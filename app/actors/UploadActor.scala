package actors

import actors.AdGroupsActor.AddAdGroups
import actors.CampaignActor.AddCampaign
import actors.CampaignBudgetActor.AddCampaignBudget
import actors.DatabaseActor.AddUserWithCampaign
import actors.ExpandedTextAdsActor.AddExpandedTextAds
import actors.FileActor.ParseFile
import actors.GoogleAdsActor._
import actors.KeywordsActor.AddKeywords
import actors.RootActor.{GetFileActor, GetGoogleAdsActor}
import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named}
import models.{AuthUser, Campaign, Product}
import monads.FutureEither
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object UploadActor {

  sealed trait Message

  case class CreateCampaign(uploadedFile: TemporaryFile, fileName: String, authUser: AuthUser, managerCustomerId: Long, clientCustomerId: Long) extends Message

}

class UploadActor @Inject()(@Named("root-actor") rootActor: ActorRef, @Named("database-actor") databaseActor: ActorRef) extends Actor {

  import UploadActor._

  implicit val timeout: Timeout = 1.minute

  override def receive: Receive = {
    case CreateCampaign(uploadedFile, fileName, authUser, managerCustomerId, clientCustomerId) =>
      val productsFuture = (rootActor ? GetFileActor(fileName)).mapTo[ActorRef].flatMap { fileActor =>
        (fileActor ? ParseFile(uploadedFile)).mapTo[Either[Throwable, List[Product]]]
      }
      val googleAdsActorFuture = (rootActor ? GetGoogleAdsActor(authUser.refreshToken, managerCustomerId, clientCustomerId)).mapTo[ActorRef]
      googleAdsActorFuture.flatMap { googleAdsActor =>
        val campaignBudgetActorFuture = (googleAdsActor ? GetCampaignBudgetActor).mapTo[ActorRef]
        val campaignActorFuture = (googleAdsActor ? GetCampaignActor).mapTo[ActorRef]
        val adGroupsActorFuture = (googleAdsActor ? GetAdGroupsActor).mapTo[ActorRef]
        val keywordsActorFuture = (googleAdsActor ? GetKeywordsActor).mapTo[ActorRef]
        val expandedTextAdsActorFuture = (googleAdsActor ? GetExpandedTextAdsActor).mapTo[ActorRef]
        val futureEither = for {
          budgetResourceName <- FutureEither(campaignBudgetActorFuture.flatMap { campaignBudgetActor =>
            (campaignBudgetActor ? AddCampaignBudget(500000, s"Budget #${System.currentTimeMillis()}")).mapTo[Either[Throwable, String]]
          })
          campaignResourceName <- FutureEither(campaignActorFuture.flatMap { campaignActor =>
            (campaignActor ? AddCampaign(budgetResourceName, s"Campaign #${System.currentTimeMillis()}")).mapTo[Either[Throwable, String]]
          })
          products <- FutureEither(productsFuture)
          adGroupsResourcesNames <- FutureEither(adGroupsActorFuture.flatMap { adGroupsActor =>
            (adGroupsActor ? AddAdGroups(campaignResourceName, products)).mapTo[Either[Throwable, List[String]]]
          })
          productsWithAdGroups = products.zip(adGroupsResourcesNames)
          _ <- FutureEither(keywordsActorFuture.flatMap { keywordsActor =>
            (keywordsActor ? AddKeywords(productsWithAdGroups)).mapTo[Either[Throwable, List[String]]]
          })
          _ <- FutureEither(expandedTextAdsActorFuture.flatMap { expandedTextAdsActor =>
            (expandedTextAdsActor ? AddExpandedTextAds(productsWithAdGroups)).mapTo[Either[Throwable, List[String]]]
          })
          user = authUser.toUser
          campaign = Campaign(campaignResourceName, user.id, clientCustomerId, products.size)
          _ = databaseActor ! AddUserWithCampaign(user, campaign)
        } yield "Campaign created"
        futureEither.value
      }.map {
        case Right(message) => Ok(message)
        case Left(exception) => InternalServerError(exception.getMessage)
      }.pipeTo(sender())
  }
}