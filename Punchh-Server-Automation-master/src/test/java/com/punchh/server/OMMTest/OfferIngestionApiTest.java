package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Hardik Bhardwaj
 */

@Listeners(TestListeners.class)
public class OfferIngestionApiTest {
	static Logger logger = LogManager.getLogger(OfferIngestionApiTest.class);
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

// Hardik
	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5464 Verify that the GET API successfully fetches the list of all redeemable's (count should match) from the DB", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5464_VerifyRedeemableListAPI() throws Exception {
		String adminKey = dataSet.get("apiKey");
		String b_id = dataSet.get("business_id");
		int dbRedeemableCount = 0;
		boolean dbCountFlag = false;

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_offers_ingestion", "check");
		pageObj.dashboardpage().updateCheckBox();

		Response redeemableListResp = pageObj.endpoints().redeemableListAPi(adminKey, "", "1", "20");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent = redeemableListResp.jsonPath().get("meta.total_records").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent);

		String query = "Select count(*) from redeemables where business_id = " + b_id + ";";

		ResultSet resultSet = DBUtils.getResultSet(env, query);

		if (resultSet.next()) {
          dbRedeemableCount = resultSet.getInt(1);
          pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
        }
		DBManager.closeConnection();
        // String dbRedeemableCount = Integer.toString(count);
        pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent)) {
			dbCountFlag = true;
		}

		Assert.assertTrue(dbCountFlag, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass(
				"Verified that API should calculate the total number of pages based on the per_page value and return the data for the last valid page.");

	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5465 Verify that API calculates the total number of pages with the per_page value and returns data for the last valid page if a large integer is given for page or per_page"
			+ "SQ-T6018 (1.0) Apply the 'recency' filter to sort the listed deals.", groups = { "unstable",
					"regression", "dailyrun" }, priority = 5)
	@Owner(name = "Hardik Bhardwaj")
	public void T5465_VerifyRedeemableListAPI() throws Exception {
		String adminKey = dataSet.get("apiKey");
		String b_id = dataSet.get("business_id");
		int dbRedeemableCount = 0;
		String page = "9999999999999";
		String per_page = "999999999";
		boolean dbCountFlag = false;
		boolean dbCountFlag1 = false;
		boolean dbCountFlag2 = false;
		boolean dbCountFlag3 = false;
		boolean dbCountFlag4 = false;
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.menupage().miscellaneousConfigInCockpit();
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_offers_ingestion", "check");
		pageObj.dashboardpage().updateCheckBox();

//		step- 1	

		Response redeemableListResp = pageObj.endpoints().redeemableListAPi(adminKey, "", page, per_page);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent = redeemableListResp.jsonPath().get("meta.total_records").toString();
		String totalEntryPerPage = redeemableListResp.jsonPath().get("meta.per_page").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent);
		if (env.equalsIgnoreCase("pp")) {
		Assert.assertEquals(totalEntryPerPage, "1000", "Per page entry is not 1000");
		pageObj.utils().logPass("Total per page entry from Dashboard Redeemable List Api is " + totalEntryPerPage);
	}

		String query = "Select count(*) from redeemables where business_id = " + b_id + ";";
		ResultSet resultSet = DBUtils.getResultSet(env, query);

		if (resultSet.next()) {
			dbRedeemableCount = resultSet.getInt(1);
			pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
		}
		DBManager.closeConnection();
//		String dbRedeemableCount = Integer.toString(count);
		DBManager.closeConnection();
		pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent)
				|| Integer.parseInt(totalRedeemablePresent) >= dbRedeemableCount) {
			dbCountFlag = true;
		}

		Assert.assertTrue(dbCountFlag, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass(
				"Verified that API should calculate the total number of pages based on the per_page value and return the data for the last valid page.");

//		Step - 2
		page = "5";
		per_page = "999999999";
		Response redeemableListResp1 = pageObj.endpoints().redeemableListAPi(adminKey, "", page, per_page);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp1.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent1 = redeemableListResp1.jsonPath().get("meta.total_records").toString();
		String totalEntryPerPage1 = redeemableListResp1.jsonPath().get("meta.per_page").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent1);
		Assert.assertEquals(totalEntryPerPage1, "1000", "Per page entry is not 1000");
		pageObj.utils().logPass("Total per page entry from Dashboard Redeemable List Api is " + totalEntryPerPage1);
		dbRedeemableCount = 0;
		String query1 = "Select count(*) from redeemables where business_id = " + b_id + ";";
		ResultSet resultSet1 = DBUtils.getResultSet(env, query1);

		if (resultSet1.next()) {
			dbRedeemableCount = resultSet1.getInt(1);
			pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
		}
