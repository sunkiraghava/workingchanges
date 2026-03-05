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
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 */

@Listeners(TestListeners.class)
public class OfferIngestion_RedeemableInLineQC_APITests {
	static Logger logger = LogManager.getLogger(OfferIngestion_RedeemableInLineQC_APITests.class);
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

	@Test(description = "SQ-T5495 Create redeemable with inline QC" +
			"SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled.- single redeemable", groups = { "regression", "dailyrun" })

	public void T5495_VerifyCreationOfRedeemableInLineQCUsingAPI() throws Exception {
		String lisName = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String redeemableName = "AutomationRedeemable_API_" + CreateDateTime.getTimeDateString();

		String baseItemClauses = "item_name,==,White Rice/item_id,!=,123456/quantity,<,1/item_family,>,123/item_major_group,<=,1234/item_serial_number,>=,1/line_item_type,in,M/item_total_amount,not_in,12.9";
		String modifierItemClauses = "item_name,like,White Rice/item_id,not_like,123456/quantity,in_case_insensitive,1/item_family,is_case_insensitive,123/item_major_group,not_in_case_insensitive,1234/item_serial_number,in_range,1/line_item_type,not_in_range,M/item_total_amount,in_special,12.9";
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";
		// 20d5af22-f834-4cfb-92ea-64341fa2fa47
		String endTime = "2026-12-13T20:53:18"; // CreateDateTime.getCurrentDateTimeInGivenFormate("yyyy-MM-dd'T'HH:mm:ss");
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

		// Step 2 Qualifier type is empty but redeeming_criterion is provided
		map.put("qualifier_type", "");

		Response response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableEmptyQualifierTypeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateQcErrorSchema, response.asString());
		Assert.assertTrue(isCreateRedeemableEmptyQualifierTypeSchemaValidated,
				"Create Redeemable Schema Validation failed for empty qualifier type");
		String actualErrorMessage = response.jsonPath().getString("results[0].errors").replace("[", "").replace("]",
				"");
		Assert.assertTrue(actualErrorMessage.contains("Discounting Rule Can't be blank if flat discount is also blank"),
				"Discounting Rule Can't be blank if flat discount is also blank  error message is NOT failed .");
		Assert.assertTrue(actualErrorMessage.contains("Redeeming criterion Invalid"),
				"Redeeming criterion Invalid error message is NOT failed .");
		utils.logPass(
				"Discounting Rule Can't be blank if flat discount is also blank  / Redeeming criterion Invalid error message is verified in response");

		// Step 3 Qualifier type is set to inline and redeeming_criterion is also
		// providede but processing method is incorrect
		map.put("qualifier_type", "new");
		map.put("qc_processing_function", "Amounts");
		response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actualErrorMessage1 = response.jsonPath().getString("results[0].errors").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actualErrorMessage1, "Redeeming criterion processing method is not included in the list",
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(
				"Redeeming criterion processing method is not included in the list error message is verified in response");

