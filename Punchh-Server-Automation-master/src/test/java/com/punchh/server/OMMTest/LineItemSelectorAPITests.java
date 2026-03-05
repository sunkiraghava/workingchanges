package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 */

@Listeners(TestListeners.class)
public class LineItemSelectorAPITests {
	static Logger logger = LogManager.getLogger(LineItemSelectorAPITests.class);
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
	public String lisDeleteBaseQueryNew = "Delete  from line_item_selectors where external_id = '${externalID}' and business_id='${buisnessID}'";

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
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5037 Verify creating a Line Item Selector using the new create LIS API with Base itemsand Modifiers."
			+ "SQ-T5035 Verify creating a Line Item Selector using the new create LIS API with only Base items."
			+ "SQ-T5036 Verify creating a Line Item Selector using the new create LIS APIwith only Modifiers."
			+ "SQ-T5040 Validate adding all the attribute with all the operators in create new LIS api." +
			"SQ-T5634 Verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled. - single LIS", groups = { "regression", "dailyrun" })

	public void T5037_VerifyCreatingLISUsingAPI() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		String businessID = dataSet.get("business_id");

		String lisDeleteBaseQuery = "Delete  from line_item_selectors where external_id = '${externalID}' and business_id='1024'";
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalId = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualExternalId.length(), 36, "STEP-1  External Id is not generated properly");
		utils.logPass(
				"Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");
		listBaseItemClauses.clear();
		listModifiresItemClauses.clear();

		String deleteLISQuery1 = lisDeleteBaseQueryNew.replace("${externalID}", actualExternalId)
				.replace("${buisnessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");

		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Step3 - If pass invalid external_id key as "extrenal_id" or "uuid", LIS will
		// be created and system will automatically generate the external_id and
		// assigned to that LIS.
		// also cover SQ-T5035 Verify creating a Line Item Selector using the new create
		// LIS API with only Base items.

		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses2 = dataSet.get("modifierItemClauses2");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses2);

		String invalid_external_id = "4f25b80a-12d6-4e46-b922";
		String lisName3 = "AutomationBaseOnlyLIS_" + CreateDateTime.getTimeDateString();
		String lisPayloadWithInvalidExternalID_Key = "{\"data\":[{\"name\":\"" + lisName3
				+ "\",\"external_invalidkey\":\"4f25b80a-12d6-4e46-b922\",\"filter_item_set\":\"base_only\",\"exclude_non_payable\":true,\"base_items\":{\"clauses\":[{\"attribute\":\"item_name\",\"operator\":\"==\",\"value\":\"lis\"},{\"attribute\":\"line_item_type\",\"operator\":\"like\",\"value\":\"M\"}]}}]}";

		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, invalid_external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price",
				lisPayloadWithInvalidExternalID_Key);
		logger.info("createLISResponse3 -- " + createLISResponse3.asPrettyString());
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-3 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-3  Success message is True");

		String actualExternalId3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualExternalId3.length(), 36, "STEP-3  External Id is not generated properly");
		utils.logPass(
				"Verified STEP-3 If pass invalid external_id key as \"extrenal_id\" or \"uuid\", LIS will be created and system will automatically generate the external_id and assigned to that LIS");

		Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.createLisAPISchema, createLISResponse3.asString());

		String deleteLISQuery3 = lisDeleteBaseQueryNew.replace("${externalID}", actualExternalId3)
				.replace("${buisnessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		utils.logPass(lisName3 + " LIS is deleted successfully");

		// SQ-T5036 (1.0) Verify creating a Line Item Selector using the new create LIS
		// API with only Modifiers.
		String lisName4 = "AutomationModifiersOnlyLIS_" + CreateDateTime.getTimeDateString();
		Response createLISResponse4 = pageObj.endpoints().createLISUsingApi(adminKey, lisName4, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Modiiers only Create LIS response status code is not 200");
		boolean actualSuccessMessage4 = createLISResponse4.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage4, "Modiiers only Create LIS success message is not matched");
		String actualExternalId4 = createLISResponse4.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualExternalId4.length(), 36,
				"Modiiers only Create LIS External Id is not generated properly");
		utils.logPass("Verify creating a Line Item Selector using the new create LIS API with only Modifiers.");

		String deleteLISQuery4 = lisDeleteBaseQueryNew.replace("${externalID}", actualExternalId4)
				.replace("${buisnessID}", businessID);
		boolean deleteLisStatus4 = DBUtils.executeQuery(env, deleteLISQuery4);
		Assert.assertTrue(deleteLisStatus4, lisName4 + " LIS is not deleted successfully");

		utils.logPass(lisName4 + " LIS is deleted successfully");

	}

	// shashank
	@Test(description = "SQ-T5032 Verify that the GET API successfully fetches the list of all LISs(count should match) from the DB"
			+ "SQ-T5033 Validate the functionality of the LIS API with all supported parameters and verify api response also"
			+ " SQ-T5034 Validate all the negative scenarios by sending incorrect data in the get LIS API request." +
			"SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled. - lis count", groups = { "regression", "dailyrun" })

	public void T5032_verifyLisCountWithDB() throws Exception {
		String adminKey = dataSet.get("apiKey");
		String businessID = dataSet.get("business_id");
		String lisFilterName = "lisName";
		Response lisCountResponse = pageObj.endpoints().getLisOfLineItemSelectorsUsingApi(adminKey);
		int lisCount = lisCountResponse.jsonPath().getInt("meta.total_records");
		String getLisCountQuery = "select count(id) as id from line_item_selectors where business_id = " + businessID
				+ ";";
		String count = DBManager.executeQueryAndGetColumnValue(env,null,getLisCountQuery, "id");
		int count1 = Integer.parseInt(count);

		Assert.assertEquals(count1, lisCount, "LIS count is not matching with DB");
		utils.logPass("Verified that LIS count is matched with DB");

		// SQ-T5033 Validate the functionality of the LIS API with all supported
		// parameters and verify api response also
		Response lisCountResponse2 = pageObj.endpoints().getLisOfLineItemSelectorsUsingApiWithPagePerItem(adminKey, "",
				lisFilterName, "1", "5");
		boolean isGetLisSchemaValidated = Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.apiGetLisSchema,
				lisCountResponse2.asString());
		Assert.assertTrue(isGetLisSchemaValidated, "Get LIS Schema Validation failed");
		int lisCount2 = lisCountResponse2.jsonPath().getInt("meta.per_page");
		Assert.assertEquals(lisCount2, 5, "Actual per_page count is not matched with lis per page");
		utils.logPass("Verified that LIS per_page is matched with per page count");

		List<Object> lisItemPerPage_List = lisCountResponse2.jsonPath().getList("data");
		Assert.assertEquals(lisItemPerPage_List.size(), 5, "LIS count is not matched with lis per page");
		utils.logPass("Verified that LIS list size is matched with per page count");

		List<Object> lisItemNameFilte_List = lisCountResponse2.jsonPath().getList("data.name");

		for (Object lisItemName : lisItemNameFilte_List) {
			Assert.assertTrue(lisItemName.toString().contains(lisFilterName),
					"LIS name is not matched with filter name");
			utils.logPass(
					lisItemName.toString() + " Verified that LIS contained the applied filter name - " + lisFilterName);

		}

		// Step 6
		Response lisCountResponse3 = pageObj.endpoints().getLisOfLineItemSelectorsUsingApiWithPagePerItem(adminKey,
				"querty", lisFilterName, "1", "5");
		Assert.assertEquals(lisCountResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "LIS count response status code is not 200");

		// SQ-T5034 Validate all the negative scenarios by sending incorrect data in the
		// get LIS API request.

		// SQ-T5034 - Step1
		Response lisCountResponse4 = pageObj.endpoints().getLisOfLineItemSelectorsUsingApiWithPagePerItem(adminKey,
				"querty", lisFilterName, "abcd", "efgh");
		Assert.assertEquals(lisCountResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "LIS count response status code is not 400");
		boolean isGetLisInvalidParamsSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, lisCountResponse4.asString());
		Assert.assertTrue(isGetLisInvalidParamsSchemaValidated,
				"Get LIS with Invalid Parameters Schema Validation failed");
		String errorMessage1 = lisCountResponse4.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(errorMessage1, "Invalid page parameter", "Error message is not matched");

		// SQ-T5034 - Step2
		Response lisCountResponse5 = pageObj.endpoints().getLisOfLineItemSelectorsUsingApiWithPagePerItem(adminKey,
				"querty", lisFilterName, "abcd", "1");
		Assert.assertEquals(lisCountResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "LIS count response status code is not 400");
		boolean isGetLisInvalidPerPageSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, lisCountResponse4.asString());
		Assert.assertTrue(isGetLisInvalidPerPageSchemaValidated,
				"Get LIS with Invalid Per Page parameter Schema Validation failed");
		String errorMessage2 = lisCountResponse5.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(errorMessage2, "Invalid page parameter", "Error message is not matched");

	}

	@Test(description = "SQ-T5038 Validate the maximum Line Item Selectors that can be created in bulk per API call."
			+ "SQ-T5423 Validate the maximum Line Item Selectors that can be updated in bulk per API call." +
			"SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled-bulk LIS creation", groups = { "regression", "dailyrun" })
	public void T5038_verifyBulkLineItemSelectionCreation() throws Exception {

		String adminKey = dataSet.get("apiKey");
		String businessID = dataSet.get("business_id");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> lis1MapDetails = new LinkedHashMap<String, String>();
		Map<String, String> lis2MapDetails = new LinkedHashMap<String, String>();

		String lisName1 = "AutomationAPILIS1_" + CreateDateTime.getTimeDateString();
		lis1MapDetails.put("lisName", lisName1);
		lis1MapDetails.put("externalId", "");
		lis1MapDetails.put("filterItemSet", "base_and_modifiers");
		lis1MapDetails.put("excludeNonPayable", "true");
		lis1MapDetails.put("attribute", "item_id");
		lis1MapDetails.put("operator", "==");
		lis1MapDetails.put("value", "111");
		lis1MapDetails.put("modifierAttributes", "item_id");
		lis1MapDetails.put("modifierOperator", "!=");
		lis1MapDetails.put("modifierValue", "123456");
		lis1MapDetails.put("maxDiscountUnits", "2");
		lis1MapDetails.put("processingMethod", "max_price");
		parentMap.put(lisName1, lis1MapDetails);

		Thread.sleep(2000);

		String lisName2 = "AutomationAPILIS2_" + CreateDateTime.getTimeDateString();
		lis2MapDetails.put("lisName", lisName1);
		lis2MapDetails.put("externalId", "");
		lis2MapDetails.put("filterItemSet", "base_and_modifiers");
		lis2MapDetails.put("excludeNonPayable", "true");
		lis2MapDetails.put("attribute", "item_id");
		lis2MapDetails.put("operator", "==");
		lis2MapDetails.put("value", "111");
		lis1MapDetails.put("modifierAttributes", null);
		lis2MapDetails.put("modifierValue", null);
		lis2MapDetails.put("modifierOperator", null);
		lis2MapDetails.put("maxDiscountUnits", null);
		lis2MapDetails.put("processingMethod", null);
		parentMap.put(lisName2, lis2MapDetails);

		Response bulkLisCreationResponse = pageObj.endpoints().bulkCreationLISUsingApi(adminKey, parentMap);
		Assert.assertEquals(bulkLisCreationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Bulk LIS creation response status code is not 200");
		boolean isCreateBulkLisSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, bulkLisCreationResponse.asString());
		Assert.assertTrue(isCreateBulkLisSchemaValidated, "Create Bulk LIS Schema Validation failed");
		String lisExternalId1 = bulkLisCreationResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String lisExternalId2 = bulkLisCreationResponse.jsonPath().getString("results[1].external_id").replace("[", "")
				.replace("]", "");

		Assert.assertTrue(!lisExternalId1.equals(""));
		utils.logPass("Verified that Bulk LIS is created successfully LIS Name - " + lisName1);

		Assert.assertTrue(!lisExternalId2.equals(""));
		utils.logPass("Verified that Bulk LIS is created successfully LIS Name - " + lisName2);
		// update the bulk LIS
		lis1MapDetails.put("attribute", "item_name");
		lis1MapDetails.put("value", "999999");
		lis1MapDetails.put("externalId", lisExternalId1);
		lis1MapDetails.put("modifierValue", "777777");

		lis2MapDetails.put("attribute", "item_name");
		lis2MapDetails.put("value", "888888");
		lis2MapDetails.put("externalId", lisExternalId2);
		lis1MapDetails.put("modifierValue", "66666");

		Response bulkLisUpdationResponse = pageObj.endpoints().bulkUpdationLISUsingApi(adminKey, parentMap);
		boolean isUpdateBulkLisSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, bulkLisUpdationResponse.asString());
		Assert.assertTrue(isUpdateBulkLisSchemaValidated, "Update Bulk LIS Schema Validation failed");
		String lisExternalId3 = bulkLisUpdationResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String lisExternalId4 = bulkLisUpdationResponse.jsonPath().getString("results[1].external_id").replace("[", "")
				.replace("]", "");

		Assert.assertTrue(!lisExternalId3.equals(""));
		utils.logPass("Verified that Bulk LIS is Updated successfully LIS Name - " + lisName1);

		Assert.assertTrue(!lisExternalId4.equals(""));
		utils.logPass("Verified that Bulk LIS is Updated successfully LIS Name - " + lisName1);

		String deleteLISQuery1 = lisDeleteBaseQueryNew.replace("${externalID}", lisExternalId1).replace("${buisnessID}",
				businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQueryNew.replace("${externalID}", lisExternalId2).replace("${buisnessID}",
				businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

	}

	@Test(description = "SQ-T5044 Verify the authorization mechanism using Bearer Token for create LIS API"
			+ "SQ-T5045 Verify the authorization mechanism using Bearer Token for get LIS list API", groups = { "regression", "dailyrun" })
	public void T5044_VerifyLISAuthorizationBearerToken() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		String businessID = dataSet.get("business_id");

		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - Admin should be created in business having dashboard api access
		// permission >> Then hit the create LIS api with valid token URL-
		// api2/dashboard/offers/lis
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean isCreateLisSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, createLISResponse.asString());
		Assert.assertTrue(isCreateLisSchemaValidated, "Create LIS Schema Validation failed");
		String lisExternalId1 = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		Assert.assertNotNull(lisExternalId1, "STEP-1  External Id is not generated properly");

		String deleteLISQuery1 = lisDeleteBaseQueryNew.replace("${externalID}", lisExternalId1).replace("${buisnessID}",
				businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Step-2 Admin should be created in business having dashboard api access
		// permission >> Then hit the create LIS api with invalid token URL-
		// api2/dashboard/offers/lis

		Response createLISResponse_invalidToken = pageObj.endpoints().createLISUsingApi(adminKey + "fsdhkh435",
				lisName1, external_id, "base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2,
				"max_price", "");

		Assert.assertEquals(createLISResponse_invalidToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"STEP-2 Create LIS response status code is not 401");
		boolean isCreateLisInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createLISResponse_invalidToken.asString());
		Assert.assertTrue(isCreateLisInvalidTokenSchemaValidated,
				"Create LIS with Invalid Token Schema Validation failed");
		String errorMessage = createLISResponse_invalidToken.jsonPath().getString("error").replace("[", "").replace("]",
				"");
		Assert.assertEquals(errorMessage, "You need to sign in or sign up before continuing.",
				"STEP-2  External Id is not generated properly");

		// Step-3 Admin should be created in business having dashboard api access
		// permission >> Then hit the create LIS api without passing token URL-
		// api2/dashboard/offers/lis

		Response createLISResponse_withoutToken = pageObj.endpoints().createLISUsingApi("", lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");

		Assert.assertEquals(createLISResponse_withoutToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"STEP-2 Create LIS response status code is not 401");
		boolean isCreateLisMissingTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createLISResponse_withoutToken.asString());
		Assert.assertTrue(isCreateLisMissingTokenSchemaValidated,
				"Create LIS with Missing Token Schema Validation failed");
		String errorMessage2 = createLISResponse_withoutToken.jsonPath().getString("error").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(errorMessage2, "You need to sign in or sign up before continuing.",
				"STEP-3  External Id is not generated properly");

	}

	@Test(description = "SQ-T5422 Verify updating a Line Item Selector using the update LIS API with Base items and Modifiers." +
			"SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled.- update lis", groups = { "regression", "dailyrun" })
	public void T5422_VerifyLineItemSelectorUpdateAPI() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String businessID = dataSet.get("business_id");

		String lisDeleteBaseQuery = "Delete  from line_item_selectors where external_id = '${externalID}' and business_id='1024'";
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean isCreateLisBlankExternalIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, createLISResponse.asString());
		Assert.assertTrue(isCreateLisBlankExternalIdSchemaValidated,
				"Create LIS with blank External ID Schema Validation failed");
		String actualExternalId = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		listBaseItemClauses.clear();
		listModifiresItemClauses.clear();

		String baseItemClauses1_Update = dataSet.get("updatedBaseItemClause");
		String modifiersItemClauses1_Update = dataSet.get("updatedModifierItemClause");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1_Update);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1_Update);

		Response updateLISResponse = pageObj.endpoints().updateLISUsingAPI(adminKey, lisName1, actualExternalId,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price");
		Assert.assertEquals(updateLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isUpdateLisSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, updateLISResponse.asString());
		Assert.assertTrue(isUpdateLisSchemaValidated, "Update LIS Schema Validation failed");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().searchAndSelectLineItemSelector(lisName1);

		pageObj.lineItemSelectorPage().verifedUpdateValuesInLis("Base Items", "Item Name", "UpdateItemName_BaseItem");
		pageObj.lineItemSelectorPage().verifedUpdateValuesInLis("Modifiers", "Item Name",
				"UpdateItemName_ModifierItem");

		String deleteLISQuery1 = lisDeleteBaseQueryNew.replace("${externalID}", actualExternalId)
				.replace("${buisnessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

	}

	@Test(description = "SQ-T5421 Verify updating a Line Item Selector using the update LIS API with only Modifiers."
			+ "SQ-T5420	Verify updating a Line Item Selector using the update LIS API with only Base items.", groups = { "regression", "dailyrun" })
	public void T5421_VerifyLineItemSelectorForModiferOnlyUpdateAPI() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String businessID = dataSet.get("business_id");

		String lisDeleteBaseQuery = "Delete  from line_item_selectors where external_id = '${externalID}' and business_id='1024'";
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		String actualExternalId = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		listBaseItemClauses.clear();
		listModifiresItemClauses.clear();

		String baseItemClauses1_Update = dataSet.get("updatedBaseItemClause");
		String modifiersItemClauses1_Update = dataSet.get("updatedModifierItemClause");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1_Update);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1_Update);

		Response updateLISResponse = pageObj.endpoints().updateLISUsingAPI(adminKey, lisName1, actualExternalId,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price");
		boolean isUpdateLisBaseOnlySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, updateLISResponse.asString());
		Assert.assertTrue(isUpdateLisBaseOnlySchemaValidated, "Update LIS with Base Only Schema Validation failed");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().searchAndSelectLineItemSelector(lisName1);

		pageObj.lineItemSelectorPage().verifedUpdateValuesInLis("Base Items", "Item Name", "UpdateItemName_BaseItem");
		pageObj.lineItemSelectorPage().verifiedFilterItemSelectedValue("Only Base Items");

		Response updateLISResponse2 = pageObj.endpoints().updateLISUsingAPI(adminKey, lisName1, actualExternalId,
				"modifiers_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price");
		boolean isUpdateLisModifiersOnlySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, updateLISResponse2.asString());
		Assert.assertTrue(isUpdateLisModifiersOnlySchemaValidated,
				"Update LIS with Modifiers Only Schema Validation failed");
		utils.refreshPage();
		pageObj.lineItemSelectorPage().verifedUpdateValuesInLis("Base Items", "Item Name", "UpdateItemName_BaseItem");
		pageObj.lineItemSelectorPage().verifiedFilterItemSelectedValue("Only Modifiers");
		pageObj.lineItemSelectorPage().verifedUpdateValuesInLis("Modifiers", "Item Name",
				"UpdateItemName_ModifierItem");
		String deleteLISQuery1 = lisDeleteBaseQueryNew.replace("${externalID}", actualExternalId)
				.replace("${buisnessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

	}

	// Its a negative test case so we are not deleting the created LIS
	@Test(description = "SQ-T5626 Verify when multiple LIS are added in the request, errors appears correctly", groups = { "regression", "dailyrun" })
	public void T5626_VerifyCreatingLISWithMultipleLISAddeddInPayload() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_1" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_2" + CreateDateTime.getTimeDateString();

		String lisMultiPayload = "{\"data\":[{\"name\":\"" + lisName1
				+ "\",\"external_id\":\"\",\"filter_item_set\":\"base_only\",\"exclude_non_payable\":true,\"base_items\":{\"clauses\":[{\"attribute\":\"item_serial_number\",\"operator\":\"in\",\"value\":\"A\"},{\"attribute\":\"item_serial_number\",\"operator\":\"in\",\"value\":\"B\"}]},\"modifiers\":{\"clauses\":[{\"attribute\":\"item_name\",\"operator\":\"in\",\"value\":\"C\"},{\"attribute\":\"item_name\",\"operator\":\"in\",\"value\":\"D\"}]}},{\"name\":\""
				+ lisName2
				+ "\",\"external_id\":\"\",\"filter_item_set\":\"base_and_modifiers\",\"exclude_non_payable\":true,\"base_items\":{\"clauses\":[{\"attribute\":\"item_name\",\"operator\":\"in\",\"value\":\"E\"},{\"attribute\":\"item_name\",\"operator\":\"iJ\",\"value\":\"F\"}]},\"modifiers\":{\"clauses\":[{\"attribute\":\"item_name\",\"operator\":\"in\",\"value\":\"G\"},{\"attribute\":\"item_name\",\"operator\":\"in\",\"value\":\"H\"}]}}]}";

		String lisDeleteBaseQuery = "Delete  from line_item_selectors where external_id = '${externalID}' and business_id='1024'";
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", lisMultiPayload);
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean isCreateLisWithWarningsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateLisWarningsSchema, createLISResponse.asString());
		Assert.assertTrue(isCreateLisWithWarningsSchemaValidated, "Create LIS with Warnings Schema Validation failed");
		String warnningMessageForLis1 = createLISResponse.jsonPath()
				.getString("results[0].warnings.base_items.clauses[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(warnningMessageForLis1, "Required parameter is duplicate: attribute",
				"Required parameter is duplicate: attribute  warnning messsage is not matched");

		String warnningMessageForLis2 = createLISResponse.jsonPath()
				.getString("results[1].warnings.base_items.clauses[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(warnningMessageForLis2, "Required parameters missing or invalid: operator",
				"Required parameters missing or invalid: operator  warnning messsage is not matched");

		String warnningMessageForLis_modifier2 = createLISResponse.jsonPath()
				.getString("results[1].warnings.modifiers.clauses[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(warnningMessageForLis_modifier2, "Required parameter is duplicate: attribute",
				"Required parameter is duplicate: attribute  warnning messsage is not matched");

		utils.logPass("Verified all error messages for multiple LIS added in payload");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
