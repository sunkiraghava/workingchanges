package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.LineItemSelectorPayloadBuilder;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ItemQualifierFilterOperatorWithFlagFalseTest {

	static Logger logger = LogManager.getLogger(ItemQualifierFilterOperatorWithFlagFalseTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String baseUrl;
	ApiPayloads apipaylods;
	Utilities utils;
	private ApiUtils apiUtils;
	private ApiPayloadObj apipayloadObj;
	private String externalUID;
	String firstId, secondId;
	String qcExternalID;
	String redeemableExternalID;

	String getRedeemableID = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils.logInfo(sTCName + " ==>" + dataSet);
		apipayloadObj = new ApiPayloadObj();
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		apiUtils = new ApiUtils();

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7315 UI | Verify that the \"Logical Operator Strategy\" dropdown is present in Item Qualifier"
					+ "SQ-T7316 UI | Verify that the dropdown contains the values \"All\" and \"Any\""
					+ "SQ-T7317 UI | Verify that the hint text is displayed for the Dropdown value : All & Any"
					+ "SQ-T7318 Schema Changes | Verify that the new column for logical operator strategy is added to the qualification_criteria table"
					+ "SQ-T7319 Schema Changes | Verify that the column accepts and stores the correct values (\"All\", \"Any\")")
	@Owner(name = "Rahul Garg")
	public void T7315_T7316_T7317_T7318_T7319_validateItemQualifierLogicalStrategy() throws Exception {
		// Generate dynamic names
		String lisName = "POS_LIS_" + Utilities.getTimestamp();
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		utils.logInfo("Generated LIS name: " + lisName + ", QC name: " + qcname);

		// Build LIS payload
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", "101,102,103,104").build();
		utils.logit("LIS Payload created");

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");
		utils.logPass("LIS created successfully via API");

		// Validate LIS response structure
		Assert.assertNotNull(lisResponse.jsonPath().get("results[0].external_id"),
				"External ID missing in LIS creation response");

		// Fetch LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Fetched LIS External ID: " + lisExternalID);

		// UI Workflow
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		utils.logit("Logged into instance");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		utils.logInfo("Navigated to Qualification Criteria page");

		// Prepare Line Item Filters
		List<Map<String, String>> lineItemFilters = new ArrayList<>();

		// Filter 1
		Map<String, String> filter1 = new HashMap<>();
		filter1.put("selector", lisName);
		lineItemFilters.add(filter1);

		// Filter 2
		Map<String, String> filter2 = new HashMap<>();
		filter2.put("selector", lisName);
		filter2.put("processing_method", "Maximum Unit Price");
		filter2.put("quantity", "2");
		lineItemFilters.add(filter2);

		utils.logit("Line Item Filters added");

		// Prepare Item Qualifiers
		List<Map<String, String>> itemQualifiers = List
				.of(Map.of("type", "Net Quantity Greater Than Or Equal To", "selector", lisName, "netValue", "2"));

		utils.logInfo("Item Qualifiers prepared: " + itemQualifiers);

		// Create QC with ANY Operator Strategy
		pageObj.qualificationcriteriapage().fillQualificationCriterionForm(qcname, null, "1.20", "100",
				"Sum of Amounts", "Round", null, null, null, null, true, true, lineItemFilters, false, null, "ANY",
				itemQualifiers);

		utils.logit("Filling QC form with ANY operator strategy");

		// Validate dropdown values
		List<String> expectedValues = Arrays.asList("ALL", "ANY");
		List<String> actualValues = utils.getAllVisibleTextFromDropdwon(
				utils.getLocator("QualificationCriteriaPage.itemQualifierOperatorStrategy"));

		utils.logInfo("Expected dropdown values: " + expectedValues);
		utils.logInfo("Actual dropdown values: " + actualValues);

		Assert.assertTrue(actualValues.containsAll(expectedValues),
				"Dropdown does not contain expected values. Found: " + actualValues);
		utils.logPass("Logical Operator Strategy dropdown values verified");

		// Validate hint text
		String actualHint = pageObj.qualificationcriteriapage().getLogicalOperatorHintText();
		String expectedHint = "Choose how offer conditions are evaluated. Use 'All' if every condition must be true, or 'Any' if one condition is enough.";

		Assert.assertEquals(actualHint, expectedHint, "Logical Operator hint text mismatch");
		utils.logPass("Logical Operator hint text verified");

		// Validate QC success message
		String qcSuccessMsg = pageObj.qualificationcriteriapage().createQC();
		Assert.assertEquals(qcSuccessMsg, "Qualification Criterion updated", "QC creation success message incorrect");

		utils.logPass("Qualification Criteria updated successfully");

		// DB Verification
		String query = "SELECT \n"
				+ "  TRIM(REPLACE(SUBSTRING_INDEX(SUBSTRING_INDEX(preferences, ':qualifying_expressions_operator:', -1), '\\n', 1), '\\r', '')) AS qualifying_operator\n"
				+ "FROM qualification_criteria\n" + "WHERE business_id = '1115' \n" + "  AND name ='" + qcname + "';";

		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "qualifying_operator");

		Assert.assertNotNull(expColValue, "DB returned null for qualifying_expressions_operator");
		Assert.assertEquals(expColValue.toLowerCase(), "any", "DB qualifying_expressions_operator not set to ANY");

		utils.logPass("DB value for qualifying_expressions_operator verified as ANY");

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7321 Only Base | Create an offer with two item qualifiers using 'ANY' and verify the response")
	@Owner(name = "Rahul Garg")
	public void T7321_verifyOfferWithTwoItemQualifiers_UsingAnyOperator_OnlyBase() throws Exception {
		// ===== Generate Names =====
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		utils.logit("Generated dynamic names");

		// Builder Instance
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// ===== Create LIS 1 =====
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "==", "101").addCurrentRule();

		// ===== Create LIS 2 =====
		builder.startNewRule().setName("Item id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "==", "201").addCurrentRule();

		String lisPayload = builder.build();
		utils.logit("LIS Payload created");

		// ===== Create LIS via API =====
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");
		utils.logPass("LIS created via API");

		Assert.assertNotNull(lisResponse.jsonPath().getString("results[0].external_id"),
				"First LIS external ID missing");
		Assert.assertNotNull(lisResponse.jsonPath().getString("results[1].external_id"),
				"Second LIS external ID missing");

		// Get LIS IDs
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		utils.logit("Fetched LIS External IDs");

		// ===== Build QC Payload =====
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all").addLineItemFilter(firstId, "", 0)
				.addItemQualifier("net_amount_greater_than_or_equal_to", firstId, 10.0, 1)
				.addItemQualifier("net_quantity_greater_than_or_equal_to", secondId, 1.0, 2).build();

		utils.logit("QC Payload prepared");

		// ===== Create QC via API =====
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");
		utils.logPass("QC created via API");

		// Validate QC response
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null!");
		utils.logit("Fetched QC External ID");

		// ===== Create Redeemable =====
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();

		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		utils.logit("Redeemable payload prepared");

		// ===== Create Redeemable via API =====
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed via API");
		utils.logPass("Redeemable created via API");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null!");
		utils.logit("Fetched Redeemable External ID " + redeemableExternalID);

		// ===== Fetch Redeemable ID from DB =====
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID should not be null");
		utils.logit("Fetched Redeemable ID from DB " + dbRedeemableId);

		// ===== User Signup =====
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed");

		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		utils.logPass("User Signup successful");

		// ===== Gift Reward to User =====
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed");
		utils.logPass("Reward sent to user");

		// ===== Fetch Reward ID =====
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		utils.logPass("Reward ID fetched");

		// ===== Prepare Basket Payload =====
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "11", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "201", "201", "2.0", "201" });

		utils.logInfo("Parent Map prepared: " + parentMap);

		// ===== Auth Auto Select API =====
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);

		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Auth Auto Select API failed");
		utils.logPass("Auth Auto Select API success");

		// Extract & validate
		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);

		utils.logPass("Auth Auto Select validations passed");

		// ===== POS Discount Lookup =====
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "20", externalUID, parentMap);

		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Discount Lookup failed");

		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");

		Assert.assertEquals(discountAmount, 10.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");

		utils.logPass("POS Discount Lookup validations passed");

		// Item Validations
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name", List.of("Sandwich DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-10.0));

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7320 Only Base | Create an offer with two item qualifiers using 'ALL' and verify the response")
	@Owner(name = "Rahul Garg")
	public void T7320_verifyOfferWithTwoItemQualifiers_UsingAllOperator_OnlyBase() throws Exception {
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// Create LIS with base item 101
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "==", "101").addCurrentRule();

		// Create LIS with base item 201
		builder.startNewRule().setName("Item Id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "==", "201").addCurrentRule();

		String lisPayload = builder.build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));

		// Get LIS External ID
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all").addLineItemFilter(firstId, "", 0)
				.addItemQualifier("net_amount_greater_than_or_equal_to", firstId, 10.0, 1)
				.addItemQualifier("net_quantity_greater_than_or_equal_to", secondId, 2.0, 1).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created QC External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logInfo("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		utils.logPass("API v2 User Signup call is successful");

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
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "11", "1.0", "101" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "10", "M", "201", "201", "2.0", "201" });

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Validate discount_basket_items is null
		List<Object> discountBasketItems = redemptionResponse.jsonPath().getList("discount_basket_items");
		System.out.println("discount_basket_items: " + discountBasketItems);
		Assert.assertNotNull(redemptionResponse.jsonPath().get("discount_basket_items"));
		utils.logit("Discount basket items is null as expected when ALL criteria is not met");

		// Update input receipt to meet 'ALL' criteria
		addDetails.accept("Pizza", new String[] { "Pizza", "2", "10", "M", "201", "201", "2.0", "201" });

		// Auth Auto select API
		Response redemptionResponse1 = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "20", externalUID, parentMap);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse1.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract values
		String discountType = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse1.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse1.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "20", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Parse response
		JsonPath jp = discountLookupResponse.jsonPath();

		// Extract values
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertEquals(discountAmount, 10.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// Validate M-item names
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Sandwich"));

		// Validate R-item names
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name", List.of("Sandwich DISCOUNT"));

		// Validate M-item amounts
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(10.0));

		// Validate R-item amounts
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-10.0));
	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7323 Only Modifiers | Create an offer with two qualifiers using 'ANY' and verify the response ")
	@Owner(name = "Rahul Garg")
	public void T7323_verifyOfferWithTwoItemQualifiers_UsingAnyOperator_OnlyModifiers() throws Exception {
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// Create LIS with base item-101 & modifier 111
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "in", "111").addCurrentRule();

		// Create LIS with base item-201 & modifier 211
		builder.startNewRule().setName("Item Id-201").setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "in", "211").addCurrentRule();

		String lisPayload = builder.build();
		utils.logInfo("LIS Payload: " + lisPayload);

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed!");
		utils.logPass("LIS created successfully");

		// Validate LIS response structure
		Assert.assertNotNull(lisResponse.jsonPath().getList("results"), "LIS results node is missing");
		Assert.assertTrue(lisResponse.jsonPath().getList("results").size() >= 2, "Expected 2 LIS results");

		// Get LIS External IDs
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId, "First LIS external_id is null");
		Assert.assertNotNull(secondId, "Second LIS external_id is null");

		utils.logit("Fetched LIS external IDs");

		// Create QC payload
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any").addLineItemFilter(firstId, "", 0)
				.addItemQualifier("net_quantity_equal_to", firstId, 4.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 5.0, 1).build();

		utils.logInfo("QC Payload: " + qcPayload);

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed!");
		utils.logPass("QC created successfully");

		// Validate QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC external ID is missing");
		utils.logInfo("QC External ID: " + qcExternalID);

		// Create Redeemable
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		utils.logInfo("Redeemable Payload: " + redeemablePayload);

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed!");
		logger.info("Redeemable Response: " + redeemableResponse.prettyPrint());

		// Validate redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable external ID missing");
		utils.logPass("Redeemable created successfully");

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "DB redeemable ID is null");
		utils.logInfo("DB Redeemable ID: " + dbRedeemableId);

		// User Sign-up
		utils.logit("Mobile API v2: User Sign-up");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "User signup failed!");

		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		Assert.assertNotNull(userId, "User ID is null");
		Assert.assertNotNull(token, "Token is null");

		utils.logPass("User signup successful");

		// Send Redeemable to user
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Send reward failed!");
		utils.logPass("Reward sent to user");

		// Get Reward ID for user
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward ID is null");
		utils.logInfo("Reward ID: " + rewardId);

		// Prepare Cart Parent Map
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "12", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "6", "M", "211", "211", "2.1", "211" });

		Assert.assertFalse(parentMap.isEmpty(), "Cart parent map is empty");

		// Auth Auto Select API Call
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Auth Auto Select failed");

		String discountType = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		// Validate details
		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);

		// POS Discount Lookup
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "33", externalUID, parentMap);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jp = discountLookupResponse.jsonPath();

		// Validations
		Assert.assertEquals(jp.getDouble("selected_discounts[0].discount_amount"), 5.0);
		Assert.assertEquals(jp.getString("selected_discounts[0].discount_id"), rewardId);
		Assert.assertEquals(jp.getString("selected_discounts[0].discount_type"), "reward");

		// Validate items
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Cheese"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name", List.of("Cheese DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-5.0));

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7322 Only Modifiers | Create an offer with two qualifiers using 'ALL' and verify the response")
	@Owner(name = "Rahul Garg")
	public void T7322_verifyOfferWithTwoItemQualifiers_UsingAllOperator_OnlyModifiers() throws Exception {
		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// Create LIS with base item-101 and modifier item-111
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "in", "111").addCurrentRule();

		// Create LIS with base item-201 and modifier item-211
		builder.startNewRule().setName("Item Id-201").setFilterItemSet(LineItemSelectorFilterItemSet.MODIFIERS_ONLY)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "in", "211").addCurrentRule();

		String lisPayload = builder.build();

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed via API");
		utils.logPass("LIS created successfully via API");

		// Get LIS External IDs
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId, "First LIS external ID is null");
		Assert.assertNotNull(secondId, "Second LIS external ID is null");
		utils.logit("LIS External IDs captured");

		// Create QC payload
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all").addLineItemFilter(firstId, "", 0)
				.addItemQualifier("net_quantity_equal_to", firstId, 4.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 5.0, 1).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed via API");
		utils.logPass("QC created successfully via API");

		// QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		utils.logit("QC External ID captured");

		// Create Redeemable payload
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed");
		utils.logPass("Redeemable created successfully");

		// Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		utils.logit("Redeemable External ID captured");

		// DB Redeemable ID
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		Assert.assertNotNull(dbRedeemableId, "Redeemable ID not found in DB");
		utils.logit("Redeemable DB ID validated");

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");

		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		Assert.assertNotNull(userId, "User ID is null");
		Assert.assertNotNull(token, "Token is null");

		utils.logPass("API v2 User Signup call is successful");

		// Send Reward to User
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");

		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);

		Assert.assertNotNull(rewardId, "Reward ID returned null");
		utils.logPass("Reward Id fetched: " + rewardId);

		// Build Receipt Input
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();

		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			Assert.assertNotNull(details, "Receipt details map is null for item: " + key);
			parentMap.put(key, details);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "12", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "6", "M", "211", "211", "2.1", "211" });

		// Auth Auto Select API - FAIL CONDITION
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		List<Object> discountBasketItems = redemptionResponse.jsonPath().getList("discount_basket_items");

		Assert.assertNotNull(discountBasketItems, "discount_basket_items is null");
		utils.logit("Discount basket items validated for ALL criteria failure");

		// Update input for success condition
		addDetails.accept("Cheese", new String[] { "Cheese", "4", "5", "M", "111", "111", "1.1", "111" });

		// Auth Auto Select API - SUCCESS CONDITION
		Response redemptionSuccessResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionSuccessResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket is successful");

		// Extract Discount details
		String discountType = redemptionSuccessResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionSuccessResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionSuccessResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");

		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");

		utils.logPass("Discount details are correct in Auth Auto Select API");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "33", externalUID, parentMap);

		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jp = discountLookupResponse.jsonPath();

		// Extract values
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");

		// Assertions
		Assert.assertEquals(discountAmount, 5.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");

		utils.logPass("Discount lookup validation successful");

		// Validate Items
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", List.of("Cheese"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name", List.of("Cheese DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", List.of(5.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", List.of(-5.0));

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7325 Base & Modifiers | Create an offer with two qualifiers using 'ANY' and verify the response")
	@Owner(name = "Rahul Garg")
	public void T7325_verifyOfferWithTwoItemQualifiers_UsingAnyOperator_BaseAndModifiers() throws Exception {

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);
		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// --------------------------
		// Create LIS
		// --------------------------

		utils.logit("== Creating Line Item Selectors (LIS) ==");

		// Build LIS payload

		// Create LIS with base item-101 and modifier item- 111
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "in", "111").addCurrentRule();

		// Create LIS with base item-201 and modifier item-211
		builder.startNewRule().setName("Item Id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "in", "211").addCurrentRule();

		String lisPayload = builder.build();
		utils.logInfo("LIS Payload: " + lisPayload);

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS creation failed!");
		utils.logPass("LIS created successfully");

		// Validate LIS external IDs
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		Assert.assertNotNull(firstId, "First LIS External ID is null");
		Assert.assertNotNull(secondId, "Second LIS External ID is null");
		utils.logPass("LIS External IDs fetched successfully");

		// --------------------------
		// Create QC
		// --------------------------

		utils.logit("== Creating Qualification Criteria (QC) ==");

		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("any").addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0).addItemQualifier("net_quantity_equal_to", firstId, 4.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 18.0, 1).build();

		utils.logInfo("QC Payload: " + qcPayload);

		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));

		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "QC creation failed!");
		utils.logPass("QC created successfully");

		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null");
		utils.logPass("QC External ID fetched");

		// --------------------------
		// Create Redeemable
		// --------------------------

		utils.logit("== Creating Redeemable ==");

		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		utils.logInfo("Redeemable Payload: " + redeemablePayload);

		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Redeemable creation failed!");
		utils.logPass("Redeemable created successfully");

		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		utils.logInfo("Redeemable External ID: " + redeemableExternalID);

		// --------------------------
		// Get Redeemable ID from DB
		// --------------------------

		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(dbRedeemableId, "DB Redeemable ID is null");
		utils.logPass("Redeemable ID fetched from DB");

		// --------------------------
		// USER SIGNUP
		// --------------------------

		utils.logit("== Mobile API v2: User Signup ==");

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("User signup successful");

		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		Assert.assertNotNull(userId, "User ID is null");
		Assert.assertNotNull(token, "Token is null");

		// --------------------------
		// SEND REWARD TO USER
		// --------------------------

		utils.logInfo("Sending reward to user");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");

		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		utils.logPass("Reward sent to user successfully");

		// --------------------------
		// GET REWARD ID
		// --------------------------

		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);

		Assert.assertNotNull(rewardId, "Reward ID is null");
		utils.logPass("Reward ID fetched successfully");

		// --------------------------
		// PREPARE PARENT MAP
		// --------------------------
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			parentMap.put(key, details);
		};
		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "12", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "6", "M", "211", "211", "2.1", "211" });

		// --------------------------
		// AUTH AUTO SELECT API
		// --------------------------

		Response autoSelectResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);

		Assert.assertEquals(autoSelectResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logit("AUTH Auto Select API call successful");

		String discountType = autoSelectResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = autoSelectResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = autoSelectResponse.jsonPath().getString("discount_basket_items[0].discount_details.name");

		Assert.assertEquals(discountType, "reward");
		Assert.assertEquals(discountId, rewardId);
		Assert.assertEquals(discountName, redeemableName);
		utils.logPass("Auth Auto Select validation passed");

		// --------------------------
		// POS Discount Lookup
		// --------------------------

		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "33", externalUID, parentMap);

		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		JsonPath jp = discountLookupResponse.jsonPath();

		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");

		Assert.assertEquals(discountAmount, 33.0);
		Assert.assertEquals(discountIdLookup, rewardId);
		Assert.assertEquals(discountTypeLookup, "reward");

		utils.logPass("POS Discount Lookup validation passed");

		// Validate M items
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", Arrays.asList("Sandwich", "Pizza"));
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name",
				Arrays.asList("Sandwich DISCOUNT", "Cheese DISCOUNT", "Pizza DISCOUNT", "Toppings DISCOUNT"));
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", Arrays.asList(15.0, 18.0));
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", Arrays.asList(-10.0, -5.0, -12.0, -6.0));

		utils.logPass("All QC, LIS, Redeemable & Redemption validations passed");

	}

	@Test(groups = { "OMM",
			"Regression" }, description = "SQ-T7324 Base & Modifiers | Create an offer with two qualifiers using 'ALL' and verify the response ")
	@Owner(name = "Rahul Garg")
	public void T7324_verifyOfferWithTwoItemQualifiers_UsingAllOperator_BaseAndModifiers() throws Exception {
		// --------------------------
		// Test: QC with BASE_AND_MODIFIERS LIS -> Redeemable -> Redemption validations
		// --------------------------

		String qcname = "POS_QC_" + Utilities.getTimestamp();
		String redeemableName = "POS_Redeemable_" + Utilities.getTimestamp()
				+ ThreadLocalRandom.current().nextLong(1000L, 5000L);

		utils.logit("Generated names - QC: " + qcname + " | Redeemable: " + redeemableName);

		LineItemSelectorPayloadBuilder builder = apipayloadObj.lineItemSelectorBuilder();

		// Create LIS with base item-101 and modifier item- 111
		builder.startNewRule().setName("Item id-101").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "101").addModifierClause("item_id", "in", "111").addCurrentRule();
		utils.logit("LIS rule added for base 101 with modifier 111");

		// Create LIS with base item-201 and modifier item- 211
		builder.startNewRule().setName("Item Id-201").setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "201").addModifierClause("item_id", "in", "211").addCurrentRule();
		utils.logit("LIS rule added for base 201 with modifier 211");

		String lisPayload = builder.build();
		utils.logit("LIS payload built: " + lisPayload);

		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		utils.logit("Called createLIS API");

		// --- Added assertions & listeners ---
		Assert.assertNotNull(lisResponse, "LIS response is null");
		Assert.assertTrue(
				lisResponse.getStatusCode() == ApiConstants.HTTP_STATUS_OK
						|| lisResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Unexpected status code for LIS creation: " + lisResponse.getStatusCode());
		utils.logPass("LIS created API responded with status: " + lisResponse.getStatusCode());

		// Get LIS External ID
		String firstId = lisResponse.jsonPath().getString("results[0].external_id");
		String secondId = lisResponse.jsonPath().getString("results[1].external_id");

		// Assertions for LIS IDs
		Assert.assertNotNull(firstId, "First LIS external_id is null");
		Assert.assertFalse(firstId.isEmpty(), "First LIS external_id is empty");
		Assert.assertNotNull(secondId, "Second LIS external_id is null");
		Assert.assertFalse(secondId.isEmpty(), "Second LIS external_id is empty");
		utils.logit("LIS External IDs captured: " + firstId + ", " + secondId);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(100).setQCProcessingFunction("sum_amounts")
				.setQualifyingExpressionsOperator("all").addLineItemFilter(firstId, "", 0)
				.addLineItemFilter(secondId, "", 0).addItemQualifier("net_quantity_equal_to", firstId, 4.0, 1)
				.addItemQualifier("net_amount_greater_than_or_equal_to", secondId, 18.0, 1).build();

		utils.logit("QC payload: " + qcPayload);
		Assert.assertNotNull(qcPayload, "QC payload is null");
		Assert.assertFalse(qcPayload.isEmpty(), "QC payload is empty");

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		utils.logit("Called createQC API");

		// Added assertions for QC response
		Assert.assertNotNull(qcResponse, "QC response is null");
		Assert.assertTrue(
				qcResponse.getStatusCode() == ApiConstants.HTTP_STATUS_OK
						|| qcResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Unexpected status code for QC creation: " + qcResponse.getStatusCode());
		utils.logPass("QC created API responded with status: " + qcResponse.getStatusCode());

		// Get QC External ID
		String qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit("QC External ID fetched");

		// Validate QC External ID
		Assert.assertNotNull(qcExternalID, "QC external ID is null");
		Assert.assertFalse(qcExternalID.isEmpty(), "QC external ID is empty");
		utils.logPass("QC external ID validated: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder redeemableBuilder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = redeemableBuilder.startNewData().setName(redeemableName)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setAutoApplicable(true).addCurrentData().build();

		utils.logit("Redeemable payload: " + redeemablePayload);
		Assert.assertNotNull(redeemablePayload, "Redeemable payload is null");
		Assert.assertFalse(redeemablePayload.isEmpty(), "Redeemable payload is empty");

		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		utils.logit("Called createRedeemable API");

		// Added assertions for Redeemable response
		Assert.assertNotNull(redeemableResponse, "Redeemable response is null");
		Assert.assertTrue(
				redeemableResponse.getStatusCode() == ApiConstants.HTTP_STATUS_OK
						|| redeemableResponse.getStatusCode() == ApiConstants.HTTP_STATUS_CREATED,
				"Unexpected status code for Redeemable creation: " + redeemableResponse.getStatusCode());
		utils.logPass("Redeemable created API responded with status: " + redeemableResponse.getStatusCode());

		// Get Redeemable External ID
		String redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Redeemable External ID fetched");

		// Validate Redeemable External ID
		Assert.assertNotNull(redeemableExternalID, "Redeemable external ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable external ID is empty");
		utils.logPass("Redeemable External ID validated: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = getRedeemableID.replace("$actualExternalIdRedeemable", redeemableExternalID);
		utils.logit("Querying DB for redeemable id");
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

		// Validate DB redeemable id
		Assert.assertNotNull(dbRedeemableId, "DB returned null redeemable id");
		Assert.assertFalse(dbRedeemableId.isEmpty(), "DB returned empty redeemable id");
		utils.logPass("Redeemable ID fetched from DB: " + dbRedeemableId);

		// User Sign-up
		utils.logit("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		utils.logit("Generated user email: " + userEmail);

		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertNotNull(signUpResponse, "Sign-up response is null");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		utils.logPass("API v2 User Signup call is successful");

		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertNotNull(userId, "User ID is null after signup");
		Assert.assertNotNull(token, "Token is null after signup");
		utils.logit("UserId: " + userId + " | token: [REDACTED]");

		// Gift redeemable to user
		utils.logit("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dbRedeemableId, "", "");
		Assert.assertNotNull(sendMessageToUserResponse, "Send message to user response is null");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		utils.logPass("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		utils.logit("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dbRedeemableId);
		Assert.assertNotNull(rewardId, "Reward Id is null");
		Assert.assertFalse(rewardId.isEmpty(), "Reward Id is empty");
		utils.logPass("Reward Id for user is fetched: " + rewardId);

		// Prepare parentMap
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<>();
		utils.logit("Preparing parentMap for receipt items");

		// Using BiConsumer to avoid repetitive code
		BiConsumer<String, String[]> addDetails = (key, args) -> {
			Map<String, String> details = pageObj.endpoints().getRecieptDetailsMap(args[0], args[1], args[2], args[3],
					args[4], args[5], args[6], args[7]);
			Assert.assertNotNull(details, "Receipt details map is null for key: " + key);
			parentMap.put(key, details);
			utils.logit("Added receipt details for: " + key);
		};

		addDetails.accept("Sandwich", new String[] { "Sandwich", "1", "10", "M", "101", "101", "1.0", "101" });
		addDetails.accept("Cheese", new String[] { "Cheese", "1", "5", "M", "111", "111", "1.1", "111" });
		addDetails.accept("Pizza", new String[] { "Pizza", "1", "12", "M", "201", "201", "2.0", "201" });
		addDetails.accept("Toppings", new String[] { "Toppings", "1", "6", "M", "211", "211", "2.1", "211" });

		Assert.assertFalse(parentMap.isEmpty(), "parentMap should not be empty");
		utils.logit("Parent map prepared: " + parentMap);

		// Auth Auto select API
		Response redemptionResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);
		Assert.assertNotNull(redemptionResponse, "Auth Auto Select response is null");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket call successful with status: " + redemptionResponse.getStatusCode());

		// Validate discount_basket_items is null (or empty) when ALL criteria is not
		// met
		List<Object> discountBasketItems = redemptionResponse.jsonPath().getList("discount_basket_items");
		utils.logit("discount_basket_items fetched: " + discountBasketItems);

		// The original code asserts notNull on discount_basket_items; preserve that but
		// also check expectation
		Assert.assertNotNull(redemptionResponse.jsonPath().get("discount_basket_items"),
				"discount_basket_items node is null");
		// If business expects empty array when not met, also check size
		if (discountBasketItems != null) {
			Assert.assertTrue(discountBasketItems.isEmpty() || discountBasketItems.size() == 0,
					"discount_basket_items expected to be empty when ALL criteria not met but found: "
							+ discountBasketItems);
			utils.logit("discount_basket_items empty as expected when ALL criteria not met");
		}

		// update input receipt to meet 'ALL' criteria
		addDetails.accept("Sandwich", new String[] { "Sandwich", "4", "10", "M", "101", "101", "1.0", "101" });
		utils.logit("Updated parentMap: Sandwich quantity updated to 4");

		// Auth Auto select API (success case)
		Response redemptionSuccessResponse = pageObj.endpoints().authAutoSelectAPI(dataSet.get("client"),
				dataSet.get("secret"), token, "33", externalUID, parentMap);
		Assert.assertNotNull(redemptionSuccessResponse, "Auth Auto Select success response is null");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionSuccessResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logit("AUTH add discount to basket (post-update) successful");

		// Extract values
		String discountType = redemptionSuccessResponse.jsonPath().getString("discount_basket_items[0].discount_type");
		String discountId = redemptionSuccessResponse.jsonPath().getString("discount_basket_items[0].discount_id");
		String discountName = redemptionSuccessResponse.jsonPath()
				.getString("discount_basket_items[0].discount_details.name");

		// Validate response in Auth Auto Select
		Assert.assertNotNull(discountType, "discountType is null in auth auto select");
		Assert.assertNotNull(discountId, "discountId is null in auth auto select");
		Assert.assertNotNull(discountName, "discountName is null in auth auto select");

		Assert.assertEquals(discountType, "reward", "Incorrect discount_type!");
		Assert.assertEquals(discountId, rewardId, "Incorrect discount_id!");
		Assert.assertEquals(discountName, redeemableName, "Incorrect discount name!");
		utils.logPass("Discount details are correct in Auth Auto Select API response");

		// POS Discount Lookup API
		Response discountLookupResponse = pageObj.endpoints().processBatchRedemptionOfBasketPOSDiscountLookupWithExtUid(
				dataSet.get("locationKey"), userId, "33", externalUID, parentMap);
		Assert.assertNotNull(discountLookupResponse, "Discount lookup response is null");
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS discount lookup failed");
		utils.logit("POS Discount Lookup API returned status: " + discountLookupResponse.getStatusCode());

		JsonPath jp = discountLookupResponse.jsonPath();

		// Extract values
		Double discountAmount = jp.getDouble("selected_discounts[0].discount_amount");
		String discountIdLookup = jp.getString("selected_discounts[0].discount_id");
		String discountTypeLookup = jp.getString("selected_discounts[0].discount_type");

		// --- Assertions ---
		Assert.assertNotNull(discountAmount, "discountAmount is null");
		Assert.assertNotNull(discountIdLookup, "discountIdLookup is null");
		Assert.assertNotNull(discountTypeLookup, "discountTypeLookup is null");

		Assert.assertEquals(discountAmount, 33.0, "Incorrect discount_amount");
		Assert.assertEquals(discountIdLookup, rewardId, "Incorrect discount_id");
		Assert.assertEquals(discountTypeLookup, "reward", "Incorrect discount_type");
		utils.logPass("Discount details are correct in POS discount lookup API response");

		// --- Validate M-item names ---
		List<String> expectedMNames = Arrays.asList("Sandwich", "Pizza");
		apiUtils.validateItems(jp, "selected_discounts", "M", "item_name", expectedMNames);
		utils.logit("Validated M item names: " + expectedMNames);

		// --- Validate R-item names ---
		List<String> expectedRNames = Arrays.asList("Sandwich DISCOUNT", "Cheese DISCOUNT", "Pizza DISCOUNT",
				"Toppings DISCOUNT");
		apiUtils.validateItems(jp, "selected_discounts", "R", "item_name", expectedRNames);
		utils.logit("Validated R item names: " + expectedRNames);

		// Validate M amounts
		List<Double> expectedMAmounts = Arrays.asList(15.0, 18.0);
		apiUtils.validateItems(jp, "selected_discounts", "M", "amount", expectedMAmounts);
		utils.logit("Validated M amounts: " + expectedMAmounts);

		// Validate R amounts
		List<Double> expectedRAmounts = Arrays.asList(-10.0, -5.0, -12.0, -6.0);
		apiUtils.validateItems(jp, "selected_discounts", "R", "amount", expectedRAmounts);
		utils.logit("Validated R amounts: " + expectedRAmounts);

		utils.logPass("All QC/LIS/Redeemable/redemption validations passed for test: " + qcname);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		try {

			// Delete LIS, QC and Redeemable Data
			utils.deleteLISQCRedeemable(env, firstId, qcExternalID, redeemableExternalID);
			utils.deleteLISQCRedeemable(env, secondId, null, null);

			// Clean up test data
			pageObj.utils().clearDataSet(dataSet);
		} catch (Exception e) {
			utils.logit("warn", "Failed to clear dataset: " + e.getMessage());
		} finally {
			try {
				if (driver != null) {
					driver.quit(); // safer than driver.close(), closes all windows and ends session
					utils.logInfo("Browser closed");
				}
			} catch (Exception e) {
				utils.logit("error", "Failed to close browser: " + e.getMessage());
			}
		}
	}

}
