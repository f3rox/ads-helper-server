package controllers

import java.nio.file.{Files, Paths}

import javax.inject._
import play.api.mvc._

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val rootPath: String = Paths.get("").toAbsolutePath.toString

  def index = Action {
    Ok("Application is ready.")
  }

  def upload = Action(parse.multipartFormData) { request =>
    println(request)
    println(request.body)
    request.body.file("file").map { uploadedFile =>
      println(uploadedFile.filename)
      println(uploadedFile.fileSize / 1024.0 / 1024.0 + " MB")
      if (!Files.exists(Paths.get(rootPath + "/tmp"))) println(Files.createDirectory(Paths.get(rootPath + "/tmp")))
      uploadedFile.ref.moveFileTo(Paths.get(s"$rootPath/tmp/${uploadedFile.filename}"), replace = true)
      Ok("File uploaded")
    }.getOrElse(BadRequest("upload error"))
  }
}