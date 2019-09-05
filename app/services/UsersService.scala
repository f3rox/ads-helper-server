package services

import javax.inject.Singleton
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import tables.{User, Users}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UsersService {
  val users = TableQuery[Users]

  def createUsersTable(implicit db: PostgresProfile.backend.Database): Future[Unit] = db.run(users.schema.create)

  def addUser(user: User)(implicit db: PostgresProfile.backend.Database): Future[Int] = db.run(users += user)

  def getUsers(implicit db: PostgresProfile.backend.Database): Future[List[User]] = db.run(users.result).map(_.toList)

  def getUsersAsJson(implicit db: PostgresProfile.backend.Database): Future[JsValue] = getUsers.map(_.map(_.toJson)).map(Json.toJson(_))

  def deleteUser(id: Int)(implicit db: PostgresProfile.backend.Database): Future[Int] = db.run(users.filter(_.id === id).delete)
}