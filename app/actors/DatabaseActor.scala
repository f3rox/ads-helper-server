package actors

import akka.actor.Actor
import akka.pattern.pipe
import javax.inject.Inject
import models.UserOptionalData
import play.api.mvc.Results._
import services.DatabaseService
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import tables.{Campaign, User}

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseActor {

  sealed trait Message

  case object CreateUsersTable extends Message

  case class AddUser(user: User) extends Message

  case object GetUsersAsJson extends Message

  case class GetUser(id: Int) extends Message

  case class DeleteUser(id: Int) extends Message

  case class UpdateUser(optionalUser: UserOptionalData) extends Message

  case object CreateCampaignsTable extends Message

  case class AddCampaign(campaign: Campaign) extends Message

  case class DeleteCampaign(resourceName: String) extends Message

}

class DatabaseActor @Inject()(dbService: DatabaseService) extends Actor {

  import actors.DatabaseActor._

  implicit private val db: PostgresProfile.backend.Database = Database.forConfig("postgresDb")

  override def receive: Receive = {
    case CreateUsersTable =>
      dbService.createUsersTable
        .map(_ => Ok("table \"USERS\" created"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case AddUser(user) =>
      dbService.addUser(user)
        .map(_ => Ok("new user added"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case GetUsersAsJson =>
      dbService.getUsersAsJson.map(Ok(_)).pipeTo(sender())
    case DeleteUser(id) =>
      dbService.deleteUser(id)
        .map {
          case numRows if numRows > 0 => Ok(s"user with id:$id deleted")
          case _ => BadRequest(s"user with id:$id does not exists")
        }
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case GetUser(id) =>
      dbService.getUser(id)
        .map(user => Ok(user.toJson))
        .recover { case _ => BadRequest(s"user with id:$id does not exists") }
        .pipeTo(sender())
    case UpdateUser(optionalUser) =>
      if (optionalUser.isDefined) dbService.updateUser(optionalUser).map {
        case numRows if numRows > 0 => Ok(s"user with id:${optionalUser.id} updated")
        case _ => BadRequest(s"user with id:${optionalUser.id} does not exists")
      }.pipeTo(sender())
      else sender() ! BadRequest("nothing to update")
    case CreateCampaignsTable =>
      dbService.createCampaignsTable
        .map(_ => Ok("Table \"CAMPAIGNS\" created"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case AddCampaign(campaign) =>
      dbService.addCampaign(campaign)
        .map(numRows => Ok(s"$numRows campaigns added"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case DeleteCampaign(resourceName) =>
      dbService.deleteCampaign(resourceName)
        .map {
          case numRows if numRows > 0 => Ok("campaign \"" + resourceName + "\" deleted")
          case _ => BadRequest("campaign \"" + resourceName + "\" does not exists")
        }
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
  }

  override def postStop(): Unit = db.close()
}