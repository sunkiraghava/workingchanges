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

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiPayloads;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EPSBugFixesTest {

	static Logger logger = LogManager.getLogger(EPSBugFixesTest.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	public Utilities utils;
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String QCDeleteBaseQuery = "Delete from qualification_criteria where external_id = '${externalID2}' and business_id='${businessID}'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='${redeemableExternalID}' and business_id ='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	String samplequery = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "OMM-T4079-Verify the discount calculation for receipt - Hit target menu item price for min priced matching item for one unit", groups = { "regression", "dailyrun" })
	public void T4079_verifyQCAmountForLIQNetAmountExcludingMinPricedItem() throws Exception {

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Create LIS
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");

		// Create QC with Sum of Amounts
		Map<String, String> map = new HashMap<String, String>();
		String lifDetails = "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":null}]";
		map.put("line_item_filters", lifDetails);
		map.put("receipt_qualifiers", "\"\"");
		map.put("item_qualifiers",
				"[{\"expression_type\":\"net_amount_excluding_min_priced_item_equal_to_or_more_than\",\"line_item_selector_id\":\""
						+ actualExternalIdLIS + "\",\"net_value\":5}]");
		map.put("amount_cap", "\"\"");
		map.put("percentage_of_processed_amount", "");
		map.put("rounding_rule", "");
		map.put("max_discount_units", "\"\"");

		// creating qc with valid location id of the same business
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", "", "",
				processingFunction, "", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "existing");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("redeeming_criterion_id", actualExternalIdQC);
		redeemableMap.put("line_item_selector_id", dataSet.get("lisExternalID"));
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");
		String query = samplequery.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String	dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

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

		// Send reward to user
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

		// POS Add Discount to Basket
		utils.logit("== POS API: Add Discount to Basket ==");

		Response addDiscountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, "123");
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), 200,
				"Status code 200 did not match for POS API Add Discount to Basket call.");
		utils.logPass("POS API Add Discount to Basket call is successful");

		// Add multiple items dynamically

		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		// Dummy Menu Item|1|5.29|M|101|306|522|1.0
		// Dummy Menu Item|1|5|M|6240|306|522|2.0
		receiptItems = ApiPayloads.getInputForReceiptItems("Sandwich", 1, 5, "M", "101", "1", "152", "1");
		lineItems.add(receiptItems);

		receiptItems = ApiPayloads.getInputForReceiptItems("Sandwich", 1, 5.29, "M", "6240", "1", "152", "2");
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// Hit POS discount lookup API and verify the qualification of discount
		// type-reward
		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, subtotal_amount, receipt_amount, punchh_key, transaction_no, userId,
				dataSet.get("locationKey"),"123");

		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		
		String discount_type = posDiscountLookupResponse.jsonPath().getString("selected_discounts.discount_type")
				.replaceAll("[\\[\\]]", "");
		double discount_amount = Double.parseDouble(posDiscountLookupResponse.jsonPath()
				.getString("selected_discounts.discount_amount").replaceAll("[\\[\\]]", ""));
		
		
		Assert.assertEquals(discount_type, "reward");
		Assert.assertEquals(discount_amount, 5);
		utils.logPass("Verified that qualified discount type is reward");

		// // Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		// Delete from redeemable tables

		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		utils.logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}



	@Test(description = "OMM-T4087-Verify the discount calculation for receipt - Hit target menu item price for max priced matching item for one unit || "
			+ "OMM-T4089-Verify the discount calculation for receipt with processing function- Hit Target Menu Item Price for single offer ", groups = { "regression", "dailyrun" })

	public void T4087_verifyQCAmountForLIQNetAmountExcludingMaxPricedItem() throws Exception {

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");

		// Create QC with Sum of Amounts

		Map<String, String> map = new HashMap<String, String>();
		String lifDetails = "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":null}]";
		map.put("line_item_filters", lifDetails);
		map.put("receipt_qualifiers", "\"\"");
		map.put("item_qualifiers",
				"[{\"expression_type\":\"net_amount_excluding_max_priced_item_equal_to_or_more_than\",\"line_item_selector_id\":\""
						+ actualExternalIdLIS + "\",\"net_value\":5}]");
		map.put("amount_cap", "\"\"");
		map.put("percentage_of_processed_amount", "");
		map.put("rounding_rule", "");
		map.put("max_discount_units", "\"\"");

		// creating qc with valid location id of the same business
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", "", "",
				processingFunction, "", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		// Create Redeemable
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");

		Map<String, String> redeemableMap = new HashMap<String, String>();
		redeemableMap.put("redeemableName", redeemableName);
		redeemableMap.put("external_id", "");
		redeemableMap.put("locationID", null);
		redeemableMap.put("external_id_redeemable", "");
		redeemableMap.put("qualifier_type", "existing");
		redeemableMap.put("end_time", endTime);
		redeemableMap.put("redeeming_criterion_id", actualExternalIdQC);

		redeemableMap.put("line_item_selector_id", dataSet.get("lisExternalID"));
		redeemableMap.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responsRedeemable = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), redeemableMap);
		Assert.assertEquals(responsRedeemable.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualExternalIdRedeemable = responsRedeemable.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");

		String samplequery = "Select id from redeemables where uuid ='$actualExternalIdRedeemable'";

		String query = samplequery.replace("$actualExternalIdRedeemable", actualExternalIdRedeemable);
		String	dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");

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

		// Send reward to user
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

		// POS Add Discount to Basket
		utils.logit("== POS API: Add Discount to Basket ==");

		Response addDiscountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, "123");
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), 200,
				"Status code 200 did not match for POS API Add Discount to Basket call.");
		utils.logPass("POS API Add Discount to Basket call is successful");

		// Add multiple items dynamically

		List<Map<String, Object>> lineItems = new ArrayList<>();
		Map<String, Object> receiptItems = new HashMap<String, Object>();

		//		Dummy Menu Item|1|5.29|M|101|306|522|1.0
		//		Dummy Menu Item|1|5|M|6240|306|522|2.0
		receiptItems = ApiPayloads.getInputForReceiptItems("Sandwich", 1, 5.29, "M", "101", "1", "152", "1");
		lineItems.add(receiptItems);

		receiptItems = ApiPayloads.getInputForReceiptItems("Sandwich", 1, 5, "M", "6240", "1", "152", "2");
		lineItems.add(receiptItems);

		String receipt_datetime = CreateDateTime.getCurrentDateTimeInUtc();
		String punchh_key = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String transaction_no = Integer.toString(Utilities.getRandomNoFromRange(500000000, 900000000));

		// Hit POS discount lookup API and verify the qualification of discount type-reward
		Response posDiscountLookupResponse = pageObj.endpoints().posDiscountLookupAPIInputPayload(lineItems,
				receipt_datetime, subtotal_amount, receipt_amount, punchh_key, transaction_no, userId,
				dataSet.get("locationKey"),"123");
		
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String discount_type = posDiscountLookupResponse.jsonPath().getString("selected_discounts.discount_type")
				.replaceAll("[\\[\\]]", "");
		double discount_amount = Double.parseDouble(posDiscountLookupResponse.jsonPath()
				.getString("selected_discounts.discount_amount").replaceAll("[\\[\\]]", ""));
		
		Assert.assertEquals(discount_type, "reward");
		Assert.assertEquals(discount_amount, 5.29);
		utils.logPass("Verified that qualified discount type is reward");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		// Delete from redeemables tables
		String deleteRedeemableQuery1 = deleteRedeemableQuery
				.replace("${redeemableExternalID}", actualExternalIdRedeemable)
				.replace("${businessID}", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		utils.logPass(actualExternalIdRedeemable + " external redeemable is deleted successfully");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