		// Step 7Validate the error messages due to invalid date in inline QC Negative
		// amount cap
		map.put("qualifier_type", "new");
		map.put("qc_processing_function", "sum_amounts");
		map.put("amount_cap", "-10.0");
		response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);

		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actualErrorMessage2 = response.jsonPath().getString("results[0].errors").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actualErrorMessage2, "Redeeming criterion amount cap must be greater than or equal to 0",
				"Redeeming criterion amount cap must be greater than or equal to 0 error message is not matched");
		utils.logPass(
				"Redeeming criterion amount cap must be greater than or equal to 0 error message is verified in response");

		// Steps4- Qualifier type is set to inline and redeeming_criterion is also
		// provided and processing method is also correct
		map.put("qc_processing_function", "sum_amounts");
		map.put("amount_cap", "10.0");
		response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableQualifierTypeSetSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateQcErrorSchema, response.asString());
		Assert.assertTrue(isCreateRedeemableQualifierTypeSetSchemaValidated,
				"Create Redeemable Schema Validation failed when qualifier type is set");
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");

		String externalID = response.jsonPath().getString("results[0].external_id");
		Assert.assertTrue(!externalID.equalsIgnoreCase(""), externalID + " externalID is not generated");
		utils.logPass(redeemableName + " redeemable is successfully created with externalID - " + externalID);

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");

		// Delete the QC from qualifying_expressions / qualification_criteria and
		// redeemable tables
		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableName, redeemableName + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "new", "Qualifier type is not new");

		utils.logPass("Verfied that qualifier type is 'existing' in api response ");

		deleteQCandRedeemableFromDB(response2);

	}

	@Test(description = "SQ-T5498 Create Redeemable with existing QC " + "SQ-T5501 Create Redeemable with flat discount"
			+ "SQ-T5507 Create active redeemable with end time "
			+ "SQ-T5500 Create redeemable with existing QC but QC id of different business", groups = { "regression", "dailyrun" })
	public void TT5498_VerifyCreationOfRedeemableWithExistingQC() throws Exception {
		CreateLISandQC lisQcObject = new CreateLISandQC(driver);
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		String lisName = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String baseItemClauses = "item_name,==,White Rice/item_id,!=,123456/quantity,<,1/item_family,>,123/item_major_group,<=,1234/item_serial_number,>=,1/line_item_type,in,M/item_total_amount,not_in,12.9";
		String modifierItemClauses = "item_name,like,White Rice/item_id,not_like,123456/quantity,in_case_insensitive,1/item_family,is_case_insensitive,123/item_major_group,not_in_case_insensitive,1234/item_serial_number,in_range,1/line_item_type,not_in_range,M/item_total_amount,in_special,12.9";
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";

		Response lisCreatedResponse = lisQcObject.createLIS(dataSet.get("apiKey"), lisName, baseItemClauses,
				modifierItemClauses, "", "base_and_modifiers");
		Assert.assertEquals(lisCreatedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String lisExternalID = lisCreatedResponse.jsonPath().getString("results[0].external_id");
		// create QC using LIS
		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(dataSet.get("apiKey"),
				QCName, "", lisExternalID, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String qcExternalID = qcCreateResponse.jsonPath().getString("results[0].external_id");

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", lisExternalID);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("redeeming_criterion_id", "InvalidQCExternalID");
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responseInvalidQCID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseInvalidQCID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actualErrorMessage = responseInvalidQCID.jsonPath().getString("results[0].errors").replace("[", "")
				.replace("]", "");
		Assert.assertTrue(actualErrorMessage.contains("Discounting Rule Can't be blank if flat discount is also blank"),
				"Discounting Rule Can't be blank if flat discount is also blank  error message is NOT failed .");
		Assert.assertTrue(actualErrorMessage.contains("Redeeming criterion Invalid"),
				"Redeeming criterion Invalid error message is NOT failed .");
		utils.logPass(
				"Discounting Rule Can't be blank if flat discount is also blank  / Redeeming criterion Invalid error message is verified in response");

		// SQ-T5500 Create redeemable with existing QC but QC id of different business
		map.put("redeeming_criterion_id", "da26e7ce-0d03-4fe7-b92d-b71ee09cc6d6");
		Response responseOtherBuisnessQC_ID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseOtherBuisnessQC_ID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actualErrorMessage2 = responseOtherBuisnessQC_ID.jsonPath().getString("results[0].errors")
				.replace("[", "").replace("]", "");
		Assert.assertTrue(actualErrorMessage2.contains("Redeeming criterion Invalid"),
				"Redeeming criterion Invalid error message is NOT failed .");

		// da26e7ce-0d03-4fe7-b92d-b71ee09cc6d6
		// SQ-T5500 Create redeemable with existing QC but QC id of different business
		map.put("redeeming_criterion_id", "da26e7ce-0d03-4fe7-b92d-b71ee09cc6d6");
		responseOtherBuisnessQC_ID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseOtherBuisnessQC_ID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		actualErrorMessage2 = responseOtherBuisnessQC_ID.jsonPath().getString("results[0].errors").replace("[", "")
				.replace("]", "");
		Assert.assertTrue(actualErrorMessage2.contains("Redeeming criterion Invalid"),
				"Redeeming criterion Invalid error message is NOT failed .");

		// Added redeemable with existing QC
		map.put("redeeming_criterion_id", qcExternalID);
		Response response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");

		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableName, redeemableName + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "existing", "Qualifier type is not new");

		utils.logPass("Verfied that qualifier type is 'existing' in api response ");

		deleteQCandRedeemableFromDB(response2);

	}

	@Test(description = "SQ-T5509 Create redeemable with infinite expiry", groups = { "regression", "dailyrun" })
	public void T5509_VerifyRedeemableIsCreatedWithInfiniteExpiryOPtionTest() throws Exception {

		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);

		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("redeeming_criterion_id", "InvalidQCExternalID");
		map.put("indefinetely", "true");
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		// Added redeemable with existing QC
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		// SQ-T5501 Create Redeemable with flat discount
		String redeemableNameWithFlatDiscount = "AutomationRedeemableFlatDiscount_API_"
				+ CreateDateTime.getTimeDateString();
		map.put("qualifier_type", "flat_discount");
		map.put("discount_amount", "230.0");
		map.put("redeemableName", redeemableNameWithFlatDiscount);
		map.put("end_time", null);
		map.put("expiry_days", "2");

		Response responseFlatDiscount = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseFlatDiscount.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(redeemableNameWithFlatDiscount + " redeemable is created successfully");

		System.out.println("responseFlatDiscount*** " + responseFlatDiscount.asPrettyString());

		String redeemableExternalID = responseFlatDiscount.jsonPath().getString("results[0].external_id")
				.replace("]", "").replace("[", "");

		/*
		 * Response response2 =
		 * pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get(
		 * "apiKey"), "query", redeemableName, "1", "5");
		 * System.out.println("response2*** "+ response2.asPrettyString());
		 * 
		 * Assert.assertEquals(response2.getStatusCode(), 200,
		 * "Status code is not 200");
		 * 
		 * String actualRedeemableName1 =
		 * response2.jsonPath().getString("data[0].name").replace("[", "").replace("]",
		 * ""); Assert.assertEquals(actualRedeemableName1, redeemableName,
		 * redeemableName + " Redeemable name is not matched");
		 * 
		 * String actualQualifierType2 =
		 * response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
		 * .replace("[", "").replace("]", ""); Assert.assertEquals(actualQualifierType2,
		 * "existing", "Qualifier type is not new");
		 * 
		 * logger.info("validate that qualifier type is 'existing' in api response ");
		 * TestListeners.extentTest.get().
		 * pass("Verfied that qualifier type is 'existing' in api response ");
		 */

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchAndClickOnRedeemable(redeemableNameWithFlatDiscount);
		pageObj.redeemablePage().clickOnFirstNextButton();
		pageObj.redeemablePage().clickOnSecondButton();
		pageObj.redeemablePage().verifyRedeemableIndefiniteExpiryToggleButtonStatus("true");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

		utils.logPass(redeemableName + " redeemable is deleted successfully");

	}

	@Test(description = "SQ-T5496 Create Redeemable with inline QC with 5 Line item Filters and 5 item qualifiers", groups = { "regression", "dailyrun" })
	public void T5496_VerifiedRedeemableIsCreatedWithMultiQCandLIS() throws Exception {
		// 20d5af22-f834-4cfb-92ea-64341fa2fa47
		String redeemableName = "AutomationRedeemable_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String endTime = "2026-12-13T20:53:18"; // CreateDateTime.getCurrentDateTimeInGivenFormate("yyyy-MM-dd'T'HH:mm:ss");
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

		// Step 2 Qualifier type is empty but redeeming_criterion is provided
		map.put("qualifier_type", "new");

		Response response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");

		String externalID = response.jsonPath().getString("results[0].external_id");
		Assert.assertTrue(!externalID.equalsIgnoreCase(""), externalID + " externalID is not generated");
		utils.logPass(redeemableName + " redeemable is successfully created with externalID - " + externalID);

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");

		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableName, redeemableName + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "new", "Qualifier type is not new");

		utils.logPass("Verfied that qualifier type is 'existing' in api response ");

		deleteQCandRedeemableFromDB(response2);

	}

	@Test(description = "SQ-T5499 Create Deal with existing QC"
			+ "SQ-T5503 Create scheduled deal with start time i.e. start date in future <Note - taking start time in future by default>", groups = { "regression", "dailyrun" })
	public void TT5499_VerifiedDealIsCreatedWithExistingQC() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "true");

		// Added redeemable with existing QC
		Response response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableExistingQcSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, response.asString());
		Assert.assertTrue(isCreateRedeemableExistingQcSchemaValidated,
				"Create Redeemable Schema Validation failed for existing QC");
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");

		String externalID = response.jsonPath().getString("results[0].external_id");
		Assert.assertTrue(!externalID.equalsIgnoreCase(""), externalID + " externalID is not generated");
		utils.logPass(redeemableName + " redeemable is successfully created with externalID - " + externalID);

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");

		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableName, redeemableName + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "existing", "Qualifier type is not new");

		utils.logPass("Verfied that qualifier type is 'existing' in api response ");

		String actSegmentDefinitionId = response2.jsonPath().getString("data[0].segment_definition_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actSegmentDefinitionId, dataSet.get("segmentID"),
				dataSet.get("segmentID") + " Segment Definition ID is not matched");

		utils.logPass("Verified that Segment Definition ID is matched in api response -- " + actSegmentDefinitionId);

		String actDistributable = response2.jsonPath().getString("data[0].distributable").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actDistributable, "true", "true Distributable is not matched");
		utils.logPass("Verified that Distributable is matched in api response -- " + actDistributable);

		String redeemableExternalID = response2.jsonPath().getString("data[0].external_id").replace("[", "")
				.replace("]", "");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);
	}

	@Test(description = "SQ-T5502 Create deal with flat discount", groups = { "regression", "dailyrun" })
	public void T5502_VerifyRedeemableDealIsCreatedWithFlatDiscount() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "flat_discount");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "true");
		map.put("discount_amount", "230.0");

		// Added redeemable with existing QC
		Response response = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");

		String externalID = response.jsonPath().getString("results[0].external_id");
		Assert.assertTrue(!externalID.equalsIgnoreCase(""), externalID + " externalID is not generated");
		utils.logPass(redeemableName + " redeemable is successfully created with externalID - " + externalID);

		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");

		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code is not 200");

		String actualRedeemableName1 = response2.jsonPath().getString("data[0].name").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemableName1, redeemableName, redeemableName + " Redeemable name is not matched");

		String actualQualifierType2 = response2.jsonPath().getString("data[0].receipt_rule.qualifier_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualQualifierType2, "flat_discount", "Qualifier type is not new");

		utils.logPass("Verfied that qualifier type is 'flat_discount' in api response ");

		String actSegmentDefinitionId = response2.jsonPath().getString("data[0].segment_definition_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actSegmentDefinitionId, dataSet.get("segmentID"),
				dataSet.get("segmentID") + " Segment Definition ID is not matched");

		utils.logPass("Verified that Segment Definition ID is matched in api response -- " + actSegmentDefinitionId);

		String actDistributable = response2.jsonPath().getString("data[0].distributable").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actDistributable, "true", "true Distributable is not matched");
		utils.logPass("Verified that Distributable is matched in api response -- " + actDistributable);

		String redeemableExternalID = response2.jsonPath().getString("data[0].external_id").replace("[", "")
				.replace("]", "");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		utils.logPass(redeemableName + " redeemable is deleted successfully");

	}

	@Test(description = "SQ-T5501 Create Redeemable with flat discount" + "SQ-T5508 Create redeemable with expiry days", groups = { "regression", "dailyrun" })
	public void T5501_VerifyCreationOfRedeemableWithFlatDiscount() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));

		// Added redeemable with existing QC
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));

		// SQ-T5501 Create Redeemable with flat discount
		String redeemableNameWithFlatDiscount = "AutomationRedeemableFlatDiscount_API_"
				+ CreateDateTime.getTimeDateString();
		map.put("qualifier_type", "flat_discount");
		map.put("discount_amount", "230.0");
		map.put("redeemableName", redeemableNameWithFlatDiscount);
		map.put("end_time", null);

		// invalid expiry days
		map.put("expiry_days", "100000000");
		Response responseFlatDiscount1 = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseFlatDiscount1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableInvalidExpiryDaysSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiCreateUpdateLisSchema, responseFlatDiscount1.asString());
		Assert.assertTrue(isCreateRedeemableInvalidExpiryDaysSchemaValidated,
				"Create Redeemable Schema Validation failed for invalid expiry days");
		String actualErrorMessage1 = responseFlatDiscount1.jsonPath().getString("results[0].errors").replace("[", "")
				.replace("]", "");

		Assert.assertTrue(actualErrorMessage1.contains("Days to Expire must be less than or equal to 999999"));
		utils.logPass(redeemableNameWithFlatDiscount + " redeemable is created successfully");

		map.put("expiry_days", "2");

		Response responseFlatDiscount = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseFlatDiscount.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(redeemableNameWithFlatDiscount + " redeemable is created successfully");

		String redeemableExternalID = responseFlatDiscount.jsonPath().getString("results[0].external_id")
				.replace("[", "").replace("]", "");

		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", dataSet.get("business_id"));

		DBUtils.executeQuery(env, deleteRedeemableQuery1);

	}

