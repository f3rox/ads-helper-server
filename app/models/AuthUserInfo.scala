package models

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Session

case class AuthUserInfo(id: String, name: String, email: String, picture: String, accessToken: String, refreshToken: String) {
  def toJson: JsValue = Json.obj(
    "access_token" -> accessToken,
    "refresh_token" -> refreshToken,
    "id" -> id,
    "name" -> name,
    "email" -> email,
    "picture" -> picture
  )

  def toSession: Session = {
    val dataMap: Map[String, String] = Map(
      "id" -> id,
      "name" -> name,
      "email" -> email,
      "picture" -> picture,
      "access_token" -> accessToken,
      "refresh_token" -> refreshToken
    )
    Session(data = dataMap)
  }

  def isDefined: Boolean = name != "" && email != "" && picture != "" && accessToken != "" && refreshToken != ""
}