package services

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class AppConfig @Inject()(config: Configuration) {
  def getClientId: String = config.get[String]("google.ads.clientId")

  def getClientSecret: String = config.get[String]("google.ads.clientSecret")

  def getDeveloperToken: String = config.get[String]("google.ads.developerToken")

  def getScopes: String = config.get[String]("google.ads.scopes")

  def getOAuth2Callback: String = config.get[String]("google.ads.oAuth2Callback")

  def getServerBaseUrl: String = config.get[String]("server.baseUrl")

  def getClientBaseUrl: String = config.get[String]("client.baseUrl")
}