// delete QC and Redeemable using DB send getRedeemablesAPIResponse as parameter
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

	@Test(description = "SQ-T5504 Create active deal with end time", groups = { "regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5504_CreateActiveDealWithEndTime() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutoRedeemableExistingQC_SQ_T5504_" + CreateDateTime.getTimeDateString();
		String QCName = "AutoQC_SQ_T5504_" + CreateDateTime.getTimeDateString();

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);

		// Added redeemable with existing QC
		Response response = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");

	}

	@Test(description = "SQ-T5505 Create Deal with Expiry Days", groups = { "regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5505_CreateActiveDealWithExpiryDays() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_SQ_T5505_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_SQ_T5505_" + CreateDateTime.getTimeDateString();

		// create deal by entering bith expiry days and end date
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", "10");
		Response response = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is not created, expected ");
		String errorMsg = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass("Deal with both expiry days and end date can't be created, expected");

		// create a deal by entering only expiry days
		String redeemableName2 = "AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("external_id", "");
		map2.put("amount_cap", "10.0");
		map2.put("percentage_of_processed_amount", "1");
		map2.put("qc_processing_function", "sum_amounts");
		map2.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map2.put("locationID", null);
		map2.put("external_id_redeemable", "");
		map2.put("redeemableProcessingFunction", "Sum Of Amount");
		map2.put("qualifier_type", "existing");
		map2.put("amount_cap", "10.0");
		map2.put("expQCName", dataSet.get("qcName"));
		map2.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map2.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map2.put("distributable", "true");
		map2.put("segment_definition_id", dataSet.get("segmentID"));
		map2.put("indefinetely", "false");
		map2.put("active_now", "true");
		map2.put("name", QCName2);
		map2.put("redeemableName", redeemableName2);
		map2.put("end_time", "");
		map2.put("expiry_days", "10");
		Response response2 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map2);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage2 = response2.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage2, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName2 + " redeemable is created successfully");
		utils.logPass("Deal with only expiry days has been created successfully");
	}

	@Test(description = "SQ-T5506 Create Deal with Infinite Expiry", groups = { "regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5506_CreateActiveDealWithInfiniteExpiry() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutoRedeemableExistingQC_SQ_T5506_" + CreateDateTime.getTimeDateString();
		String QCName = "AutoQC_SQ_T5506_" + CreateDateTime.getTimeDateString();

		// create deal with infinite expiry and expiry days
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", "");
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "true");
		map.put("active_now", "true");
		map.put("expiry_days", "10");
		Response response = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass("Active Deal with indefinite expiry has been created successfully");

		// create deal with infinite expiry,end time and expiry days
		String redeemableName2 = "AutoRedeemable2ExistingQC_SQ_T5506_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutoQC2_SQ_T5506_" + CreateDateTime.getTimeDateString();
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("external_id", "");
		map2.put("amount_cap", "10.0");
		map2.put("percentage_of_processed_amount", "1");
		map2.put("qc_processing_function", "sum_amounts");
		map2.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map2.put("locationID", null);
		map2.put("external_id_redeemable", "");
		map2.put("redeemableProcessingFunction", "Sum Of Amount");
		map2.put("qualifier_type", "existing");
		map2.put("amount_cap", "10.0");
		map2.put("expQCName", dataSet.get("qcName"));
		map2.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map2.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map2.put("distributable", "true");
		map2.put("segment_definition_id", dataSet.get("segmentID"));
		map2.put("indefinetely", "true");
		map2.put("active_now", "true");
		map2.put("name", QCName2);
		map2.put("redeemableName", redeemableName2);
		map2.put("end_time", endTime);
		map2.put("expiry_days", "10");
		Response response2 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map2);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage2 = response2.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage2, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass("Active Deal with indefinite expiry has been created successfully");
	}

	@Test(description = "SQ-T5494 Create active deal with end time", groups = { "regression", "dailyrun" })
	public void T5494_ValidateRedeemableShouldNotBeCreated() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "T5494AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "T5494AutomationQC_API_" + CreateDateTime.getTimeDateString();

		// create redeemable with segment_definition_id as true
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName);
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", dataSet.get("qcName"));
		map.put("end_time", endTime);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", "true");
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		Response response = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is not created");
		String errorMsg = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass(
				"User is not able to create a redeemable, expected, when segment_definition_id is set as true");

		// create redeemable with segment_definition_id as null
		String redeemableName2 = "T5494AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName2 = "T5494AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("external_id", "");
		map2.put("amount_cap", "10.0");
		map2.put("percentage_of_processed_amount", "1");
		map2.put("qc_processing_function", "sum_amounts");
		map2.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map2.put("locationID", null);
		map2.put("external_id_redeemable", "");
		map2.put("redeemableProcessingFunction", "Sum Of Amount");
		map2.put("qualifier_type", "existing");
		map2.put("amount_cap", "10.0");
		map2.put("expQCName", dataSet.get("qcName"));
		map2.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map2.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map2.put("distributable", "true");
		map2.put("segment_definition_id", "null");
		map2.put("indefinetely", "true");
		map2.put("active_now", "true");
		map2.put("name", QCName2);
		map2.put("redeemableName", redeemableName2);
		map2.put("end_time", endTime);
		map2.put("expiry_days", "10");
		Response response2 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map2);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage2 = response2.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage2, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName2 + " redeemable is not created");
		String errorMsg2 = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg2, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass(
				"User is not able to create a redeemable, expected, when segment_definition_id is set as null");

		// create redeemable with segment_definition_id as false
		String redeemableName3 = "AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map3 = new HashMap<String, String>();
		map3.put("external_id", "");
		map3.put("amount_cap", "10.0");
		map3.put("percentage_of_processed_amount", "1");
		map3.put("qc_processing_function", "sum_amounts");
		map3.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map3.put("locationID", null);
		map3.put("external_id_redeemable", "");
		map3.put("redeemableProcessingFunction", "Sum Of Amount");
		map3.put("qualifier_type", "existing");
		map3.put("amount_cap", "10.0");
		map3.put("expQCName", dataSet.get("qcName"));
		map3.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map3.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map3.put("distributable", "true");
		map3.put("segment_definition_id", "false");
		map3.put("indefinetely", "true");
		map3.put("active_now", "true");
		map3.put("name", QCName3);
		map3.put("redeemableName", redeemableName3);
		map3.put("end_time", endTime);
		map3.put("expiry_days", "10");
		Response response3 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map3);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage3 = response3.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage3, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName3 + " redeemable is not created");
		String errorMsg3 = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg3, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass(
				"User is not able to create a redeemable, expected, when segment_definition_id is set as false");

		// create redeemable with valid segment_definition_id
		String redeemableName4 = "AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName4 = "AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map4 = new HashMap<String, String>();
		map4.put("external_id", "");
		map4.put("amount_cap", "10.0");
		map4.put("percentage_of_processed_amount", "1");
		map4.put("qc_processing_function", "sum_amounts");
		map4.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map4.put("locationID", null);
		map4.put("external_id_redeemable", "");
		map4.put("redeemableProcessingFunction", "Sum Of Amount");
		map4.put("qualifier_type", "existing");
		map4.put("amount_cap", "10.0");
		map4.put("expQCName", dataSet.get("qcName"));
		map4.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map4.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map4.put("distributable", "true");
		map4.put("segment_definition_id", dataSet.get("segmentID"));
		map4.put("indefinetely", "false");
		map4.put("active_now", "true");
		map4.put("name", QCName4);
		map4.put("redeemableName", redeemableName4);
		map4.put("end_time", endTime);
		Response response4 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map4);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage4 = response4.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage4, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName4 + " redeemable is created successfully");
		utils.logPass("Active Deal has been created successfully");

		// create redeemable with invalid segment_definition_id
		String redeemableName5 = "AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName5 = "AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map5 = new HashMap<String, String>();
		map5.put("external_id", "");
		map5.put("amount_cap", "10.0");
		map5.put("percentage_of_processed_amount", "1");
		map5.put("qc_processing_function", "sum_amounts");
		map5.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map5.put("locationID", null);
		map5.put("external_id_redeemable", "");
		map5.put("redeemableProcessingFunction", "Sum Of Amount");
		map5.put("qualifier_type", "existing");
		map5.put("amount_cap", "10.0");
		map5.put("expQCName", dataSet.get("qcName"));
		map5.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map5.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map5.put("distributable", "true");
		map5.put("segment_definition_id", dataSet.get("invalidSegmentID"));
		map5.put("indefinetely", "true");
		map5.put("active_now", "true");
		map5.put("name", QCName5);
		map5.put("redeemableName", redeemableName5);
		map5.put("end_time", endTime);
		map5.put("expiry_days", "10");
		Response response5 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map5);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage5 = response5.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage5, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName5 + " redeemable is not created");
		String errorMsg5 = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg5, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass(
				"User is not able to create a redeemable, expected, when user has provided invalid segment id");

		// create redeemable with valid segment_definition_id but belonging to other
		// business
		String redeemableName6 = "AutomationRedeemableExistingQC_API2_" + CreateDateTime.getTimeDateString();
		String QCName6 = "AutomationQC_API2_" + CreateDateTime.getTimeDateString();
		Map<String, String> map6 = new HashMap<String, String>();
		map6.put("external_id", "");
		map6.put("amount_cap", "10.0");
		map6.put("percentage_of_processed_amount", "1");
		map6.put("qc_processing_function", "sum_amounts");
		map6.put("line_item_selector_id", dataSet.get("lisExternalID"));
		map6.put("locationID", null);
		map6.put("external_id_redeemable", "");
		map6.put("redeemableProcessingFunction", "Sum Of Amount");
		map6.put("qualifier_type", "existing");
		map6.put("amount_cap", "10.0");
		map6.put("expQCName", dataSet.get("qcName"));
		map6.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map6.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map6.put("distributable", "true");
		map6.put("segment_definition_id", dataSet.get("segmentIdOtherBusiness"));
		map6.put("indefinetely", "true");
		map6.put("active_now", "true");
		map6.put("name", QCName6);
		map6.put("redeemableName", redeemableName6);
		map6.put("end_time", endTime);
		map6.put("expiry_days", "10");
		Response response6 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map6);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage6 = response6.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage6, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName6 + " redeemable is not created");
		String errorMsg6 = response.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg6, dataSet.get("expectedErrorMsg"), "Error message doesn't match");
		utils.logPass(
				"User is not able to create a redeemable, expected, when user has provided valid segment id belonging to other business");
	}

	@Test(description = "SQ-T5565 Validate the warning structure of receipt_qualifiers in update redeemable api", groups = { "regression", "dailyrun" })
	public void T5565_ValidateWarningStructureOfReceiptQualifierUpdateRedeemableAPI() throws Exception {
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

		// Step2 Qualifier type is provided but invalid redeeming_criterion
		Response responseInvalidQCID = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(responseInvalidQCID.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Step1- Provide duplicate receipt qualifiers
		String externalID = responseInvalidQCID.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("receipt_qualifiers",
				"[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":10},{\"attribute\":\"total_amount\",\"operator\":\"in\",\"value\":1}]");
		map.put("external_id_redeemable", externalID);
		Response updateResponse = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableDuplicateReceiptQualifierSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierSchema, updateResponse.asString());
		Assert.assertTrue(isUpdateRedeemableDuplicateReceiptQualifierSchemaValidated,
				"Update Redeemable with duplicate receipt qualifiers Schema Validation failed");
		String actWarningMessage = updateResponse.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage, "Required parameter is duplicate: attribute",
				"Required parameter is duplicate: attribute Warning message is not matched");

		utils.logPass("Verified warning message Required parameter is duplicate: attribute for receipt_qualifiers");

		// Step3- Missing attribute key value
		map.put("receipt_qualifiers", "[{\"attribute\":\"\",\"operator\":\">=\",\"value\":10}]");

		Response updateResponse2 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingAttributeValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierSchema, updateResponse2.asString());
		Assert.assertTrue(isUpdateRedeemableMissingAttributeValueSchemaValidated,
				"Update Redeemable with missing attribute key's value Schema Validation failed");
		String actWarningMessage2 = updateResponse2.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage2, "Required parameters missing or invalid: attribute",
				"Required parameters missing or invalid: attribute Warning message is not matched");

		utils.logPass(
				"Verified warning message: Required parameters missing or invalid: attribute for receipt_qualifiers");

		// Step4- Missing operator key
		map.put("receipt_qualifiers", "[{\"attribute\":\"total_amount\",\"\":\">=\",\"value\":10}]");

		Response updateResponse3 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingOperatorSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierMissingOperatorSchema,
				updateResponse3.asString());
		Assert.assertTrue(isUpdateRedeemableMissingOperatorSchemaValidated,
				"Update Redeemable with missing operator key Schema Validation failed");
		String actWarningMessage3 = updateResponse3.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage3, "Required parameters missing or invalid: operator",
				"Required parameters missing or invalid: operator Warning message is not matched");

		utils.logPass(
				"Verified warning message: Required parameters missing or invalid: operator for receipt_qualifiers");

		// Strep 5- Missing operator key's value

		map.put("receipt_qualifiers", "[{\"attribute\":\"total_amount\",\"operator\":\"\",\"value\":10}]");

		Response updateResponse4 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingOperatorValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierSchema, updateResponse4.asString());
		Assert.assertTrue(isUpdateRedeemableMissingOperatorValueSchemaValidated,
				"Update Redeemable with missing operator key's value Schema Validation failed");
		String actWarningMessage4 = updateResponse4.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage4, "Required parameters missing or invalid: operator",
				"Required parameters missing or invalid: operator Warning message is not matched");

		utils.logPass(
				"Verified warning message: Required parameters missing or invalid: operator for receipt_qualifiers");

		// Step6-Missing value key

		map.put("receipt_qualifiers", "[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"\":10}]");

		Response updateResponse5 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierMissingValueSchema,
				updateResponse5.asString());
		Assert.assertTrue(isUpdateRedeemableMissingValueSchemaValidated,
				"Update Redeemable with missing value key Schema Validation failed");
		String actWarningMessage5 = updateResponse5.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage5, "Required parameters missing or invalid: value",
				"Required parameters missing or invalid: value Warning message is not matched");

		utils.logPass("Verified warning message: Required parameters missing or invalid: value for receipt_qualifiers");

		// Step-7 Missing value key's value
		map.put("receipt_qualifiers", "[{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":\"\"}]");

		Response updateResponse6 = pageObj.endpoints().updateRedeemableUsingAPI(dataSet.get("apiKey"), map);
		boolean isUpdateRedeemableMissingValueKeysValueSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateRedeemableReceiptQualifierStringValueSchema, updateResponse6.asString());
		Assert.assertTrue(isUpdateRedeemableMissingValueKeysValueSchemaValidated,
				"Update Redeemable with missing value key's value Schema Validation failed");
		String actWarningMessage6 = updateResponse6.jsonPath()
				.getString("results[0].warnings.receipt_rule.redeeming_criterion.receipt_qualifiers[0].message")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actWarningMessage6, "Required parameters missing or invalid: value",
				"Required parameters missing or invalid: value Warning message is not matched");

		utils.logPass("Verified warning message: Required parameters missing or invalid: value for receipt_qualifiers");

		// deleting QC and redeemable from DB
		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
				"query", redeemableName, "1", "5");
		deleteQCandRedeemableFromDB(response2);
		utils.logit(redeemableName + " redeemble and associated QC has been deleted from DB");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
