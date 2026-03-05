package com.punchh.server.OMMTest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.api.payloadbuilder.LineItemSelectorPayloadBuilder;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;


import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OMM1322R1AnyOperatorFlagFalseTest {

	static Logger logger = LogManager.getLogger(OMM1322R1AnyOperatorFlagFalseTest.class);

	private PageObj pageObj;
	private String sTCName;
	public WebDriver driver;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private ApiPayloadObj apipayloadObj;
	Utilities utils ;
	private String firstId, secondId, qcExternalID, redeemableExternalID ;
	 
	

	// DB Query
	private final String getRedeemableID = "SELECT id FROM redeemables WHERE uuid ='$actualExternalIdRedeemable'";


	// =====================================================
	// BEFORE METHOD → Load JSON test data
	// =====================================================
	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {

		sTCName = method.getName();
		pageObj = new PageObj(driver);

		// Load test data
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(
				pageObj.readData().getJsonFilePath(run, env), sTCName
				);
		dataSet = pageObj.readData().readTestData;
     	apipayloadObj = new ApiPayloadObj();
     	utils = new Utilities();
		// Load Secrets
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"),
				dataSet.get("slug")
				);
		dataSet.putAll(pageObj.readData().readTestData);

		utils.logInfo(sTCName + " test-data loaded → " + dataSet);

		redeemableExternalID = String.valueOf(Utilities.getRandomNoFromRange(50000, 100000));

		
	}
	// Disable Flag -disable_decoupled_redemption_engine from the DB
	private void disableDecoupledRedemptionAndEnableReturnQCItemsFlag() throws Exception {

		String businessQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, businessQuery, "preferences");

		// Disable decoupled redemption engine
		boolean decoupleFlag = DBUtils.updateBusinessesPreference(env, expColValue, "false",
				"enable_decoupled_redemption_engine", dataSet.get("business_id"));
		Assert.assertTrue(decoupleFlag, "enable_decoupled_redemption_engine value is not updated to false");
		utils.logInfo("enable_decoupled_redemption_engine value is updated to false");

		// Enable return_qualifying_menu_items_to_pos
		boolean returnQCItemsFlag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"return_qualifying_menu_items_to_pos", dataSet.get("business_id"));
		Assert.assertTrue(returnQCItemsFlag, "return_qualifying_menu_items_to_pos value is not updated to true");
		utils.logInfo("return_qualifying_menu_items_to_pos value is updated to true");
	}


	// ---------------------------------------------------------------------
	// Test Case: Create offer using ANY operator and validate API flows
	// ---------------------------------------------------------------------
	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7468 | R1 | Base & Modifiers | Verify that an offer qualifies when at least one condition is met using the 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7468_R1_Flag_False_verify_single_item_qualifier_AnyOperator_BaseAndModifiers() throws Exception {
		disableDecoupledRedemptionAndEnableReturnQCItemsFlag();


		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		 firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		 secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);



		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				//.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("line_item_exists", firstId, 1.0, 1)
				// .addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 10.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		 redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);

		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "33", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 15.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7469 | R1 | Base & Modifiers | Verify offer does not qualify when no conditions are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7469_R1_Flag_False_verify_offer_does_not_qualify_for_item_qualifier_AnyOperator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0)

				// UPDATED AS REQUESTED
				.addItemQualifier("line_item_does_not_exist", firstId, 1.0, 1)
				.addItemQualifier("line_item_does_not_exist", secondId, 1.0, 1)

				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);



		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");

		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Send reward to user
		Response sendMessageToUserResponse = pageObj.endpoints()
				.sendMessageToUser(userId, dataSet.get("apiKey"), "", dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Fetch rewardId
		String rewardId = pageObj.redeemablesPage()
				.getRewardId(token, dataSet.get("client"), dataSet.get("secret"), dbRedeemableId);

		// *******************************************************************
		// STEP 6: Build Receipt
		// *******************************************************************
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		parentMap.put("Sandwich_1",
				pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "23", "1.0", "101"));
		parentMap.put("Fries_1",
				pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "23", "1.1", "111"));
		parentMap.put("Sandwich_2",
				pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "23", "2.0", "201"));
		parentMap.put("Fries_2",
				pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "23", "2.1", "211"));

		// Fetch Auth Token
		String authTokenQuery =
				"SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB =
				DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// *******************************************************************
		// STEP 7: Auth Redemption Call (EXPECTED FAILURE)
		// *******************************************************************
		Response authRedemptionResponse =
				pageObj.endpoints().authOnlineRewardRedemption(
						dataSet.get("client"),
						dataSet.get("secret"),
						authTokenFromDB,
						rewardId,
						"33",
						parentMap);

		// *******************************************************************
		// STEP 8: ASSERTIONS FOR FAILURE CASE
		// *******************************************************************

		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// 1️⃣ Expected discount = 0.0
		double discountAmount = jsonPath.getDouble("redemption_amount");
		Assert.assertEquals(discountAmount, 0.0, "Expected discount should be 0.0");

		// 2️⃣ Status should contain failure message
		String status = jsonPath.getString("status");
		Assert.assertTrue(
				status.contains("Discount qualification on receipt failed"),
				"Status does not contain expected failure message"
				);

		// 3️⃣ qualified_menu_items must be empty/null
		List<Object> qualifiedList = jsonPath.getList("qualified_menu_items");
		Assert.assertTrue(
				qualifiedList == null || qualifiedList.isEmpty(),
				"qualified_menu_items should be empty"
				);

		utils.logInfo("Validation complete — QC failure scenario passed successfully.");
	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7470 | R1 | Base & Modifiers | Verify offer qualifies when two condition are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7470_R1_Flag_False_verify_offer_two_condition_match_for_item_qualifier_AnyOperator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		


		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("net_quantity_greater_than_or_equal_to", firstId, 1.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 2.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

	    redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "33", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 33.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1","Sandwich_2");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");


	}



	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7471 | R1 | Base & Modifiers | Verify that an offer qualifies when at least one condition is met using the 'AND' operator")
	@Owner(name = "Hitesh Popli")
	public void T7471_R1_Flag_False_verify_single_item_qualifier_And_Operator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		


		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("and")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				//.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("line_item_exists", firstId, 1.0, 1)
				// .addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 10.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "102");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "112");
		parentMap.put("Fries_2", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "33", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 15.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");


	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7472 | R1 | Base & Modifiers | Verify offer does not qualify when no conditions are met with 'AND' operator")
	@Owner(name = "Hitesh Popli")
	public void T7472_R1_Flag_False_verify_offer_does_not_qualify_for_item_qualifier_AllOperator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all")
				.addLineItemFilter(firstId, "", 0)

				// UPDATED AS REQUESTED
				.addItemQualifier("line_item_does_not_exist", firstId, 1.0, 1)
				.addItemQualifier("line_item_does_not_exist", secondId, 1.0, 1)

				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);

		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);
		



		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");

		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Send reward to user
		Response sendMessageToUserResponse = pageObj.endpoints()
				.sendMessageToUser(userId, dataSet.get("apiKey"), "", dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Fetch rewardId
		String rewardId = pageObj.redeemablesPage()
				.getRewardId(token, dataSet.get("client"), dataSet.get("secret"), dbRedeemableId);

		// *******************************************************************
		// STEP 6: Build Receipt
		// *******************************************************************
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		parentMap.put("Sandwich_1",
				pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "23", "1.0", "101"));
		parentMap.put("Fries_1",
				pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "23", "1.1", "111"));
		parentMap.put("Sandwich_2",
				pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "23", "2.0", "201"));
		parentMap.put("Fries_2",
				pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "23", "2.1", "211"));

		// Fetch Auth Token
		String authTokenQuery =
				"SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB =
				DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// *******************************************************************
		// STEP 7: Auth Redemption Call (EXPECTED FAILURE)
		// *******************************************************************
		Response authRedemptionResponse =
				pageObj.endpoints().authOnlineRewardRedemption(
						dataSet.get("client"),
						dataSet.get("secret"),
						authTokenFromDB,
						rewardId,
						"33",
						parentMap);

		// *******************************************************************
		// STEP 8: ASSERTIONS FOR FAILURE CASE
		// *******************************************************************

		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// 1️⃣ Expected discount = 0.0
		double discountAmount = jsonPath.getDouble("redemption_amount");
		Assert.assertEquals(discountAmount, 0.0, "Expected discount should be 0.0");

		// 2️⃣ Status should contain failure message
		String status = jsonPath.getString("status");
		Assert.assertTrue(
				status.contains("Discount qualification on receipt failed"),
				"Status does not contain expected failure message"
				);

		// 3️⃣ qualified_menu_items must be empty/null
		List<Object> qualifiedList = jsonPath.getList("qualified_menu_items");
		Assert.assertTrue(
				qualifiedList == null || qualifiedList.isEmpty(),
				"qualified_menu_items should be empty"
				);

		utils.logInfo("Validation complete — QC failure scenario passed successfully.");
	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7473 | R1 | Base & Modifiers | Verify offer qualifies when two condition are met with 'AND' operator")
	@Owner(name = "Hitesh Popli")
	public void T7473_R1_Flag_False_verify_offer_two_condition_match_for_item_qualifier_AndOperator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("net_quantity_greater_than_or_equal_to", firstId, 1.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 2.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		
		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		

		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);

		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "33", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 33.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1","Sandwich_2");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");


	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7462 | R1 | Base Only| Verify that an offer qualifies when at least one condition is met using the 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7462_R1_Flag_False_verify_single_item_qualifier_AnyOperator_Base() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addCurrentRule();

		// Rule 2 → Base=201
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				//.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("line_item_exists", firstId, 1.0, 1)
				// .addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 10.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap2);



		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "22", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 10.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7463 | R1 |Only Base | Verify offer does not qualify when no conditions are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7463_R1_Flag_False_verify_offer_does_not_qualify_for_item_qualifier_AnyOperator_Base() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addCurrentRule();

		// Rule 2 → Base=201
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "LIS creation failed");

		firstId  = lisResponse .jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);


		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0)

				// UPDATED AS REQUESTED
				.addItemQualifier("line_item_does_not_exist", firstId, 1.0, 1)
				.addItemQualifier("line_item_does_not_exist", secondId, 1.0, 1)

				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");

		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Send reward to user
		Response sendMessageToUserResponse = pageObj.endpoints()
				.sendMessageToUser(userId, dataSet.get("apiKey"), "", dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Fetch rewardId
		String rewardId = pageObj.redeemablesPage()
				.getRewardId(token, dataSet.get("client"), dataSet.get("secret"), dbRedeemableId);

		// *******************************************************************
		// STEP 6: Build Receipt
		// *******************************************************************
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap2);

		// Fetch Auth Token
		String authTokenQuery =
				"SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB =
				DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// *******************************************************************
		// STEP 7: Auth Redemption Call (EXPECTED FAILURE)
		// *******************************************************************
		Response authRedemptionResponse =
				pageObj.endpoints().authOnlineRewardRedemption(
						dataSet.get("client"),
						dataSet.get("secret"),
						authTokenFromDB,
						rewardId,
						"33",
						parentMap);

		// *******************************************************************
		// STEP 8: ASSERTIONS FOR FAILURE CASE
		// *******************************************************************

		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// 1️⃣ Expected discount = 0.0
		double discountAmount = jsonPath.getDouble("redemption_amount");
		Assert.assertEquals(discountAmount, 0.0, "Expected discount should be 0.0");

		// 2️⃣ Status should contain failure message
		String status = jsonPath.getString("status");
		Assert.assertTrue(
				status.contains("Discount qualification on receipt failed"),
				"Status does not contain expected failure message"
				);

		// 3️⃣ qualified_menu_items must be empty/null
		List<Object> qualifiedList = jsonPath.getList("qualified_menu_items");
		Assert.assertTrue(
				qualifiedList == null || qualifiedList.isEmpty(),
				"qualified_menu_items should be empty"
				);

		utils.logInfo("Validation complete — QC failure scenario passed successfully.");
	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7464 | R1 | Base | Verify offer qualifies when two condition are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7464_R1_Flag_False_verify_offer_two_condition_match_for_item_qualifier_AnyOperator_BaseOnly() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addCurrentRule();

		// Rule 2 → Base=201
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		


		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("net_quantity_greater_than_or_equal_to", firstId, 1.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 2.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();


		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);


		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap2);


		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "22", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 22.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Sandwich_1","Sandwich_2");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");


	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7465 | R1 | Modifier Only| Verify that an offer qualifies when at least one condition is met using the 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7465_R1_Flag_False_verify_single_item_qualifier_AnyOperator_Modifier() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		secondId = lisResponse.jsonPath().getString("results[1].external_id");
		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				//.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("line_item_exists", firstId, 1.0, 1)
				// .addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 10.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);

		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);



		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "22", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 5.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Fries_1");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");

	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7466 | R1 | Only Modifier | Verify offer does not qualify when no conditions are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7466_R1_Flag_False_verify_offer_does_not_qualify_for_item_qualifier_AnyOperator_Modifier() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");

		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "LIS creation failed");

		 firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		 secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

	

		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				.addLineItemFilter(firstId, "", 0)

				// UPDATED AS REQUESTED
				.addItemQualifier("line_item_does_not_exist", firstId, 1.0, 1)
				.addItemQualifier("line_item_does_not_exist", secondId, 1.0, 1)

				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED, "QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);
	

		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		


		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");

		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Send reward to user
		Response sendMessageToUserResponse = pageObj.endpoints()
				.sendMessageToUser(userId, dataSet.get("apiKey"), "", dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Fetch rewardId
		String rewardId = pageObj.redeemablesPage()
				.getRewardId(token, dataSet.get("client"), dataSet.get("secret"), dbRedeemableId);

		// *******************************************************************
		// STEP 6: Build Receipt
		// *******************************************************************
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);

		// Fetch Auth Token
		String authTokenQuery =
				"SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB =
				DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery, "authentication_token");

		// *******************************************************************
		// STEP 7: Auth Redemption Call (EXPECTED FAILURE)
		// *******************************************************************
		Response authRedemptionResponse =
				pageObj.endpoints().authOnlineRewardRedemption(
						dataSet.get("client"),
						dataSet.get("secret"),
						authTokenFromDB,
						rewardId,
						"33",
						parentMap);

		// *******************************************************************
		// STEP 8: ASSERTIONS FOR FAILURE CASE
		// *******************************************************************

		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// 1️⃣ Expected discount = 0.0
		double discountAmount = jsonPath.getDouble("redemption_amount");
		Assert.assertEquals(discountAmount, 0.0, "Expected discount should be 0.0");

		// 2️⃣ Status should contain failure message
		String status = jsonPath.getString("status");
		Assert.assertTrue(
				status.contains("Discount qualification on receipt failed"),
				"Status does not contain expected failure message"
				);

		// 3️⃣ qualified_menu_items must be empty/null
		List<Object> qualifiedList = jsonPath.getList("qualified_menu_items");
		Assert.assertTrue(
				qualifiedList == null || qualifiedList.isEmpty(),
				"qualified_menu_items should be empty"
				);

		utils.logInfo("Validation complete — QC failure scenario passed successfully.");
	}

	@Test(groups = {"OMM", "Regression"},
			description = "SQ-T7467 | R1 | Only Modifier | Verify offer qualifies when two condition are met with 'OR' operator")
	@Owner(name = "Hitesh Popli")
	public void T7467_R1_Flag_False_verify_offer_two_condition_match_for_item_qualifier_AnyOperator_Modifier_only() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp();

		// *******************************************************************
		// STEP 1: Create Line Item Selectors (LIS)
		// *******************************************************************
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		utils.logInfo("== Creating Line Item Selectors (LIS) ==");


		// Rule 1 → Base=101, Modifier=111
		builder.startNewRule()
		.setName("Item id-101")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "101")
		.addModifierClause("item_id", "in", "111")
		.addCurrentRule();

		// Rule 2 → Base=201, Modifier=211
		builder.startNewRule()
		.setName("Item Id-201")
		.setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
		.addBaseItemClause("item_id", "==", "201")
		.addModifierClause("item_id", "in", "211")
		.addCurrentRule();

		String lisPayload = builder.build();
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(lisResponse, "LIS API returned null");
		Assert.assertTrue(lisResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || lisResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"LIS creation failed");

		 firstId  = lisResponse.jsonPath().getString("results[0].external_id");
		 secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId);
		Assert.assertNotNull(secondId);

		


		// *******************************************************************
		// STEP 2: Create Qualification Criteria (QC) using ANY operator
		// *******************************************************************
		utils.logInfo("== Creating Qualification Criteria (QC) ==");

		String qcPayload =
				apipayloadObj.qualificationCriteriaBuilder()
				.setName(qcname)
				.setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any")
				// Add LIS filters
				.addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0)
				// Add item qualifiers
				.addItemQualifier("net_quantity_greater_than_or_equal_to", firstId, 1.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 2.0, 1)
				.build();

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertNotNull(qcResponse);
		Assert.assertTrue(qcResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || qcResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"QC creation failed!");

		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID);

		


		// *******************************************************************
		// STEP 3: Create Redeemable associated with QC
		// *******************************************************************
		utils.logInfo("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload =
				redeemableBuilder.startNewData()
				.setName(redeemableName)
				.setReceiptRule(
						RedeemableReceiptRule.builder()
						.qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID)
						.build()
						)
				.setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true)
				.setDiscountChannel("all")
				.setAutoApplicable(true)
				.addCurrentData()
				.build();

		Response redeemableResponse =
				pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		Assert.assertNotNull(redeemableResponse);
		Assert.assertTrue(redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_OK || redeemableResponse.statusCode() == ApiConstants.HTTP_STATUS_CREATED);

		redeemableExternalID =
				redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID);
		



		// *******************************************************************
		// STEP 4: Fetch Redeemable ID from DB
		// *******************************************************************
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		


		// *******************************************************************
		// STEP 5: User Signup
		// *******************************************************************
		utils.logInfo("== Mobile API v2: User Signup ==");

		String userEmail = "test+" + Utilities.getTimestamp() + "@example.com";

		Response signUpResponse = pageObj.endpoints()
				.Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));

		Assert.assertNotNull(signUpResponse);
		Assert.assertEquals(signUpResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String userId = signUpResponse.jsonPath().getString("user.user_id");
		String token  = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(userId);
		Assert.assertNotNull(token);

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		utils.logit("Reward Id for user is fetched: " + rewardId);


		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_1", "1", "10", "M", "101", "100", "1.0", "101");
		parentMap.put("Sandwich_1", detailsMap1);

		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Fries_1", "1", "5", "M", "111", "100", "1.1", "111");
		parentMap.put("Fries_1", detailsMap2);

		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("Sandwich_2", "1", "12", "M", "201", "100", "2.0", "201");
		parentMap.put("Sandwich_2", detailsMap3);

		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("Fries_2", "1", "6", "M", "211", "100", "2.1", "211");
		parentMap.put("Fries_2", detailsMap4);


		// Get Auth token from DB
		String authTokenQuery = "SELECT authentication_token FROM users WHERE id='" + userId + "'";
		String authTokenFromDB = DBUtils.executeQueryAndGetColumnValue(env, authTokenQuery,
				"authentication_token");

		// Auth Reward Redemption
		Response authRedemptionResponse = pageObj.endpoints().authOnlineRewardRedemption(dataSet.get("client"),
				dataSet.get("secret"), authTokenFromDB, rewardId, "22", parentMap);

		// Get discount amount from API response
		double discountAmount = authRedemptionResponse.jsonPath().getDouble("redemption_amount");
		utils.logInfo("Discount Amount: " + discountAmount);
		double expectedDiscountAmount = 11.0;

		// Validate response
		Assert.assertEquals(authRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth redemption API");
		Assert.assertEquals(discountAmount, expectedDiscountAmount, "Discount amount did not matched");
		utils.logPass("Auth redemption api call is successful and discount amount matched");

		// Validate status-Please honor it in api response
		Assert.assertTrue(authRedemptionResponse.jsonPath().get("status").toString().contains("Please HONOR it."));

		JsonPath jsonPath = new JsonPath(authRedemptionResponse.asString());

		// Item names
		List<String> actualItemNames = jsonPath.getList("qualified_menu_items.item_name");
		List<String> expectedItems = Arrays.asList("Fries_1","Fries_2");
		Assert.assertEqualsNoOrder(actualItemNames.toArray(), expectedItems.toArray(),
				"Items in response do not match expected");
		utils.logPass("Items in response match expected");

		// Sum of item amounts
		List<Float> itemAmounts = jsonPath.getList("qualified_menu_items.item_amount");
		double sum = itemAmounts.stream().mapToDouble(Float::doubleValue).sum();
		Assert.assertEquals(sum, discountAmount, "Sum of item amounts does not match redemption amount");
		utils.logPass("Sum of item amounts matches redemption amount");


	}
	@AfterMethod(alwaysRun = true)
	public void cleanup() throws Exception {
		
		// Delete for LIS(QC, Redeemable)
		utils.deleteLISQCRedeemable(env, firstId, qcExternalID, redeemableExternalID);

		// Delete second LIS
		utils.deleteLISQCRedeemable(env, secondId, null, null);

		utils.logInfo("=== Cleanup completed successfully ===");
		pageObj.utils().clearDataSet(dataSet);
		utils.logInfo("DataSet cleared");
	}
}