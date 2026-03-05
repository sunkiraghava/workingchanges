package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
/*
 * @Author : Shashank Sharma
 */
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
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

@Listeners(TestListeners.class)
public class OfferIngestion_QualificationCriteria_APITests {
	static Logger logger = LogManager.getLogger(OfferIngestion_QualificationCriteria_APITests.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl, businessID, decoupledFlagStatusStr;
	private static Map<String, String> dataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	public Utilities utils;
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String QCDeleteBaseQuery = "Delete from qualification_criteria where external_id = '${externalID2}' and business_id='${businessID}'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	private OfferIngestionUtilities offerUtils;
	private boolean decoupledFlagStatus;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
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
		offerUtils = new OfferIngestionUtilities(driver);
		businessID = dataSet.get("business_id");
		// Get current value of Decoupled Redemption Engine flag
		decoupledFlagStatus = offerUtils.getDecoupledRedemptionEngineFlagStatus(env, businessID);
		decoupledFlagStatusStr = "_decoupledFlag_" + decoupledFlagStatus;
	}

	@Test(description = "SQ-T5434 QC creation with passing all valid details"
			+ "SQ-T5435 Updating the QC from one type to another type from api."
			+ "SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled- update QC"
			+ "SQ-T5633 (1.0) verify the Offers Ingestion API, when the Enable Offers Ingestion flag disabled", groups = {
					"unstable", "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5434_VerifyCreationOfQualificationCriteriaUsingAPI() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";
		String businessID = dataSet.get("business_id");
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
		utils.logPass(
				"Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		boolean isCreateQcSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, qcCreateResponse.asString());
		Assert.assertTrue(isCreateQcSchemaValidated, "Create QC Schema Validation failed");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1);

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
				actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
		utils.logPass("Verified that processing function name is selected in UI : " + actualProcessingFunctionUI);

		String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();
		Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

		String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		// Updating QC with new LIS name and verify in UI
		// SQ-T5435 Updating the QC from one type to another type from api.
		String lisName2 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalLISId2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		Response qcUpdateResponse = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC, actualExternalLISId2, updatedProcessingFunction, "10");
		Assert.assertEquals(qcUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");
		boolean isUpdateQcSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, qcUpdateResponse.asString());
		Assert.assertTrue(isUpdateQcSchemaValidated, "Update QC Schema Validation failed");
		utils.logPass(QCName + "As expected QC is NOT updated successfully with LIS " + lisName2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualSelectedLineItemFilterValueUpdated = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();

		Assert.assertEquals(actualSelectedLineItemFilterValueUpdated, lisName2, actualSelectedLineItemFilterValueUpdated
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI i.e "
						+ actualSelectedLineItemFilterValueUpdated);

		String actualSelectedLineItemQualifierValue2 = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue2, lisName2, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		String actualUpdatedProcessingFunction = pageObj.qualificationcriteriapage()
				.getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualUpdatedProcessingFunction, updatedProcessingfunctionNameUI,
				actualUpdatedProcessingFunction + " Processing function is not updated successfully in UI");

		utils.logPass("Verified that Updated Proceesing Function name is selected in UI : "
				+ actualUpdatedProcessingFunction);

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete LIS 1
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalLISId2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);

		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	@Test(description = "SQ-T5428 QC updation with Invalid Values "
			+ "SQ-T5427 QC updation with Missing Required Fields "
			+ "SQ-T5426 (1.0)  QC updation without passing any values in optional fields", groups = { "regression",
					"dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5428_VerifyQCUpdateWithInvalidDetails() throws Exception {
		// String lisDeleteBaseQuery =
		// "Delete from line_item_selectors where external_id = '${externalID}' and
		// business_id='1043'";
		String businessID = dataSet.get("business_id");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";

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
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass(
				"Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1);

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		// ********************** SQ-T5428 QC updation with Invalid Values*/
		logger.info("****************************START Updation**********************");
		Response qcUpdateResponseInvaildDetails = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey,
				QCName, actualExternalIdQC, actualExternalIdLIS, processingFunction, "10abc");
		Assert.assertEquals(qcUpdateResponseInvaildDetails.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");
		boolean isUpdateQcInvalidPercentageSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateQcErrorSchema, qcUpdateResponseInvaildDetails.asString());
		Assert.assertTrue(isUpdateQcInvalidPercentageSchemaValidated,
				"Update QC with invalid percentage_of_processed_amount Schema Validation failed");

		boolean actualSuccessMessageInvalidDeails = qcUpdateResponseInvaildDetails.jsonPath()
				.getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageInvalidDeails, "STEP-1  Success message is not False");
		utils.logPass("verified that success message is False for invalid percentage of processed amount ");

		String errormessageInvaildDeails = qcUpdateResponseInvaildDetails.jsonPath().getString("results[0].errors")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(errormessageInvaildDeails, "Percentage of Processed Amount is not a number");

		utils.logPass("verified that ERROR message is matched for invalid percentage of processed amount ");

		Response qcUpdateResponseInvaildUUID = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC + "dfgdf5645cvb", actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcUpdateResponseInvaildUUID.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");
		boolean isUpdateQcInvalidExternalIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateQcErrorSchema, qcUpdateResponseInvaildUUID.asString());
		Assert.assertTrue(isUpdateQcInvalidExternalIdSchemaValidated,
				"Update QC with invalid External ID Schema Validation failed");
		boolean actualSuccessMessageInvalidUUID = qcUpdateResponseInvaildUUID.jsonPath()
				.getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageInvalidUUID, "STEP-1  Success message is not False");
		utils.logPass("verified that success message is False for invalid External ID ");

		String errormessageInvaildUUID = qcUpdateResponseInvaildUUID.jsonPath().getString("results[0].errors")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(errormessageInvaildUUID, "External ID is invalid");

		utils.logPass("verified that ERROR message is matched for invalid External ID ");

		Response qcUpdateResponseBlank = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC, actualExternalIdLIS, "", "");
		Assert.assertEquals(qcUpdateResponseBlank.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");
		boolean isUpdateQcInvalidProcessingFunctionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiUpdateQcErrorSchema, qcUpdateResponseBlank.asString());
		Assert.assertTrue(isUpdateQcInvalidProcessingFunctionSchemaValidated,
				"Update QC with blank Processing Function Schema Validation failed");
		String invalidErrorMessage = qcUpdateResponseBlank.jsonPath().getString("results[0].errors");
		Assert.assertTrue(invalidErrorMessage.contains("Processing Function is not included in the list"));
		Assert.assertTrue(invalidErrorMessage.contains("Processing Function can't be blank"));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// No need to Delete QC
