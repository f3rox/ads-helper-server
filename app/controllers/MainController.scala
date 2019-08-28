package controllers

import java.nio.file.Paths

import actors.AdGroupsActor.AddAdGroups
import actors.CampaignActor.AddCampaign
import actors.CampaignBudgetActor.AddCampaignBudget
import actors.ExpandedTextAdsActor.AddExpandedTextAds
import actors.FileActor.ParseFile
import actors.GoogleAdsActor._
import actors.HelloActor.SayHello
import actors.KeywordsActor.AddKeywords
import actors.RootActor.{GetFileActor, GetGoogleAdsActor}
import akka.actor.{ActorRef, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import models.Product
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc._
import utils.{AppConfig, CsvParser, GoogleAds, GoogleAuth}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class MainController @Inject()(@Named("hello-actor") helloActor: ActorRef, @Named("root-actor") rootActor: ActorRef, googleAuth: GoogleAuth, csvParser: CsvParser, googleAds: GoogleAds, appConfig: AppConfig, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 10.minutes
  val rootPath: String = Paths.get("").toAbsolutePath.toString

  def index = Action.async {
    (helloActor ? SayHello).mapTo[String].map { message =>
      Ok(message)
    }
  }

  def auth = Action(Ok(googleAuth.getRedirectUrl))

  def deleteSession = Action(Ok.withNewSession)

  def oAuth2Callback = Action { request =>
    val state = request.getQueryString("state")
    val code = request.getQueryString("code")
    if (state.isDefined && code.isDefined) {
      val authUserInfo = googleAuth.getAuthUserInfoFromCallback(state.get, code.get)
      Redirect(appConfig.getClientBaseUrl).withSession(authUserInfo.toSession)
    }
    else BadRequest
  }

  def authUserInfo = Action { request =>
    val authUserInfo = googleAuth.getAuthUserInfoFromSession(request.session)
    if (authUserInfo.isDefined) Ok(authUserInfo.toJson)
    else Unauthorized
  }

  def upload = Action(parse.multipartFormData) { request =>
    val start = System.currentTimeMillis()
    val refreshToken = request.body.asFormUrlEncoded("refresh_token").head
    val managerCustomerId = 1169899225L
    val clientCustomerId = 2515161029L
    println(s"\nNEW REQUEST:\nRefresh token: $refreshToken")
    request.body.file("file").map { uploadedFile =>
      val fileActor = Await.result((rootActor ? GetFileActor(uploadedFile.filename)).mapTo[ActorRef], 10.minutes)
      val products = Await.result((fileActor ? ParseFile(uploadedFile.ref)).mapTo[List[Product]], 10.minutes)
      fileActor ! PoisonPill
      val googleAdsActor = Await.result((rootActor ? GetGoogleAdsActor(refreshToken, managerCustomerId, clientCustomerId)).mapTo[ActorRef], 10.minutes)
      val campaignBudgetActor = Await.result((googleAdsActor ? GetCampaignBudgetActor).mapTo[ActorRef], 10.minutes)
      val budgetResourceName = Await.result((campaignBudgetActor ? AddCampaignBudget(500000, s"CampaignBudget #${System.currentTimeMillis()}")).mapTo[String], 10.minutes)
      campaignBudgetActor ! PoisonPill
      val campaignActor = Await.result((googleAdsActor ? GetCampaignActor).mapTo[ActorRef], 10.minutes)
      val campaignResourceName = Await.result((campaignActor ? AddCampaign(budgetResourceName, s"Campaign #${System.currentTimeMillis()}")).mapTo[String], 10.minutes)
      campaignActor ! PoisonPill
      val adGroupsActor = Await.result((googleAdsActor ? GetAdGroupsActor).mapTo[ActorRef], 10.minutes)
      val adGroupsResourcesNames = Await.result((adGroupsActor ? AddAdGroups(campaignResourceName, products)).mapTo[List[String]], 10.minutes)
      adGroupsActor ! PoisonPill
      val productsWithAdGroups = products.zip(adGroupsResourcesNames)
      val keywordsActor = Await.result((googleAdsActor ? GetKeywordsActor).mapTo[ActorRef], 10.minutes)
      Await.ready(keywordsActor ? AddKeywords(productsWithAdGroups), 10.minutes)
      keywordsActor ! PoisonPill
      val expandedTextAdsActor = Await.result((googleAdsActor ? GetExpandedTextAdsActor).mapTo[ActorRef], 10.minutes)
      Await.ready(expandedTextAdsActor ? AddExpandedTextAds(productsWithAdGroups), 10.minutes)
      expandedTextAdsActor ! PoisonPill
      googleAdsActor ! PoisonPill
      println(s"Work finished in ${(System.currentTimeMillis() - start) / 1000.0} sec")
      Ok("File successfully uploaded")
    }.getOrElse(BadRequest("Upload error"))
  }
}