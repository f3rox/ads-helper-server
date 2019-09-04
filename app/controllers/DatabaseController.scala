package controllers

import javax.inject.Inject
import play.api.libs.concurrent.InjectedActorSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import slick.jdbc.PostgresProfile.api._
import tables.Users

import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseController @Inject()(components: ControllerComponents) extends AbstractController(components) with InjectedActorSupport {

  def slickTest = Action { request =>
    val db = Database.forConfig("postgresDb")
    try {
      val users = TableQuery[Users]

      val setupAction: DBIO[Unit] = DBIO.seq(
        users.schema.create
      )

      val setupFuture = db.run(setupAction)
      val f = setupFuture.flatMap { _ =>
        val insertAction: DBIO[Option[Int]] = users ++= Seq(
          (234, "", "", "", ""),
          (235, "", "", "", ""),
          (236, "", "", "", "")
        )
        val insertAndPrintAction = insertAction.map { usersInsertResult =>
          usersInsertResult.foreach { numRows =>
            println(s"Inserted $numRows rows into the Users table")
          }
        }

        db.run(insertAndPrintAction.andThen(users.result)).map { users =>
          users.foreach(println)
        }
      }
    } finally db.close
    Created("Table USERS created")
  }
}