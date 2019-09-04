package tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

class Users(tag: Tag) extends Table[(Int, String, String, String, String)](tag, "USERS") {
  def id: Rep[Int] = column[Int]("USER_ID", O.PrimaryKey)

  def name: Rep[String] = column[String]("NAME")

  def city: Rep[String] = column[String]("CITY")

  def email: Rep[String] = column[String]("EMAIL")

  def role: Rep[String] = column[String]("ROLE")

  override def * : ProvenShape[(Int, String, String, String, String)] = (id, name, city, email, role)
}