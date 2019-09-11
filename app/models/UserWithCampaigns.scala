package models

import play.api.libs.json.Json

case class UserWithCampaigns(user: User, campaigns: List[Campaign]) {
  def toJson = Json.obj(
    "user" -> user.toJson,
    "campaigns" -> campaigns.map(_.toJson)
  )
}