//		String dbRedeemableCount = Integer.toString(count);
		pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent1)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent1)
				|| (dbRedeemableCount) == (Integer.parseInt(totalRedeemablePresent1) - 1)
				|| Integer.parseInt(totalRedeemablePresent1) >= dbRedeemableCount) {
			dbCountFlag1 = true;
		}

		Assert.assertTrue(dbCountFlag1, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass("Verified Dashboard Redeemable List Api behaviour when page is " + page + " and per_page is "
				+ per_page);

//		Step - 3
		page = "5";
		per_page = "101";
		Response redeemableListResp2 = pageObj.endpoints().redeemableListAPi(adminKey, "", page, per_page);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp2.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent2 = redeemableListResp2.jsonPath().get("meta.total_records").toString();
		String totalEntryPerPage2 = redeemableListResp2.jsonPath().get("meta.per_page").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent2);
		Assert.assertEquals(totalEntryPerPage2, "101", "Per page entry is not 101");
		pageObj.utils().logPass("Total per page entry from Dashboard Redeemable List Api is " + totalEntryPerPage2);
		dbRedeemableCount = 0;
		String query2 = "Select count(*) from redeemables where business_id = " + b_id + ";";
		ResultSet resultSet2 = DBUtils.getResultSet(env, query2);

		if (resultSet2.next()) {
			dbRedeemableCount = resultSet2.getInt(1);
			pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
		}
//		String dbRedeemableCount2 = Integer.toString(count);
		pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent2)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent2)
				|| (dbRedeemableCount - 2) == Integer.parseInt(totalRedeemablePresent2)
				|| (dbRedeemableCount) == (Integer.parseInt(totalRedeemablePresent2) - 1)
				|| Integer.parseInt(totalRedeemablePresent2) >= dbRedeemableCount) {
			dbCountFlag2 = true;
		}

		Assert.assertTrue(dbCountFlag2, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass("Verified Dashboard Redeemable List Api behaviour when page is " + page + " and per_page is "
				+ per_page);

//		Step - 4
		page = "4";
		per_page = "99.5";
		Response redeemableListResp3 = pageObj.endpoints().redeemableListAPi(adminKey, "", page, per_page);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp3.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent3 = redeemableListResp3.jsonPath().get("meta.total_records").toString();
		String totalEntryPerPage3 = redeemableListResp3.jsonPath().get("meta.per_page").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent3);
		Assert.assertEquals(totalEntryPerPage3, "99", "Per page entry is not 100");
		pageObj.utils().logPass("Total per page entry from Dashboard Redeemable List Api is " + totalEntryPerPage3);
		dbRedeemableCount = 0;
		String query3 = "Select count(*) from redeemables where business_id = " + b_id + ";";
		ResultSet resultSet3 = DBUtils.getResultSet(env, query3);

		if (resultSet3.next()) {
			dbRedeemableCount = resultSet3.getInt(1);
			pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
		}
