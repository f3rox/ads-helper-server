package services

import java.math.BigInteger
import java.net.URI
import java.security.SecureRandom

import actors.AuthActor.{GetUserAuthorizer, NewAuth}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.google.auth.oauth2.{ClientId, UserAuthorizer}
import javax.inject.{Inject, Named, Singleton}
import models.{AuthUserInfo, UserInfo}
import play.api.libs.ws.WSClient
import play.api.mvc.Session

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class GoogleAuth @Inject()(@Named("auth-actor") authActor: ActorRef, ws: WSClient, appConfig: AppConfig) {
  implicit val timeout: Timeout = 10.seconds

  def getUserInfo(accessToken: String): UserInfo = {
    Await.result(ws.url("https://www.googleapis.com/oauth2/v2/userinfo").addQueryStringParameters("access_token" -> accessToken).get().map { response =>
      val name = (response.json \ "name").as[String]
      val email = (response.json \ "email").as[String]
      val picture = (response.json \ "picture").as[String]
      val id = (response.json \ "id").as[String]
      UserInfo(id, name, email, picture)
    }, 10.seconds)
  }

  def getAuthUserInfoFromSession(session: Session): AuthUserInfo = {
    val sessionData = session.data
    AuthUserInfo(
      accessToken = sessionData.getOrElse("access_token", ""),
      refreshToken = sessionData.getOrElse("refresh_token", ""),
      id = sessionData.getOrElse("id", ""),
      name = sessionData.getOrElse("name", ""),
      email = sessionData.getOrElse("email", ""),
      picture = sessionData.getOrElse("picture", "")
    )
  }

  def getAuthUserInfoFromCallback(state: String, code: String): AuthUserInfo = {
    val userAuthorizer = getUserAuthorizer(state)
    val userCredentials = userAuthorizer.getCredentialsFromCode(code, URI.create(appConfig.getServerBaseUrl))
    val accessToken = userCredentials.getAccessToken.getTokenValue
    println("Access token: " + accessToken)
    val refreshToken = userCredentials.getRefreshToken
    val userInfo = getUserInfo(accessToken)
    val authUserInfo = userInfo.toAuthUserInfo(accessToken, refreshToken)
    authUserInfo
  }

  def getRedirectUrl: String = {
    val scopes = appConfig.getScopes.split(" ").seq.asJavaCollection
    val oAuth2Callback = appConfig.getOAuth2Callback
    val clientId = ClientId.newBuilder().setClientId(appConfig.getClientId).setClientSecret(appConfig.getClientSecret).build()
    val state = new BigInteger(130, new SecureRandom()).toString(32)
    val userAuthorizer = UserAuthorizer.newBuilder()
      .setClientId(clientId)
      .setScopes(scopes)
      .setCallbackUri(URI.create(oAuth2Callback))
      .build()
    val redirectURL = userAuthorizer.getAuthorizationUrl(null, state, URI.create(appConfig.getServerBaseUrl)).toString
    authActor ! NewAuth(state, userAuthorizer)
    redirectURL
  }

  def getUserAuthorizer(state: String): UserAuthorizer = Await.result((authActor ? GetUserAuthorizer(state)).mapTo[UserAuthorizer], 30.seconds)
}