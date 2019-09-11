package models

import play.api.libs.json.{JsValue, Json}

case class User(id: String, name: String, email: String, picture: String) {
  def toJson: JsValue = Json.obj(
    "id" -> id,
    "name" -> name,
    "email" -> email,
    "picture" -> picture
  )

  def toAuthUser(accessToken: String, refreshToken: String): AuthUser = {
    AuthUser(
      id = id,
      name = name,
      email = email,
      picture = picture,
      accessToken = accessToken,
      refreshToken = refreshToken
    )
  }
}