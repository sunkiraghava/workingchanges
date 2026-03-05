package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class POSRedemptionWithQCTest {

	static Logger logger = LogManager.getLogger(POSRedemptionWithQCTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	ApiPayloads apipaylods;
	private List<String> codeNameList;
	private Utilities utils;

	private ApiUtils apiUtils;
	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	private String endDateTime;
	private ApiPayloadObj apipayloadObj;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);

		codeNameList = new ArrayList<String>();
		apiUtils = new ApiUtils();

		endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
	}

	@Test(description = "SQ-T6795-Verify Reward Redemption When QC with 'Sum of Amount'-> 100% is Applied", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6795_qcSumOfAmounts100PercentOffer() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "101", "100", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "10", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 10.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6796-Verify Reward Redemption When 'Rate Rollback' QC is Applied", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6796_qcRateRollbackOffer() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("rate_rollback").setMinimumUnitRate(0.01).setUnitDiscount(1.0)
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "2", "10", "M", "101", "100", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "10", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 2.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6797-Verify Reward Redemption When 'Hit target menu item price' QC is Applied", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6797_rewardRedemptionWithHitTargetMenuItemPriceQC() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("hit_target_price").setTargetPrice(1.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, null).build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "2", "10", "M", "101", "100", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "10", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 8.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6798-Verify Reward Redemption When 'BOGO ANY' QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6798_rewardRedemptionWithBogoAnyQC() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("bogof2").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, null).build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap = new HashMap<String, String>();
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Pizza", "2", "5", "M", "101", "100", "1.0", "101");
		parentMap.put("Pizza", detailsMap);

		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "2", "10", "M", "101", "100", "2.0", "101");
		parentMap.put("Sandwich", detailsMap);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "15", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 2.5;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6799-Verify Reward Redemption When 'Target bundle Advanced' QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6799_rewardRedemptionWithTargetBundleAdvancedQC() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("bundle_price_target_advanced").setTargetPrice(1.0)
				.addLineItemFilter(lisExternalID, "", 1).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "2", "10", "M", "101", "100", "1", "101");
		parentMap.put("Pizza1", detailsMap1);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "10", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 8.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6834-Verify Reward Redemption When 'BOGO FIX' QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6834_rewardRedemptionWithBogoFixQC() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		// verify api response code is 200 or not
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Create LIS api");
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname).setQCProcessingFunction("bogof")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		// verify api response code is 200 or not
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for Create QC api");
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		// verify api response code is 200 or not
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Create Redeemable api");
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Gift redeemable to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// create Input receipt
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap = new HashMap<String, String>();
		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Pizza", "2", "5", "M", "101", "100", "1.0", "101");
		parentMap.put("Pizza", detailsMap);

		detailsMap = pageObj.endpoints().getRecieptDetailsMap("Sandwich", "2", "10", "M", "101", "100", "2.0", "101");
		parentMap.put("Sandwich", detailsMap);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "15", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 5.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6800-Verify Redeemable Redemption When QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6800_verifyRedeemableRedemptionWhenQCIsApplied() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "101")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setPoints(1).addCurrentData().build();

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		redeemableResponse.prettyPrint();
		System.out.println("Created Redeemable External ID: " + redeemableExternalID);

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// Gift points to user
		Response sendGiftResponse = pageObj.endpoints().sendPointsToUser(userId, "100", dataSet.get("apiKey"));
		Assert.assertEquals(sendGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched ");
		logger.info("Successfully gifted points to user ");
		TestListeners.extentTest.get().pass("Successfully gifted points to user ");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRedeemableRedemption(authTokenFromDB,
				dataSet.get("client"), dataSet.get("secret"), "101", dbRedeemableId);
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth redemption api");

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 4.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Delete LIS, QC and Redeemable Data
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);

	}

	@Test(description = "SQ-T6801-Verify Subscription Redemption When QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6801_verifySubscriptionRedemptionWhenQCIsApplied() throws Exception {

		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		// get access token from response
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// subscription purchase using API2 mobile
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("subcriptionPlanId"), dataSet.get("client"), dataSet.get("secret"), "200", endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// get subscription id from response
		String subscription_id = purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString();
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("subcriptionPlanId"));
		TestListeners.extentTest.get()
				.pass(userEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("subcriptionPlanId"));

		// POS redemption-> subscription
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response posRedemptionResponse = pageObj.endpoints().posRedemptionOfSubscription(userEmail, date,
				subscription_id, key, txn, dataSet.get("locationKey"), "10", "101");

		// verify api response code is 200 or not
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posRedemptionResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		logger.info("Verified that user is able to do redemption of subscription plan ");
		TestListeners.extentTest.get().pass("Verified that user is able to do redemption of subscription plan ");

		// Get discount amount from API response
		double discountAmount = posRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 2.0;

		// Validate response
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		logger.info("Pos redemption api call is successful and discount amount matched");
		TestListeners.extentTest.get().pass("Pos redemption api call is successful and discount amount matched");

	}

	@Test(description = "SQ-T6802-Verify Discount Amount Redemption When QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6802_verifyDiscountAmountRedemptionWhenQCIsApplied() throws Exception {

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");

		// verify api response code is 200 or not
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// get user id and access token from response
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// Gift reward amount to user
		Response sendRewardAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60",
				"", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardAmountResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send amount to the user successfully");
		TestListeners.extentTest.get().pass("Send amount to the user successfully");

		// POS redemption-> amount
		// Base redeemable is attached with item id=101
		// Input receipt contains item id=101
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationkey"));

		// verify api response code is 200 or not
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// Get discount amount from API response
		double discountAmount = respo.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 1.0;

		// Validate response
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		logger.info("Pos redemption api call is successful and discount amount matched");
		TestListeners.extentTest.get().pass("Pos redemption api call is successful and discount amount matched");

	}

	@Test(description = "SQ-T6803-Verify Coupon Redemption When QC is Applied.", groups = "Regression", priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6803_couponRedemptionWhenQCIsApplied() throws Exception {

		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "==", "3419")
				.build();

		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		lisResponse.prettyPrint();
		System.out.println("Created LIS External ID: " + lisExternalID);

		// Create QC with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0).addItemQualifier("line_item_exists", lisExternalID, null)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		qcResponse.prettyPrint();
		System.out.println("Created QC External ID: " + qcExternalID);

		// user creation using auth signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(signUpResponse, "Auth API user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		TestListeners.extentTest.get().pass("Auth API Signup is successful");
		String authToken = signUpResponse.jsonPath().get("authentication_token");

		// get user id from response
		String userID = signUpResponse.jsonPath().getString("user_id");
		logger.info("UserIDUser1=" + userID);

		// Print coupon campaign name
		String couponCampaignName = "Auto_CouponCampaign" + CreateDateTime.getTimeDateString();
		logger.info("couponCampaignName == " + couponCampaignName);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Campaign Page

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", qcname, false);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		Thread.sleep(20000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		// get coupon code
		String couponCode = codeNameList.get(0);
		logger.info("couponCode == " + couponCode);

		// Auth_redemption_online_order
		Response resp = pageObj.endpoints().authOnlineCouponPromoRedemption(authToken, dataSet.get("client"),
				dataSet.get("secret"), couponCode);
		// verify api response code is 200 or not
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Get discount amount from API response
		double discountAmount = resp.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 1.0;

		// Validate response
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		logger.info("Auth coupon redemption api call is successful and discount amount matched");
		TestListeners.extentTest.get()
				.pass("Auth coupon redemption api call is successful and discount amount matched");

//		// Delete coupon campaign
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//		// pageObj.campaignspage().searchCampaign("Auto_CouponCampaign15292701102025");
//
//		pageObj.newCamHomePage().searchCampaign("Auto_CouponCampaign15292701102025");
//		pageObj.campaignspage().deactivateOrDeleteTheCoupon("delete");

	}

	@Test(description = "SQ-T6805-Verify Card Redemption When QC is Applied.", groups = { "Regression",
			"nonNightly" }, priority = 0)
	@Owner(name = "Rahul Garg")
	public void T6805_cardRedemptionWhenQCIsApplied() throws Exception {

		// User Sign-up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// Gift points to user
		Response sendGiftResponse = pageObj.endpoints().sendPointsToUser(resp.jsonPath().get("id").toString(), "100",
				dataSet.get("apiKey"));
		Assert.assertEquals(sendGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched ");

		// POS redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response posRedemptionResponse = pageObj.endpoints().posRedemptionOfCard(userEmail, date, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posRedemptionResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(posRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		// Get discount amount from API response
		double discountAmount = posRedemptionResponse.jsonPath().getDouble("redemption_amount");
		logger.info("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 15.0;

		// Validate response
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		logger.info("Pos card redemption api call is successful and discount amount matched");
		TestListeners.extentTest.get().pass("Pos card redemption api call is successful and discount amount matched");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
