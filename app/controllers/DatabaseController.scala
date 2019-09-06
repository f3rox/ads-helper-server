package controllers

import actors.DatabaseActor._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.{Inject, Named}
import models.UserOptionalData
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import tables.{Campaign, User}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DatabaseController @Inject()(@Named("database-actor") databaseActor: ActorRef, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 1.minute

  def createUsersTable = Action.async {
    (databaseActor ? CreateUsersTable).mapTo[Result]
  }

  def addUser = Action.async(parse.formUrlEncoded) { request =>
    val userForm: Form[User] = Form(
      mapping(
        "id" -> number,
        "name" -> text,
        "email" -> email,
        "picture" -> text
      )(User.apply)(User.unapply)
    )
    userForm.bindFromRequest(request.body).fold(
      formErrorsHandler,
      user => (databaseActor ? AddUser(user)).mapTo[Result]
    )
  }

  def getUsers = Action.async {
    (databaseActor ? GetUsersAsJson).mapTo[Result]
  }

  def getUser(id: String) = Action.async {
    Try(id.toInt) match {
      case Success(userId) => (databaseActor ? GetUser(userId)).mapTo[Result]
      case Failure(exception) => Future.successful(BadRequest(exception.toString))
    }
  }

  def deleteUser(id: String) = Action.async {
    Try(id.toInt) match {
      case Success(userId) => (databaseActor ? DeleteUser(userId)).mapTo[Result]
      case Failure(exception) => Future.successful(BadRequest(exception.toString))
    }
  }

  def updateUser = Action.async(parse.formUrlEncoded) { request =>
    val userFormOptional = Form(
      mapping(
        "id" -> number,
        "name" -> optional(text),
        "email" -> optional(email),
        "picture" -> optional(text)
      )(UserOptionalData.apply)(UserOptionalData.unapply)
    )
    userFormOptional.bindFromRequest(request.body).fold(
      formErrorsHandler,
      optionalUser => (databaseActor ? UpdateUser(optionalUser)).mapTo[Result]
    )
  }

  def addCampaign = Action {
    (databaseActor ? AddCampaign(Campaign("resourceName", 129, 2342343, 10))).mapTo[Result]
    Ok("")
  }

  private def formErrorsHandler[T](formWithErrors: Form[T]): Future[Result] =
    Future.successful(BadRequest(formWithErrors.errors.map(error => s"${error.message} ${error.key}").mkString("\n")))
}