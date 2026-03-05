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
import com.punchh.server.LisCreationUtilityClasses.CreateLISandQC;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
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
 * @Author : Shashank Sharma 
 */

@Listeners(TestListeners.class)
public class OfferIngestion_RedeemableInLineQC_APITests2 {
	static Logger logger = LogManager.getLogger(OfferIngestion_RedeemableInLineQC_APITests2.class);
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
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '$businessID'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '$businessID'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";

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

	@Test(description = "SQ-T5564 Validate the warning structure of Item Qualifiers in update redeemable api response", groups = { "regression", "dailyrun" })
	public void T5564_ValidateWarningStructureOfItemQualifierUpdateRedeemableAPI() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("redeeming_criterion_id", "1");
		map.put("locationID", null);
		map.put("redeemableName", redeemableName);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("qualifier_type", "new");
		map.put("qc_processing_function", "sum_amounts");
		map.put("amount_cap", "10.0");
		map.put("item_qualifiers",
				"{\"expression_type\":\"\",\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"net_value\":null}");

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responseInvalidQCID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseInvalidQCID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableQualifierTypeNullNetValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateRedeemableItemQualifierNullNetValueSchema,
				responseInvalidQCID.asString());
		Assert.assertTrue(isCreateRedeemableQualifierTypeNullNetValueSchemaValidated,
				"Create Redeemable with Item Qualifier (Qualifier type is provided but invalid redeeming_criterion) Schema Validation failed");

		// Step1- Provide duplicate receipt qualifiers
		String externalID = responseInvalidQCID.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers",
				"{\"expression_type\":\"\",\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"net_value\":5}");
		map.put("external_id_redeemable", externalID);
		Response updateResponse = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeDuplicateSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierSchema, updateResponse.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeDuplicateSchemaValidated,
				"Update Redeemable with Item Qualifier (duplicate receipt qualifiers) Schema Validation failed");
		String actWarningMessage = updateResponse.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage, "Required parameters missing or invalid: expression_type",
				"Required parameters missing or invalid: expression_type Warning message is not matched");

		utils.logPass(
				"Verified warnning message Required parameters missing or invalid: expression_type for receipt_qualifiers");

		// Step3- Missing attribute key value
		map.put("item_qualifiers",
				"{\"\":\"line_item_does_not_exist\",\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"net_value\":5}");
		Response updateResponse2 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeMissingExpressionTypeSchemaValidated = Utilities
				.validateJsonAgainstSchema(
						ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierMissingExpressionTypeSchema,
						updateResponse2.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeMissingExpressionTypeSchemaValidated,
				"Update Redeemable with Item Qualifier (Missing attribute key value) Schema Validation failed");
		String actWarningMessage2 = updateResponse2.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage2, "Required parameters missing or invalid: expression_type",
				"Required parameters missing or invalid: expression_type Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: expression_type for receipt_qualifiers");

		// Step4- Missing operator key
		map.put("item_qualifiers",
				"{\"expression_type\":\"line_item_does_not_exist\",\"\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"net_value\":5}");

		Response updateResponse3 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeMissingLineItemSelectorIdSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierMissingLisIdSchema,
						updateResponse3.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeMissingLineItemSelectorIdSchemaValidated,
				"Update Redeemable with Item Qualifier (Missing operator key) Schema Validation failed");
		String actWarningMessage3 = updateResponse3.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage3, "Required parameters missing or invalid: line_item_selector_id",
				"Required parameters missing or invalid: line_item_selector_id Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: line_item_selector_id for receipt_qualifiers");

		// Strep 5- Missing operator key's value

		map.put("item_qualifiers",
				"{\"expression_type\":\"line_item_does_not_exist\",\"line_item_selector_id\":\"\",\"net_value\":5}");

		Response updateResponse4 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeMissingLisIdValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierSchema, updateResponse4.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeMissingLisIdValueSchemaValidated,
				"Update Redeemable with Item Qualifier (Missing operator key's value) Schema Validation failed");
		String actWarningMessage4 = updateResponse4.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage4, "Required parameters missing or invalid: line_item_selector_id",
				"Required parameters missing or invalid: line_item_selector_id Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: line_item_selector_id for receipt_qualifiers");

		// Step6-Missing net_value key-- Need to check with @rahul garg

		map.put("item_qualifiers",
				"{\"expression_type\":\"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"\":5}");
		Response updateResponse5 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeMissingNetValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierMissingNetValueSchema,
				updateResponse5.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeMissingNetValueSchemaValidated,
				"Update Redeemable with Item Qualifier (Missing net_value key) Schema Validation failed");
		String actWarningMessage5 = updateResponse5.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage5, "Required parameters missing or invalid: net_value",
				"Required parameters missing or invalid: net_value Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: net_value for receipt_qualifiers");

		// Step-7 Missing net_value key value - -- Need to check with @rahul garg
		map.put("item_qualifiers",
				"{\"expression_type\":\"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"net_value\":\"\"}");

		Response updateResponse6 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableQualifierTypeMissingNetValueValueSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateRedeemableItemQualifierStringNetValueSchema,
						updateResponse6.asString());
		Assert.assertTrue(isUpdateRedeemableQualifierTypeMissingNetValueValueSchemaValidated,
				"Update Redeemable with Item Qualifier (Missing net_value key value) Schema Validation failed");
		String actWarningMessage6 = updateResponse6.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.item_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage6, "Required parameters missing or invalid: net_value",
				"Required parameters missing or invalid: net_value Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: net_value for receipt_qualifiers");

		// deleting QC and redeemable from DB
		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		deleteQCandRedeemableFromDB(response2);

		utils.logit(redeemableName + " redeemble and associated QC has been deleted from DB");

	}

	@Test(description = "SQ-T5563 Validate the warning structure of line_item_filters in update redeemable api response", groups = {
			"regression", "unstable", "dailyrun" })
	public void T5563_ValidateWarningStructureOfLineItemFilterUpdateRedeemableAPI() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("redeeming_criterion_id", "1");
		map.put("locationID", null);
		map.put("redeemableName", redeemableName);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("end_time", endTime);
		map.put("qualifier_type", "new");
		map.put("qc_processing_function", "sum_amounts");
		map.put("amount_cap", "10.0");
		map.put("item_qualifiers", dataSet.get("lisExternalID"));

		map.put("lineitemSelector", dataSet.get("lineItemSelector"));

		Response responseInvalidQCID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseInvalidQCID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Step1- line_item_selector_id value missing
		String externalID = responseInvalidQCID.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		// map.put("lineitemSelector",
		// "{\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"processing_method\":\"max_price\",\"quantity\":5}");

		map.put("lineitemSelector",
				"{\"line_item_selector_id\":\"\",\"processing_method\":\"max_price\",\"quantity\":5}");

		map.put("external_id_redeemable", externalID);
		Response updateResponse = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersMissingEmptyLisIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableLineItemFiltersSchema, updateResponse.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersMissingEmptyLisIdSchemaValidated,
				"Update Redeemable with Line Item Filters (line_item_selector_id value missing) Schema Validation failed");
		String actWarningMessage = updateResponse.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage, "Required parameters missing or invalid: line_item_selector_id",
				"Required parameters missing or invalid: line_item_selector_id Warning message is not matched");

		utils.logPass(
				"Verified warnning message Required parameters missing or invalid: line_item_selector_id for line_item_filters");

		// Step2- line_item_selector_id key missing
		map.put("lineitemSelector",
				"{\"\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"processing_method\":\"max_price\",\"quantity\":5}");

		Response updateResponse2 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersMissingLisIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableLineItemFiltersMissingLisIdSchema, updateResponse2.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersMissingLisIdSchemaValidated,
				"Update Redeemable with Line Item Filters (line_item_selector_id key missing) Schema Validation failed");
		String actWarningMessage2 = updateResponse2.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage2, "Required parameters missing or invalid: line_item_selector_id",
				"Required parameters missing or invalid: line_item_selector_id Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: line_item_selector_id for line_item_filters");

		// Step3- processing_method key missing
		map.put("lineitemSelector",
				"{\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"\":\"max_price\",\"quantity\":5}");

		Response updateResponse3 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersMissingProcessingMethodSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, updateResponse3.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersMissingProcessingMethodSchemaValidated,
				"Update Redeemable with Line Item Filters (processing_method key missing) Schema Validation failed");
		List actWarningMessage3 = updateResponse3.jsonPath()
				.getList("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message");
		Assert.assertEquals(actWarningMessage3, null,
				"Required parameters missing or invalid: line_item_selector_id Warning message is not matched");

		utils.logPass(
				"Verified that No warnning message is coming if processing_method key missing for line_item_filters");

		// Strep 4- Invalid processing_method value

		map.put("lineitemSelector",
				"{\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"processing_method\":\"min\",\"quantity\":5}");

		Response updateResponse4 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersInvalidProcessingMethodSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateRedeemableLineItemFiltersSchema,
						updateResponse4.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersInvalidProcessingMethodSchemaValidated,
				"Update Redeemable with Line Item Filters (Invalid processing_method value) Schema Validation failed");
		String actWarningMessage4 = updateResponse4.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage4, "Required parameters missing or invalid: processing_method",
				"Required parameters missing or invalid: processing_method Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: processing_method for line_item_filters");

		// Step5 quantity key missing

		map.put("lineitemSelector",
				"{\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"processing_method\":\"max_price\",\"\":5}");

		Response updateResponse5 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersMissingQuantitySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableLineItemFiltersMissingQuantitySchema,
				updateResponse5.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersMissingQuantitySchemaValidated,
				"Update Redeemable with Line Item Filters (quantity key missing) Schema Validation failed");
		String actWarningMessage5 = updateResponse5.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage5, "Required parameters missing or invalid: quantity",
				"Required parameters missing or invalid: quantity Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: line_item_selector_id, quantity for line_item_filters");

		// Step-6 quantity value set to NULL
		map.put("lineitemSelector",
				"{\"line_item_selector_id\":\"20d5af22-f834-4cfb-92ea-64341fa2fa47\",\"processing_method\":\"max_price\",\"quantity\":null}");

		Response updateResponse6 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableLineItemFiltersQuantityNullSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableLineItemFiltersNullQuantitySchema, updateResponse6.asString());
		Assert.assertTrue(isUpdateRedeemableLineItemFiltersQuantityNullSchemaValidated,
				"Update Redeemable with Line Item Filters (quantity value set to NULL) Schema Validation failed");
		String actWarningMessage6 = updateResponse6.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.line_item_filters[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage6, "Required parameters missing or invalid: quantity",
				"Required parameters missing or invalid: quantity Warning message is not matched");

		utils.logPass(
				"Verified warnning message: Required parameters missing or invalid: quantity for line_item_filters");

		// deleting QC and redeemable from DB
		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		deleteQCandRedeemableFromDB(response2);

		utils.logit(redeemableName + " redeemble and associated QC has been deleted from DB");

	}

	@Test(description = "SQ-T5562 Validate warnings for translation description while Updating Redeemable", groups = {
			"regression", "unstable", "dailyrun" })
	public void T5562_ValidateWarningStructureForTranslationDescriptionInUpdatingRedeemableAPI() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String slug = dataSet.get("slug");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Business");
		pageObj.dashboardpage().navigateToTabs("Address");
		pageObj.settingsPage().selectAlternateLanguage("French");

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("redeeming_criterion_id", "1");
		map.put("locationID", null);
		map.put("redeemableName", redeemableName);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("end_time", endTime);
		map.put("qualifier_type", "new");
		map.put("qc_processing_function", "sum_amounts");
		map.put("amount_cap", "10.0");
		map.put("item_qualifiers", dataSet.get("lisExternalID"));

		map.put("lineitemSelector", dataSet.get("lineItemSelector"));

		Response responseInvalidQCID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseInvalidQCID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Step1- Enter description in multiple languages
		String externalID = responseInvalidQCID.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		map.put("description",
				"[{\"language\":\"fr\",\"translation\":\"FR $0.25 Marl sContent Delivery Loyalty Offer\"},{\"language\":\"en\",\"translation\":\"$0.25 Marl sContent Delivery Loyalty Offer\"},{\"language\":\"es\",\"translation\":\"ES $0.25 Marl sContent Delivery Loyalty Offer\"}]");
		map.put("external_id_redeemable", externalID);
		Response updateResponse = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMultiLanguagesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableAlternateLocaleNameSchema, updateResponse.asString());
		Assert.assertTrue(isUpdateRedeemableMultiLanguagesSchemaValidated,
				"Update Redeemable (description in multiple languages) Schema Validation failed");
		String actSuccessMessage = updateResponse.jsonPath().getString("results[0].success").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actSuccessMessage, "true",
				"Redeemable description updated successfully with multiple languages");

		utils.logPass("Verified Redeemable description updated successfully with multiple languages");

		// Step2- Enter description in single language- ENGLISH
		map.put("description",
				"[{\"language\":\"en\",\"translation\":\"$0.25 Marl sContent Delivery Loyalty Offer\"}]");

		Response updateResponse2 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableOneLanguageSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, updateResponse2.asString());
		Assert.assertTrue(isUpdateRedeemableOneLanguageSchemaValidated,
				"Update Redeemable (description in single language) Schema Validation failed");
		String actSuccessMessage1 = updateResponse2.jsonPath().getString("results[0].success").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actSuccessMessage1, "true",
				"Redeemable description updated successfully with English languages");

		utils.logPass("Verified Redeemable description updated successfully with English languages");

		// Need to discuss with @rahul garg for these points
		// Step5-translation key empty
		map.put("description", "[{\"language\":\"en\",\"\":\"$0.25 Marl sContent Delivery Loyalty Offer\"}]");
		Response updateResponse3 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingTranslationSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableAlternateLocaleNameMissingTranslationSchema,
				updateResponse3.asString());
		Assert.assertTrue(isUpdateRedeemableMissingTranslationSchemaValidated,
				"Update Redeemable (translation key missing) Schema Validation failed");
		String actWarningMessage3 = updateResponse3.jsonPath()
				.get("results[0].warnings.alternate_locale_name[0].message").toString().replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actWarningMessage3, "Required parameters missing or invalid: translation",
				"Required parameters missing or invalid: translation Warning message is not matched");

		utils.logPass(
				"Verified that Required parameters missing or invalid: translation warning message for description");

		// Strep 6-Language key empty

		map.put("description", "[{\"\":\"en\",\"translation\":\"$0.25 Marl sContent Delivery Loyalty Offer\"}]");

		Response updateResponse4 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingLanguageSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableAlternateLocaleNameMissingLanguageSchema,
				updateResponse4.asString());
		Assert.assertTrue(isUpdateRedeemableMissingLanguageSchemaValidated,
				"Update Redeemable (Language key missing) Schema Validation failed");
		String actWarningMessage4 = updateResponse4.jsonPath()
				.getString("results[0].warnings.alternate_locale_name[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage4, "Required parameters missing or invalid: language",
				"Required parameters missing or invalid: language Warning message is not matched");

		utils.logPass("Verified warnning message: Required parameters missing or invalid: language for description");

		// Step7 - translation key value empty

		map.put("description", "[{\"language\":\"en\",\"translation\":\"\"}]");

		Response updateResponse5 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableTranslationValueEmptySchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableAlternateLocaleNameSchema, updateResponse5.asString());
		Assert.assertTrue(isUpdateRedeemableTranslationValueEmptySchemaValidated,
				"Update Redeemable (translation key value empty) Schema Validation failed");
		String actWarningMessage5 = updateResponse5.jsonPath()
				.getString("results[0].warnings.alternate_locale_name[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage5, "Required parameters missing or invalid: translation",
				"Required parameters missing or invalid: translation Warning message is not matched");

		utils.logPass("Verified warnning message: Required parameters missing or invalid: translation for description");

		// deleting QC and redeemable from DB
		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		deleteQCandRedeemableFromDB(response2);

		utils.logit(redeemableName + " redeemble and associated QC has been deleted from DB");

	}

	// delete QC and Redeemable using DB
	public void deleteQCandRedeemableFromDB(Response getRedeemablesAPIResponse) throws Exception {

		// Delete the QC from qualifying_expressions / qualification_criteria and
		// redeemable tables
		String redeemableExternalID = getRedeemablesAPIResponse.jsonPath().getString("data[0].external_id")
				.replace("[", "").replace("]", "");

		String qc_externalID = getRedeemablesAPIResponse.jsonPath()
				.getString("data[0].receipt_rule.redeeming_criterion.external_id").replace("[", "").replace("]", "");

		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", qc_externalID).replace("$businessID",
				dataSet.get("business_id"));
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", qc_externalID).replace("$businessID", dataSet.get("business_id"));

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		utils.logPass(redeemableExternalID + " external redeemable is deleted successfully");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}