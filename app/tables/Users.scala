package tables

import models.User
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

class Users(tag: Tag) extends Table[User](tag, "USERS") {
  def id: Rep[String] = column[String]("USER_ID", O.PrimaryKey)

  def name: Rep[String] = column[String]("NAME")

  def email: Rep[String] = column[String]("EMAIL")

  def picture: Rep[String] = column[String]("PICTURE")

  override def * : ProvenShape[User] = (id, name, email, picture).mapTo[User]
}