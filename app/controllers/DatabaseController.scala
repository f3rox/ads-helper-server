package controllers

import actors.DatabaseActor.{AddUser, CreateUsersTable, DeleteUser, GetUsersAsJson}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.{Inject, Named}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import tables.User

import scala.concurrent.Future
import scala.concurrent.duration._

class DatabaseController @Inject()(@Named("database-actor") databaseActor: ActorRef, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 1.minute

  def createUsersTable = Action.async { _ =>
    (databaseActor ? CreateUsersTable).mapTo[Result]
  }

  def addUser = Action.async(parse.formUrlEncoded) { request =>
    val userForm: Form[User] = Form(
      mapping(
        "id" -> number,
        "name" -> text,
        "email" -> text,
        "picture" -> text
      )(User.apply)(User.unapply)
    )
    userForm.bindFromRequest(request.body).fold(
      formWithErrors =>
        Future.successful(BadRequest(formWithErrors.errors.map(error => s"${error.message} ${error.key}").mkString("\n"))),
      user => (databaseActor ? AddUser(user)).mapTo[Result]
    )
  }

  def getUsers = Action.async {
    (databaseActor ? GetUsersAsJson).mapTo[Result]
  }

  def deleteUser = Action.async(parse.formUrlEncoded) { request =>
    val id = request.body.get("id").map(_.head)
    if (id.isDefined) (databaseActor ? DeleteUser(id.get.toInt)).mapTo[Result]
    else Future.successful(BadRequest("required id"))
  }
}