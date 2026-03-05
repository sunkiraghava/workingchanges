package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class UserAssociationWithRedemptionTest {
	private static Logger logger = LogManager.getLogger(UserAssociationWithRedemptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName, env, baseUrl, businessId, businessesQuery, redeemableQuery;
	String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String campaignName, redeemableExternalID1, redeemableExternalID2;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
		businessId = dataSet.get("business_id");
		businessesQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id = $business_id;";
		businessesQuery = businessesQuery.replace("$business_id", businessId);
		redeemableQuery = "SELECT id FROM redeemables WHERE uuid = '$external_id';";
		campaignName = null;
		redeemableExternalID1 = null;
		redeemableExternalID2 = null;
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6997: Verify `api2/mobile/redemptions/reward` API and user association with redemption code", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T6997_verifyMobileApi2RedemptionsReward() throws Exception {
		// This test case covers OMM-T4181, OMM-T4189, OMM-T4184, OMM-T4147, OMM-T4146
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		// pageObj.dashboardpage().navigateToTabs("Redemption Codes");
		// pageObj.dashboardpage().checkUncheckAnyFlag("Apply user association
		// validation on redemption code", "check");

		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Create Redeemable via API {$5 Flat Discount Redeemable needing 10 points to
		// redeem}
		String redeemableName = "T6997_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName);
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(5.0).build())
				.setAutoApplicable(false).setPoints(10).setApplicable_as_loyalty_redemptionFlag(false)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		utils.logit(
				redeemableName + " redeemable created successfully. API response: " + redeemableResponse.prettyPrint());
		String getRedeemableIdQuery = redeemableQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery, "id");

		// Signup user A
		String aUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response aSignUpResponse = pageObj.endpoints().Api2SignUp(aUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String aToken = aSignUpResponse.jsonPath().get("access_token.token").toString();
		String aUserId = aSignUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(aSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 User Signup");
		utils.logit("User A signed up with user id: " + aUserId);

		// Send reward to user A using redeemable_id
		Response aSendRewardResponse = pageObj.endpoints().sendMessageToUser(aUserId, dataSet.get("apiKey"), "",
				redeemableId, "", "");
		Assert.assertEquals(aSendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Dashboard Send reward to user API");
		utils.logit("Sent reward to user A with redeemable id: " + redeemableId);
		// Mobile API2 Get User Offers for user A
		Response aOfferResponse = pageObj.endpoints().getUserOffers(aToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(aOfferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 Get User Offers");
		String aRewardId = aOfferResponse.jsonPath().get("rewards[0].reward_id").toString();
		Assert.assertNotNull(aRewardId);
		utils.logit("Fetched reward_id: " + aRewardId + " from Get User Offers API for user A");

		// Hit 'api2/mobile/redemptions/reward' without location_id for user A
		Response aGenerateRedemptionCodeResponse = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aToken, aRewardId, "");
		Assert.assertEquals(aGenerateRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 create redemption using reward_id");
		String aRedemptionCode = aGenerateRedemptionCodeResponse.jsonPath().get("redemption_tracking_code").toString();
		String aRedeemableName = aGenerateRedemptionCodeResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionCode);
		Assert.assertEquals(aRedeemableName, redeemableName);
		utils.logPass("Redemption code: " + aRedemptionCode + " generated successfully without location_id for user A");

		// Again, send reward to user A using redeemable_id
		aSendRewardResponse = pageObj.endpoints().sendMessageToUser(aUserId, dataSet.get("apiKey"), "", redeemableId,
				"", "");
		Assert.assertEquals(aSendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Dashboard Send reward to user API");
		utils.logit("Sent reward to user A with redeemable id: " + redeemableId);
		// Mobile API2 Get User Offers for user A
		aOfferResponse = pageObj.endpoints().getUserOffers(aToken, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(aOfferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 Get User Offers");
		aRewardId = aOfferResponse.jsonPath().get("rewards[0].reward_id").toString();
		Assert.assertNotNull(aRewardId);
		utils.logit("Fetched reward_id: " + aRewardId + " from Get User Offers API for user A");

		// Hit 'api2/mobile/redemptions/reward' for A with different business
		// location_id
		Response aGenerateRedemptionCodeWithLocationResponse = pageObj.endpoints()
				.Api2RedemptionWithRewardIdAndLocationId(dataSet.get("client"), dataSet.get("secret"), aToken,
						aRewardId, dataSet.get("differentLocationId"));
		Assert.assertEquals(aGenerateRedemptionCodeWithLocationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 create redemption using reward_id with different business location id");
		String aRedemptionCodeWithLocation = aGenerateRedemptionCodeWithLocationResponse.jsonPath()
				.get("redemption_tracking_code").toString();
		String aRedeemableNameWithLocation = aGenerateRedemptionCodeWithLocationResponse.jsonPath()
				.get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionCodeWithLocation);
		Assert.assertEquals(aRedeemableNameWithLocation, redeemableName);
		utils.logPass("Redemption code: " + aRedemptionCodeWithLocation
				+ " generated successfully with different business's location_id");

		// Signup user B
		String bUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response bSignUpResponse = pageObj.endpoints().Api2SignUp(bUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String bToken = bSignUpResponse.jsonPath().get("access_token.token").toString();
		String bUserId = bSignUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(bSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 User Signup");
		utils.logit("User B signed up with user id: " + bUserId);

		// Hit 'api2/mobile/redemptions/reward' using A's reward_id and B's token
		Response bGenerateRedemptionCodeResponse = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), bToken, aRewardId, "");
		Assert.assertEquals(bGenerateRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code not matched for Mobile API2 create redemption using reward_id with different user token");
		String errorMessage = bGenerateRedemptionCodeResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(errorMessage, MessagesConstants.rewardNotAccessible);
		utils.logPass("Redemption code generation failed when User B tried with User A's reward ID");

		// Hit `/api/pos/redemptions` to redeem A's redemption_code using B's email_id
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response bPosRedemptionResponse = pageObj.endpoints().posRedemptionOfCode(bUserEmail, date, aRedemptionCode,
				key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(bPosRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		String bPosRedemptionStatus = bPosRedemptionResponse.jsonPath().get("status").toString();
		Assert.assertEquals(bPosRedemptionStatus, "Code " + aRedemptionCode + " not found");
		utils.logPass("POS redemption failed for User B using User A's redemption code");

		// Now hit `/api/pos/redemptions` to redeem A's redemption_code using A's
		// details
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionResponse = pageObj.endpoints().posRedemptionOfCode(aUserEmail, date, aRedemptionCode,
				key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		String aPosRedemptionStatus = aPosRedemptionResponse.jsonPath().get("status").toString();
		Assert.assertTrue(aPosRedemptionStatus.contains(" at " + dataSet.get("locationName") + ". Please HONOR it."));
		utils.logPass("POS redemption successful for User A using its own redemption code");

		// Verify the successful redemption on User A's timeline and DB
		pageObj.instanceDashboardPage().navigateToGuestTimeline(aUserEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");
		Assert.assertTrue(title, "Redeemed Redemption title not found on timeline");
		String redeemedRedemptionTitle = pageObj.iframeConfigurationPage()
				.getElementText("guestTimeLine.redeemedRedemptionTitle", "");
		Assert.assertEquals(redeemedRedemptionTitle, "Redeemed Redemption " + aRedemptionCode);
		utils.logPass("Redeemed Redemption verified on User A's timeline");
		verifyRedemptionTablesEntries(aUserId, "RewardRedemption", "processed");

	}

	@Test(description = "SQ-T7006: Verify the Online Ordering API for the generation of redemption_code for (discount_type=reward); "
			+ "SQ-T7007: Verify the Online Ordering API for the generation of redemption_code for (discount_type= redeemable)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7006_verifyAuthApiRedemptionCodeGeneration() throws Exception {
		// This test case covers OMM-T4191 and OMM-T4192
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Create Redeemable via API {$5 Flat Discount Redeemable needing 10 points to
		// redeem}
		String redeemableName1 = "T7006_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName1);
		RedeemablePayloadBuilder builder1 = apipayloadObj.redeemableBuilder();
		String redeemablePayload1 = builder1.startNewData().setName(redeemableName1).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(5.0).build())
				.setAutoApplicable(false).setPoints(10).setApplicable_as_loyalty_redemptionFlag(false)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse1 = pageObj.endpoints().createRedeemable(redeemablePayload1, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		utils.logit(redeemableName1 + " redeemable created successfully. API response: "
				+ redeemableResponse1.prettyPrint());
		String getRedeemableIdQuery1 = redeemableQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");

		// Signup user A
		String aUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response aSignUpResponse = pageObj.endpoints().authApiSignUp(aUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String aAccessToken = aSignUpResponse.jsonPath().get("access_token").toString();
		String aAuthToken = aSignUpResponse.jsonPath().get("authentication_token").toString();
		String aUserId = aSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(aSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User A signed up with user id: " + aUserId);

		// Send reward to user A using redeemable_id and also gift {50} points
		Response aSendRewardResponse = pageObj.endpoints().sendMessageToUser(aUserId, dataSet.get("apiKey"), "",
				redeemableId1, "", "50");
		Assert.assertEquals(aSendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Dashboard Send message to user API");
		utils.logit("Sent reward to user A with redeemable id: " + redeemableId1 + " and gifted 50 points");

		// Get Reward ID for user A
		Response aOfferResponse = pageObj.endpoints().getUserOffers(aAccessToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(aOfferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 Get User Offers");
		String aRewardId = aOfferResponse.jsonPath().get("rewards[0].reward_id").toString();
		Assert.assertNotNull(aRewardId);
		utils.logit("Fetched reward_id: " + aRewardId + " from Get User Offers API for user A");

		// Hit 'api/auth/redemptions/online_order' for user A with reward_id
		Response aOnlineRewardRedemptionResponse = pageObj.endpoints().posRedemptionOfRewardIdAuthOnlineOrder(
				aAuthToken, aRewardId, dataSet.get("secret"), dataSet.get("client"));
		Assert.assertEquals(aOnlineRewardRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redeemable_id");
		String aOnlineRewardRedemptionStatus = aOnlineRewardRedemptionResponse.jsonPath().get("status").toString();
		String aOnlineRewardRedemptionId = aOnlineRewardRedemptionResponse.jsonPath().get("redemption_id").toString();
		Assert.assertNotNull(aOnlineRewardRedemptionId);
		Assert.assertTrue(aOnlineRewardRedemptionStatus.contains("Please HONOR it."));
		utils.logPass("Online Order redemption code generation is successful for User A using reward_id");

		// Create Redeemable via API {$50 Flat Discount Redeemable needing 50 points to
		// redeem}
		String redeemableName2 = "T7007_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName2);
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(50.0).build())
				.setAutoApplicable(false).setPoints(50).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID2);
		utils.logit(redeemableName2 + " redeemable created successfully. API response: "
				+ redeemableResponse2.prettyPrint());
		String getRedeemableIdQuery2 = redeemableQuery.replace("$external_id", redeemableExternalID2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");

		// Hit 'api/auth/redemptions/online_order' for user A with redeemable_id
		Response aOnlineRedeemableRedemptionResponse = pageObj.endpoints().authOnlineRedeemableRedemption(aAuthToken,
				dataSet.get("client"), dataSet.get("secret"), "101", redeemableId2);
		Assert.assertEquals(aOnlineRedeemableRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redeemable_id");
		String aOnlineRedeemableRedemptionStatus = aOnlineRedeemableRedemptionResponse.jsonPath().get("status")
				.toString();
		String aOnlineRedeemableRedemptionId = aOnlineRedeemableRedemptionResponse.jsonPath().get("redemption_id")
				.toString();
		Assert.assertNotNull(aOnlineRedeemableRedemptionId);
		Assert.assertTrue(aOnlineRedeemableRedemptionStatus.contains("Please HONOR it."));
		utils.logPass("Online Order redemption code generation is successful for User A using redeemable_id");

	}

	@Test(description = "SQ-T7002: Redeem the redemption_code for discount_type- redeemable generated redeemable for the Guest (User A); "
			+ "SQ-T7022: Verify the parameters on redemptions API, for email, redemption_code, card_number, phone, email, secondary_email", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7002_verifyCodeGeneratedByRedeemableRedemption() throws Exception {
		// This test case covers OMM-T4150
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Create Redeemable via API {$5 Flat Discount Redeemable needing 10 points to
		// redeem}
		String redeemableName = "T7002_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName);
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(5.0).build())
				.setAutoApplicable(false).setPoints(10).setApplicable_as_loyalty_redemptionFlag(false)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		utils.logit(
				redeemableName + " redeemable created successfully. API response: " + redeemableResponse.prettyPrint());
		String getRedeemableIdQuery = redeemableQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery, "id");

		// Signup user A
		String aUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response aSignUpResponse = pageObj.endpoints().authApiSignUp(aUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String aAccessToken = aSignUpResponse.jsonPath().get("access_token").toString();
		String aUserId = aSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(aSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User A signed up with user id: " + aUserId);

		// Gift {20} points to user A
		Response aSendRewardResponse = pageObj.endpoints().sendMessageToUser(aUserId, dataSet.get("apiKey"), "", "", "",
				"20");
		Assert.assertEquals(aSendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Dashboard Send message to user API");
		utils.logPass("Gifted 20 points to user A");

		// Hit `/api2/mobile/users/ to update user A's secondary_email and phone
		String aSecondaryEmail = "secondary_" + pageObj.iframeSingUpPage().generateEmail();
		String aPhone = Utilities.phonenumber();
		Response updateUserSecondaryEmailResponse = pageObj.endpoints().Api2UpdateUserProfile2(dataSet.get("client"),
				"", dataSet.get("secret"), aAccessToken, "", "", aSecondaryEmail, aPhone);
		Assert.assertEquals(updateUserSecondaryEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 Update user profile");
		utils.logit("Updated user A's secondary_email to: " + aSecondaryEmail + " and phone to: " + aPhone);

		// Hit `api2/mobile/redemptions/redeemable` to generate redemption_code for A
		Response aRedemptionCodeGenerateResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, redeemableId, dataSet.get("locationId"));
		Assert.assertEquals(aRedemptionCodeGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 Generate redemption code using redeemable_id");
		String aRedemptionTrackingCode = aRedemptionCodeGenerateResponse.jsonPath().get("redemption_tracking_code")
				.toString();
		String aRedeemableName = aRedemptionCodeGenerateResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionTrackingCode);
		Assert.assertEquals(aRedeemableName, redeemableName);
		utils.logit("Fetched redemption_tracking_code: " + aRedemptionTrackingCode + " for user A");

		// Hit `/api/pos/redemptions` to redeem A's redemption_code
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionResponse = pageObj.endpoints().posRedemptionOfCode(aUserEmail, date,
				aRedemptionTrackingCode, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		String aPosRedemptionStatus = aPosRedemptionResponse.jsonPath().get("status").toString();
		String aPosRedemptionCode = aPosRedemptionResponse.jsonPath().get("redemption_code").toString();
		Assert.assertTrue(aPosRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(aPosRedemptionCode, aRedemptionTrackingCode);
		utils.logPass("POS redemption successful for User A");

		// Verify the successful redemption on User A's timeline and DB
		pageObj.instanceDashboardPage().navigateToGuestTimeline(aUserEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");
		Assert.assertTrue(title, "Redeemed Redemption title not found on timeline");
		String redeemedRedemptionTitle = pageObj.iframeConfigurationPage()
				.getElementText("guestTimeLine.redeemedRedemptionTitle", "");
		Assert.assertEquals(redeemedRedemptionTitle, "Redeemed Redemption " + aPosRedemptionCode);
		utils.logPass("Redeemed Redemption verified on User A's timeline");
		verifyRedemptionTablesEntries(aUserId, "RedeemableRedemption", "processed");

		// From here, OMM-T4190 starts. Again, hit
		// `api2/mobile/redemptions/redeemable` to generate another
		// redemption_code
		utils.longWaitInSeconds(5); // Adding wait to avoid ["Another transaction is currently accessing the same
									// code. Please try after some time."]
		aRedemptionCodeGenerateResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, redeemableId, dataSet.get("locationId"));
		Assert.assertEquals(aRedemptionCodeGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 Generate redemption code using redeemable_id");
		aRedemptionTrackingCode = aRedemptionCodeGenerateResponse.jsonPath().get("redemption_tracking_code").toString();
		aRedeemableName = aRedemptionCodeGenerateResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionTrackingCode);
		Assert.assertEquals(aRedeemableName, redeemableName);
		utils.logit("Fetched redemption_tracking_code: " + aRedemptionTrackingCode);
		// Hit `/api/pos/redemptions` to redeem A's redemption_code
		// without any email and redemption_code
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionWithoutRedemptionCodeResponse = pageObj.endpoints().posRedemptionOfCode(null, null, null, date,
				null, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionWithoutRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code not matched for POS Perform redemption API");
		aPosRedemptionStatus = aPosRedemptionWithoutRedemptionCodeResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(aPosRedemptionStatus, "User not found");
		utils.logPass("POS redemption failed because of missing email and redemption_code");
		// Now, hit `/api/pos/redemptions` to redeem the same redemption_code
		// in previous step. This time without any email but with redemption_code
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionWithoutEmailResponse = pageObj.endpoints().posRedemptionOfCode(null, null, null, date,
				aRedemptionTrackingCode, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionWithoutEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		aPosRedemptionStatus = aPosRedemptionWithoutEmailResponse.jsonPath().get("status").toString();
		aPosRedemptionCode = aPosRedemptionWithoutEmailResponse.jsonPath().get("redemption_code").toString();
		Assert.assertTrue(aPosRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(aPosRedemptionCode, aRedemptionTrackingCode);
		utils.logPass("POS redemption successful without any email but using the redemption code");

		// Again, hit `api2/mobile/redemptions/redeemable` to generate another
		// redemption_code
		utils.longWaitInSeconds(5); // Adding wait for same reason as previously mentioned
		aRedemptionCodeGenerateResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, redeemableId, dataSet.get("locationId"));
		Assert.assertEquals(aRedemptionCodeGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 Generate redemption code using redeemable_id");
		aRedemptionTrackingCode = aRedemptionCodeGenerateResponse.jsonPath().get("redemption_tracking_code").toString();
		aRedeemableName = aRedemptionCodeGenerateResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionTrackingCode);
		Assert.assertEquals(aRedeemableName, redeemableName);
		utils.logit("Fetched redemption_tracking_code: " + aRedemptionTrackingCode);
		// Now, hit `/api/pos/redemptions` to redeem the redemption_code
		// with only secondary email along with redemption_code
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionWithSecondEmailResponse = pageObj.endpoints().posRedemptionOfCode(null, aSecondaryEmail,
				null, date, aRedemptionTrackingCode, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionWithSecondEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		aPosRedemptionStatus = aPosRedemptionWithSecondEmailResponse.jsonPath().get("status").toString();
		aPosRedemptionCode = aPosRedemptionWithSecondEmailResponse.jsonPath().get("redemption_code").toString();
		Assert.assertTrue(aPosRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(aPosRedemptionCode, aRedemptionTrackingCode);
		utils.logPass("POS redemption successful with only secondary email along with the redemption code");

		// Again hit `api2/mobile/redemptions/redeemable` to generate redemption_code
		utils.longWaitInSeconds(5); // Adding wait for same reason as previously mentioned
		aRedemptionCodeGenerateResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, redeemableId, dataSet.get("locationId"));
		Assert.assertEquals(aRedemptionCodeGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 Generate redemption code using redeemable_id");
		aRedemptionTrackingCode = aRedemptionCodeGenerateResponse.jsonPath().get("redemption_tracking_code").toString();
		aRedeemableName = aRedemptionCodeGenerateResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedemptionTrackingCode);
		Assert.assertEquals(aRedeemableName, redeemableName);
		utils.logit("Fetched redemption_tracking_code: " + aRedemptionTrackingCode);
		// Now, hit `/api/pos/redemptions` to redeem the redemption_code
		// with only phone number along with redemption_code
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		key = CreateDateTime.getTimeDateString();
		Response aPosRedemptionWithPhoneResponse = pageObj.endpoints().posRedemptionOfCode(null, null, aPhone, date,
				aRedemptionTrackingCode, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(aPosRedemptionWithPhoneResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS Perform redemption API");
		aPosRedemptionStatus = aPosRedemptionWithPhoneResponse.jsonPath().get("status").toString();
		aPosRedemptionCode = aPosRedemptionWithPhoneResponse.jsonPath().get("redemption_code").toString();
		Assert.assertTrue(aPosRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(aPosRedemptionCode, aRedemptionTrackingCode);
		utils.logPass("POS redemption successful with only phone number along with the redemption code");

	}

	@Test(description = "SQ-T7008: Verify the Online Ordering API using Auth Token of (different user on the same business) for (discount_type=reward); "
			+ "SQ-T7009: Verify the Online Ordering API using Auth Token of (different user on the same business) (discount_type= redeemable)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7008_onlineOrderingApiForRedemptionCodeGeneration() throws Exception {
		// This test case covers OMM-T4195 and OMM-T4196

		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Create Redeemable via API {$5 Flat Discount Redeemable needing 10 points to
		// redeem}
		String redeemableName1 = "T7008_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName1);
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName1).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(5.0).build())
				.setAutoApplicable(false).setPoints(10).setApplicable_as_loyalty_redemptionFlag(false)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID1 = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID1);
		utils.logit(
				redeemableName1 + " redeemable created successfully. API response: " + redeemableResponse.prettyPrint());
		String getRedeemableIdQuery1 = redeemableQuery.replace("$external_id", redeemableExternalID1);
		String redeemableId1 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");

		// Signup user A
		String aUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response aSignUpResponse = pageObj.endpoints().authApiSignUp(aUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String aAccessToken = aSignUpResponse.jsonPath().get("access_token").toString();
		String aUserId = aSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(aSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User A signed up with user id: " + aUserId);

		// Send reward to user A using redeemable_id and also gift {50} points
		Response aSendRewardResponse = pageObj.endpoints().sendMessageToUser(aUserId, dataSet.get("apiKey"), "",
				redeemableId1, "", "50");
		Assert.assertEquals(aSendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Dashboard Send reward to user API");
		utils.logit("Sent reward to user A with redeemable id: " + redeemableId1);
		// Get Reward ID for user A
		Response aOfferResponse = pageObj.endpoints().getUserOffers(aAccessToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(aOfferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Mobile API2 Get User Offers");
		String aRewardId = aOfferResponse.jsonPath().get("rewards[0].reward_id").toString();
		Assert.assertNotNull(aRewardId);
		utils.logit("Fetched reward_id: " + aRewardId + " from Get User Offers API for user A");

		// Hit `/api2/mobile/redemptions/reward` to generate redemption_code for A
		Response aGenerateRedemptionCodeResponse = pageObj.endpoints().Api2RedemptionWithRewardIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, aRewardId, dataSet.get("locationId"));
		Assert.assertEquals(aGenerateRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 create redemption using reward_id");
		String aRewardRedemptionCode = aGenerateRedemptionCodeResponse.jsonPath().get("redemption_tracking_code")
				.toString();
		String aRedeemableName = aGenerateRedemptionCodeResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRewardRedemptionCode);
		Assert.assertEquals(aRedeemableName, redeemableName1);
		utils.logPass("Redemption tracking code: " + aRewardRedemptionCode + " generated successfully for user A's reward");

		// Signup user B on same business
		String bUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response bSignUpResponse = pageObj.endpoints().authApiSignUp(bUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String bAuthToken = bSignUpResponse.jsonPath().get("authentication_token").toString();
		String bUserId = bSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(bSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User B signed up with user id: " + bUserId);

		// Hit `/api/auth/redemptions/online_order` with B's token and A's
		// redemption_code (discount_type=redemption_code)
		Response bOnlineRewardRedemptionResponse = pageObj.endpoints().authOnlineCouponPromoRedemption(bAuthToken,
				dataSet.get("client"), dataSet.get("secret"), aRewardRedemptionCode);
		Assert.assertEquals(bOnlineRewardRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redemption_code");
		String bOnlineRewardRedemptionStatus = bOnlineRewardRedemptionResponse.jsonPath().get("status").toString();
		String bOnlineRewardRedemptionId = bOnlineRewardRedemptionResponse.jsonPath().get("redemption_id");
		Assert.assertEquals(bOnlineRewardRedemptionStatus, "Code " + aRewardRedemptionCode + " not found");
		Assert.assertNull(bOnlineRewardRedemptionId);
		utils.logPass("Online Order redemption failed for User B using User A's redemption code of reward");

		// Create Redeemable via API {$50 Flat Discount Redeemable needing 50 points to
		// redeem}
		String redeemableName2 = "T7009_AutomationRedeemableFlatDiscount_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName2);
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName2).setAutoApplicable(true)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(50.0).build())
				.setAutoApplicable(false).setPoints(50).setApplicable_as_loyalty_redemptionFlag(true)
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID2);
		utils.logit(redeemableName2 + " redeemable created successfully. API response: "
				+ redeemableResponse2.prettyPrint());
		String getRedeemableIdQuery2 = redeemableQuery.replace("$external_id", redeemableExternalID2);
		String redeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");

		// Hit `/api2/mobile/redemptions/redeemable` to generate redemption_code for A
		Response aRedemptionCodeGenerateResponse = pageObj.endpoints().Api2RedemptionWitReedemableIdAndLocationId(
				dataSet.get("client"), dataSet.get("secret"), aAccessToken, redeemableId2, dataSet.get("locationId"));
		Assert.assertEquals(aRedemptionCodeGenerateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code not matched for Mobile API2 Generate redemption code using redeemable_id");
		String aRedeemableRedemptionCode = aRedemptionCodeGenerateResponse.jsonPath().get("redemption_tracking_code")
				.toString();
		String aRedeemableNameWithLocation = aRedemptionCodeGenerateResponse.jsonPath().get("redeemable_name").toString();
		Assert.assertNotNull(aRedeemableRedemptionCode);
		Assert.assertEquals(aRedeemableNameWithLocation, redeemableName2);
		utils.logit("Redemption tracking code: " + aRedeemableRedemptionCode
				+ " generated successfully for user A's redeemable");

		// Hit `/api/auth/redemptions/online_order` with B's token and A's
		// redemption_code (discount_type=redemption_code)
		Response bOnlineRedeemableRedemptionResponse = pageObj.endpoints().authOnlineCouponPromoRedemption(bAuthToken,
				dataSet.get("client"), dataSet.get("secret"), aRedeemableRedemptionCode);
		Assert.assertEquals(bOnlineRedeemableRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redemption_code");
		String bOnlineRedeemableRedemptionStatus = bOnlineRedeemableRedemptionResponse.jsonPath().get("status")
				.toString();
		String bOnlineRedeemableRedemptionId = bOnlineRedeemableRedemptionResponse.jsonPath().get("redemption_id");
		Assert.assertEquals(bOnlineRedeemableRedemptionStatus, "Code " + aRedeemableRedemptionCode + " not found");
		Assert.assertNull(bOnlineRedeemableRedemptionId);
		utils.logPass("Online Order redemption failed for User B using User A's redemption code of redeemable");

	}

	@Test(description = "SQ-T7023: Verify the Online Ordering API for the generation of redemption_code for (discount_type=redemption_code= COUPON); "
			+ "SQ-T7024: Verify the Online Ordering API using Auth Token of (different user on the same business) (discount_type=redemption_code= COUPON)", groups = "regression")
	@Owner(name = "Vaibhav Agnihotri")
	public void T7023_onlineOrderingApiForCouponRedemptionCode() throws Exception {
		/*
		 * This test case covers OMM-T4194 and OMM-T4197. Pre-requisite: Needs coupon
		 * campaign whose Usage Type=Directly processed at POS, Dynamic Generation,
		 * Number of Guests=0, Uses per Guest=1000, Gift Type=$ Off, Amount Discount=2
		 */

		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Sign up user A using Auth API
		String aUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response aSignUpResponse = pageObj.endpoints().authApiSignUp(aUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String aAuthToken = aSignUpResponse.jsonPath().get("authentication_token").toString();
		String aUserId = aSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(aSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User A signed up with user id: " + aUserId);

		// Hit `/api2/dashboard/dynamic_coupons` to get Coupon code for A
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints().postDynamicCoupon(dataSet.get("apiKey"),
				aUserEmail, dataSet.get("campaignUuid"));
		Assert.assertEquals(postDynamicCouponScheduledEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, 
				"Status code not matched for Dashboard API Dynamic Coupon generation");
		String couponCode = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");
		Assert.assertTrue(!couponCode.isEmpty(), "Coupon code is empty");

		// Hit `/api/auth/redemptions/online_order` with redemption_code
		// (discount_type=redemption_code)
		Response onlineCouponRedemptionResponse = pageObj.endpoints().authOnlineCouponPromoRedemption(aAuthToken,
				dataSet.get("client"), dataSet.get("secret"), couponCode);
		Assert.assertEquals(onlineCouponRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redemption_code");
		String onlineCouponRedemptionStatus = onlineCouponRedemptionResponse.jsonPath().get("status").toString();
		String onlineCouponRedemptionCategory = onlineCouponRedemptionResponse.jsonPath().get("category").toString();
		String onlineCouponRedemptionCode = onlineCouponRedemptionResponse.jsonPath().get("redemption_code");
		Assert.assertTrue(onlineCouponRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(onlineCouponRedemptionCategory, "redeemable");
		Assert.assertEquals(onlineCouponRedemptionCode, couponCode);
		utils.logPass("Online Order API redemption is successful with redemption_code: " + couponCode);

		// Sign up user B on same business
		String bUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response bSignUpResponse = pageObj.endpoints().authApiSignUp(bUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String bAuthToken = bSignUpResponse.jsonPath().get("authentication_token").toString();
		String bUserId = bSignUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(bSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User B signed up with user id: " + bUserId);

		// Hit `/api/auth/redemptions/online_order` with B's Auth token and A's coupon
		// code
		Response bOnlineCouponRedemptionResponse = pageObj.endpoints().authOnlineCouponPromoRedemption(bAuthToken,
				dataSet.get("client"), dataSet.get("secret"), couponCode);
		Assert.assertEquals(bOnlineCouponRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redemption_code");
		String bOnlineCouponRedemptionStatus = bOnlineCouponRedemptionResponse.jsonPath().get("status").toString();
		String bOnlineCouponRedemptionCode = bOnlineCouponRedemptionResponse.jsonPath().get("redemption_code");
		Assert.assertEquals(bOnlineCouponRedemptionStatus, "Code " + couponCode + " not found");
		Assert.assertNull(bOnlineCouponRedemptionCode);
		utils.logPass("Online Order redemption failed for User B using User A's coupon code");

		// Set enable_user_validation_on_redemption_code to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag1"), businessId);
	}

	@Test(description = "SQ-T2865: Verify admin is able to create Promo Campaign and perform redemption using promo code; "
			+ "SQ-T7011: Verify the Online Ordering API for the generation of redemption_code for (discount_type=redemption_code= PROMO)", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T2865_verifyAdminIsAbleToCreatePromoCampaign() throws Exception {
		// This test case covers OMM-T4193 and OMM-T4158
		
		// Set enable_user_validation_on_redemption_code to true
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", dataSet.get("dbFlag1"), businessId);

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		
		// Create Promo campaign
		campaignName = "AutomationPromoCampaign" + CreateDateTime.getTimeDateString();
		pageObj.campaignspage().selectOfferdrpValue("Promo");
		pageObj.campaignspage().clickNewCampaignBtn();
		String promoCode = "P" + CreateDateTime.getTimeDateAsneed("HHmmssddMM");
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(campaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setPromoCampaignWhomDetails(dataSet.get("noOfGuests"), dataSet.get("giftType"),
				dataSet.get("amount"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().setCamTimeZone("(GMT+05:30) New Delhi ( IST )");
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// User creation using Auth API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userId = signUpResponse.jsonPath().get("user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code not matched for Auth API User Signup");
		utils.logit("User signed up with user id: " + userId);

		// Hit `/api/pos/redemptions` for the redemption of Promo code
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedemptionResponse = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, promoCode, key,
				txn, dataSet.get("locationKey"));
		Assert.assertEquals(posRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for POS redemption API");
		String posRedemptionCode = posRedemptionResponse.jsonPath().get("redemption_code").toString();
		String posRedemptionStatus = posRedemptionResponse.jsonPath().get("status").toString();
		String posRedemptionCategory = posRedemptionResponse.jsonPath().get("category").toString();
		Assert.assertEquals(posRedemptionCode, promoCode);
		Assert.assertTrue(posRedemptionStatus.contains("Redeemed at "));
		Assert.assertEquals(posRedemptionCategory, "redeemable");
		utils.logPass("POS Redemption API is successful with redemption_code: " + posRedemptionCode);

		// Validate promo code redemption on guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		String redeemedCouponStatus = pageObj.guestTimelinePage().verifyRedeemedCouponCode();
		Assert.assertTrue(redeemedCouponStatus.contains(promoCode),
				"Redeemed promo code details did not display on timeline");
		utils.logit("pass", "Promo campaign with POS redemption validated successfully on timeline");

		// Hit `/api/auth/redemptions/online_order` with redemption_code
		// (discount_type=redemption_code)
		Response onlineRewardRedemptionResponse = pageObj.endpoints().authOnlineCouponPromoRedemption(authToken,
				dataSet.get("client"), dataSet.get("secret"), posRedemptionCode);
		Assert.assertEquals(onlineRewardRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code not matched for Auth API Online Order redemption using redemption_code");
		String onlineRewardRedemptionStatus = onlineRewardRedemptionResponse.jsonPath().get("status").toString();
		String onlineRewardRedemptionCode = onlineRewardRedemptionResponse.jsonPath().get("redemption_code");
		Assert.assertTrue(onlineRewardRedemptionStatus.contains("Please HONOR it."));
		Assert.assertEquals(onlineRewardRedemptionCode, posRedemptionCode);
		utils.logit("pass", "Online Order API redemption is successful with redemption_code: " + posRedemptionCode);

		// Set enable_user_validation_on_redemption_code to false
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessesQuery, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", dataSet.get("dbFlag1"), businessId);

	}

	// Helper method to verify redemption tables entries
	public void verifyRedemptionTablesEntries(String userId, String redemptionsType, String redemptionCodesStatus)
			throws Exception {
		// Verify the type in redemptions table
		String redemptionsQuery = "SELECT type,redemption_code_id FROM redemptions WHERE user_id = $user_id AND business_id = $business_id;";
		redemptionsQuery = redemptionsQuery.replace("$user_id", userId).replace("$business_id", businessId);
		String[] redemptionsColumns = { "type", "redemption_code_id" };
		List<Map<String, String>> redemptionsValues = DBUtils.executeQueryAndGetMultipleColumns(env, redemptionsQuery,
				redemptionsColumns);
		Assert.assertEquals(redemptionsValues.get(0).get("type"), redemptionsType);
		String redemptionCodeId = redemptionsValues.get(0).get("redemption_code_id");

		// Verify the status in redemption_codes table
		String redemptionCodesQuery = "SELECT status FROM redemption_codes WHERE id = $id AND business_id = $business_id;";
		redemptionCodesQuery = redemptionCodesQuery.replace("$id", redemptionCodeId).replace("$business_id",
				businessId);
		String status = DBUtils.executeQueryAndGetColumnValue(env, redemptionCodesQuery, "status");
		Assert.assertEquals(status, redemptionCodesStatus);

		// Verify the entry in redemption_logs table
		String redemptionLogsQuery = "SELECT id FROM redemption_logs WHERE redemption_code_id = $redemption_code_id AND business_id = $business_id;";
		redemptionLogsQuery = redemptionLogsQuery.replace("$redemption_code_id", redemptionCodeId)
				.replace("$business_id", businessId);
		String id = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, redemptionLogsQuery, "id", 40);
		Assert.assertTrue(!id.isEmpty(), "No entry found in redemption_logs table for the redemption_code_id");
		utils.logit("pass",
				"Verified that for user ID " + userId + ", table: `redemptions` has type as " + redemptionsType
						+ ", `redemption_codes` has status as " + redemptionCodesStatus
						+ " and has entry in `redemption_logs`");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID1);
		utils.deleteLISQCRedeemable(env, null, null, redeemableExternalID2);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}