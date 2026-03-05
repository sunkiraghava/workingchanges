package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiTest.POJO;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SingletonDBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Vansham Mishra
 */

@Listeners(TestListeners.class)

public class StackingAndReusabilityTest {
	static Logger logger = LogManager.getLogger(StackingAndReusabilityTest.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> DataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T6508 Verify the Test Case-1 for ticket 1188 for Modifiers only", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Vansham Mishra")
	public void T6508_verifyTestCase1ForTicket1188ModifiersOnly() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6508_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"modifiers_only", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().searchAndSelectLineItemSelector(lisName1);
		pageObj.lineItemSelectorPage().clearMaxDiscountedModifiers();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6508_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items", "[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
				+ "            \"item_qty\": 4,\n" + "            \"amount\": 20,\n"
				+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 101,\n"
				+ "            \"item_family\": \"11\",\n" + "            \"item_group\": \"23\",\n"
				+ "            \"serial_number\": \"1.0\"\n" + "        },\n" + "        { \n"
				+ "            \"item_name\": \"Topping1\",\n" + "            \"item_qty\": 2,\n"
				+ "            \"amount\": 2,\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": 111,\n" + "            \"item_family\": \"11\",\n"
				+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.1\"\n" + "        },\n"
				+ "		{ \n" + "            \"item_name\": \"Topping2\",\n" + "            \"item_qty\": 2,\n"
				+ "            \"amount\": 3,\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": 112,\n" + "            \"item_family\": \"11\",\n"
				+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.2\"\n" + "        }]");
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "29", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "111", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-1.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "111", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-1.0",
					"Discount amount for the second item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "111", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "111", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "111", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			pageObj.utils().logPass("Item id for the item is matched with 111 for the first discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "111", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for both first discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6509 Verify the Test Case-1 for ticket 1188 for Modifiers only", groups = { "regression",
			"dailyrun" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T6509_verifyTestCase2ForTicket1188ModifiersOnly() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_T6509" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_T6509" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_T6509" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"modifiers_only", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().searchAndSelectLineItemSelector(lisName1);
		pageObj.lineItemSelectorPage().clearMaxDiscountedModifiers();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_T6509" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_T6509" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items", "[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
				+ "            \"item_qty\": 4,\n" + "            \"amount\": 20,\n"
				+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 101,\n"
				+ "            \"item_family\": \"11\",\n" + "            \"item_group\": \"23\",\n"
				+ "            \"serial_number\": \"1.0\"\n" + "        },\n" + "        { \n"
				+ "            \"item_name\": \"Topping1\",\n" + "            \"item_qty\": 2,\n"
				+ "            \"amount\": 2,\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": 111,\n" + "            \"item_family\": \"11\",\n"
				+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.1\"\n" + "        },\n"
				+ "		{ \n" + "            \"item_name\": \"Topping2\",\n" + "            \"item_qty\": 2,\n"
				+ "            \"amount\": 3,\n" + "            \"item_type\": \"M\",\n"
				+ "            \"item_id\": 112,\n" + "            \"item_family\": \"11\",\n"
				+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.2\"\n" + "        }]");
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "29", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "111", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-1.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "111", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-1.0",
					"Discount amount for the second item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "111", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "111", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "111", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			pageObj.utils().logPass("Item id for the item is matched with 111 for the first discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "111", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for both first discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6510 Verify the Test Case-3 for ticket 1188 for Base only", groups = { "regression",
			"dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T6510_verifyTestCase3ForTicket1188BaseOnly() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        pageObj.utils().logPass("API2 Signup is successful");
        Map<String, String> map = new HashMap<String, String>();
        String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
        String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
        String QCName2 = "AutomationQC2_API_T6510_" + CreateDateTime.getTimeDateString();
        String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
        String processingFunction2 = "bundle_price_target";
        String processingFunction3 = "rate_rollback";
        map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6510_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6510_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
        map.put("external_id", "");
        map.put("amount_cap", "10.0");
        map.put("percentage_of_processed_amount", "1");
        map.put("qc_processing_function", "bogof2");
        map.put("line_item_selector_id", actualExternalIdLIS);
        map.put("locationID", null);
        map.put("external_id_redeemable", "");
        map.put("redeemableProcessingFunction", "Sum Of Amount");
        map.put("qualifier_type", "existing");
        map.put("amount_cap", "10.0");
        map.put("expQCName", QCName2);
        map.put("lineitemSelector", dataSet.get("lineItemSelector"));
        map.put("redeeming_criterion_id", actualExternalIdQC2);
        map.put("distributable", "true");
        map.put("segment_definition_id", dataSet.get("segmentID"));
        map.put("indefinetely", "false");
        map.put("active_now", "true");
        map.put("expiry_days", null);
        // leap year start date > 30 for feb month
        String redeemableName21 = "AutomationRedeemableExistingQC21_API_T6510_" + CreateDateTime.getTimeDateString();
        map.put("name", QCName2);
        map.put("redeemableName", redeemableName21);
        map.put("start_time", "2027-12-12T00:00:00");
        map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 1,\n"
						+ "            \"amount\": 10,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"11\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "		{ \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 1,\n" + "            \"amount\": 10,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 101,\n"
						+ "            \"item_family\": \"11\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        }]");
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "20", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6511 Verify the Test Case-4 for ticket 1188 for Base only", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6511_verifyTestCase4ForTicket1188BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));
//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);
		logger.info("Line item list JSON payload: " + employeeListPrettyJson);
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6511_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6511_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6511_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6511_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6511_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "40", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for the both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the first item");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6512 Verify the Test Case-5 for ticket 1188 for Base only", groups = { "regression",
			"dailyrun" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T6512_verifyTestCase4ForTicket1188BaseOnly() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6512_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6512_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6512_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6512_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6512_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 3,\n"
						+ "            \"amount\": 30,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"11\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "		{ \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 1,\n" + "            \"amount\": 10,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 101,\n"
						+ "            \"item_family\": \"11\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        }]");
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "40", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "29", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6481 Verify Discount calculation on Case 3 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Vansham Mishra")
	public void T6481_verifyTestCase1ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));
