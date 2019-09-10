package controllers

import actors.DatabaseActor._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.{Inject, Named}
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import utils.Forms._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DatabaseController @Inject()(@Named("database-actor") databaseActor: ActorRef, components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {
  implicit val timeout: Timeout = 1.minute

  def createUsersTable = Action.async {
    (databaseActor ? CreateUsersTable).mapTo[Result]
  }

  def addUser = Action.async(parse.formUrlEncoded) { request =>
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
    userUpdateForm.bindFromRequest(request.body).fold(
      formErrorsHandler,
      userUpdateData => (databaseActor ? UpdateUser(userUpdateData)).mapTo[Result]
    )
  }

  def createCampaignsTable = Action.async {
    (databaseActor ? CreateCampaignsTable).mapTo[Result]
  }

  def addCampaign = Action.async(parse.formUrlEncoded) { request =>
    campaignForm.bindFromRequest(request.body).fold(
      formErrorsHandler,
      campaign => (databaseActor ? AddCampaign(campaign)).mapTo[Result]
    )
  }

  def deleteCampaign(resourceName: String) = Action.async {
    (databaseActor ? DeleteCampaign(resourceName)).mapTo[Result]
  }

  def getCampaigns = Action.async {
    (databaseActor ? GetCampaignsAsJson).mapTo[Result]
  }

  def getCampaignsByUserId(id: String) = Action.async {
    Try(id.toInt) match {
      case Success(userId) => (databaseActor ? GetCampaignsByUserId(userId)).mapTo[Result]
      case Failure(exception) => Future.successful(BadRequest(exception.toString))
    }
  }
}