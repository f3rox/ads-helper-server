package actors

import java.nio.file.{Files, Paths}

import akka.actor.Actor
import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import play.api.libs.Files.TemporaryFile
import services.CsvParser

object FileActor {

  sealed trait Message

  case class ParseFile(uploadedFile: TemporaryFile) extends Message

  trait Factory {
    def apply(fileName: String): Actor
  }

}

class FileActor @Inject()(@Assisted fileName: String, csvParser: CsvParser) extends Actor {

  import FileActor._

  val rootPath: String = Paths.get("").toAbsolutePath.toString

  override def receive: Receive = {
    case ParseFile(uploadedFile) => {
      if (!Files.exists(Paths.get(rootPath + "/tmp"))) Files.createDirectory(Paths.get(rootPath + "/tmp"))
      val localFilePath = uploadedFile.moveFileTo(Paths.get(s"$rootPath/tmp/$fileName"), replace = true)
      val productsList = csvParser.parseCsv(localFilePath)
      if (Files.deleteIfExists(localFilePath)) println(s"${localFilePath.getFileName} deleted")
      sender() ! productsList
    }
  }
}