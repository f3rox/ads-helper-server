package models

import play.api.libs.json.{JsValue, Json}

case class Campaign(resourceName: String, userId: String, customerId: Long, size: Int) {
  def toJson: JsValue = Json.obj(
    "resourceName" -> resourceName,
    "userId" -> userId,
    "customerId" -> customerId,
    "size" -> size
  )
}