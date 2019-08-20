package controllers

import java.nio.file.{Files, Paths}
import java.util.Date

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.auth.oauth2.AccessToken
import javax.inject._
import play.api.mvc._
import services.{CsvParser, GoogleAds}

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val rootPath: String = Paths.get("").toAbsolutePath.toString

  def index = Action {
    Ok("Application is ready.")
  }

  def upload = Action(parse.multipartFormData) { request =>
    val tokenValue = request.body.asFormUrlEncoded("access_token").head
    val expirationTime = new Date(request.body.asFormUrlEncoded("expires_at").head.toLong)
    println(s"\nNEW REQUEST:\nToken value: $tokenValue\nExpiration time: $expirationTime")

    request.body.file("file").map { uploadedFile =>
      println(s"File name: ${uploadedFile.filename}\nFile size: ${uploadedFile.fileSize / 1024.0 / 1024.0} MB")
      if (!Files.exists(Paths.get(rootPath + "/tmp"))) println(Files.createDirectory(Paths.get(rootPath + "/tmp")))
      val filePath = uploadedFile.ref.moveFileTo(Paths.get(s"$rootPath/tmp/${uploadedFile.filename}"), replace = true).toString
      println(s"File path: $filePath")
      val products = CsvParser.parseCsv(filePath)
      println("File successfully parsed")

      val token = new AccessToken(tokenValue, expirationTime)
      val managerCustomerId = 1169899225L
      val clientCustomerId = 2515161029L

      implicit val googleAdsClient: GoogleAdsClient = GoogleAds.getGoogleAdsClient(token, managerCustomerId)
      val budgetResourceName: String = GoogleAds.addCampaignBudget(clientCustomerId, s"Test budget #${System.currentTimeMillis()}", 500000)
      val response = GoogleAds.addCampaign(clientCustomerId, budgetResourceName, GoogleAds.getNetworkSettings, s"Test campaign #${System.currentTimeMillis()}")
      println("Response: " + response)
      println("Added " + response.getResultsCount + " campaigns")
      response.getResultsList.forEach(result => println(result.getResourceName))

      Ok("File successfully uploaded")
    }.getOrElse(BadRequest("Upload error"))
  }
}