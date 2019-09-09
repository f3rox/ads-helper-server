package controllers

import actors.HelloActor.SayHello
import actors.UploadActor.CreateCampaign
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc._
import services.{AppConfig, GoogleAuth}
import utils.Utils.formErrorsHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class MainController @Inject()(@Named("hello-actor") helloActor: ActorRef, @Named("upload-actor") uploadActor: ActorRef, googleAuth: GoogleAuth, appConfig: AppConfig, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 1.minute

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

  def upload = Action.async(parse.multipartFormData) { request =>
    request.session.data.get("refresh_token").map(refreshToken => {
      case class CustomerIDs(managerCustomerId: Long, clientCustomerId: Long)
      val customerIDsForm: Form[CustomerIDs] = Form(
        mapping(
          "managerCustomerId" -> longNumber,
          "clientCustomerId" -> longNumber
        )(CustomerIDs.apply)(CustomerIDs.unapply)
      )
      customerIDsForm.bindFromRequest(request.body.asFormUrlEncoded).fold(
        formErrorsHandler,
        customerIDs => {
          request.body.file("file").map(uploadedFile => (uploadActor ? CreateCampaign(uploadedFile.ref, uploadedFile.filename, refreshToken, customerIDs.managerCustomerId, customerIDs.clientCustomerId)).mapTo[Result]
          ).getOrElse(Future.successful(BadRequest("file is missed")))
        })
    }).getOrElse(Future.successful(Unauthorized))
  }
}