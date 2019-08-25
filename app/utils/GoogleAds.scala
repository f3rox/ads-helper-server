package utils

import java.math.BigInteger
import java.security.SecureRandom

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.common.{ExpandedTextAdInfo, KeywordInfo, ManualCpc}
import com.google.ads.googleads.v2.enums.AdGroupAdStatusEnum.AdGroupAdStatus
import com.google.ads.googleads.v2.enums.AdGroupCriterionStatusEnum.AdGroupCriterionStatus
import com.google.ads.googleads.v2.enums.AdGroupStatusEnum.AdGroupStatus
import com.google.ads.googleads.v2.enums.AdGroupTypeEnum.AdGroupType
import com.google.ads.googleads.v2.enums.AdvertisingChannelTypeEnum.AdvertisingChannelType
import com.google.ads.googleads.v2.enums.BudgetDeliveryMethodEnum.BudgetDeliveryMethod
import com.google.ads.googleads.v2.enums.CampaignStatusEnum.CampaignStatus
import com.google.ads.googleads.v2.enums.KeywordMatchTypeEnum.KeywordMatchType
import com.google.ads.googleads.v2.resources.Campaign.NetworkSettings
import com.google.ads.googleads.v2.resources._
import com.google.ads.googleads.v2.services._
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.auth.Credentials
import com.google.auth.oauth2.UserCredentials
import com.google.common.collect.ImmutableList
import com.google.protobuf.{BoolValue, Int64Value, StringValue}
import javax.inject.{Inject, Singleton}
import models.Product

import scala.collection.JavaConverters._

@Singleton
class GoogleAds @Inject()(appConfig: AppConfig) {
  def getGoogleAdsClient(refreshToken: String, loginCustomerId: Long): GoogleAdsClient = {
    val credentials: Credentials = UserCredentials.newBuilder()
      .setClientId(appConfig.getClientId)
      .setClientSecret(appConfig.getClientSecret)
      .setRefreshToken(refreshToken)
      .build()
    val googleAdsClient: GoogleAdsClient = GoogleAdsClient.newBuilder()
      .setCredentials(credentials)
      .setDeveloperToken(appConfig.getDeveloperToken)
      .setLoginCustomerId(loginCustomerId)
      .build()
    googleAdsClient
  }

  def getCustomer(implicit googleAdsClient: GoogleAdsClient): Customer = {
    val customerServiceClient: CustomerServiceClient = googleAdsClient.getLatestVersion.createCustomerServiceClient()
    val customer: Customer = customerServiceClient.getCustomer(ResourceNames.customer(googleAdsClient.getLoginCustomerId))
    customer
  }

