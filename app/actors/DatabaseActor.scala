package actors

import akka.actor.Actor
import akka.pattern.pipe
import javax.inject.Inject
import play.api.mvc.Results._
import services.UsersService
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import tables.User

import scala.concurrent.ExecutionContext.Implicits.global

object DatabaseActor {

  sealed trait Message

  case object CreateUsersTable extends Message

  case class AddUser(user: User) extends Message

  case object GetUsersAsJson extends Message

  case class DeleteUser(id: Int) extends Message

}

class DatabaseActor @Inject()(usersService: UsersService) extends Actor {

  import actors.DatabaseActor._

  implicit private val db: PostgresProfile.backend.Database = Database.forConfig("postgresDb")

  override def receive: Receive = {
    case CreateUsersTable =>
      usersService.createUsersTable
        .map(_ => Ok("Table \"USERS\" created"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case AddUser(user) =>
      usersService.addUser(user)
        .map(numRows => Ok(s"$numRows users added"))
        .recover { case exception => BadRequest(exception.getMessage) }
        .pipeTo(sender())
    case GetUsersAsJson =>
      usersService.getUsersAsJson.map(Ok(_)).pipeTo(sender())
    case DeleteUser(id) =>
      usersService.deleteUser(id).map(numRows =>
        if (numRows > 0) Ok(s"user with id:$id deleted")
        else BadRequest("user does not exists")).pipeTo(sender())
  }

  override def postStop(): Unit = db.close()
}