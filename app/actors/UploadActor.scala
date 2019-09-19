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
import cats.data.EitherT
import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._
import javax.inject.{Inject, Named}
import models.{AuthUser, Campaign, Product}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Results._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object UploadActor {

  sealed trait Message

  case class CreateCampaign(uploadedFile: TemporaryFile, fileName: String, authUser: AuthUser, managerCustomerId: Long, clientCustomerId: Long) extends Message

  case class CreateCampaigns(uploadedFile: TemporaryFile, fileName: String, authUser: AuthUser, managerCustomerId: Long, clientCustomerIds: List[Long]) extends Message

}

class UploadActor @Inject()(@Named("root-actor") rootActor: ActorRef, @Named("database-actor") databaseActor: ActorRef) extends Actor {

  import UploadActor._

  implicit val timeout: Timeout = 1.minute

  def getProductsList(uploadedFile: TemporaryFile, fileName: String): EitherT[Future, Throwable, List[Product]] =
    EitherT((rootActor ? GetFileActor(fileName)).mapTo[ActorRef].flatMap { fileActor =>
      (fileActor ? ParseFile(uploadedFile)).mapTo[Either[Throwable, List[Product]]]
    })

  def createCampaignForCustomer(products: List[Product], authUser: AuthUser, managerCustomerId: Long, clientCustomerId: Long): EitherT[Future, Throwable, String] =
    EitherT {
      val googleAdsActorFuture = (rootActor ? GetGoogleAdsActor(authUser.refreshToken, managerCustomerId, clientCustomerId)).mapTo[ActorRef]
      googleAdsActorFuture.flatMap { googleAdsActor =>
        val campaignBudgetActorFuture = (googleAdsActor ? GetCampaignBudgetActor).mapTo[ActorRef]
        val campaignActorFuture = (googleAdsActor ? GetCampaignActor).mapTo[ActorRef]
        val adGroupsActorFuture = (googleAdsActor ? GetAdGroupsActor).mapTo[ActorRef]
        val keywordsActorFuture = (googleAdsActor ? GetKeywordsActor).mapTo[ActorRef]
        val expandedTextAdsActorFuture = (googleAdsActor ? GetExpandedTextAdsActor).mapTo[ActorRef]
        val futureEither = for {
          budgetResourceName <- EitherT(campaignBudgetActorFuture.flatMap { campaignBudgetActor =>
            (campaignBudgetActor ? AddCampaignBudget(500000, s"Budget #${System.currentTimeMillis()}")).mapTo[Either[Throwable, String]]
          })
          campaignResourceName <- EitherT(campaignActorFuture.flatMap { campaignActor =>
            (campaignActor ? AddCampaign(budgetResourceName, s"Campaign #${System.currentTimeMillis()}")).mapTo[Either[Throwable, String]]
          })
          adGroupsResourcesNames <- EitherT(adGroupsActorFuture.flatMap { adGroupsActor =>
            (adGroupsActor ? AddAdGroups(campaignResourceName, products)).mapTo[Either[Throwable, List[String]]]
          })
          productsWithAdGroups = products.zip(adGroupsResourcesNames)
          _ <- EitherT(keywordsActorFuture.flatMap { keywordsActor =>
            (keywordsActor ? AddKeywords(productsWithAdGroups)).mapTo[Either[Throwable, List[String]]]
          })
          _ <- EitherT(expandedTextAdsActorFuture.flatMap { expandedTextAdsActor =>
            (expandedTextAdsActor ? AddExpandedTextAds(productsWithAdGroups)).mapTo[Either[Throwable, List[String]]]
          })
          user = authUser.toUser
          campaign = Campaign(campaignResourceName, user.id, clientCustomerId, products.size)
          _ = databaseActor ! AddUserWithCampaign(user, campaign)
        } yield campaignResourceName
        futureEither.value
      }
    }

  override def receive: Receive = {
    case CreateCampaigns(uploadedFile, fileName, authUser, managerCustomerId, clientCustomerIDs) =>
      val result = for {
        productsList <- getProductsList(uploadedFile, fileName)
        createdCampaigns <- clientCustomerIDs.traverse(clientCustomerID =>
          createCampaignForCustomer(productsList, authUser, managerCustomerId, clientCustomerID))
      } yield createdCampaigns
      result.value.map {
        case Right(createdCampaignsList) => Ok("Created campaigns:\n" + createdCampaignsList.mkString("\n"))
        case Left(exception) => InternalServerError(exception.getMessage)
      }.pipeTo(sender())
  }
}