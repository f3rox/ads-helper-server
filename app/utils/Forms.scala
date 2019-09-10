package utils

import models.{CustomerIDs, User, UserUpdateData}
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import tables.Campaign

import scala.concurrent.Future

object Forms {
  def formErrorsHandler[T](formWithErrors: Form[T]): Future[Result] =
    Future.successful(BadRequest(formWithErrors.errors.map(error => s"${error.message} ${error.key}").mkString("\n")))

  val customerIDsForm: Form[CustomerIDs] = Form(
    mapping(
      "managerCustomerId" -> longNumber,
      "clientCustomerId" -> longNumber
    )(CustomerIDs.apply)(CustomerIDs.unapply)
  )

  val userForm: Form[User] = Form(
    mapping(
      "id" -> text,
      "name" -> text,
      "email" -> email,
      "picture" -> text
    )(User.apply)(User.unapply)
  )

  val userUpdateForm = Form(
    mapping(
      "id" -> text,
      "name" -> optional(text),
      "email" -> optional(email),
      "picture" -> optional(text)
    )(UserUpdateData.apply)(UserUpdateData.unapply)
  )

  val campaignForm: Form[Campaign] = Form(
    mapping(
      "resourceName" -> text,
      "userId" -> text,
      "customerId" -> longNumber,
      "size" -> number
    )(Campaign.apply)(Campaign.unapply)
  )
}