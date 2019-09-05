package tables

import play.api.libs.json.{JsValue, Json}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

case class User(id: Int, name: String, email: String, picture: String) {
  def toJson: JsValue = Json.obj(
    "id" -> id,
    "name" -> name,
    "email" -> email,
    "picture" -> picture
  )
}

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id: Rep[Int] = column[Int]("USER_ID", O.PrimaryKey)

  def name: Rep[String] = column[String]("NAME")

  def email: Rep[String] = column[String]("EMAIL")

  def picture: Rep[String] = column[String]("PICTURE")

  override def * : ProvenShape[User] = (id, name, email, picture).mapTo[User]
}