//		String dbRedeemableCount3 = Integer.toString(count);
		pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent3)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent3)
				|| (dbRedeemableCount) == (Integer.parseInt(totalRedeemablePresent3) - 1)
				|| Integer.parseInt(totalRedeemablePresent3) >= dbRedeemableCount) {
			dbCountFlag3 = true;
		}

		Assert.assertTrue(dbCountFlag3, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass("Verified Dashboard Redeemable List Api behaviour when page is " + page + " and per_page is "
				+ per_page);

//		Step - 5
		page = "4";
		per_page = "99";
		Response redeemableListResp4 = pageObj.endpoints().redeemableListAPi(adminKey, "", page, per_page);
		Assert.assertEquals(redeemableListResp4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Dashboard Redeemable List Api");
//		boolean isListRedeemablesSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.dashboardListRedeemablesSchema, redeemableListResp4.asString());
//		Assert.assertTrue(isListRedeemablesSchemaValidated, "Get List Redeemables API Schema Validation failed");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		String totalRedeemablePresent4 = redeemableListResp4.jsonPath().get("meta.total_records").toString();
		String totalEntryPerPage4 = redeemableListResp4.jsonPath().get("meta.per_page").toString();
		pageObj.utils().logit(
				"Total Redeemable count obtained from Dashboard Redeemable List Api is " + totalRedeemablePresent4);
		Assert.assertEquals(totalEntryPerPage4, "99", "Per page entry is not 100");
		pageObj.utils().logPass("Total per page entry from Dashboard Redeemable List Api is " + totalEntryPerPage4);
		dbRedeemableCount = 0;
		String query4 = "Select count(*) from redeemables where business_id = " + b_id + ";";
		ResultSet resultSet4 = DBUtils.getResultSet(env, query4);

		if (resultSet4.next()) {
			dbRedeemableCount = resultSet4.getInt(1);
			pageObj.utils().logit("Total Redeemable count obtained from DB is " + dbRedeemableCount);
		}
//		String dbRedeemableCount4 = Integer.toString(count);
		pageObj.utils().logPass("Redeemable count DB count is " + dbRedeemableCount);

		if (dbRedeemableCount >= Integer.parseInt(totalRedeemablePresent4)
				|| (dbRedeemableCount - 1) == Integer.parseInt(totalRedeemablePresent4)
				|| (dbRedeemableCount) == (Integer.parseInt(totalRedeemablePresent4) - 1)
				|| Integer.parseInt(totalRedeemablePresent4) >= dbRedeemableCount) {
			dbCountFlag4 = true;
		}

		Assert.assertTrue(dbCountFlag4, "API and DB count for Redeemable is not matching");
		pageObj.utils().logPass("Verified Dashboard Redeemable List Api behaviour when page is " + page + " and per_page is "
				+ per_page);
	}

	// Shashank
	@Test(description = "OMM-T2781 / SQ-T5510 Validate that For new QC and flat discount, key values are getting displayed correctly in API response", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Shashank Sharma")
	public void T5510_ValidateReceiptRuleInGetRedeemableAPI() {

		String redeemableNameAsReceiptRule_NewQualifier = dataSet.get("redeemableNameAsReceiptRule_NewQualifier");
		String redeemableNameAsReceiptRule_FlatDiscount = dataSet.get("redeemableNameAsReceiptRule_FlatDiscount");

		Response response1 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableNameAsReceiptRule_NewQualifier, "1", "5");

		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
		boolean isListRedeemablesNewQualifierSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardListRedeemablesNewQualifierSchema, response1.asString());
		Assert.assertTrue(isListRedeemablesNewQualifierSchemaValidated,
				"Get List Redeemables API Schema Validation failed");
		String actualRedeemableName = response1.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName, redeemableNameAsReceiptRule_NewQualifier,
				redeemableNameAsReceiptRule_NewQualifier + " Redeemable name is not matched");

		String actualQualifierType = response1.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType, "new", "Qualifier type is not new");

		pageObj.utils().logPass("Verified that qualifier type is 'NEW' in api response ");

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableNameAsReceiptRule_FlatDiscount, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");
		boolean isListRedeemablesFlatDiscountSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardListRedeemablesFlatDiscountSchema, response2.asString());
		Assert.assertTrue(isListRedeemablesFlatDiscountSchemaValidated,
				"Get List Redeemables API Schema Validation failed");
		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableNameAsReceiptRule_FlatDiscount,
				redeemableNameAsReceiptRule_FlatDiscount + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "flat_discount", "Qualifier type is not new");

		pageObj.utils().logPass("Verified that qualifier type is 'flat_discount' in api response ");

		pageObj.utils().logit("== Get List Redeemables API with invalid API key ==");
		Response getListRedeemablesInvalidApiKeyResponse = pageObj.endpoints()
				.getLisOfRedeemableUsingApiWithPagePerItem("1", "query", redeemableNameAsReceiptRule_FlatDiscount, "1",
						"5");
		Assert.assertEquals(getListRedeemablesInvalidApiKeyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isListRedeemablesInvalidApiKeySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, getListRedeemablesInvalidApiKeyResponse.asString());
		Assert.assertTrue(isListRedeemablesInvalidApiKeySchemaValidated,
				"Get List Redeemables with invalid API key Schema Validation failed");
		String getListRedeemablesInvalidApiKeyMsg = getListRedeemablesInvalidApiKeyResponse.jsonPath()
				.getString("error");
		Assert.assertEquals(getListRedeemablesInvalidApiKeyMsg, "You need to sign in or sign up before continuing.",
				"Message does not match");
		pageObj.utils().logPass("Get List Redeemables API with invalid API key is unsuccessful");
	}

	@Test(description = "SQ-T5733 Auth Auto Select>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced"
			+ "SQ-T5588 Verify creating a QC using the \"Rate Rollback\" processing_function with both mandatory and optional fields"
			+ "SQ-T5728 (1.0) Validate that user is able to get QC data only if Admin user is having Dashboard API access permission", groups = {
					"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5733_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {

		// set the flag as true preference in db
		String b_id = dataSet.get("business_id");
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");

		// Checking and Marking "enable_banking_based_on_unified_balance" -> true in
		// business.preference table in DB
		pageObj.olfdbQueriesPage().updateDbFlags(expColValue, b_id, env, "true", "deprecate_processing_function");
		pageObj.utils().logit("Updated business_preferences table for deprecate_processing_function preference");

		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target_advanced";
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");

		// create redeemable having QC with processing
		// function-bundle_price_target_advanced having 5 valid line item filters and 5
		// item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers",
				"[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\"" + actualExternalIdLIS + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// create redeemable having QC with processing function-Target Price for Bundle
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS2 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is unable to create QC with processing function-bundle target price for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		String redeemableName21 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery2 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID3);
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		// commenting below code as the fix according to - OMM -1265
//		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
//				"", redeemable_ID2, "", "", "");
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
//				"Status code 201 did not matched for api2 send message to user");
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");
//		commenting below code as the fix according to - OMM -1265
//		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
//				redeemable_ID2);
//		logger.info("Reward id " + rewardId2 + " is generated successfully ");
//		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);
		pageObj.utils().logPass("Reward id " + rewardId3 + " is generated successfully ");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "123456");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

