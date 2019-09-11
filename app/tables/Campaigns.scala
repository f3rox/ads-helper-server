package tables

import models.{Campaign, User}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

class Campaigns(tag: Tag) extends Table[Campaign](tag, "CAMPAIGNS") {
  def resourceName: Rep[String] = column[String]("RESOURCE_NAME", O.PrimaryKey)

  def userId: Rep[String] = column[String]("USER_ID")

  def customerId: Rep[Long] = column[Long]("CUSTOMER_ID")

  def size: Rep[Int] = column[Int]("SIZE")

  override def * : ProvenShape[Campaign] = (resourceName, userId, customerId, size).mapTo[Campaign]

  def user: ForeignKeyQuery[Users, User] = foreignKey("USER_FK", userId, TableQuery[Users])(_.id, onDelete = ForeignKeyAction.Cascade)
}