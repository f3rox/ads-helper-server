package utils

import play.api.data.Form
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.Future

object Utils {

  def formErrorsHandler[T](formWithErrors: Form[T]): Future[Result] =
    Future.successful(BadRequest(formWithErrors.errors.map(error => s"${error.message} ${error.key}").mkString("\n")))
}