//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6481_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6481_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6481_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6481_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "30", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the first item");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for first discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
            // assert discount amount and item of the qualified items
            String discountAmount1 = discountLookupResponse0.jsonPath()
                    .get("selected_discounts[0].qualified_items[1].amount").toString();
            // assert discountAmount1 should be -1
            Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
            // assert discountAmount2 should be -1
            logger.info("Discount amount for the item is matched with -10.0 for the first discount");
            pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
            // assert selected_discounts[0].qualified_items[0].item_id should be 102
            String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
                    .toString();
            Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
            pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

            // POS Process Batch Redemption
            Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
                    .processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
                            external_id, map);
            Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
                    "Status code 200 did not match with Process Batch Redemption ");
            pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

            // assert discount amount and item of the qualified items
            String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
                    .get("success[0].qualified_items[1].amount").toString();
            // assert discountAmount1 should be -1
            Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
                    "Discount amount for the first item is not matched");
            logger.info("Discount amount for the item is matched with -10.0 for the first item");
            pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

            // assert selected_discounts[0].qualified_items[0].item_id should be 102
            String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
                    .get("success[0].qualified_items[1].item_id").toString();
            Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
            pageObj.utils().logPass("Item id for the item is matched with 101 for first discount");
            // verify the two entries in redemptions table should be created for the user
            String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
            getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
                    businessID);
            pageObj.singletonDBUtilsObj();
            List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
                    "redemption_code_id");
            Assert.assertEquals(redemptionIds.size(), 1,
                    "Redemption code id is not created for the user in redemptions table");
            pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
            // verify the status should be processed in redemption_codes table by taking
            // redemption_code_id from redemtions table
            String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
            getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
                    .replace("${businessID}", businessID);
            pageObj.singletonDBUtilsObj();
            String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
            Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
            logger.info("Status is processed in redemption_codes table for the redemption code id: "
                    + redemptionIds.get(0));
            pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
                            + redemptionIds.get(0));
		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6482 Verify Discount calculation on Case 2 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6482_verifyTestCase2ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6482_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_6482_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6482_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6482_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// create QC and verified error message
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS,
				"", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName3);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6482_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

        // create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_T6482_" + CreateDateTime.getTimeDateString();
        map.put("name", QCName3);
        map.put("redeemableName", redeemableName22);
        Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
        Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
        Assert.assertEquals(successMessage22, true,
                "Redeeming criterion processing method is not included in the list error message is not matched");
        logger.info(redeemableName22 + " redeemable is created successfully");
        String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
        String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID + "'";
        String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
        String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3,
                "id");
        logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
        // send reward amount to user Reedemable
        Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
                "", redeemable_ID2, "", "", "");
        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
                "Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "20", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-1.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "20", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-1.0",
					"Discount amount for the second item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Redemption not possible since amount is 0"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Redemption not possible since amount is 0."),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria3 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria3);
		Assert.assertTrue(statusQualification_criteriaQuery3, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");
		pageObj.utils().logit(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6483 Verify Discount calculation on Case 3 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6483_verifyTestCase3ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
        String userID = signUpResponse.jsonPath().get("user.user_id").toString();
        Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
        pageObj.utils().logPass("API2 Signup is successful");
        Map<String, String> map = new HashMap<String, String>();
        String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
        String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
        String QCName2 = "AutomationQC2_API_T6483_" + CreateDateTime.getTimeDateString();
        String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
        String processingFunction3 = "sum_amounts";
        map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
        map.put("external_id", "");
        map.put("amount_cap", "10.0");
        map.put("percentage_of_processed_amount", "1");
        map.put("qc_processing_function", "bogof2");
        map.put("line_item_selector_id", actualExternalIdLIS);
        map.put("locationID", null);
        map.put("external_id_redeemable", "");
        map.put("redeemableProcessingFunction", "Sum Of Amount");
        map.put("qualifier_type", "existing");
        map.put("amount_cap", "10.0");
        map.put("expQCName", QCName2);
        map.put("lineitemSelector", dataSet.get("lineItemSelector"));
        map.put("redeeming_criterion_id", actualExternalIdQC2);
        map.put("distributable", "true");
        map.put("segment_definition_id", dataSet.get("segmentID"));
        map.put("indefinetely", "false");
        map.put("active_now", "true");
        map.put("expiry_days", null);
        // leap year start date > 30 for feb month
        String redeemableName21 = "AutomationRedeemableExistingQC21_API_T6483_" + CreateDateTime.getTimeDateString();
        map.put("name", QCName2);
        map.put("redeemableName", redeemableName21);
        map.put("start_time", "2027-12-12T00:00:00");
        map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
        // create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_T6483_" + CreateDateTime.getTimeDateString();
        map.put("name", QCName2);
        map.put("redeemableName", redeemableName22);
        Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
        Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
        boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
        Assert.assertEquals(successMessage22, true,
                "Redeeming criterion processing method is not included in the list error message is not matched");
        logger.info(redeemableName22 + " redeemable is created successfully");
        String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
        String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID + "'";
        String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
        String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3,
                "id");
        logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
        // send reward amount to user Reedemable
        Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
                "", redeemable_ID2, "", "", "");
        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
                "Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "40", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6484 Verify Discount calculation on Case 4 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6484_verifyTestCase4ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6484_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6484_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6484_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "uncheck");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6484_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6484_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "40", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6485 Verify Discount calculation on Case 5 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6485_verifyTestCase5ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6485_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_6485_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6485_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6485_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "1");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "1");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("amount_cap", "0");
		// create QC and verified error message
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName3);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6485_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6485_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "160", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-1.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -1.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-1.0",
					"Discount amount for the first item is not matched");
			logger.info("Discount amount for the item is matched with -1.0 for the first item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria3 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria3);
		Assert.assertTrue(statusQualification_criteriaQuery3, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");
		pageObj.utils().logit(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6486 Verify Discount calculation on Case 6 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6486_verifyTestCase6ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6486_" + CreateDateTime.getTimeDateString()+CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "uncheck");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6486_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6486_" + CreateDateTime.getTimeDateString();
		pageObj.utils().logit("second redeemable name " + redeemableName22);
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "20", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "40", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6487 Verify Discount calculation on Case 7 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6487_verifyTestCase7ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6487_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_6487_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6487_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6487_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("amount_cap", "0");
		// create QC and verified error message
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "uncheck");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "uncheck");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName3);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "uncheck");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC22_API_6487_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6487_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "160", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-10.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -10.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "1.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-10.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -10.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-10.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 101 for the first discount");

			String discountMessage2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].message[0]")
					.toString();
			// assert discountMessage2 should contains Discount qualification on receipt
			// failed
			Assert.assertTrue(discountMessage2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-10.0",
					"Discount amount for the first item is not matched");
			pageObj.utils().logPass("Discount amount for the item is matched with -10.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("failures[0].message[0]").toString();
			// assert discountAmountLookupResponse2 should contains Discount qualification
			// on receipt failed
			Assert.assertTrue(discountAmountLookupResponse2.contains("Discount qualification on receipt failed"),
					"Discount message for the second item is not matched");
			pageObj.utils().logPass(
					"Discount message for the second item is matched with Discount qualification on receipt failed");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 1,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria3 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria3);
		Assert.assertTrue(statusQualification_criteriaQuery3, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");
		pageObj.utils().logit(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6504 Verify Discount calculation on Case 8 on the 1128 ticket- BOGO (Any Item)", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T6504_verifyTestCase8ForTicket1128BaseOnly() throws Exception {
		POJO lineItem = new POJO();
		lineItem.setitem_name(dataSet.get("item_name"));
		lineItem.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem.setAmount(Integer.parseInt(dataSet.get("amount")));
		lineItem.setitem_type("M");
		lineItem.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem.setitem_family(dataSet.get("item_family"));
		lineItem.setitem_group(dataSet.get("item_group"));
		lineItem.setserial_number(dataSet.get("serial_number"));

		POJO lineItem2 = new POJO();
		lineItem2.setitem_name(dataSet.get("item_name"));
		lineItem2.setitem_qty(Integer.parseInt(dataSet.get("item_qty")));
		lineItem2.setAmount(17);
		lineItem2.setitem_type("M");
		lineItem2.setitem_id(Integer.parseInt(dataSet.get("item_id")));
		lineItem2.setitem_family(dataSet.get("item_family"));
		lineItem2.setitem_group(dataSet.get("item_group"));
		lineItem2.setserial_number("2.0");

//        String external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Creating a List of Employees
		List<POJO> lineItemList = new ArrayList<POJO>();
		lineItemList.add(lineItem);
		lineItemList.add(lineItem2);
		// Converting a Java class object to a JSON Array Payload as string
		ObjectMapper mapper = new ObjectMapper();
		String employeeListPrettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(lineItemList);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6504_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_6504_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "sum_amounts";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_6504_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_6504_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bogof2";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 1, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("amount_cap", "0");
		map.put("item_qualifiers", "[{\"expression_type\": \"\",\"line_item_selector_id\": \"\",\"net_value\":null}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"max_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");
		// create QC and verified error message
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS,
				"", "sum_amounts", "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "uncheck");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName3);
//		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bogof2");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_6504_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Redeemable_id for the first redeemable is: " + redeemable_ID2);

		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// create 2nd redeemable
		String redeemableName22 = "AutoRedeemableExistingQC22_API_6504_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		pageObj.singletonDBUtilsObj();
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// send 2nd reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		// get 2nd reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		map.put("line_items", employeeListPrettyJson);
		external_id = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationKey"),
				userID, "101", "160", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		pageObj.singletonDBUtilsObj();
		String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		pageObj.singletonDBUtilsObj();
		List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences,
				"enable_decoupled_redemption_engine");
		if (keyValueFromPreferences_Kafka.contains("true")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is true in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-17.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -17.0 for the first discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -17.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-2.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -2.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -2.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "2.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-17.0",
					"Discount amount for the first item is not matched");
			logger.info("Discount amount for the item is matched with -17.0 for the first item");
			pageObj.utils().logPass("Discount amount for the item is matched with -17.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-2.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -2.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -2.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));
		} else if (keyValueFromPreferences_Kafka.contains("false")) {
			pageObj.utils().logPass("value of enable_decoupled_redemption_engine is false in preferences");
			// assert discount amount and item of the qualified items
			String discountAmount1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount1, "-17.0", "Discount amount for the first item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -17.0 for the first discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -17.0 for the first discount");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
			// assert selected_discounts[1].qualified_items[0].item_id should be 111
			// assert discount amount and item of the qualified items
			String discountAmount2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmount2, "-2.0", "Discount amount for the second item is not matched");
			// assert discountAmount2 should be -1
			logger.info("Discount amount for the item is matched with -2.0 for the second discount");
			pageObj.utils().logPass("Discount amount for the item is matched with -2.0 for the second discount");
			String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
					.toString();
			Assert.assertEquals(itemId2, "101", "Item id for the second item is not matched");
			pageObj.utils().logPass("Item id for the item is matched with 111 for the both first and second discount");
			// assert the serial number should be 1.0 for the second item
			String serialNumber1 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[0].qualified_items[1].serial_number").toString();
			String serialNumber2 = discountLookupResponse0.jsonPath()
					.get("selected_discounts[1].qualified_items[1].serial_number").toString();
			Assert.assertEquals(serialNumber1, "2.0", "Serial number for the first item is not matched");
			Assert.assertEquals(serialNumber2, "1.0", "Serial number for the second item is not matched");
			logger.info("Serial number for the item is matched with 1.0 for both first and second discount");
			pageObj.utils().logPass("Serial number for the item is matched with 1.0 for both first and second discount");

			// POS Process Batch Redemption
			Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
					.processBatchRedemptionPosApiPayloadWithMap(dataSet.get("locationKey"), userID, "160", "1", "101",
							external_id, map);
			Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
					"Status code 200 did not match with Process Batch Redemption ");
			pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

			// assert discount amount and item of the qualified items
			String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse1, "-17.0",
					"Discount amount for the first item is not matched");
			logger.info("Discount amount for the item is matched with -17.0 for the first item");
			pageObj.utils().logPass("Discount amount for the item is matched with -17.0 for the first item");

			String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].amount").toString();
			// assert discountAmount1 should be -1
			Assert.assertEquals(discountAmountLookupResponse2, "-2.0",
					"Discount amount for the second item is not matched");
			logger.info("Discount amount for the item is matched with -2.0 for the second item");
			pageObj.utils().logPass("Discount amount for the item is matched with -2.0 for the second item");
			// assert selected_discounts[0].qualified_items[0].item_id should be 102
			String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[0].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
			String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
					.get("success[1].qualified_items[1].item_id").toString();
			Assert.assertEquals(itemIdLookUpResponse2, "101", "Item id for the second item is not matched");
			logger.info("Item id for the item is matched with 101 for both first and second discount");
			pageObj.utils().logPass("Item id for the item is matched with 101 for both first and second discount");
			// verify the two entries in redemptions table should be created for the user
			String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
			getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}",
					businessID);
			pageObj.singletonDBUtilsObj();
			List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
					"redemption_code_id");
			Assert.assertEquals(redemptionIds.size(), 2,
					"Redemption code id is not created for the user in redemptions table");
			pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
			Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(0));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(0));
			// verify the status should be processed in redemption_codes table by taking
			// redemption_code_id from redemtions table
			String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
			getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
					.replace("${businessID}", businessID);
			pageObj.singletonDBUtilsObj();
			String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
			Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
			logger.info("Status is processed in redemption_codes table for the redemption code id: "
					+ redemptionIds.get(1));
			pageObj.utils().logPass("Status is processed in redemption_codes table for the redemption code id: "
							+ redemptionIds.get(1));

		}

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria3 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria3);
		Assert.assertTrue(statusQualification_criteriaQuery3, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");
		pageObj.utils().logit(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}