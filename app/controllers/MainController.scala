package controllers

import java.nio.file.Paths

import actors.HelloActor.SayHello
import actors.UploadActor.CreateCampaign
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc._
import utils.{AppConfig, GoogleAuth}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class MainController @Inject()(@Named("hello-actor") helloActor: ActorRef, @Named("upload-actor") uploadActor: ActorRef, googleAuth: GoogleAuth, appConfig: AppConfig, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
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

  def upload = Action.async(parse.multipartFormData) { request =>
    val refreshToken = request.body.asFormUrlEncoded("refresh_token").head
    val managerCustomerId = 1169899225L
    val clientCustomerId = 2515161029L
    val uploadedFile = request.body.file("file").get
    (uploadActor ? CreateCampaign(uploadedFile.ref, uploadedFile.filename, refreshToken, managerCustomerId, clientCustomerId)).mapTo[Result]
  }
}