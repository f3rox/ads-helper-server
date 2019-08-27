import actors._
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[HelloActor]("hello-actor")
    bindActor[AuthActor]("auth-actor")
    bindActor[RootActor]("root-actor")
    bindActorFactory[FileActor, FileActor.Factory]
    bindActorFactory[AdGroupsActor, AdGroupsActor.Factory]
    bindActorFactory[CampaignActor, CampaignActor.Factory]
    bindActorFactory[CampaignBudgetActor, CampaignBudgetActor.Factory]
    bindActorFactory[FileActor, FileActor.Factory]
    bindActorFactory[GoogleAdsActor, GoogleAdsActor.Factory]
    bindActorFactory[KeywordsActor, KeywordsActor.Factory]
    bindActorFactory[ExpandedTextAdsActor, ExpandedTextAdsActor.Factory]
  }
}