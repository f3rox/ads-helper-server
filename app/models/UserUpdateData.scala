package models

case class UserUpdateData(id: String, name: Option[String], email: Option[String], picture: Option[String]) {
  def isDefined: Boolean = name.isDefined || email.isDefined || picture.isDefined
}