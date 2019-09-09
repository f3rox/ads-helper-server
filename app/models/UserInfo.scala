package models

case class UserInfo(id: String, name: String, email: String, picture: String) {
  def toAuthUserInfo(accessToken: String, refreshToken: String): AuthUserInfo = {
    AuthUserInfo(
      id = id,
      name = name,
      email = email,
      picture = picture,
      accessToken = accessToken,
      refreshToken = refreshToken
    )
  }
}