package services

import java.io.FileInputStream
import java.util.Properties

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.common.ManualCpc
import com.google.ads.googleads.v2.enums.AdvertisingChannelTypeEnum.AdvertisingChannelType
import com.google.ads.googleads.v2.enums.BudgetDeliveryMethodEnum.BudgetDeliveryMethod
import com.google.ads.googleads.v2.enums.CampaignStatusEnum.CampaignStatus
import com.google.ads.googleads.v2.resources.Campaign.NetworkSettings
import com.google.ads.googleads.v2.resources.{Campaign, CampaignBudget, Customer}
import com.google.ads.googleads.v2.services._
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.auth.Credentials
import com.google.auth.oauth2.{AccessToken, UserCredentials}
import com.google.common.collect.ImmutableList
import com.google.protobuf.{BoolValue, Int64Value, StringValue}

import scala.collection.JavaConverters._

object GoogleAds {
  private def getProperties(path: String = "conf/ads.properties"): Properties = {
    val props = new Properties()
    props.load(new FileInputStream(path))
    props
  }

  def getGoogleAdsClient(accessToken: AccessToken, loginCustomerId: Long): GoogleAdsClient = {
    val props = getProperties()
    val credentials: Credentials = UserCredentials.newBuilder()
      .setClientId(props.getProperty("clientId"))
      .setClientSecret(props.getProperty("clientSecret"))
      .setAccessToken(accessToken)
      .build()
    val googleAdsClient: GoogleAdsClient = GoogleAdsClient.newBuilder()
      .setCredentials(credentials)
      .setDeveloperToken(props.getProperty("developerToken"))
      .setLoginCustomerId(loginCustomerId)
      .build()
    googleAdsClient
  }

  def getCustomer(implicit googleAdsClient: GoogleAdsClient): Customer = {
    val customerServiceClient: CustomerServiceClient = googleAdsClient.getLatestVersion.createCustomerServiceClient()
    val customer: Customer = customerServiceClient.getCustomer(ResourceNames.customer(googleAdsClient.getLoginCustomerId))
    customer
  }

  def printCustomerInfo(customer: Customer): Unit = {
    println("Customer:")
    println("ID: " + customer.getId.getValue)
    println("Name: " + customer.getDescriptiveName.getValue)
    println("Code: " + customer.getCurrencyCode.getValue)
    println("TimeZone: " + customer.getTimeZone.getValue)
  }

  def addCampaignBudget(customerId: Long, name: String, amount: Int)(implicit googleAdsClient: GoogleAdsClient): String = {
    val budget: CampaignBudget = CampaignBudget.newBuilder()
      .setName(StringValue.of(name))
      .setDeliveryMethod(BudgetDeliveryMethod.STANDARD)
      .setAmountMicros(Int64Value.of(amount))
      .build()
    val op: CampaignBudgetOperation = CampaignBudgetOperation.newBuilder().setCreate(budget).build()
    val campaignBudgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()
    val response: MutateCampaignBudgetsResponse = campaignBudgetServiceClient
      .mutateCampaignBudgets(customerId.toString, ImmutableList.of(op))
    val budgetResourceName: String = response.getResults(0).getResourceName
    println("Added budget: " + budgetResourceName)
    budgetResourceName
  }

  def getNetworkSettings: NetworkSettings = {
    val networkSettings = NetworkSettings.newBuilder()
      .setTargetGoogleSearch(BoolValue.of(true))
      .setTargetSearchNetwork(BoolValue.of(true))
      .setTargetContentNetwork(BoolValue.of(true))
      .setTargetPartnerSearchNetwork(BoolValue.of(false))
      .build()
    networkSettings
  }

  def addCampaign(customerId: Long, budgetResourceName: String, networkSettings: NetworkSettings, name: String)(implicit googleAdsClient: GoogleAdsClient): MutateCampaignsResponse = {
    val campaign: Campaign = Campaign.newBuilder()
      .setName(StringValue.of(name))
      .setAdvertisingChannelType(AdvertisingChannelType.SEARCH)
      .setStatus(CampaignStatus.PAUSED)
      .setManualCpc(ManualCpc.newBuilder().build())
      .setCampaignBudget(StringValue.of(budgetResourceName))
      .setNetworkSettings(networkSettings)
      .build()
    val operations = List(CampaignOperation.newBuilder().setCreate(campaign).build())
    val campaignServiceClient: CampaignServiceClient = googleAdsClient.getLatestVersion.createCampaignServiceClient()
    val response = campaignServiceClient.mutateCampaigns(java.lang.Long.toString(customerId), operations.asJava)
    response
  }
}