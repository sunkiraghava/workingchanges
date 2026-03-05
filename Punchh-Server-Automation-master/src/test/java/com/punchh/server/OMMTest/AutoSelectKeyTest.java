package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
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

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AutoSelectKeyTest {
	private static Logger logger = LogManager.getLogger(AutoSelectKeyTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Properties props;
	Utilities utils;
	String externalUID, deactivateDate;
	private ApiPayloadObj apipayloadObj;
	private OfferIngestionUtilities offerUtils;
	private String lisExternalID, qcExternalID, redeemableExternalID, redeemableExternalID2;


	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
		apipayloadObj = new ApiPayloadObj();
		deactivateDate = CreateDateTime.getFutureDate(0);
		offerUtils = new OfferIngestionUtilities(driver);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		props = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils = new Utilities(driver);
		utils.logit(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T4447 Verify auto_select key is returned in 1.0 API's response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected || "
			+ "SQ-T4451 Verify auto_select key is not returned in api2/mobile/users/balance API response for loyalty_reward when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected", priority = 0, groups = {
					"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4447_AutoSelectKey() throws Exception {
		// ===================== Deactivate old redeemables =====================
//		String deactivateQuery = "UPDATE redeemables SET deactivated_at = '" + deactivateDate + "' WHERE business_id = "+ dataSet.get("business_id") + " AND name LIKE 'AutoSelectRedeemableAutoRedemptionON_T4447_%';";
//		@SuppressWarnings("static-access")
//		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
//		utils.logit("pass", rs + " redeemables deactivated for business " + dataSet.get("business_id"));

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable with Auto Redemption =
		// ON=====================
		String redeemableName_AutoRedemptionFlagON = "AutoSelectRedeemableAutoRedemptionON_T4447_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagON + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️⃣ Configure Business UI =====================
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion(redeemableName_AutoRedemptionFlagON);
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "20", dbRedeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successfull");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		Assert.assertNotEquals(rewardId, null, "Reward Id is null");
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Mobile User Balance -> Step-1
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");

		String userBalanceRewardIdMobile = userBalanceResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("API2 User Balance is successful");

		boolean flag = pageObj.guestTimelinePage().verifyApiResponseVariable(userBalanceResponse,
				userBalanceRewardIdMobile, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag, true, "In Mobile User Balance Api response auto_select variable is present");
		utils.logPass("In Mobile User Balance Api response auto_select variable is not present");

		// Mobile User Balance -> Step-2
		Response apiV1UserOfferResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"), userEmail,
				utils.decrypt(props.getProperty("password")));
		Assert.assertEquals(apiV1UserOfferResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"V1 Offers API is not working ");
		utils.logPass("V1 User Offers API call successful");

		String userBalanceRewardIdV1Api = apiV1UserOfferResponse.jsonPath().get("reward_id").toString();
		utils.logit("V1 Api User Balance is successful");

		String variableName = apiV1UserOfferResponse.jsonPath().get("auto_select").toString();
		if (utils.textContains(userBalanceRewardIdV1Api, rewardId)) {
			Assert.assertEquals(variableName, "[true]",
					"In V1 User Balance Api response auto_select variable is present");
			utils.logit("In V1 User Balance Api response auto_select variable is not present");
		}

		// Web User Balance -> Step-3
		Response webUserOfferResponse = pageObj.endpoints().webApi(dataSet.get("punchhAppKey"), userEmail,
				utils.decrypt(props.getProperty("password")), dataSet.get("slug"), rewardId);
		Assert.assertEquals(webUserOfferResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"V1 Redemption API is not working ");
		String userBalanceRewardIdWebApi = webUserOfferResponse.jsonPath().get("reward_id").toString();
		utils.logit("V1 Api User Balance is successful");

		String variableName1 = apiV1UserOfferResponse.jsonPath().get("auto_select").toString();
		if (utils.textContains(userBalanceRewardIdWebApi, rewardId)) {
			Assert.assertEquals(variableName1, "[true]",
					"In Web User Balance Api response auto_select variable is present");
			utils.logit("In Web User Balance Api response auto_select variable is not present");
		}

		// call user balance api of auth (/api/auth/users/balance) -> Step-4
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(userInfo.get("authToken"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Auth API Account balance response");
		String userBalanceRewardIdAuth = authUserBalanceResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Auth User Balance is successful");

		boolean flag1 = pageObj.guestTimelinePage().verifyApiResponseVariable(authUserBalanceResponse,
				userBalanceRewardIdAuth, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag1, true, "In Auth User Balance Api response auto_select variable is present");
		utils.logit("In Auth User Balance Api response auto_select variable is not present");

		// Mobile Account Balance -> Step-5
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		String accountBalanceRewardIdMobile = accountBalResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Mobile Account Balance is successful");

		boolean flag2 = pageObj.guestTimelinePage().verifyApiResponseVariable(accountBalResponse,
				accountBalanceRewardIdMobile, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag2, true, "In Mobile Account Balance Api response auto_select variable is present");
		utils.logit("In Mobile Account Balance Api response auto_select variable is not present");

		// fetch user offers/ reward_id using ap2 mobileOffers -> Step-6
		Response offerResponse = pageObj.endpoints().getUserOffers(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 user offers");
		String accountBalanceRewardIdMobileOffer = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful");

		boolean flag5 = pageObj.guestTimelinePage().verifyApiResponseVariable(offerResponse,
				accountBalanceRewardIdMobileOffer, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag5, true, "In Mobile offer Api response auto_select variable is present");
		utils.logit("In Mobile offer Api response auto_select variable is not present");

		// Call user balance api of mobile v1 (/api/mobile/users/balance") -> Step-7
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		String accountBalanceRewardIdSecure = balance_Response.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Secure Account Balance is successful");

		boolean flag3 = pageObj.guestTimelinePage().verifyApiResponseVariable(balance_Response,
				accountBalanceRewardIdSecure, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag3, true, "In Secure User Balance Api response auto_select variable is present");
		utils.logit("In Secure User Balance Api response auto_select variable is not present");

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

	}

	// autoapplicable false will turn off the auto redemption feature
	@Test(description = "SQ-T4446 Verify auto_select key is not returned in 1.0 API's response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Blank", priority = 1, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4446_AutoSelectKey() throws Exception {

		// ===================== Deactivate old redeemables =====================
//		String deactivateQuery = "UPDATE redeemables SET deactivated_at = '" + deactivateDate + "' WHERE business_id = "
//				+ dataSet.get("business_id") + " AND name LIKE 'AutoSelectRedeemableAutoRedemptionOff_T4446_%';";
//		@SuppressWarnings("static-access")
//		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
//		utils.logit("pass", rs + " redeemables deactivated for business " + dataSet.get("business_id"));

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable =====================
		String redeemableName_AutoRedemptionFlagOFF = "AutoSelectRedeemableAutoRedemptionOff_T4446_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagOFF)
				.setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF + " redeemable is Created Redeemable External ID: "
				+ redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// ===================== Configure Business UI =====================
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Unselect");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Unselect");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "20", dbRedeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successfull");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Mobile User Balance -> Step-1
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceRewardIdMobile = userBalanceResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("API2 User Balance is successful");

		boolean flag = pageObj.guestTimelinePage().verifyApiResponseVariable(userBalanceResponse,
				userBalanceRewardIdMobile, rewardId, "rewards", "auto_select");
		Assert.assertEquals(false, flag, "In Mobile User Balance Api response auto_select variable is present");
		utils.logPass("In Mobile User Balance Api response auto_select variable is not present");

		// Mobile User Balance -> Step-2
		Response apiV1UserOfferResponse = pageObj.endpoints().apiV1UserOffers(dataSet.get("punchhAppKey"), userEmail,
				utils.decrypt(props.getProperty("password")));
		Assert.assertEquals(apiV1UserOfferResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"V1 Redemption API is not working ");
		String userBalanceRewardIdV1Api = apiV1UserOfferResponse.jsonPath().get("reward_id").toString();
		utils.logit("V1 Api User Balance is successful");

		String variableName = apiV1UserOfferResponse.jsonPath().get("auto_select").toString();
		if (utils.textContains(userBalanceRewardIdV1Api, rewardId)) {
			Assert.assertTrue(utils.textContains(variableName, "[null]"),
					"In V1 User Balance Api response auto_select variable is present");
			utils.logPass("In V1 User Balance Api response auto_select variable is not present");
		}

		// Web User Balance -> Step-3
		Response webUserOfferResponse = pageObj.endpoints().webApi(dataSet.get("punchhAppKey"), userEmail,
				utils.decrypt(props.getProperty("password")), dataSet.get("slug"), rewardId);
		Assert.assertEquals(webUserOfferResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"V1 Redemption API is not working ");
		String userBalanceRewardIdWebApi = webUserOfferResponse.jsonPath().get("reward_id").toString();
		utils.logit("V1 Api User Balance is successful");

		String variableName1 = apiV1UserOfferResponse.jsonPath().get("auto_select").toString();
		if (utils.textContains(userBalanceRewardIdWebApi, rewardId)) {
			Assert.assertTrue(utils.textContains(variableName1, "[null]"),
					"In Web User Balance Api response auto_select variable is present");
			utils.logPass("In Web User Balance Api response auto_select variable is not present");
		}

		// call user balance api of auth (/api/auth/users/balance) -> Step-4
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(userInfo.get("authToken"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Auth API Account balance response");
		String userBalanceRewardIdAuth = authUserBalanceResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Auth User Balance is successful");

		boolean flag1 = pageObj.guestTimelinePage().verifyApiResponseVariable(authUserBalanceResponse,
				userBalanceRewardIdAuth, rewardId, "rewards", "auto_select");
		Assert.assertEquals(false, flag1, "In Auth User Balance Api response auto_select variable is present");
		utils.logPass("In Auth User Balance Api response auto_select variable is not present");

		// Mobile Account Balance -> Step-5
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		String accountBalanceRewardIdMobile = accountBalResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Mobile Account Balance is successful");

		boolean flag2 = pageObj.guestTimelinePage().verifyApiResponseVariable(accountBalResponse,
				accountBalanceRewardIdMobile, rewardId, "rewards", "auto_select");
		Assert.assertEquals(false, flag2, "In Mobile Account Balance Api response auto_select variable is present");
		utils.logPass("In Mobile Account Balance Api response auto_select variable is not present");

		// fetch user offers/ reward_id using ap2 mobileOffers -> Step-6
		Response offerResponse = pageObj.endpoints().getUserOffers(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String accountBalanceRewardIdMobileOffer = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Api2 user fetch user offers is successful");

		boolean flag5 = pageObj.guestTimelinePage().verifyApiResponseVariable(offerResponse,
				accountBalanceRewardIdMobileOffer, rewardId, "rewards", "auto_select");
		Assert.assertEquals(false, flag5, "In Mobile offer Api response auto_select variable is present");
		utils.logPass("In Mobile offer Api response auto_select variable is not present");

		// Call user balance api of mobile v1 (/api/mobile/users/balance") -> Step-7
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		String accountBalanceRewardIdSecure = balance_Response.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("Secure Account Balance is successful");

		boolean flag3 = pageObj.guestTimelinePage().verifyApiResponseVariable(balance_Response,
				accountBalanceRewardIdSecure, rewardId, "rewards", "auto_select");
		Assert.assertEquals(false, flag3, "In Secure User Balance Api response auto_select variable is present");
		utils.logPass("In Secure User Balance Api response auto_select variable is not present");

	}

	@Test(description = "SQ-T4448 Verify auto_select key is not returned in deals API's response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Blank", priority = 2, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4448_AutoSelectKey() throws Exception {
	
		// ===================== Deactivate old redeemables =====================
//		String deactivateQuery = "UPDATE redeemables SET deactivated_at = '" + deactivateDate + "' WHERE business_id = "
//				+ dataSet.get("business_id") + " AND name LIKE 'AutoSelectRedeemableAutoRedemptionOFF_T4448_%';";
//		@SuppressWarnings("static-access")
//		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
//		utils.logit("pass", rs + " redeemables deactivated for business " + dataSet.get("business_id"));

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable =====================
		// Create Redeemable with above QC
		String redeemableName_AutoRedemptionFlagOFF = "AutoSelectRedeemableAutoRedemptionOFF_T4448_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagOFF)
				.setDistributable(true).setDistributableToAllUsers(true).setPoints(15)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️⃣ Configure Business UI =====================
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Unselect");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Unselect");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "20", dbRedeemableId, "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successfull");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");
		utils.longWaitInSeconds(15);

		// Mobile Api -> List all deals
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				userInfo.get("token"));
		Assert.assertEquals(listdealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals");
		utils.logit("List all deals of Mobile Api is successful");

		boolean flag = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listdealsResponse, "redeemable_id",
				dbRedeemableId, "auto_select");
		Assert.assertEquals(false, flag, "In Mobile Api List all deals response, auto_select variable is present");
		utils.logit("pass", "In Secure User Balance Api response, auto_select variable is not present");

		// Auth Api -> list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(userInfo.get("authToken"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		utils.logit("List all deals of Auth Api is successful");

		boolean flag1 = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listAuthDealsResponse,
				"redeemable_id", dbRedeemableId, "auto_select");
		Assert.assertEquals(false, flag1, "In Auth User Balance Api response auto_select variable is present");
		utils.logit("pass", "In Auth User Balance Api response, auto_select variable is not present");

		// Secure Api -> list all deals
		Response listApi1DealsResponse = pageObj.endpoints().Api1ListAllDeals(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listApi1DealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		utils.logit("List all deals of Secure Api is successful");

		boolean flag2 = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listApi1DealsResponse,
				"redeemable_id", dbRedeemableId, "auto_select");
		Assert.assertEquals(false, flag2, "In Secure User Balance Api response auto_select variable is present");
		utils.logPass("In Secure Api List all deals response, auto_select variable is not present");

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

	}

	@Test(description = "SQ-T4449 Verify auto_select key is returned in deals API's response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected", priority = 3, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4449_AutoSelectKey() throws Exception {

		// ===================== Deactivate old redeemables =====================
//		String deactivateQuery = "UPDATE redeemables SET deactivated_at = '" + deactivateDate + "' WHERE business_id = "
//				+ dataSet.get("business_id") + " AND name LIKE 'AutoSelectRedeemableAutoRedemptionON_T4449_%';";
//		@SuppressWarnings("static-access")
//		int rs = DBUtils.executeUpdateQuery(env, deactivateQuery);
//		utils.logit("pass", rs + " redeemables deactivated for business " + dataSet.get("business_id"));

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// ===================== Create Redeemable =====================
		// Create Redeemable with above QC

		String redeemableName_AutoRedemptionFlagON = "AutoSelectRedeemableAutoRedemptionON_T4449_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setDistributable(true).setDistributableToAllUsers(true).setPoints(15).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️Configure Business UI =====================

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();
		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion(redeemableName_AutoRedemptionFlagON);
		pageObj.dashboardpage().updateCheckBox();

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		utils.longWaitInSeconds(6);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "20", dbRedeemableId, "", "120");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successfull");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Mobile Api -> List all deals
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				userInfo.get("token"));
		Assert.assertEquals(listdealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals");
		utils.logit("List all deals of Mobile Api is successful");

		boolean flag = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listdealsResponse, "redeemable_id",
				dbRedeemableId, "auto_select");
		Assert.assertEquals(flag, true, "In Secure User Balance Api response auto_select variable is not present");
		utils.logPass("In Secure User Balance Api response, auto_select variable is present");

		// Auth Api -> list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(userInfo.get("authToken"),
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 list all deals");
		utils.logit("List all deals of Auth Api is successful");

		boolean flag1 = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listAuthDealsResponse,
				"redeemable_id", dbRedeemableId, "auto_select");
		Assert.assertEquals(flag1, true, "In Auth User Balance Api response auto_select variable is not present");
		utils.logPass("In Auth User Balance Api response, auto_select variable is present");

		// Secure Api -> list all deals
		Response listApi1DealsResponse = pageObj.endpoints().Api1ListAllDeals(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(listApi1DealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 list all deals");
		utils.logit("List all deals of Secure Api is successful");

		boolean flag2 = pageObj.guestTimelinePage().verifyApiResponseVariableArray(listApi1DealsResponse,
				"redeemable_id", dbRedeemableId, "auto_select");
		Assert.assertEquals(flag2, true, "In Secure User Balance Api response auto_select variable is not present");
		utils.logPass("In Secure Api List all deals response, auto_select variable is present");

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

	}

	@Test(description = "SQ-T4451 Verify auto_select key is not returned in api2/mobile/users/balance API response for loyalty_reward when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected", priority = 4, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4451_AutoSelectKey() throws Exception {
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion(dataSet.get("redeemableName_Off"));
		pageObj.dashboardpage().updateCheckBox();

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// =====================️ Create Redeemable where Enable Auto Redemption = OFF
		// =====================
		String redeemableName_AutoRedemptionFlagOFF = "Automation_Deal_Enable_Auto_Redemption_OFF_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagOFF)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️ Signup using mobile api =====================

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", "", "", "120");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Mobile User Balance -> Step-1
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"));
		Assert.assertEquals(userBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user balance");
		String userBalanceRewardIdMobile = userBalanceResponse.jsonPath().get("rewards[0].reward_id").toString();
		utils.logit("API2 User Balance is successful");

		boolean flag = pageObj.guestTimelinePage().verifyApiResponseVariable(userBalanceResponse,
				userBalanceRewardIdMobile, rewardId, "rewards", "auto_select");
		Assert.assertEquals(flag, false, "In Mobile User Balance Api response auto_select variable is present");
		utils.logPass("In Mobile User Balance Api response auto_select variable is not present");
	}

	@Test(description = "SQ-T4450 Verify auto_select key is returned in 2.0 API's response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected", priority = 5, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4450_AutoSelectKey() throws Exception {
		
		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Loyalty Goal Completion");
		pageObj.earningPage().loyaltyGoalCompletion("Base Redeemable");
		pageObj.dashboardpage().updateCheckBox();

		// =====================️ Signup using mobile api ====================
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// =====================️ Create Redeemable where Enable Auto Redemption = ON
		// =====================
		String redeemableName_AutoRedemptionFlagON = "Automation_Deal_Enable_Auto_Redemption_ON_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(10).addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("pass",
				redeemableName_AutoRedemptionFlagON + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableId, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// Step - 1
		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userInfo.get("userID"), "30", "1",
				dataSet.get("item_id"), externalUID, dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");

		String autoSelect1 = autoUnlockResponse1.jsonPath().get("discount_basket_items[0].discount_details.auto_select")
				.toString();
		Assert.assertEquals(autoSelect1, "true", "In POS Auto Unlock, auto_select variable is not present");
		utils.logit("pass", "In POS Auto Unlock, auto_select variable is present");

		// Step - 2
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(
				dataSet.get("locationkeyRedemption2_0"), userInfo.get("userID"), dataSet.get("item_id"), "30",
				externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");
		utils.logit("POS Discount Lookup Api is successful");

		String autoSelect2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].discount_details.auto_select").toString();
		Assert.assertEquals(autoSelect2, "true", "In POS Discount Lookup Api, auto_select variable is not present");
		utils.logit("pass", "In POS Discount Lookup Api, auto_select variable is present");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Step - 3
		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"), "14", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		String autoSelect3 = redemptionResponse.jsonPath().get("discount_basket_items[0].discount_details.auto_select")
				.toString();
		Assert.assertEquals(autoSelect3, "true", "In POS Discount Lookup Api, auto_select variable is not present");
		utils.logit("pass", "In POS Discount Lookup Api, auto_select variable is present");

		// =====================️ Create Redeemable where Enable Auto Redemption = ON
		// =====================
		String redeemableName_AutoRedemptionFlagON_2 = "Automation_Deal_Enable_Auto_Redemption_ON_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder2 = apipayloadObj.redeemableBuilder();
		String redeemablePayload2 = builder2.startNewData().setName(redeemableName_AutoRedemptionFlagON_2)
				.setDescription(redeemableName_AutoRedemptionFlagON_2)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse2 = pageObj.endpoints().createRedeemable(redeemablePayload2, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponse2.jsonPath().getString("results[0].external_id");
		utils.logit("pass", redeemableName_AutoRedemptionFlagON_2 + " Redeemable created with External ID: "
				+ redeemableExternalID2);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableId2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId2);

		// Signup using mobile api
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo2 = offerUtils.signUpUser(userEmail2, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userInfo2.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableId2, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(userInfo2.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId2);
		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		// Step - 4
		// Secure Adding reward discount basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(userInfo2.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "reward", rewardId1);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Secure API add discount to basket is successful");
		String discount_basket_item_id = discountBasketResponse3.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		String autoSelect4 = discountBasketResponse3.jsonPath()
				.get("discount_basket_items[0].discount_details.auto_select").toString();
		Assert.assertEquals(autoSelect4, "true",
				"In Secure Adding reward discount basket Api, auto_select variable is not present");
		utils.logit("pass", "In Secure Adding reward discount basket Api, auto_select variable is present");

		// Step - 5
		// Secure Api fetch active basket
		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(
				userInfo2.get("token"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(basketDiscountDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 404 did not matched for fetch active basket error that state User don't have any active basket");
		utils.logit("Secure Api fetch active basket is Successfull");

		String autoSelect5 = basketDiscountDetailsResponse.jsonPath()
				.get("discount_basket_items[0].discount_details.auto_select").toString();
		Assert.assertEquals(autoSelect5, "true",
				"In Secure Api fetch active basket, auto_select variable is not present");
		utils.logit("pass", "In Secure Api fetch active basket, auto_select variable is present");

		// Step - 6
		// Secure Api remove discount basket
		Response deleteBasketResponse = pageObj.endpoints().removeDiscountBasketExtUIDSecureAPI(userInfo2.get("token"),
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		utils.logit("pass", "Secure API remove discount from basket is successful with external uid");

		// Step - 8
		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationkeyRedemption2_0"), userInfo.get("userID"), "30", "1", dataSet.get("item_id"),
				externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		utils.logit("POS Process Batch Redemption Api is successful");

		String autoSelect6 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].discount_details.auto_select").toString();
		Assert.assertEquals(autoSelect6, "true",
				"In POS Process Batch Redemption Api, auto_select variable is not present");
		utils.logit("pass", "In POS Process Batch Redemption Api, auto_select variable is present");

	}

	@Test(description = "SQ-T4452 Verify auto_select_strategy key is returned blank in api2/mobile/meta.json API response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Blank", priority = 6, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4452_AutoSelectKey() throws Exception {
		
		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Unselect");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Unselect");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Mobile meta api
		Response userMeta = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userMeta.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("Mobile Meta API is successfully");

		String strategy = userMeta.jsonPath().get("auto_select_strategy").toString();
		Assert.assertEquals(strategy, "", "Auto Select Strategy is not visible");
		utils.logit("pass", "Verified that Auto Select Strategy is selected as : " + dataSet.get("autoStrategy"));

		String multiple_redemptions = userMeta.jsonPath().getString("multiple_redemptions_enabled");
		Assert.assertEquals(multiple_redemptions, "true", "multiple redemptions enabled is false");
		utils.logit("pass", "Verified that multiple redemptions enabled is selected as : " + multiple_redemptions);

		String autoRedemption = userMeta.jsonPath().getString("auto_redemption");
		Assert.assertEquals(autoRedemption, "true", "auto_redemption  is false");
		utils.logit("pass", "Verified that auto redemption is selected as : " + autoRedemption);
	}

	@Test(description = "SQ-T4453 Verify auto_select_strategy key and value is returned in api2/mobile/meta.json API response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On and Auto redemption discounts -> Offers, Subscriptions selected", priority = 7, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4453_AutoSelectKey() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Select");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// Mobile meta api
		Response userMeta = pageObj.endpoints().metaAPI2SubscriptionCancelReason(dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(userMeta.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("pass", "Mobile Meta API is successfully");

		String strategy = userMeta.jsonPath().getString("auto_select_strategy");
		Assert.assertEquals(strategy, dataSet.get("autoStrategy"), "Auto Select Strategy is not visible");
		utils.logit("pass", "Verified that Auto Select Strategy is selected as : " + dataSet.get("autoStrategy"));

		String multiple_redemptions = userMeta.jsonPath().getString("multiple_redemptions_enabled");
		Assert.assertEquals(multiple_redemptions, "true", "multiple redemptions enabled is false");
		utils.logit("pass", "Verified that multiple redemptions enabled is selected as : " + multiple_redemptions);

		String autoRedemption = userMeta.jsonPath().getString("auto_redemption");
		Assert.assertEquals(autoRedemption, "true", "auto_redemption  is false");
		utils.logit("pass", "Verified that auto redemption is selected as : " + autoRedemption);
	}

	@Test(description = "SQ-T4454 Verify auto_select_strategy key is not returned in api/pos/meta API response when allow_multiple_redemptions -> true and Enable Auto-redemption -> On", priority = 8, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4454_AutoSelectKey() throws Exception {
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// POS meta API
		Response response2 = pageObj.endpoints().posProgramMeta(dataSet.get("locationkey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos program meta api");
		utils.logit("POS Meta API is successfully");

		String multiple_redemptions = response2.jsonPath().getString("multiple_redemptions_enabled");
		Assert.assertEquals(multiple_redemptions, "true", "multiple redemptions enabled is false");
		utils.logit("pass", "Verified that multiple redemptions enabled is selected as : " + multiple_redemptions);

		String autoRedemption = response2.jsonPath().getString("auto_redemption");
		Assert.assertEquals(autoRedemption, "true", "auto_redemption  is false");
		utils.logit("pass", "Verified that auto redemption is selected as : " + autoRedemption);

		// Secure Meta API
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 cards");
		utils.logit("Secure Meta API is successfully");

		String multiple_redemptions1 = cardsResponse.jsonPath().getString("multiple_redemptions_enabled");
		Assert.assertEquals(multiple_redemptions1, "[true]", "multiple redemptions enabled is false");
		utils.logit("pass", "Verified that multiple redemptions enabled is selected as : " + multiple_redemptions1);

		// Auth Meta API
		Response authCardsResponse = pageObj.endpoints().authCardsAPI(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authCardsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auth Meta API ");
		utils.logit("Auth Meta API is successfully");

		String multiple_redemptions2 = authCardsResponse.jsonPath().getString("multiple_redemptions_enabled");
		Assert.assertEquals(multiple_redemptions2, "true", "multiple redemptions enabled is false");
		utils.logit("pass", "Verified that multiple redemptions enabled is selected as : " + multiple_redemptions2);
	}

	@Test(description = "SQ-T4442 Verify reward associated with redeemable having Enable Auto Redemption flag On are added to discount_basket via api/pos/discounts/auto_select API when Auto redemption discounts has Only Offers selected", priority = 9, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4442_AutoSelectKey() throws Exception {
		
		// login to instance
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Offers", "Select");
		pageObj.cockpitRedemptionsPage().setAutoRedemptionDiscounts("Subscription", "Unselect");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.dashboardpage().updateCheckBox();

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// =====================️ Create Redeemable where Enable Auto Redemption = ON
		// =====================

		String redeemableName_AutoRedemptionFlagON = "Automation_Deal_Enable_Auto_Redemption_ON_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").setPoints(10).addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("pass",
				redeemableName_AutoRedemptionFlagON + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️ Signup using mobile api =====================
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableId, "", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("pass", "Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userInfo.get("userID"), "30", "1",
				dataSet.get("item_id"), externalUID, dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");

		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(autoUnlockResponse1,
				"discount_basket_items", "discount_id", rewardId, "discount_details.auto_select");
		Assert.assertEquals(value1, "true", "In POS Auto Select Api response auto_select variable is not present");
		utils.logPass("In POS Auto Select Api response auto_select variable is present");

		// =====================️ Create Redeemable where Enable Auto Redemption = ON
		// =====================

		String redeemableName_AutoRedemptionFlagON_2 = "Automation_Deal_Enable_Auto_Redemption_ON_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builderOn2 = apipayloadObj.redeemableBuilder();
		String redeemablePayloadOn2 = builderOn2.startNewData().setName(redeemableName_AutoRedemptionFlagON_2)
				.setDescription(redeemableName_AutoRedemptionFlagON_2)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponseOn2 = pageObj.endpoints().createRedeemable(redeemablePayloadOn2,
				dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponseOn2.jsonPath().getString("results[0].external_id");
		utils.logit("pass", redeemableName_AutoRedemptionFlagON_2 + " Redeemable created with External ID: "
				+ redeemableExternalID2);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableIdOn2 = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableIdOn2);

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableIdOn2, "", "120");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableIdOn2);
		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userInfo.get("userID"), "30", "1",
				dataSet.get("item_id"), externalUID, dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");

		String value = pageObj.guestTimelinePage().verifyDiscountBasketVariable(autoUnlockResponse,
				"discount_basket_items", "discount_id", rewardId1, "discount_details.auto_select");
		Assert.assertEquals(value, "true", "In POS Auto Select Api response auto_select variable is not present");
		utils.logPass("In POS Auto Select Api response auto_select variable is present");
	}

	@Test(description = "SQ-T4443 Verify reward associated with redeemable having Enable Auto Redemption flag Off are not added to discount_basket via api/auth/discounts/auto_select API when Auto redemption discounts has Only Offers selected", priority = 10, groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4443_AutoSelectKey() throws Exception {
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// =====================️ Create Redeemable where Enable Auto Redemption = OFF
		// =====================
		String redeemableName_AutoRedemptionFlagOFF = "Automation_Deal_Enable_Auto_Redemption_OFF_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagOFF)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️ Signup using mobile api =====================
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableId, "", "120");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("pass", "Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1",
				dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"), "14", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		String value1 = pageObj.guestTimelinePage().verifyDiscountBasketVariable(redemptionResponse,
				"discount_basket_items", "discount_id", rewardId, "discount_details.auto_select");
		Assert.assertEquals(value1, null, "In Auth Auto Select Api response auto_select variable is present");
		utils.logPass("In Auth Auto Select Api response auto_select variable is not present");

		// =====================️ Create Redeemable where Enable Auto Redemption = OFF
		// =====================
		String redeemableName_AutoRedemptionFlagOFF_2 = "Automation_Deal_Enable_Auto_Redemption_OFF_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builderOff = apipayloadObj.redeemableBuilder();
		String redeemablePayloadOff = builderOff.startNewData().setName(redeemableName_AutoRedemptionFlagOFF_2)
				.setDescription(redeemableName_AutoRedemptionFlagOFF_2)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		Response redeemableResponseOff = pageObj.endpoints().createRedeemable(redeemablePayloadOff,
				dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponseOff.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF_2 + " Redeemable created with External ID: "
				+ redeemableExternalID2);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableIdOff = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableIdOff);

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableIdOff, "", "120");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableIdOff);
		utils.logit("Reward id " + rewardId1 + " is generated successfully ");

		// Auth Auto select API
		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), userInfo.get("token"), "14", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		String value = pageObj.guestTimelinePage().verifyDiscountBasketVariable(redemptionResponse1,
				"discount_basket_items", "discount_id", rewardId, "discount_details.auto_select");
		Assert.assertEquals(value, null, "In Auth Auto Select Api response auto_select variable is present");
		utils.logPass("In Auth Auto Select Api response auto_select variable is not present");
	}

	@Test(description = "SQ-T4444 Verify deal associated with redeemable having Enable Auto Redemption flag Off are not added to discount_basket via api/pos/discounts/auto_select API when Auto redemption discounts has Only Offers selected || "
			+ "SQ-T4445	Verify deal associated with redeemable having Enable Auto Redemption flag On are added to discount_basket via api/pos/discounts/auto_select API when Auto redemption discounts has Offers, Subscriptions selected", priority = 11, groups = {
					"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T4444_AutoSelectKey() throws Exception {
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// ===================== Create LIS =====================
		String lisName = "AutoSelect_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("LIS Created: " + response.prettyPrint());
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "AutoSelect_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createLIS API");
		utils.logit("API response: " + qcResponse.prettyPrint());

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// =====================️ Create Redeemable where Enable Auto Redemption = OFF
		// =====================
		String redeemableName_AutoRedemptionFlagOFF = "Automation_Deal_Enable_Auto_Redemption_OFF_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagOFF)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(true)
				.setDistributableToAllUsers(true).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setPoints(15).addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit(redeemableName_AutoRedemptionFlagOFF + " Redeemable created with External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// =====================️ Signup using mobile api =====================

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Redeemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableId, "", "120");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableId);
		utils.logit("Reward id " + rewardId + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userInfo.get("userID"), "30", "1",
				dataSet.get("item_id"), externalUID, dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");

		String discountBasketItems = autoUnlockResponse1.jsonPath().get("discount_basket_items").toString();
		Assert.assertEquals(discountBasketItems, "[]",
				"In POS Auto Select Api response auto_select variable is present");
		utils.logPass("In POS Auto Select Api response auto_select variable is not present");

		// =====================️ Create Redeemable where Enable Auto Redemption = ON
		// =====================

		// Create Redeemable with above QC where Enable Auto Redemption = ON
		String redeemableName_AutoRedemptionFlagON = "Automation_Deal_Enable_Auto_Redemption_ON_"
				+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builderOn = apipayloadObj.redeemableBuilder();
		String redeemablePayloadOn = builderOn.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setBooleanField("allow_for_support_gifting", true).setDistributable(true)
				.setDistributableToAllUsers(true).setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setPoints(15).addCurrentData().build();

		Response redeemableResponseOn = pageObj.endpoints().createRedeemable(redeemablePayloadOn,
				dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID2 = redeemableResponseOn.jsonPath().getString("results[0].external_id");
		utils.logit(
				redeemableName_AutoRedemptionFlagON + " Redeemable created with External ID: " + redeemableExternalID2);

		// Get Redeemable ID from DB
		String query2 = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID2 + "'";
		String dbRedeemableIdOn = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableIdOn);

		// send reward amount to user Reedemable
		Response sendRewardResponseOn = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"),
				dataSet.get("apiKey"), "40", dbRedeemableIdOn, "", "120");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponseOn.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Api2  send reward amount to user is successful");

		// get reward id
		String rewardIdOn = pageObj.redeemablesPage().getRewardId(userInfo.get("token"), dataSet.get("client"),
				dataSet.get("secret"), dbRedeemableIdOn);
		utils.logit("Reward id " + rewardIdOn + " is generated successfully ");

		// POS Auto Unlock
		Response autoUnlockResponse = pageObj.endpoints().autoSelectPosApi(userInfo.get("userID"), "30", "1",
				dataSet.get("item_id"), externalUID, dataSet.get("locationkeyRedemption2_0"));
		Assert.assertEquals(autoUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Unlock Api is successful");

		String value = pageObj.guestTimelinePage().verifyDiscountBasketVariable(autoUnlockResponse,
				"discount_basket_items", "discount_id", rewardIdOn, "discount_details.auto_select");
		Assert.assertEquals(value, "true", "In POS Auto Select Api response auto_select variable is not present");
		utils.logPass("In POS Auto Select Api response auto_select variable is present");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete created LIS, QC, Redeemable
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		if (redeemableExternalID2 != null && !redeemableExternalID2.isEmpty()) {
			utils.deleteLISQCRedeemable(env, "", "", redeemableExternalID2);
		}
		pageObj.utils().clearDataSet(dataSet);
		utils.logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		utils.logit("Browser closed");
	}

}