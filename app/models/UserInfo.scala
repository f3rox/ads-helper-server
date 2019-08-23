package models

case class UserInfo(name: String, email: String, picture: String) {
  def toAuthUserInfo(accessToken: String, refreshToken: String): AuthUserInfo = {
    AuthUserInfo(
      name = name,
      email = email,
      picture = picture,
      accessToken = accessToken,
      refreshToken = refreshToken
    )
  }
}