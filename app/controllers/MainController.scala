package controllers

import actors.HelloActor.SayHello
import actors.UploadActor.{CreateCampaign, CreateCampaigns}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc._
import services.{AppConfig, GoogleAuth}
import utils.Forms.{formErrorsHandler, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class MainController @Inject()(@Named("hello-actor") helloActor: ActorRef, @Named("upload-actor") uploadActor: ActorRef, googleAuth: GoogleAuth, appConfig: AppConfig, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 1.minute

  def index = Action.async {
    (helloActor ? SayHello).mapTo[String].map(Ok(_))
  }

  def auth = Action(Ok(googleAuth.getRedirectUrl))

  def deleteSession = Action(Ok.withNewSession)

  def oAuth2Callback = Action.async { request =>
    val state = request.getQueryString("state")
    val code = request.getQueryString("code")
    if (state.isDefined && code.isDefined) {
      googleAuth.getAuthUserInfoFromCallback(state.get, code.get).map(authUser => Redirect(appConfig.getClientBaseUrl).withSession(authUser.toSession))
    }
    else Future.successful(BadRequest)
  }

  def authUserData = Action { request =>
    val authUserData = googleAuth.getAuthUserDataFromSession(request.session)
    if (authUserData.isDefined) Ok(authUserData.toJson)
    else Unauthorized
  }

  def upload = Action.async(parse.multipartFormData) { request =>
    val authUser = googleAuth.getAuthUserDataFromSession(request.session)
    customerIDsForm.bindFromRequest(request.body.asFormUrlEncoded).fold(
      formErrorsHandler,
      customerIDs => {
        request.body.file("file").map(uploadedFile =>
          (uploadActor ? CreateCampaign(uploadedFile.ref, uploadedFile.filename, authUser, customerIDs.managerCustomerId, customerIDs.clientCustomerId)).mapTo[Result]
        ).getOrElse(Future.successful(BadRequest("file is missed")))
      })
  }

  def createCampaigns = Action.async(parse.multipartFormData) { request =>
    val start = System.currentTimeMillis()
    val authUser = googleAuth.getAuthUserDataFromSession(request.session)
    val managerCustomerID = 1169899225L
    val clientCustomerIDs = List(2515161029L, 8046022333L, 1293349074L, 4861434519L, 9854279244L)
    val result = request.body.file("file").map(uploadedFile =>
      (uploadActor ? CreateCampaigns(uploadedFile.ref, uploadedFile.filename, authUser, managerCustomerID, clientCustomerIDs)).mapTo[Result]
    ).getOrElse(Future.successful(BadRequest("file is missed")))
    result.onComplete(res => println(s"\nWORK FINISHED IN ${(System.currentTimeMillis() - start) / 1000.0} SEC\n$res\n"))
    result
  }
}