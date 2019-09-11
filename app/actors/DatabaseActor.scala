package actors

import akka.actor.Actor
import akka.pattern.pipe
import javax.inject.Inject
import models.{Campaign, User, UserUpdateData, UserWithCampaigns}
import play.api.mvc.Results._
import services.DatabaseService
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseActor {

  sealed trait Message

  case object CreateUsersTable extends Message

  case class AddUser(user: User) extends Message

  case object GetUsersAsJson extends Message

  case class GetUser(id: String) extends Message

  case class DeleteUser(id: String) extends Message

  case class UpdateUser(optionalUser: UserUpdateData) extends Message

  case object CreateCampaignsTable extends Message

  case class AddCampaign(campaign: Campaign) extends Message

  case class DeleteCampaign(resourceName: String) extends Message

  case object GetCampaignsAsJson extends Message

  case class GetCampaignsByUserId(userId: String) extends Message

  case class AddUserWithCampaign(user: User, campaign: Campaign) extends Message

  // Test
  case class GetUserWithCampaigns(id: String) extends Message

  case class AddUserWithCampaigns(userWithCampaigns: UserWithCampaigns) extends Message

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
        .map(_ => Ok(s"new user with id:${user.id} added"))
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
    case UpdateUser(userUpdateData) =>
      if (userUpdateData.isDefined) dbService.updateUser(userUpdateData).map {
        case numRows if numRows > 0 => Ok(s"user with id:${userUpdateData.id} updated")
        case _ => BadRequest(s"user with id:${userUpdateData.id} does not exists")
      }.pipeTo(sender())
      else sender() ! BadRequest("nothing to update")
    case CreateCampaignsTable =>
      dbService.createCampaignsTable
        .map(_ => Ok("table \"CAMPAIGNS\" created"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case AddCampaign(campaign) =>
      dbService.addCampaign(campaign)
        .map(_ => Ok("new campaign \"" + campaign.resourceName + "\" added"))
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
    case GetCampaignsAsJson =>
      dbService.getCampaignsAsJson.map(Ok(_)).pipeTo(sender())
    case GetCampaignsByUserId(userId) =>
      dbService.getCampaignsByUserIdAsJson(userId).map(Ok(_)).pipeTo(sender())
    case AddUserWithCampaign(user, campaign) =>
      dbService.addUserWithCampaign(user, campaign)
    case GetUserWithCampaigns(id) =>
      dbService.getUserWithCampaigns(id).map(res => Ok(res.toJson)).pipeTo(sender())
    case AddUserWithCampaigns(userWithCampaigns) =>
      dbService.addUserWithCampaigns(userWithCampaigns)
        .map(_ => Ok(s"new userWithCampaigns with id:${userWithCampaigns.user.id} added"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
  }

  override def postStop(): Unit = db.close()
}