//		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
//				.replace("${businessID}", businessID);
//		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
//
//		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
//				qcID_DB);
//
//		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
//				deleteQCFromQualifying_expressions);
//		Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
//		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
//		logger.info(QCName + " QC is deleted successfully");
//
//		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
//				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
//
//		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
//				deleteQCFromQualification_criteria);
//		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
//		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
//		logger.info(QCName + " QC is deleted successfully ");

	}

	@Test(description = "SQ-T5431 QC updation with Line item Selector having Filter Item Set-> base and modifier items."
			+ "SQ-T5653 QC creation with Line item Selector having Filter Item Set-> Only base items"
			+ "SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled.- create single QC", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5431_VerifyQcUpdationWithLisHavingFilterItemBaseAndModifierItems() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";

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
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_only has been created successfully");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1
				+ " having filter item set as base_only has been created successfully");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
				actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
		utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

		String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();
		Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

		String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		// Updating QC with new LIS name and verify in UI
		// SQ-T5435 Updating the QC from one type to another type from api.
		String lisName2 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalLISId2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_and_modifiers has been created successfully");

		Response qcUpdateResponse = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC, actualExternalLISId2, updatedProcessingFunction, "10");
		Assert.assertEquals(qcUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");

		utils.logPass(QCName + " QC is updated successfully with LIS " + lisName2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualSelectedLineItemFilterValueUpdated = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();

		Assert.assertEquals(actualSelectedLineItemFilterValueUpdated, lisName2, actualSelectedLineItemFilterValueUpdated
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass("Verified " + QCName + " QC has been updated successfully with LIS "
				+ lisName2 + " having filter item set as base_and_modifiers.");

		String actualSelectedLineItemQualifierValue2 = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue2, lisName2, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		String actualUpdatedProcessingFunction = pageObj.qualificationcriteriapage()
				.getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualUpdatedProcessingFunction, updatedProcessingfunctionNameUI,
				actualUpdatedProcessingFunction + " Processing function is not updated successfully in UI");

		utils.logPass("Verified that Updated Proceesing Function name is selected in UI : "
				+ actualUpdatedProcessingFunction);

		// deleting the first LIS from db

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete LIS 2
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalLISId2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	@Test(description = "SQ-T5429 QC updation with Line item Selector having Filter Item Set-> Only base items."
			+ "SQ-T5655 QC creation with Line item Selector having Filter Item Set-> base and modifier items", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5429_VerifyQcUpdationWithLisHavingFilterItemOnlyBaseItems() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";
		String businessID = dataSet.get("business_id");

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
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_and_modifiers has been created successfully");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1
				+ " having filter item set as base_and_modifiers has been created successfully");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
				actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
		utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

		String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();
		Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

		String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		// Updating QC with new LIS name and verify in UI
		// SQ-T5435 Updating the QC from one type to another type from api.
		String lisName2 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalLISId2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_only has been created successfully");

		Response qcUpdateResponse = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC, actualExternalLISId2, updatedProcessingFunction, "10");
		Assert.assertEquals(qcUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");

		utils.logPass(QCName + " QC is updated successfully with LIS " + lisName2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualSelectedLineItemFilterValueUpdated = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();

		Assert.assertEquals(actualSelectedLineItemFilterValueUpdated, lisName2, actualSelectedLineItemFilterValueUpdated
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass("Verified " + QCName + " QC has been updated successfully with LIS "
				+ lisName2 + " having filter item set as base_only.");

		String actualSelectedLineItemQualifierValue2 = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue2, lisName2, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		String actualUpdatedProcessingFunction = pageObj.qualificationcriteriapage()
				.getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualUpdatedProcessingFunction, updatedProcessingfunctionNameUI,
				actualUpdatedProcessingFunction + " Processing function is not updated successfully in UI");

		utils.logPass("Verified that Updated Proceesing Function name is selected in UI : "
				+ actualUpdatedProcessingFunction);

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete LIS 1
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalLISId2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	@Test(description = "SQ-T5430 QC updation with Line item Selector having Filter Item Set-> Only modifier items", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5430_VerifyQcUpdationWithLisHavingFilterItemOnlyModifierItems() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String processingFunctionNameUI = "Sum of Amounts";
		String updatedProcessingFunction = "sum_qty";
		String updatedProcessingfunctionNameUI = "Sum of Quantities";
		String businessID = dataSet.get("business_id");

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
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_only has been created successfully");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1
				+ " having filter item set as base_only has been created successfully");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualProcessingFunctionUI = pageObj.qualificationcriteriapage().getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualProcessingFunctionUI, processingFunctionNameUI,
				actualProcessingFunctionUI + " Processing function is not updated successfully in UI");
		utils.logPass("Verified that Proceesing function name is selected in UI : " + actualProcessingFunctionUI);

		String actualSelectedLineItemFilterValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();
		Assert.assertEquals(actualSelectedLineItemFilterValue, lisName1, actualSuccessMessage
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Filter Name is matched with expected Line Item Filter Name in QC from UI");

		String actualSelectedLineItemQualifierValue = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue, lisName1, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		// Updating QC with new LIS name and verify in UI
		// SQ-T5435 Updating the QC from one type to another type from api.
		String lisName2 = "AutomationLIS_" + CreateDateTime.getTimeDateString();
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"modifiers_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalLISId2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as modifiers_only has been created successfully");

		Response qcUpdateResponse = pageObj.endpoints().updateQualificationCriteriaUsingApi(adminKey, QCName,
				actualExternalIdQC, actualExternalLISId2, updatedProcessingFunction, "10");
		Assert.assertEquals(qcUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not Updated and status code is not 200");

		utils.logPass(QCName + " QC is updated successfully with LIS " + lisName2);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);

		String actualSelectedLineItemFilterValueUpdated = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemFilterName();

		Assert.assertEquals(actualSelectedLineItemFilterValueUpdated, lisName2, actualSelectedLineItemFilterValueUpdated
				+ " actual Line Item Filter Name is not matched with expected Line Item Filter Name");
		utils.logPass("Verified " + QCName + " QC has been updated successfully with LIS "
				+ lisName2 + " having filter item set as modifiers_only.");

		String actualSelectedLineItemQualifierValue2 = pageObj.qualificationcriteriapage()
				.getFirstSelectedLineItemQualifierName();
		Assert.assertEquals(actualSelectedLineItemQualifierValue2, lisName2, actualSuccessMessage
				+ " actual Line Item Qualifier Name is not matched with expected Line Item Filter Name");
		utils.logPass(
				"Verified that Line Item Qualifier Name is matched with expected Line Item Qualifier Name in QC from UI");

		String actualUpdatedProcessingFunction = pageObj.qualificationcriteriapage()
				.getSelectedProcessingFunctionsValue();
		Assert.assertEquals(actualUpdatedProcessingFunction, updatedProcessingfunctionNameUI,
				actualUpdatedProcessingFunction + " Processing function is not updated successfully in UI");

		utils.logPass("Verified that Updated Proceesing Function name is selected in UI : "
				+ actualUpdatedProcessingFunction);

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete LIS 1
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalLISId2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	// shashank
	@Test(description = "SQ-T5654 / (OMM-T2623) QC creation with Line item Selector having Filter Item Set-> Only modifier items"
			+ "SQ-T5653 (OMM-T2622) QC creation with Line item Selector having Filter Item Set-> Only base items"
			+ "SQ-T5655 / OMM-T2624 QC creation with Line item Selector having Filter Item Set-> base and modifier items"
			+ "SQ-T5624 Verify the create LIS API functionality for a single LIS item with base and modifiers"
			+ "SQ-T5711 Verify by creating new QC entry using Create QC API and its retrieval through the get QC API.", groups = {
					"regression", "dailyrun" }, dataProvider = "T5654_DataProvider")
	@Owner(name = "Shashank Sharma")
	public void T5654_VerifyCreateQCWithLineItemSelectorWithDifferentFilterTypes(String filterType) throws Exception {
		utils.logPass("T5654_VerifyCreateQCWithLineItemSelectorWithDifferentFilterTypes Started for filter type - "
				+ filterType);
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");

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
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, filterType,
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalLISID = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_only has been created successfully");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalLISID, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1
				+ " having filter item set as base_only has been created successfully");

		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "1", "20");
		boolean isGetQcDetailsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiGetQcDetailsSchema, getQCDetailsResponse.asString());
		Assert.assertTrue(isGetQcDetailsSchemaValidated, "Get QC Details Schema Validation failed");
		String lisName_In_line_item_filters = getQCDetailsResponse.jsonPath()
				.get("data[0].line_item_filters[0].line_item_selector.name").toString().replace("[", "")
				.replace("]", "");
		String externaleID_In_line_item_filters = getQCDetailsResponse.jsonPath()
				.getString("data[0].line_item_filters[0].line_item_selector.external_id").replace("[", "")
				.replace("]", "");
		String filterItemSet_in_line_item_filters = getQCDetailsResponse.jsonPath()
				.getString("data[0].line_item_filters[0].line_item_selector.filter_item_set").replace("[", "")
				.replace("]", "");

		Assert.assertEquals(lisName_In_line_item_filters, lisName1,
				lisName1 + " expected LIS Name is not matched with expected value");

		utils.logPass("Verified that " + lisName1
				+ " expected LIS Name is coming in line_item_filters in response for filter type - " + filterType);

		Assert.assertEquals(externaleID_In_line_item_filters, actualExternalLISID,
				actualExternalLISID + " expected LIS external_id is not matched with expected value");

		utils.logPass("Verified that " + actualExternalLISID
				+ " expected LIS External ID is matched in line_item_filters in response for filter type - "
				+ filterType);

		Assert.assertEquals(filterItemSet_in_line_item_filters, filterType,
				filterType + " expected LIS filter type is not matched with expected value");
		utils.logPass("Verified that " + filterType
				+ " expected LIS Filter Type is matched in line_item_filters in response for filter type - "
				+ filterType);

		if (!filterType.equalsIgnoreCase("base_only")) {
			String lisName_In_item_qualifiers = getQCDetailsResponse.jsonPath()
					.get("data[0].item_qualifiers[0].line_item_selector.name").toString().replace("[", "")
					.replace("]", "");
			String externaleID_In_item_qualifiers = getQCDetailsResponse.jsonPath()
					.getString("data[0].item_qualifiers[0].line_item_selector.external_id").replace("[", "")
					.replace("]", "");
			String filterItemSet_in_item_qualifiers = getQCDetailsResponse.jsonPath()
					.getString("data[0].item_qualifiers[0].line_item_selector.filter_item_set").replace("[", "")
					.replace("]", "");

			Assert.assertEquals(lisName_In_item_qualifiers, lisName1,
					lisName1 + " expected LIS Name is not matched with expected value");
			utils.logPass("Verified that " + lisName1
					+ " expected LIS Name is coming in item_qualifiers in response for filter type - " + filterType);

			Assert.assertEquals(externaleID_In_item_qualifiers, actualExternalLISID,
					actualExternalLISID + " expected LIS external_id is not matched with expected value");
			utils.logPass("Verified that " + actualExternalLISID
					+ " expected LIS External ID is matched in item_qualifiers in response for filter type - "
					+ filterType);

			Assert.assertEquals(filterItemSet_in_item_qualifiers, filterType,
					filterType + " expected LIS filter type is not matched with expected value");
			utils.logPass("Verified that " + filterType
					+ " expected LIS Filter Type is matched in item_qualifiers in response for filter type - "
					+ filterType);
		}
		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalLISID)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	// vansham
	@Test(description = "SQ-T5673 Validate Handling of Excess Line Item Filters and Item qualifiers in QC Creation."
			+ "SQ-T5672 Duplicate Line Item Filters."
			+ "SQ-T5671 (1.0) Create a QC using the API with a valid location ID but belongs to different business."
			+ "SQ-5670 (1.0) QC creation with valid location id."
			+ "SQ-5675 Validate that error message gets displayed if user passes big integer value in page or per_page parameter"
			+ "SQ- T5682 Verify that the GET API successfully fetches the list of all QC's (count should match) from the DB"
			+ "SQ-5751 (1.0) Create a QC using the API with a invalid location ID i.e. does not belong to any business", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5673_VerifyHandlingOfExcessLineItemFiltersAndItemQualifiersInQCCreation() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='1043'";
		String QCDeleteBaseQuery = "Delete from qualification_criteria where external_id = '${externalID2}' and business_id='1043'";
		String getLisIdQuery = "select id from line_item_selectors where external_id = '${externalID}' and business_id='1043'";
		Map<String, String> map = new HashMap<String, String>();
		map.put("receipt_qualifiers", null);
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
//  		// Add base item cluases

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
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		// create 2nd lis
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS having filter item set as base_and_modifiers has been created successfully");

		// Create a QC using the API with a valid location ID but belongs to different
		// business
		Response qcCreateResponse1 = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				actualExternalIdLIS2, processingFunction, "10", dataSet.get("otherLocationID"), map);
		Assert.assertEquals(qcCreateResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC1 = qcCreateResponse1.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC1, "QC success message is not True");

		String actualErrorMessage = qcCreateResponse1.jsonPath().getString("results[0].errors[0]");
		Assert.assertEquals(actualErrorMessage, dataSet.get("expectedErrorMessage"), "Error message does not match");

		utils.logPass("QC is not created with a valid location ID but belongs to different business");

		// creating qc with valid location id of the same business
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				actualExternalIdLIS2, processingFunction, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");

		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1
				+ " having filter item set as base_and_modifiers has been created successfully");

		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, "", "2", "13");
		Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		int total_records = getQCDetailsResponse.jsonPath().getInt("meta.total_records");
		String getLisCountQuery = "select count(id) as id from qualification_criteria where business_id = " + businessID
				+ ";";
		String count = DBUtils.executeQueryAndGetColumnValue(env, getLisCountQuery, "id");
		int count3 = Integer.parseInt(count);
		Assert.assertEquals(count3, total_records, "qualification_criteria count is not matching with DB");
		utils.logPass("Verified that qualification_criteria count is matched with DB");

		// get the line item selector id from line_item_selectors_table
		String getLisIdQuery1 = getLisIdQuery.replace("${externalID}", actualExternalIdLIS2);
		String LIS_ID = DBUtils.executeQueryAndGetColumnValue(env, getLisIdQuery1, "id");

		String getLisIdQuery2 = getLisIdQuery.replace("${externalID}", actualExternalIdLIS);
		String LIS_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getLisIdQuery2, "id");

		// verifying the list of created records in qualifying_expressions table
		String expectedCount = "select id from qualifying_expressions where line_item_selector_id = '${LIS_ID}' and business_id='1043'";
		String expectedCount1 = expectedCount.replace("${LIS_ID}", LIS_ID);
		List<String> count1 = DBUtils.getValueFromColumnInList(env, expectedCount1, "id");
		Assert.assertEquals(count1.size(), 0, "6th filter has been saved in qualifying_expressions table");
		String expectedCount3 = expectedCount.replace("${LIS_ID}", LIS_ID2);
		List<String> count2 = DBUtils.getValueFromColumnInList(env, expectedCount3, "id");
		Assert.assertEquals(count2.size(), 10, "Actual count is not matching in qualifying expressions table");
		utils.logPass(
				"Verified that only 5 line item filters and 5 QCs are present in the database for the QC, and the 6th filter is not saved.");

		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		// deleting the first LIS from db
		String deleteLISQuerys = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS);
		DBUtils.executeQuery(env, deleteLISQuerys);

		// deleting the second lis from db
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2);
		DBUtils.executeQuery(env, deleteLISQuery2);

		// deleting the QC from db
		String deleteLISQuery3 = QCDeleteBaseQuery.replace("${externalID2}", actualExternalIdQC);
		DBUtils.executeQuery(env, deleteLISQuery3);

	}

	@Test(description = "SQ-T5703 Invalid UUID Format in Line Item Selector"
			+ "SQ-T5710 Invalid Expression Type in Item Qualifiers"
			+ "SQ-T5714 Validate all the negative scenarios by sending incorrect data in the GET QC API request.", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5682_VerifyQCCreationWithInvalidData() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		Map<String, String> map = new HashMap<String, String>();
		map.put("receipt_qualifiers", null);
//

		// filter_item_set == base_only, modifiers_only and base_and_modifiers
//  		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// qc creation with invalid line item filter id
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				"actualExternalIdLIS", processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String warningMsg = qcCreateResponse.jsonPath().getString("results[0].warnings.line_item_filters[0].message");
		Assert.assertEquals(warningMsg, dataSet.get("expectedWarningMsgForInvalidLif"),
				"Warning message for qc creation with invalid line item filter id doesn't match");
		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "2", "13");
		Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		String actualLIF = getQCDetailsResponse.jsonPath().getString("data[0].line_item_filters");
		Assert.assertEquals(actualLIF, dataSet.get("expectedLIF"), "line_item_selector_id has not been ignored");
		utils.logPass(
				"line_item_selector_id has been ignored when correct line_item_selector_id is not provided while creating QC");

		// qc creation with invalid Expression Type in Item Qualifiers
		map.put("item_qualifiers", "[{\"expression_typ\":\"line_item_exists\",\"line_item_selector_id\":\""
				+ actualExternalIdLIS
				+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exiss\",\"line_item_selector_id\":\""
				+ actualExternalIdLIS
				+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector\":\""
				+ actualExternalIdLIS
				+ "\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\"22id\",\"net_value\":null},{\"expression_type\":\"line_item_exists\",\"line_item_selector_id\":\""
				+ actualExternalIdLIS + "\",\"net_value\":\"null\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String invalidItemQualifierWarningMsg1 = qcCreateResponse2.jsonPath()
				.getString("results[0].warnings.item_qualifiers[0].message");
		Assert.assertEquals(invalidItemQualifierWarningMsg1, dataSet.get("invalidItemQualifierWarningMsg1"),
				"Invalid item Qualifier error message doesn't match");
		String invalidItemQualifierWarningMsg2 = qcCreateResponse2.jsonPath()
				.getString("results[0].warnings.item_qualifiers[1].message");
		Assert.assertEquals(invalidItemQualifierWarningMsg2, dataSet.get("invalidItemQualifierWarningMsg1"),
				"Invalid item Qualifier error message doesn't match");
		String invalidItemQualifierWarningMsg3 = qcCreateResponse2.jsonPath()
				.getString("results[0].warnings.item_qualifiers[2].message");
		Assert.assertEquals(invalidItemQualifierWarningMsg3, dataSet.get("invalidItemQualifierWarningMsg2"),
				"Invalid item Qualifier error message doesn't match");
		String invalidItemQualifierWarningMsg4 = qcCreateResponse2.jsonPath()
				.getString("results[0].warnings.item_qualifiers[3].message");
		Assert.assertEquals(invalidItemQualifierWarningMsg4, dataSet.get("invalidItemQualifierWarningMsg2"),
				"Invalid item Qualifier error message doesn't match");
		Response getQCDetailsResponse2 = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName2, "2", "13");
		Assert.assertEquals(getQCDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		String actualItem_Qualifier = getQCDetailsResponse.jsonPath().getString("data[0].item_qualifiers");
		Assert.assertEquals(actualItem_Qualifier, dataSet.get("expectedIQ"),
				"line_item_selector_id has not been ignored");
		utils.logPass("Qc with invalid expression_type has not been created");

		// Hit the get qc api with incorrect json
		Response getQCDetailsResponse3 = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName2, "as",
				"13");
		Assert.assertEquals(getQCDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"User is able to fetch list of QC's with invalid page parameter");
		String getQcListApiErrorMsg = getQCDetailsResponse3.jsonPath().getString("error");
		Assert.assertEquals(getQcListApiErrorMsg, dataSet.get("expectedGetQCApiErrorMessage"),
				"Error message doesn't match");
		utils.logPass("User is unable to fetch list of QC's with incorrect json, expected");

		// Negative value passed in per_page parameter
		Response getQCDetailsResponse4 = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName2, "-1",
				"13");
		Assert.assertEquals(getQCDetailsResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"User is able to fetch list of QC's with invalid page parameter");
		String getQcListApiErrorMsg2 = getQCDetailsResponse4.jsonPath().getString("error");
		Assert.assertEquals(getQcListApiErrorMsg2, dataSet.get("expectedGetQCApiErrorMessage"),
				"Error message doesn't match");
		utils.logPass(
				"User is unable to fetch list of QC's with Negative value passed in per_page parameter, expected");

		// blank admin key
		Response getQCDetailsResponse5 = pageObj.endpoints().getQualificationListUsingAPI("", QCName2, "1", "5");
		Assert.assertEquals(getQCDetailsResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"User is able to fetch list of QC's with invalid page parameter");
		String getQcListApiErrorMsg3 = getQCDetailsResponse5.jsonPath().getString("error");
		logger.info(getQcListApiErrorMsg3);
		Assert.assertEquals(getQcListApiErrorMsg3, dataSet.get("blankAdminKeyErrorMsg"), "Error message doesn't match");
		utils.logPass("User is unable to fetch list of QC's with blank admin key passed in api, expected");

		// Invalid admin key
		Response getQCDetailsResponse6 = pageObj.endpoints().getQualificationListUsingAPI("invalidAdminKey", QCName2,
				"1", "13");
		Assert.assertEquals(getQCDetailsResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"User is able to fetch list of QC's with invalid page parameter");
		String getQcListApiErrorMsg4 = getQCDetailsResponse6.jsonPath().getString("error");
		Assert.assertEquals(getQcListApiErrorMsg4, dataSet.get("blankAdminKeyErrorMsg"), "Error message doesn't match");
		utils.logPass("User is unable to fetch list of QC's with invalid admin key passed in api, expected");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		utils.logPass(QCName2 + " QC is deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env, deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		utils.logPass(QCName2 + " QC is deleted successfully");
	}

	@Test(description = "SQ-T5597 Validate that user is able to create QC with processing function-Target Price for Bundle having 5 valid line item filters and 5 item qualifiers"
			+ "SQ-T5598 Validate that user is able to create QC with processing function-Target Price for Bundle Advanced having 5 valid line item filters and 5 item qualifiers"
			+ "SQ-T5599 Validate that user is able to create QC with processing function-Hit Target Menu Item Price having 5 valid line item filters and 5 item qualifiers"
			+ "SQ-T5648 Verify the valid processing functions in API for QC creation", groups = { "regression",
					"dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5597_VerifyQCCreationWithDifferentProcessingFunctionPartOne() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target";
		String processingFunction2 = "bundle_price_target_advanced";
		String processingFunction3 = "hit_target_price";
		Map<String, String> map = new HashMap<String, String>();
		map.put("receipt_qualifiers", null);
		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// create QC with processing function-Target Price for Bundle having 5 valid
		// line item filters and 5 item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, dataSet.get("expectedErrorMsg2"),
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Target Price for Bundle having 5 valid line item filters and 5 item qualifiers");

		// create QC with processing function-Target Price for Bundle Advanced having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, dataSet.get("expectedErrorMsg"),
				"Warning message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Target Price for Bundle Advanced having 5 valid line item filters and 5 item qualifiers");

		// create QC with processing function-Hit Target Menu Item Price having 5 valid
		// line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, dataSet.get("expectedErrorMsg"),
				"Warning message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Hit Target Menu Item Price having 5 valid line item filters and 5 item qualifiers");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		String deleteLISQuery3 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		utils.logPass(lisName3 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertFalse(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

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
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName + " QC is not deleted successfully");
		utils.logPass(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria3 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery3 = DBUtils.executeQuery(env, deleteQCFromQualification_criteria3);
		Assert.assertTrue(statusQualification_criteriaQuery3, QCName + " QC is not deleted successfully");
		utils.logPass(QCName3 + " QC is deleted successfully");
	}

	@Test(description = "SQ-T5600 Validate that user is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers"
			+ "SQ-T5601 Validate that user is able to create QC with processing function-Hit Target Menu for Minimum Price unit having 5 valid line item filters and 5 item qualifiers"
			+ "SQ-T5634 verify the Offers Ingestion API, when the Enable Offers Ingestion flag enabled.- get QC count"
			+ "SQ-T5638 Verify the create QC functionality flow", groups = { "regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5600_VerifyQCCreationWithDifferentProcessingFunctionPartTwo() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "hit_target_price_max_price_once";
		String processingFunction2 = "hit_target_price_min_price_once";
		Map<String, String> map = new HashMap<String, String>();
		map.put("receipt_qualifiers", null);
		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// create QC with processing function-Hit Target Menu for Maximum Price unit
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, dataSet.get("expectedErrorMsg"),
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// create QC with processing function-Hit Target Menu for Minimum Price unit
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName2 + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, dataSet.get("expectedErrorMsg"),
				"Warning message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC2, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Hit Target Menu for Minimum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		utils.logPass(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		utils.logPass(QCName2 + " QC is deleted successfully");

		// Remove Dashboard API permission to admin and hit GET QC aPI
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));
		pageObj.AdminUsersPage().turnPermissionoff(dataSet.get("permissionTitle"));
		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(dataSet.get("siteAdminKey"),
				QCName2, "1", "13");
		Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN,
				"User is able to fetch list of QC's successfully");
		String getQcListApiPermissionMsg = getQCDetailsResponse.jsonPath().getString("no_permission_error");
		Assert.assertEquals(getQcListApiPermissionMsg, dataSet.get("permissionErrorMsg"),
				"Error message doesn't match");
		utils.logPass("User is unable to fetch list of QC's with removed Dashboard API permission, expected");

		// commenting the below as fix of OMM-1265
		// Grant Dashboard API permission to admin and hit GET QC aPI