//		commenting below code as the fix according to - OMM -1265
		// AUTH Add Discount to Basket
//        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
//                dataSet.get("secret"), "reward", rewardId2, externalUID);
//        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
//                "Status code 200 did not match with add discount to basket ");
		// TestListeners.extentTest.get().pass("AUTH add discount to basket is
		// successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId3, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationkey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");
		// Verifying the response contains the max_applicable_quantity field inside the
		// discount_details object.

		String actual_maq1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("failures[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq1, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");

//      commenting below code as the fix according to - OMM -1265
//		String actual_maq2 = batchRedemptionProcessResponseUser1.jsonPath()
//				.get("failures[1].discount_details.max_applicable_quantity").toString();
//		Assert.assertEquals(actual_maq2, dataSet.get("exceptedMaq"),
//				"Max applicable quantity does not match in discount basket items");
		String actual_maq3 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("failures[1].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq3, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");

		pageObj.utils().logPass(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName2 + " LIS is deleted successfully");

		String deleteLISQuery3 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName3 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env, deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery3 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery3);
		pageObj.utils().logPass(redeemableExternalId3 + " external redeemable is deleted successfully");

	}

	@Test(description = "SQ-T5735 POS Batch Redemption>Verify that Max discounted units is getting displayed for reward in failures even if it does not qualify input receipt"
			+ "SQ-T5736 (1.0) POS discount lookup>Verify that Max discounted units field is not getting displayed for discount type-Discount Amount", groups = {
					"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5735_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\">=\",\"value\":\"10\"}]");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");

		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "rate_rollback");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID3);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// Add reward in basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationKey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		String redeemableNameFailedRewards = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].discount_details.name");
		Assert.assertEquals(redeemableNameFailedRewards, redeemableName22,
				"Redeemable name does not match in POS Process Batch Redemption api response");
		String expectedMaxDiscountUnits = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].discount_details.max_applicable_quantity");
		Assert.assertEquals(expectedMaxDiscountUnits, dataSet.get("exceptedMaq"),
				"Max discounted units value is not getting displayed for reward in failures object in API response");
		pageObj.utils().logPass(
				"Verified Max discounted units value is getting get displayed for reward in failures object in API response");

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey"), userID,
				"101", "30", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// verify that Max discounted units field should not get displayed for
		// "discount_type": "discount_amount"
		String maxDiscounted = discountLookupResponse0.jsonPath()
				.getString("unselected_discounts[1].max_applicable_quantity");
		Assert.assertNull(maxDiscounted,
				"Max discounted units field is getting displayed for  discount_type: discount_amount");
		pageObj.utils().logPass(
				"Verified that Max discounted units field should not get displayed for  discount_type: discount_amount");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName3 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName3 + " LIS is deleted successfully");

		// QC not created so no need to Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId3 + " external redeemable is deleted successfully");

	}

	@Test(description = "SQ-T5737 POS discount lookup>Verify that Max discounted units is getting displayed for reward in unselected discounts if it is not added in discount basket", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5737_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\">=\",\"value\":\"10\"}]");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");

		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "rate_rollback");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID3);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// Again send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey"), userID,
				"123456", "30", externalUID);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// verify Max discounted units value should get displayed for reward in
		// unselected discounts
		String discountType = discountLookupResponse0.jsonPath().getString("unselected_discounts[0].discount_type");
		Assert.assertEquals(discountType, "reward",
				"Discount type in unselected discounts object does not match in POS Discount Lookup api response");
		String redeemableNameUnselectedDiscounts = discountLookupResponse0.jsonPath()
				.getString("unselected_discounts[0].discount_details.name");
		Assert.assertEquals(redeemableNameUnselectedDiscounts, redeemableName22,
				"Redeemable name does not match in POS Discount Lookup api response");
		String expectedMaxDiscountUnits = discountLookupResponse0.jsonPath()
				.getString("unselected_discounts[0].discount_details.max_applicable_quantity");
		Assert.assertEquals(expectedMaxDiscountUnits, dataSet.get("exceptedMaq"),
				"Max discounted units value is not getting displayed for reward in unselected discounts object in POS Discount Lookup api response");
		pageObj.utils().logPass(
				"Verified Max discounted units value is getting get displayed for reward in unselected discounts object in POS Discount Lookup api response");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName3 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName3 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId3 + " external redeemable is deleted successfully");

	}

	// Shashank new
	@Test(description = "OMM-T3328/SQ-T5796  POS Batch Redemption>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Shashank Sharma")
	public void T3328_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {

		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target_advanced";
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");

		// create redeemable having QC with processing
		// function-bundle_price_target_advanced having 5 valid line item filters and 5
		// item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers",
				"[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\"" + actualExternalIdLIS + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// create redeemable having QC with processing function-Target Price for Bundle
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS2 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC2, "QC success message is not True");
		pageObj.utils().logPass(
				"User is unable to create QC with processing function-bundle target price for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		String redeemableName21 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery2 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);

		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID3);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		// commenting below code as the fix according to - OMM -1265
