package services

import javax.inject.Singleton
import models.UserOptionalData
import play.api.libs.json.{JsValue, Json}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import tables.{Campaign, Campaigns, User, Users}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DatabaseService {
  val users = TableQuery[Users]
  val campaigns = TableQuery[Campaigns]

  def createUsersTable(implicit db: PostgresProfile.backend.Database): Future[Unit] = db.run(users.schema.create)

  def addUser(user: User)(implicit db: PostgresProfile.backend.Database): Future[Int] = db.run(users += user)

  def getUsers(implicit db: PostgresProfile.backend.Database): Future[List[User]] = db.run(users.result).map(_.toList)

  def getUsersAsJson(implicit db: PostgresProfile.backend.Database): Future[JsValue] = getUsers.map(_.map(_.toJson)).map(Json.toJson(_))

  def getUser(id: Int)(implicit db: PostgresProfile.backend.Database): Future[User] = db.run(users.filter(_.id === id).result).map(_.head)

  def deleteUser(id: Int)(implicit db: PostgresProfile.backend.Database): Future[Int] = db.run(users.filter(_.id === id).delete)

  def updateUser(optionalUser: UserOptionalData)(implicit db: PostgresProfile.backend.Database): Future[Int] = {
    getUser(optionalUser.id).flatMap(existingUser =>
      db.run(users.filter(_.id === optionalUser.id).map(user => (user.name, user.email, user.picture))
        .update((optionalUser.name.getOrElse(existingUser.name),
          optionalUser.email.getOrElse(existingUser.email),
          optionalUser.picture.getOrElse(existingUser.picture)))))
      .recover { case _ => 0 }
  }

  def addCampaign(campaign: Campaign)(implicit db: PostgresProfile.backend.Database): Future[Int] = db.run(campaigns += campaign)
}