//		pageObj.AdminUsersPage().turnPermissionOn(dataSet.get("permissionTitle"));
//		Response getQCDetailsResponse2 = pageObj.endpoints().getQualificationListUsingAPI(dataSet.get("siteAdminKey"),
//				QCName2, "1", "13");
//		Assert.assertEquals(getQCDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
//				"User is unable to fetch list of QC's successfully");
//		String getQcNameFromListApi = getQCDetailsResponse2.jsonPath().getString("data[0].name");
//		Assert.assertEquals(getQcNameFromListApi, QCName2, "Qc Name does not match");
//		logger.info("User is able to fetch list of QC's with granted Dashboard API permission");
//		TestListeners.extentTest.get()
//				.pass("User is unable to fetch list of QC's with granted Dashboard API permission");

	}

	@Test(description = "SQ-T5602 Verify creating a QC using the \"Sum of amount incremental\" processing_function with both mandatory and optional fields", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T5602_VerifyCreationOfQualificationCriteriaAsProcessingFunctionSumOfAmountIncrement() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts_incremental";
		String processingFunctionNameUI = "Sum of Amounts Incremental";
		String businessID = dataSet.get("business_id");
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
		utils.logPass(
				"Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		boolean isCreateQcSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, qcCreateResponse.asString());
		Assert.assertTrue(isCreateQcSchemaValidated, "Create QC Schema Validation failed");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		String qcExternalID = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Assert.assertTrue(qcExternalID != null, QCName + " QC is not created successfully ");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1);

		// Delete QC shanky

		// Delete LIS 1
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", qcExternalID).replace("${businessID}",
				businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		// Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not
		// deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", qcExternalID).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	@Test(description = "OMM-T2608/SQ-T5790 QC Creation without passing any values in optional fields", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void OMMT2608_VerifyCreationOfQualificationCriteriaWithoutOptionDetails() throws Exception {
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts_incremental";
		String processingFunctionNameUI = "Sum of Amounts Incremental";
		String businessID = dataSet.get("business_id");
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
		utils.logPass(
				"Verified Step1:- If pass blank external_id, LIS will be created and system will automatically generate the external_id and assigned to that LIS");

		Response qcCreateResponse = pageObj.endpoints().createQualificationCriteriaUsingApi(adminKey, QCName, "",
				actualExternalIdLIS, processingFunction, "10");
		logger.info("qcCreateResponse--" + qcCreateResponse.asPrettyString());
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		boolean isCreateQcSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiCreateUpdateLisSchema, qcCreateResponse.asString());
		Assert.assertTrue(isCreateQcSchemaValidated, "Create QC Schema Validation failed");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");

		String qcExternalID = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		Assert.assertTrue(qcExternalID != null, QCName + " QC is not created successfully ");

		utils.logPass(QCName + " QC is created successfully with LIS " + lisName1);

		// Delete QC shanky

		// Delete LIS 1
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName1 + " LIS is not deleted successfully");
		utils.logPass(lisName1 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", qcExternalID).replace("${businessID}",
				businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);

		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", qcExternalID).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env, deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

	}

	@Test(description = "OMM-T4800 - Verify LIS functionality with base items and exact modifier quantity", groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void validate_OMM_T4800() throws Exception {

		double amount1 = 3.0;
		double amount2 = 7.0;
		double amount3 = 10.0;

		double expTotal1 = amount1 + amount2 + amount3;

		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
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

		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 3, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

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

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();
		Map<String, String> detailsMap9 = new HashMap<String, String>();
		Map<String, String> detailsMap10 = new HashMap<String, String>();

		// String item_name, String item_qty, String amount, String item_type, String
		// item_family, String item_group, String serial_number, String item_id
		// Item1|1|10|M|101|1001|1000|1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("item1", "1", "10", "M", "", "", "1.0", "101");
		parentMap.put("item1", detailsMap1);

		// Item2|1|8|M|111|1001|1000|1.1
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("item2", "1", "8", "M", "", "", "1.1", "111");
		parentMap.put("item2", detailsMap2);

		// Item3|1|10|M|102|1001|1000|2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("item3", "1", amount3 + "", "M", "", "", "2.0", "102");
		parentMap.put("item3", detailsMap3);

		// Item4|1|1|M|112|1001|1000|2.1
		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("item4", "1", amount2 + "", "M", "", "", "2.1", "112");
		parentMap.put("item4", detailsMap4);

		// Item5|1|3|M|113|1001|1000|2.2
		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("item5", "1", amount1 + "", "M", "", "", "2.2", "113");
		parentMap.put("item5", detailsMap5);

		// Item7|1|12|M|102|1001|1000|3.0
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("item7", "1", "12", "M", "", "", "3.0", "102");
		parentMap.put("item7", detailsMap7);

		// Item8|1|4|M|111|1001|1000|3.1
		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("item8", "1", "4", "M", "", "", "3.1", "111");
		parentMap.put("item8", detailsMap8);

		// Item9|1|6|M|112|1001|1000|3.2
		detailsMap9 = pageObj.endpoints().getRecieptDetailsMap("item9", "1", "6", "M", "", "", "3.2", "112");
		parentMap.put("item9", detailsMap9);

		// Item10|1|5|M|113|1001|1000|3.3
		detailsMap10 = pageObj.endpoints().getRecieptDetailsMap("item10", "1", "5", "M", "", "", "3.3", "113");
		parentMap.put("item10", detailsMap10);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		utils.logPass("AUTH add discount to basket is successful");

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with process redemption of basket");
		double actualTotalAmount = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualTotalAmount, expTotal1,
				actualTotalAmount + " actual Total amount is not matched with expected total " + expTotal1);
		utils.logPass("actual Total amount is matched with expected total " + expTotal1);

//		// Delete LIS 1
//		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
//				.replace("${businessID}", businessID);
//		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
//		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
//		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
//		logger.info(lisName1 + " LIS is deleted successfully");
//		
//		// deleting QC and redeemable from DB
//		Response response2 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"),
//				"query", redeemableName21, "1", "5");
//
//		deleteQCandRedeemableFromDB(response21);		

	}

	@Test(description = "OMM-T4801 Verify LIS functionality with modifier quantity greater than or equal to a specified value", groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void validate_OMM_T4801() throws Exception {
		double amount1 = 3.0;
		double amount2 = 7.0;
		double amount3 = 10.0;
		double amount4 = 3.0;
		double amount5 = 7.0;
		double amount6 = 10.0;
		double amount7 = 10.0;
		double amount8 = 3.0;

		double expTotal1 = amount1 + amount2 + amount3 + amount4 + amount5 + amount6 + amount7;

		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
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

		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 3, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

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

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();
		Map<String, String> detailsMap9 = new HashMap<String, String>();
		Map<String, String> detailsMap10 = new HashMap<String, String>();

		// String item_name, String item_qty, String amount, String item_type, String
		// item_family, String item_group, String serial_number, String item_id
		// Item1|1|10|M|101|1001|1000|1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("item1", "1", "10", "M", "", "", "1.0", "101");
		parentMap.put("item1", detailsMap1);

		// Item2|1|8|M|111|1001|1000|1.1
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("item2", "1", "8", "M", "", "", "1.1", "111");
		parentMap.put("item2", detailsMap2);

		// Item3|1|10|M|102|1001|1000|2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("item3", "1", amount3 + "", "M", "", "", "2.0", "102");
		parentMap.put("item3", detailsMap3);

		// Item4|1|1|M|112|1001|1000|2.1
		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("item4", "1", amount2 + "", "M", "", "", "2.1", "112");
		parentMap.put("item4", detailsMap4);

		// Item5|1|3|M|113|1001|1000|2.2
		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("item5", "1", amount4 + "", "M", "", "", "2.2", "113");
		parentMap.put("item5", detailsMap5);

		// Item7|1|12|M|102|1001|1000|3.0
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("item7", "1", amount5 + "", "M", "", "", "3.0", "102");
		parentMap.put("item7", detailsMap7);

		// Item8|1|4|M|111|1001|1000|3.1
		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("item8", "1", amount6 + "", "M", "", "", "3.1", "111");
		parentMap.put("item8", detailsMap8);

		// Item9|1|6|M|112|1001|1000|3.2
		detailsMap9 = pageObj.endpoints().getRecieptDetailsMap("item9", "1", amount7 + "", "M", "", "", "3.2", "112");
		parentMap.put("item9", detailsMap9);

		// Item10|1|5|M|113|1001|1000|3.3
		detailsMap10 = pageObj.endpoints().getRecieptDetailsMap("item10", "1", amount8 + "", "M", "", "", "3.3", "113");
		parentMap.put("item10", detailsMap10);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		utils.logPass("AUTH add discount to basket is successful");

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with process redemption of basket");
		double actualTotalAmount = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualTotalAmount, expTotal1,
				actualTotalAmount + " actual Total amount is not matched with expected total " + expTotal1);
		utils.logPass("actual Total amount is matched with expected total " + expTotal1);

	}

	@Test(description = "OMM-T4802 Verify LIS functionality with modifier quantity less than or equal to a specified value", groups = {
			"unstable", "regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void validate_OMM_T4802() throws Exception {
		double amount1 = 3.0;
		double amount2 = 7.0;
		double amount3 = 10.0;
		double amount4 = 3.0;
		double amount5 = 7.0;
		double amount7 = 10.0;
		double amount8 = 3.0;
		double amount9 = 10.0;
		double amount10 = 3.0;

		double expTotal1 = amount1 + amount2 + amount3 + amount4 + amount5;

		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
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

		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 3, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

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

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();
		Map<String, String> detailsMap9 = new HashMap<String, String>();
		Map<String, String> detailsMap10 = new HashMap<String, String>();

		// String item_name, String item_qty, String amount, String item_type, String
		// item_family, String item_group, String serial_number, String item_id
		// Item1|1|10|M|101|1001|1000|1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("item1", "1", amount1 + "", "M", "", "", "1.0", "101");
		parentMap.put("item1", detailsMap1);

		// Item2|1|8|M|111|1001|1000|1.1
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("item2", "1", amount2 + "", "M", "", "", "1.1", "111");
		parentMap.put("item2", detailsMap2);

		// Item3|1|10|M|102|1001|1000|2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("item3", "1", amount3 + "", "M", "", "", "2.0", "102");
		parentMap.put("item3", detailsMap3);

		// Item4|1|1|M|112|1001|1000|2.1
		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("item4", "1", amount4 + "", "M", "", "", "2.1", "112");
		parentMap.put("item4", detailsMap4);

		// Item5|1|3|M|113|1001|1000|2.2
		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("item5", "1", amount5 + "", "M", "", "", "2.2", "113");
		parentMap.put("item5", detailsMap5);

		// Item7|1|12|M|102|1001|1000|3.0
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("item7", "1", amount7 + "", "M", "", "", "3.0", "102");
		parentMap.put("item7", detailsMap7);

		// Item8|1|4|M|111|1001|1000|3.1
		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("item8", "1", amount8 + "", "M", "", "", "3.1", "111");
		parentMap.put("item8", detailsMap8);

		// Item9|1|6|M|112|1001|1000|3.2
		detailsMap9 = pageObj.endpoints().getRecieptDetailsMap("item9", "1", amount9 + "", "M", "", "", "3.2", "112");
		parentMap.put("item9", detailsMap9);

		// Item10|1|5|M|113|1001|1000|3.3
		detailsMap10 = pageObj.endpoints().getRecieptDetailsMap("item10", "1", amount10 + "", "M", "", "", "3.3",
				"113");
		parentMap.put("item10", detailsMap10);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		utils.logPass("AUTH add discount to basket is successful");

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with process redemption of basket");
		double actualTotalAmount = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualTotalAmount, expTotal1,
				actualTotalAmount + " actual Total amount is not matched with expected total " + expTotal1);
		utils.logPass("actual Total amount is matched with expected total " + expTotal1);

	}

	@Test(description = "OMM-T4803 Verify LIS functionality with exact modifier quantity and amount. "
			+ "OMM-T4804 Verify LIS functionality with modifier quantity and amount greater than or equal to specified values. "
			+ "OMM-T4805 Verify LIS functionality with modifier quantity and amount less than or equal to specified values.", groups = {
					"unstable", "regression", "dailyrun" }, dataProvider = "validateOMMT4803DataProvider")
	@Owner(name = "Shashank Sharma")
	public void validate_OMM_T4803(String operatorName, String baseItemClausesDP) throws Exception {

		Map<String, Double> amounts = new HashMap<>();
		amounts.put("amount1", 3.0);
		amounts.put("amount2", 7.0);
		amounts.put("amount3", 10.0);
		amounts.put("amount4", 3.0);
		amounts.put("amount5", 7.0);
		amounts.put("amount7", 10.0);
		amounts.put("amount8", 3.0);
		amounts.put("amount9", 10.0);
		amounts.put("amount10", 3.0);

		double expTotal1 = 0;

		switch (operatorName) {
		case "isEqualTo":
			expTotal1 = amounts.get("amount3") + amounts.get("amount4") + amounts.get("amount5");
			break;

		case "greaterThanEqualTo":
			expTotal1 = amounts.get("amount3") + amounts.get("amount4") + amounts.get("amount5")
					+ amounts.get("amount7") + amounts.get("amount8") + amounts.get("amount9")
					+ amounts.get("amount10");
			break;
		case "lessThanEqualTo":
			expTotal1 = amounts.get("amount1") + amounts.get("amount2") + amounts.get("amount3")
					+ amounts.get("amount4") + amounts.get("amount5");
			break;
		default:
			break;
		}

		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
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

		String baseItemClauses1 = baseItemClausesDP;

		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 3, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

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

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();
		Map<String, String> detailsMap9 = new HashMap<String, String>();
		Map<String, String> detailsMap10 = new HashMap<String, String>();

		// String item_name, String item_qty, String amount, String item_type, String
		// item_family, String item_group, String serial_number, String item_id
		// Item1|1|10|M|101|1001|1000|1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("item1", "1", amounts.get("amount1") + "", "M", "", "",
				"1.0", "101");
		parentMap.put("item1", detailsMap1);

		// Item2|1|8|M|111|1001|1000|1.1
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("item2", "1", amounts.get("amount2") + "", "M", "", "",
				"1.1", "111");
		parentMap.put("item2", detailsMap2);

		// Item3|1|10|M|102|1001|1000|2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("item3", "1", amounts.get("amount3") + "", "M", "", "",
				"2.0", "102");
		parentMap.put("item3", detailsMap3);

		// Item4|1|1|M|112|1001|1000|2.1
		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("item4", "1", amounts.get("amount4") + "", "M", "", "",
				"2.1", "112");
		parentMap.put("item4", detailsMap4);

		// Item5|1|3|M|113|1001|1000|2.2
		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("item5", "1", amounts.get("amount5") + "", "M", "", "",
				"2.2", "113");
		parentMap.put("item5", detailsMap5);

		// Item7|1|12|M|102|1001|1000|3.0
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("item7", "1", amounts.get("amount7") + "", "M", "", "",
				"3.0", "102");
		parentMap.put("item7", detailsMap7);

		// Item8|1|4|M|111|1001|1000|3.1
		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("item8", "1", amounts.get("amount8") + "", "M", "", "",
				"3.1", "111");
		parentMap.put("item8", detailsMap8);

		// Item9|1|6|M|112|1001|1000|3.2
		detailsMap9 = pageObj.endpoints().getRecieptDetailsMap("item9", "1", amounts.get("amount9") + "", "M", "", "",
				"3.2", "112");
		parentMap.put("item9", detailsMap9);

		// Item10|1|5|M|113|1001|1000|3.3
		detailsMap10 = pageObj.endpoints().getRecieptDetailsMap("item10", "1", amounts.get("amount10") + "", "M", "",
				"", "3.3", "113");
		parentMap.put("item10", detailsMap10);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		utils.logPass("AUTH add discount to basket is successful");

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with process redemption of basket");
		double actualTotalAmount = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualTotalAmount, expTotal1,
				actualTotalAmount + " actual Total amount is not matched with expected total " + expTotal1);
		utils.logPass("actual Total amount is matched with expected total " + expTotal1);

	}

	@DataProvider(name = "T5654_DataProvider")
	public Object[][] getDataForTestCase() {
		return new Object[][] {
				// filter_item_set == base_only, modifiers_only and base_and_modifiers
				{ "base_only" }, { "modifiers_only" }, { "base_and_modifiers" } };
	}

	@DataProvider(name = "validateOMMT4803DataProvider")
	public Object[][] validateOMMT4803DataProvider() {
		return new Object[][] { { "isEqualTo", "item_id,in,101$102/modifiers_quantity,==,2/modifiers_amount,==,10" },
				{ "greaterThanEqualTo", "item_id,in,101$102/modifiers_quantity,>=,2/modifiers_amount,>=,10" },
				{ "lessThanEqualTo", "item_id,in,101$102/modifiers_quantity,<=,2/modifiers_amount,<=,10" } };
	}

	@Test(description = "OMM-T4806 Verify LIS functionality with modifier conditions and additional item qualifier"
			+ "OMM-T4807 Verify LIS functionality with modifier conditions, min price, and net quantity condition "
			+ "OMM-T4808 Verify LIS functionality with modifier conditions, max price, and net quantity condition"
			+ "OMM-T4809 Verify LIS functionality with base item quantity and multiple modifier conditions"
			+ "OMM-T4810 Verify LIS functionality with multiple quantity conditions for base items and modifiers", groups = {
					"unstable", "regression", "dailyrun" }, dataProvider = "validate_OMM_T4806_dataprovider")
	@Owner(name = "Shashank Sharma")
	public void validate_OMM_T4806(String testCaseID, String operatorName, String baseItemClausesDP,
			String lineItemFiltersVar, String itemQualifierVar) throws Exception {
		utils.logInfo("enable_decoupled_redemption_engine flag current value is: " + decoupledFlagStatus);

		Map<String, Double> amounts = new HashMap<>();
		amounts.put("amount1", 10.0);
		amounts.put("amount2", 8.0);
		amounts.put("amount3", 10.0);
		amounts.put("amount4", 1.0);
		amounts.put("amount5", 3.0);
		amounts.put("amount7", 12.0);
		amounts.put("amount8", 4.0);
		amounts.put("amount9", 6.0);
		amounts.put("amount10", 5.0);
		amounts.put("amount11", 8.0);
		amounts.put("amount12", 1.0);
		amounts.put("amount14", 2.0);
		amounts.put("amount15", 1.0);

		double expTotal1 = 0;
		operatorName = testCaseID + "_" + operatorName;

		switch (operatorName) {

		case "isEqualTo":
			expTotal1 = amounts.get("amount3") + amounts.get("amount4") + amounts.get("amount5");
			break;

		case "greaterThanEqualTo":
			expTotal1 = amounts.get("amount3") + amounts.get("amount4") + amounts.get("amount5")
					+ amounts.get("amount7") + amounts.get("amount8") + amounts.get("amount9")
					+ amounts.get("amount10");
			break;

		case "T4806_net_quantity_greater_than_or_equal_to":
			expTotal1 = amounts.get("amount3") + amounts.get("amount4") + amounts.get("amount5");
			break;

		case "T4807_net_quantity_greater_than_or_equal_to":
			expTotal1 = amounts.get("amount3") + amounts.get("amount11") + amounts.get("amount4")
					+ amounts.get("amount5") + amounts.get("amount12") + amounts.get("amount14")
					+ amounts.get("amount15");
			break;

		case "T4808_net_quantity_greater_than_or_equal_to":
			expTotal1 = 46.0;
			break;

		case "T4809_net_quantity_greater_than_or_equal_to":
			expTotal1 = 14.0;
			break;

		case "T4811_net_quantity_greater_than_or_equal_to":
			expTotal1 = 14.0;
			break;

		default:
			break;
		}

		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_6508_" + CreateDateTime.getTimeDateString();
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

		String baseItemClauses1 = baseItemClausesDP;

		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 3, "", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		utils.logPass("LIS has been created successfully");
		map.put("amount_cap", "0");

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers

		String itemQualifier = itemQualifierVar.replace("${actualExternalIdLIS}", actualExternalIdLIS);
		map.put("item_qualifiers", itemQualifier);

		String line_item_filters = lineItemFiltersVar.replace("${actualExternalIdLIS}", actualExternalIdLIS);
		map.put("line_item_filters", line_item_filters);

		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"", processingFunction, "100", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();
		Map<String, String> detailsMap3 = new HashMap<String, String>();
		Map<String, String> detailsMap4 = new HashMap<String, String>();
		Map<String, String> detailsMap5 = new HashMap<String, String>();
		Map<String, String> detailsMap7 = new HashMap<String, String>();
		Map<String, String> detailsMap8 = new HashMap<String, String>();
		Map<String, String> detailsMap9 = new HashMap<String, String>();
		Map<String, String> detailsMap10 = new HashMap<String, String>();
		Map<String, String> detailsMap11 = new HashMap<String, String>();
		Map<String, String> detailsMap12 = new HashMap<String, String>();
		Map<String, String> detailsMap14 = new HashMap<String, String>();
		Map<String, String> detailsMap15 = new HashMap<String, String>();

		// String item_name, String item_qty, String amount, String item_type, String
		// item_family, String item_group, String serial_number, String item_id
		// Item1|1|10|M|101|1001|1000|1.0
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("item1", "2", amounts.get("amount1") + "", "M", "", "",
				"1.0", "101");
		parentMap.put("item1", detailsMap1);

		// Item2|1|8|M|111|1001|1000|1.1
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("item2", "1", amounts.get("amount2") + "", "M", "", "",
				"1.1", "111");
		parentMap.put("item2", detailsMap2);

		// Item3|1|10|M|102|1001|1000|2.0
		detailsMap3 = pageObj.endpoints().getRecieptDetailsMap("item3", "2", amounts.get("amount3") + "", "M", "", "",
				"2.0", "102");
		parentMap.put("item3", detailsMap3);

		// Item4|1|1|M|112|1001|1000|2.1
		detailsMap4 = pageObj.endpoints().getRecieptDetailsMap("item4", "1", amounts.get("amount4") + "", "M", "", "",
				"2.1", "112");
		parentMap.put("item4", detailsMap4);

		// Item5|1|3|M|113|1001|1000|2.2
		detailsMap5 = pageObj.endpoints().getRecieptDetailsMap("item5", "1", amounts.get("amount5") + "", "M", "", "",
				"2.2", "113");
		parentMap.put("item5", detailsMap5);

		// Item7|1|12|M|102|1001|1000|3.0
		detailsMap7 = pageObj.endpoints().getRecieptDetailsMap("item7", "1", amounts.get("amount7") + "", "M", "", "",
				"3.0", "102");
		parentMap.put("item7", detailsMap7);

		// Item8|1|4|M|111|1001|1000|3.1
		detailsMap8 = pageObj.endpoints().getRecieptDetailsMap("item8", "1", amounts.get("amount8") + "", "M", "", "",
				"3.1", "111");
		parentMap.put("item8", detailsMap8);

		// Item9|1|6|M|112|1001|1000|3.2
		detailsMap9 = pageObj.endpoints().getRecieptDetailsMap("item9", "1", amounts.get("amount9") + "", "M", "", "",
				"3.2", "112");
		parentMap.put("item9", detailsMap9);

		// Item10|1|5|M|113|1001|1000|3.3
		detailsMap10 = pageObj.endpoints().getRecieptDetailsMap("item10", "1", amounts.get("amount10") + "", "M", "",
				"", "3.3", "113");
		parentMap.put("item10", detailsMap10);

		// Item11|1|8|M|102|1001|1000|4.0
		detailsMap11 = pageObj.endpoints().getRecieptDetailsMap("item11", "1", amounts.get("amount11") + "", "M", "",
				"", "4.0", "102");
		parentMap.put("item11", detailsMap11);

		// Item12|1|1|M|111|1001|1000|4.1
		detailsMap12 = pageObj.endpoints().getRecieptDetailsMap("item12", "1", amounts.get("amount12") + "", "M", "",
				"", "4.1", "111");
		parentMap.put("item12", detailsMap12);

		// Item14|1|2|M|112|1001|1000|4.2
		detailsMap14 = pageObj.endpoints().getRecieptDetailsMap("item14", "1", amounts.get("amount14") + "", "M", "",
				"", "4.2", "112");
		parentMap.put("item14", detailsMap14);

		// Item15|1|1|M|113|1001|1000|4.3
		detailsMap15 = pageObj.endpoints().getRecieptDetailsMap("item15", "1", amounts.get("amount15") + "", "M", "",
				"", "4.3", "113");
		parentMap.put("item15", detailsMap15);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

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
		utils.logPass("AUTH add discount to basket is successful");

		String subAmount = Integer.toString(Utilities.getRandomNoFromRange(100, 150));

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationkey"), userID, subAmount, parentMap);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with process redemption of basket");
		double actualTotalAmount = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success.discount_amount").replace("[", "").replace("]", ""));
		if(testCaseID.equalsIgnoreCase("T4808")) {
			expTotal1 = Double.parseDouble(dataSet.get("expectedDiscountAmt_" + testCaseID + decoupledFlagStatusStr));
		}
		Assert.assertEquals(actualTotalAmount, expTotal1,
				actualTotalAmount + " actual Total amount is not matched with expected total " + expTotal1);
		utils.logPass("actual Total amount is matched with expected total " + expTotal1);

	}

	@DataProvider(name = "validate_OMM_T4806_dataprovider")
	public Object[][] validate_OMM_T4806_dataprovider() {
		return new Object[][] { { "T4806", "net_quantity_greater_than_or_equal_to",
				"item_id,in,101$102/modifiers_quantity,<=,2/modifiers_amount,<=,4",
				"[{\"line_item_selector_id\":\"${actualExternalIdLIS}\",\"processing_method\":\"\",\"quantity\":\"1\"}]",
				"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \"${actualExternalIdLIS}\",\"net_value\":2}]" },
				{ "T4807", "net_quantity_greater_than_or_equal_to",
						"item_id,in,101$102/modifiers_quantity,>=,2/modifiers_amount,>=,4",
						"[{\"line_item_selector_id\":\"${actualExternalIdLIS}\",\"processing_method\":\"min_price\",\"quantity\":\"3\"}]",
						"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \"${actualExternalIdLIS}\",\"net_value\":2}]" },
				{ "T4808", "net_quantity_greater_than_or_equal_to",
						"item_id,in,101$102/modifiers_quantity,>=,2/modifiers_amount,>=,4",
						"[{\"line_item_selector_id\":\"${actualExternalIdLIS}\",\"processing_method\":\"max_price\",\"quantity\":\"3\"}]",
						"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \"${actualExternalIdLIS}\",\"net_value\":2}]" },
				{ "T4809", "net_quantity_greater_than_or_equal_to",
						"item_id,in,101$102/quantity,>=,2/modifiers_quantity,>=,2/modifiers_amount,>=,4",
						"[{\"line_item_selector_id\":\"${actualExternalIdLIS}\",\"processing_method\":\"min_price\",\"quantity\":\"3\"}]",
						"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \"${actualExternalIdLIS}\",\"net_value\":2}]" },
				{ "T4811", "net_quantity_greater_than_or_equal_to",
						"item_id,in,101$102/item_total_amount,>=,10/quantity,>=,2/modifiers_quantity,>=,2/modifiers_amount,>=,4",
						"[{\"line_item_selector_id\":\"${actualExternalIdLIS}\",\"processing_method\":\"min_price\",\"quantity\":\"3\"}]",
						"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \"${actualExternalIdLIS}\",\"net_value\":2}]" } };
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}