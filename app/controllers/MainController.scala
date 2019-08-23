package controllers

import java.nio.file.{Files, Paths}

import actors.HelloActor.SayHello
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.ads.googleads.lib.GoogleAdsClient
import javax.inject._
import play.api.mvc._
import utils.{AppConfig, CsvParser, GoogleAds, GoogleAuth}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class MainController @Inject()(@Named("hello-actor") helloActor: ActorRef, googleAuth: GoogleAuth, csvParser: CsvParser, googleAds: GoogleAds, appConfig: AppConfig, components: ControllerComponents) extends AbstractController(components) {
  implicit val timeout: Timeout = 10.seconds
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
    val refreshToken = request.body.asFormUrlEncoded("refresh_token").head
    println(s"\nNEW REQUEST:\nRefresh token: $refreshToken")
    request.body.file("file").map { uploadedFile =>
      println(s"File name: ${uploadedFile.filename}\nFile size: ${uploadedFile.fileSize / 1024.0 / 1024.0} MB")
      if (!Files.exists(Paths.get(rootPath + "/tmp"))) println(Files.createDirectory(Paths.get(rootPath + "/tmp")))
      val filePath = uploadedFile.ref.moveFileTo(Paths.get(s"$rootPath/tmp/${uploadedFile.filename}"), replace = true).toString
      println(s"File path: $filePath")
      val managerCustomerId = 1169899225L
      implicit val clientCustomerId: Long = 2515161029L
      implicit val googleAdsClient: GoogleAdsClient = googleAds.getGoogleAdsClient(refreshToken, managerCustomerId)
      val budgetResourceName: String = googleAds.addCampaignBudget(500000, s"Test CampaignBudget #${System.currentTimeMillis()}")
      val campaignResourceName = googleAds.addCampaign(budgetResourceName, s"Test Campaign #${System.currentTimeMillis()}")
      val products = csvParser.parseCsv(filePath)
      products.foreach { product =>
        val adGroupResourceName = googleAds.addAdGroup(campaignResourceName, s"Test AdGroup #${System.currentTimeMillis()}")
        val keywords = googleAds.addKeyword(adGroupResourceName, s"${product.category} ${product.name}")
        val ad = googleAds.addExpandedTextAd(adGroupResourceName, product)
      }
      Ok("File successfully uploaded")
    }.getOrElse(BadRequest("Upload error"))
  }
}