  def addCampaignBudget(amount: Int, name: String)(implicit googleAdsClient: GoogleAdsClient, customerId: Long): String = {
    val startTime = System.currentTimeMillis()
    val budget: CampaignBudget = CampaignBudget.newBuilder()
      .setName(StringValue.of(name))
      .setDeliveryMethod(BudgetDeliveryMethod.STANDARD)
      .setAmountMicros(Int64Value.of(amount))
      .build()
    val op: CampaignBudgetOperation = CampaignBudgetOperation.newBuilder().setCreate(budget).build()
    val campaignBudgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()
    val response: MutateCampaignBudgetsResponse = campaignBudgetServiceClient
      .mutateCampaignBudgets(customerId.toString, ImmutableList.of(op))
    campaignBudgetServiceClient.shutdownNow()
    val budgetResourceName: String = response.getResults(0).getResourceName
    println(s"Added CampaignBudget $budgetResourceName in ${(System.currentTimeMillis() - startTime) / 1000.0} sec")
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

  def addCampaign(budgetResourceName: String, name: String)(implicit googleAdsClient: GoogleAdsClient, customerId: Long): String = {
    val campaign: Campaign = Campaign.newBuilder()
      .setName(StringValue.of(name))
      .setAdvertisingChannelType(AdvertisingChannelType.SEARCH)
      .setStatus(CampaignStatus.PAUSED)
      .setManualCpc(ManualCpc.newBuilder().build())
      .setCampaignBudget(StringValue.of(budgetResourceName))
      .setNetworkSettings(getNetworkSettings)
      .build()
    val operations = List(CampaignOperation.newBuilder().setCreate(campaign).build())
    val campaignServiceClient: CampaignServiceClient = googleAdsClient.getLatestVersion.createCampaignServiceClient()
    val response = campaignServiceClient.mutateCampaigns(customerId.toString, operations.asJava)
    campaignServiceClient.shutdownNow()
    val campaignResourceName = response.getResults(0).getResourceName
    println("Added Campaign: " + campaignResourceName)
    campaignResourceName
  }

  def addAdGroup(campaignResourceName: String, name: String)(implicit googleAdsClient: GoogleAdsClient, customerId: Long): String = {
    val adGroup: AdGroup = AdGroup.newBuilder()
      .setName(StringValue.of(name))
      .setStatus(AdGroupStatus.ENABLED)
      .setCampaign(StringValue.of(campaignResourceName))
      .setType(AdGroupType.SEARCH_STANDARD)
      .setCpcBidMicros(Int64Value.of(10000000L))
      .build()
    val operations = List(AdGroupOperation.newBuilder().setCreate(adGroup).build())
    val adGroupServiceClient = googleAdsClient.getLatestVersion.createAdGroupServiceClient()
    val response = adGroupServiceClient.mutateAdGroups(customerId.toString, operations.asJava)
    adGroupServiceClient.shutdownNow()
    val adGroupResourceName = response.getResults(0).getResourceName
    println("Added AdGroup: " + adGroupResourceName)
    adGroupResourceName
  }

  def addAdGroups(campaignResourceName: String, products: List[Product])(implicit googleAdsClient: GoogleAdsClient, customerId: Long): List[String] = {
    val operations = for (product <- products) yield {
      val adGroup = AdGroup.newBuilder()
        .setName(StringValue.of(s"${product.name} #${new BigInteger(130, new SecureRandom()).toString(32)}"))
        .setStatus(AdGroupStatus.ENABLED)
        .setCampaign(StringValue.of(campaignResourceName))
        .setType(AdGroupType.SEARCH_STANDARD)
        .setCpcBidMicros(Int64Value.of(10000000L))
        .build()
      val adGroupOperation = AdGroupOperation.newBuilder().setCreate(adGroup).build()
      adGroupOperation
    }
    val adGroupServiceClient = googleAdsClient.getLatestVersion.createAdGroupServiceClient()
    val response = adGroupServiceClient.mutateAdGroups(customerId.toString, operations.asJava)
    adGroupServiceClient.shutdownNow()
    val adGroupsResourcesNames = response.getResultsList.asScala.map(_.getResourceName).toList
    println(s"Added ${adGroupsResourcesNames.size} AdGroups")
    adGroupsResourcesNames
  }

  def addKeyword(adGroupResourceName: String, keywordText: String)(implicit googleAdsClient: GoogleAdsClient, customerId: Long): String = {
    val keywordInfo = KeywordInfo.newBuilder()
      .setText(StringValue.of(keywordText))
      .setMatchType(KeywordMatchType.BROAD)
      .build()
    val criterion = AdGroupCriterion.newBuilder()
      .setAdGroup(StringValue.of(adGroupResourceName))
      .setStatus(AdGroupCriterionStatus.ENABLED)
      .setKeyword(keywordInfo)
      .build()
    val operations = List(AdGroupCriterionOperation.newBuilder().setCreate(criterion).build())
    val adGroupCriterionServiceClient = googleAdsClient.getLatestVersion.createAdGroupCriterionServiceClient()
    val response = adGroupCriterionServiceClient.mutateAdGroupCriteria(customerId.toString, operations.asJava)
    adGroupCriterionServiceClient.shutdownNow()
    val keywordsResourceName = response.getResults(0).getResourceName
    println("Added Keyword: " + keywordsResourceName)
    keywordsResourceName
  }

  def addKeywords(productsWithAdGroups: List[(Product, String)])(implicit googleAdsClient: GoogleAdsClient, customerId: Long): List[String] = {
    val operations = for ((product, adGroupResourceName) <- productsWithAdGroups) yield {
      val keywordInfo = KeywordInfo.newBuilder()
        .setText(StringValue.of(s"${product.category} ${product.name}"))
        .setMatchType(KeywordMatchType.BROAD)
        .build()
      val criterion = AdGroupCriterion.newBuilder()
        .setAdGroup(StringValue.of(adGroupResourceName))
        .setStatus(AdGroupCriterionStatus.ENABLED)
        .setKeyword(keywordInfo)
        .build()
      val adGroupCriterionOperation = AdGroupCriterionOperation.newBuilder().setCreate(criterion).build()
      adGroupCriterionOperation
    }
    val adGroupCriterionServiceClient = googleAdsClient.getLatestVersion.createAdGroupCriterionServiceClient()
    val response = adGroupCriterionServiceClient.mutateAdGroupCriteria(customerId.toString, operations.asJava)
    adGroupCriterionServiceClient.shutdownNow()
    val keywordsResourcesNames = response.getResultsList.asScala.map(_.getResourceName).toList
    println(s"Added ${keywordsResourcesNames.size} Keywords")
    keywordsResourcesNames
  }

  def addExpandedTextAd(adGroupResourceName: String, product: Product)(implicit googleAdsClient: GoogleAdsClient, customerId: Long): String = {
    val expandedTextAdInfo = ExpandedTextAdInfo.newBuilder()
      .setHeadlinePart1(StringValue.of(product.category))
      .setHeadlinePart2(StringValue.of(product.name))
      .setHeadlinePart3(StringValue.of(product.price.toString))
      .setDescription(StringValue.of(product.description))
      .build()
    val ad = Ad.newBuilder()
      .setExpandedTextAd(expandedTextAdInfo)
      .addFinalUrls(StringValue.of(product.url))
      .build()
    val adGroupAd = AdGroupAd.newBuilder()
      .setAdGroup(StringValue.of(adGroupResourceName))
      .setStatus(AdGroupAdStatus.PAUSED)
      .setAd(ad)
      .build()
    val operations = List(AdGroupAdOperation.newBuilder().setCreate(adGroupAd).build())
    val adGroupServiceClient = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()
    val response = adGroupServiceClient.mutateAdGroupAds(customerId.toString, operations.asJava)
    adGroupServiceClient.shutdownNow()
    val adResourceName = response.getResults(0).getResourceName
    println("Added Ad: " + adResourceName)
    adResourceName
  }

  def addExpandedTextAds(productsWithAdGroups: List[(Product, String)])(implicit googleAdsClient: GoogleAdsClient, customerId: Long): List[String] = {
    val operations = for ((product, adGroupResourceName) <- productsWithAdGroups) yield {
      val expandedTextAdInfo = ExpandedTextAdInfo.newBuilder()
        .setHeadlinePart1(StringValue.of(product.category))
        .setHeadlinePart2(StringValue.of(product.name))
        .setHeadlinePart3(StringValue.of(product.price.toString))
        .setDescription(StringValue.of(product.description))
        .build()
      val ad = Ad.newBuilder()
        .setExpandedTextAd(expandedTextAdInfo)
        .addFinalUrls(StringValue.of(product.url))
        .build()
      val adGroupAd = AdGroupAd.newBuilder()
        .setAdGroup(StringValue.of(adGroupResourceName))
        .setStatus(AdGroupAdStatus.PAUSED)
        .setAd(ad)
        .build()
      val adGroupAdOperation = AdGroupAdOperation.newBuilder().setCreate(adGroupAd).build()
      adGroupAdOperation
    }
    val adGroupServiceClient = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()
    val response = adGroupServiceClient.mutateAdGroupAds(customerId.toString, operations.asJava)
    adGroupServiceClient.shutdownNow()
    val adResourcesNames = response.getResultsList.asScala.map(_.getResourceName).toList
    println(s"Added ${adResourcesNames.size} Ads")
    adResourcesNames
  }
}