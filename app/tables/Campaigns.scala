package tables

import play.api.libs.json.{JsValue, Json}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

case class Campaign(resourceName: String, userId: Int, customerId: Long, size: Int) {
  def toJson: JsValue = Json.obj(
    "resourceName" -> resourceName,
    "userId" -> userId,
    "customerId" -> customerId,
    "size" -> size
  )
}

class Campaigns(tag: Tag) extends Table[Campaign](tag, "CAMPAIGNS") {
  def resourceName: Rep[String] = column[String]("RESOURCE_NAME", O.PrimaryKey)

  def userId: Rep[Int] = column[Int]("USER_ID")

  def customerId: Rep[Long] = column[Long]("CUSTOMER_ID")

  def size: Rep[Int] = column[Int]("SIZE")

  override def * : ProvenShape[Campaign] = (resourceName, userId, customerId, size).mapTo[Campaign]

  def user: ForeignKeyQuery[Users, User] = foreignKey("USER_FK", userId, TableQuery[Users])(_.id, onDelete = ForeignKeyAction.Cascade)
}