//		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
//				"", redeemable_ID2, "", "", "");
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
//				"Status code 201 did not matched for api2 send message to user");
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");
		// commenting below code as the fix according to - OMM -1265
//		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
//				redeemable_ID2);
//		logger.info("Reward id " + rewardId2 + " is generated successfully ");
//		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);
		pageObj.utils().logPass("Reward id " + rewardId3 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "123456");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// AUTH Add Discount to Basket
		// commenting below code as the fix according to - OMM -1265
		// AUTH Add Discount to Basket
//        Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
//                dataSet.get("secret"), "reward", rewardId2, externalUID);
//        Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
//                "Status code 200 did not match with add discount to basket ");
		// TestListeners.extentTest.get().pass("AUTH add discount to basket is
		// successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId3, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationKey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		// Verifying the response contains the max_applicable_quantity field inside the
		// discount_details object.

		String actual_maq1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("failures[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq1, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");
		// commenting below code as the fix according to - OMM -1265
//		String actual_maq2 = batchRedemptionProcessResponseUser1.jsonPath()
//				.get("failures[1].discount_details.max_applicable_quantity").toString();
//		Assert.assertEquals(actual_maq2, dataSet.get("exceptedMaq"),
//				"Max applicable quantity does not match in discount basket items");
		String actual_maq3 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("failures[1].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq3, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");

		pageObj.utils().logPass(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName2 + " LIS is deleted successfully");

		String deleteLISQuery3 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName3 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env, deleteQCFromQualification_criteria2);
		Assert.assertFalse(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName3 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

		String deleteRedeemableQuery3 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery3);
		pageObj.utils().logPass(redeemableExternalId3 + " external redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6391 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIF has max price with quantity 1", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6391_verifyDiscountCalculationForStackingAndReusabilityWithMaxPriceAndQuantity() throws Exception {

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
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

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
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"max_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// login to punchh
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS2 + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"max_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction, "", "", map);
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
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName2);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
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
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
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
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 10,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 8,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 102,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        },\n" + "        { \n"
						+ "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 4,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 201,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"3.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 6,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 202,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"4.0\"\n" + "        }\n" + "    ]");

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "28", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		String discountAmount2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-5.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmount2, "-3.0", "Discount amount for the second item is not matched");
		pageObj.utils().logPass("Discount amount for the first item is matched with -5.0 and second item is matched with -3.0");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId2, "202", "Item id for the second item is not matched");
		pageObj.utils().logPass("Item id for the first item is matched with 101 and second item is matched with 202");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "28", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-5.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmountLookupResponse2, "-3.0",
				"Discount amount for the second item is not matched");
		pageObj.utils().logPass(
				"Discount amount for the first item is matched with -5.0 and second item is matched with -3.0 in discount lookup response");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse2, "202", "Item id for the second item is not matched");
		pageObj.utils().logPass("Item id for the first item is matched with 101 and second item is matched with 202");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery, "redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 2,
				"Redemption code id is not created for the user in redemptions table");
		pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		pageObj.utils().logPass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
				.replace("${businessID}", businessID);
		String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
		Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
		pageObj.utils().logPass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env, deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6390 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIF has min price with quantity 1", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T6390_verifyDiscountCalculationForStackingAndReusabilityWithMinPriceAndQuantity() throws Exception {

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
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

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
				listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		pageObj.utils().logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// login to punchh
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.utils().logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS2 + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction, "", "", map);
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
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Exists", lisName2);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
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
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
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
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2, "id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 10,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 8,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 102,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        },\n" + "        { \n"
						+ "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 4,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 201,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"3.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 6,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 202,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"4.0\"\n" + "        }\n" + "    ]");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying menu items", "check");
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "28", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		String discountAmount2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-4.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmount2, "-2.0", "Discount amount for the second item is not matched");
		pageObj.utils().logPass("Discount amount for the first item is matched with -4.0 and second item is matched with -2.0");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId2, "201", "Item id for the second item is not matched");
		pageObj.utils().logPass("Item id for the first item is matched with 102 and second item is matched with 201");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "28", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-4.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmountLookupResponse2, "-2.0",
				"Discount amount for the second item is not matched");
		pageObj.utils().logPass(
				"Discount amount for the first item is matched with -5.0 and second item is matched with -3.0 in discount lookup response");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse2, "201", "Item id for the second item is not matched");
		pageObj.utils().logPass("Item id for the first item is matched with 102 and second item is matched with 201");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery, "redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 2,
				"Redemption code id is not created for the user in redemptions table");
		pageObj.utils().logPass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery, "status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		pageObj.utils().logPass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
				.replace("${businessID}", businessID);
		String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2, "status");
		Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
		pageObj.utils().logPass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		pageObj.utils().logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		// Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not
		// deleted successfully");
		pageObj.utils().logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		pageObj.utils().logPass(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		pageObj.utils().logPass(redeemableExternalId + " external redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		pageObj.utils().logPass(redeemableExternalId2 + " external redeemable is deleted successfully");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}