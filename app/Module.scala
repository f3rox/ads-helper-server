import actors.{AuthActor, HelloActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[HelloActor]("hello-actor")
    bindActor[AuthActor]("auth-actor")
  }
}