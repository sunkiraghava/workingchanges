package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.annotations.Owner;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;

import io.restassured.response.Response;

/*
 * @Author : vansham mishra
 */

@Listeners(TestListeners.class)

public class OfferIngestion_WithInvalidTimeFormatTest {
	static Logger logger = LogManager.getLogger(OfferIngestion_WithInvalidTimeFormatTest.class);
	public WebDriver driver;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	public Utilities utils;
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();

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

	@Test(description = "SQ-T5493 Create active deal with end time", groups = { "regression", "dailyrun" })
	public void T5493_ValidateStartTimeAndEndTimeInValidDateTimeFormatPart1() throws Exception {
		String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(3, "yyyy-MM-dd'T'HH:mm:ss");
		String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String futureStartTime = CreateDateTime.getFutureDateTimeInGivenFormate(2, "yyyy-MM-dd'T'HH:mm:ss");

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
		map.put("active_now", "false");
		map.put("expiry_days", null);
		map.put("start_time", futureStartTime);

		// Added redeemable with existing QC
		Response response = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage = response.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is created successfully");
		utils.logPass("Deal having future start time has been created");
		Response resp = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"), "query",
				redeemableName, "1", "1");
		deleteRedeemable(resp);

//      creating deal having start time from past
		String redeemableName2 = "AutomationRedeemableExistingQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName2);
		map.put("start_time", "2023-06-25T23:59:59");
		Response response2 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage2 = response2.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage2, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName + " redeemable is not created");
		String errorMsg2 = response2.jsonPath().get("results[0].errors").toString();
		Assert.assertEquals(errorMsg2, "[Start time cannot be in the past.]", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time from the past");
		utils.longWaitInSeconds(5);

		// start time greater than the end time
		String redeemableName3 = "AutomationRedeemableExistingQC3_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName3);
		map.put("start_time", "2029-06-25T23:59:59");
		map.put("end_time", "2023-06-25T23:59:59");
		Response response3 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableInvalidStartTimeSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateQcErrorSchema, response3.asString());
		Assert.assertTrue(isCreateRedeemableInvalidStartTimeSchemaValidated,
				"Create Redeemable with invalid start time Schema Validation failed");
		boolean successMessage3 = response3.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage3, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName3 + " redeemable is not created");
		String errorMsg3 = response3.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg3, "Start time must be before the end time.", "Error message doesn't match");
		String errorMsg4 = response3.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg4, "End time cannot be in the past.", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when start time less than the end time");

		// day gr than 30 for sep and mar for start
		String redeemableName4 = "AutomationRedeemableExistingQC4_API_" + CreateDateTime.getTimeDateString();
		String QCName4 = "AutomationQC4_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName4);
		map.put("redeemableName", redeemableName4);
		map.put("start_time", "2024-09-31T23:59:59");
		map.put("end_time", "2025-03-31T23:59:59");
		Response response4 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response4.getStatusCode(), 200);
		boolean successMessage4 = response4.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage4, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName4 + " redeemable is not created");
		String errorMsg5 = response4.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg5, "Start time Invalid", "Error message doesn't match");
		String errorMsg6 = response4.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg6, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time greater than 30 for sep month");
		String redeemableName5 = "AutomationRedeemableExistingQC5_API_" + CreateDateTime.getTimeDateString();
		String QCName5 = "AutomationQC5_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName5);
		map.put("redeemableName", redeemableName5);
		map.put("start_time", "2024-03-32T23:59:59");
		Response response5 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response5.getStatusCode(), 200);
		boolean successMessage5 = response5.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage5, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName5 + " redeemable is not created");
		String errorMsg7 = response5.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg7, "Start time Invalid", "Error message doesn't match");
		String errorMsg8 = response5.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg8, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time greater than 31 for march month");

		// day less than 1
		String redeemableName6 = "AutomationRedeemableExistingQC6_API_" + CreateDateTime.getTimeDateString();
		String QCName6 = "AutomationQC6_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName6);
		map.put("redeemableName", redeemableName6);
		map.put("start_time", "2027-01-00T23:59:59");
		map.put("end_time", "2025-03-31T23:59:59");
		Response response6 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response6.getStatusCode(), 200);
		boolean successMessage6 = response6.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage6, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName6 + " redeemable is not created");
		String errorMsg9 = response6.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg9, "Start time Invalid", "Error message doesn't match");
		String errorMsg10 = response6.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg10, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time 00");

		// month greater than 12
		String redeemableName7 = "AutomationRedeemableExistingQC7_API_" + CreateDateTime.getTimeDateString();
		String QCName7 = "AutomationQC7_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName7);
		map.put("redeemableName", redeemableName7);
		map.put("start_time", "2027-13-00T23:59:59");
		map.put("end_time", "2025-03-31T23:59:59");
		Response response7 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response7.getStatusCode(), 200);
		boolean successMessage7 = response7.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage7, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName7 + " redeemable is not created");
		String errorMsg11 = response7.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg11, "Start time Invalid", "Error message doesn't match");
		String errorMsg12 = response7.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg12, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time with month greater than 12");

		// month less than 1 in start time
		String redeemableName8 = "AutomationRedeemableExistingQC8_API_" + CreateDateTime.getTimeDateString();
		String QCName8 = "AutomationQC8_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName8);
		map.put("redeemableName", redeemableName8);
		map.put("start_time", "2027-00-12T23:59:59");
		map.put("end_time", "2025-03-31T23:59:59");
		Response response8 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response8.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage8 = response8.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage8, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName8 + " redeemable is not created");
		String errorMsg13 = response8.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg13, "Start time Invalid", "Error message doesn't match");
		String errorMsg14 = response8.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg14, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having start time with month less than 1");

		// month less than 1 in end time
		String redeemableName9 = "AutomationRedeemableExistingQC9_API_" + CreateDateTime.getTimeDateString();
		String QCName9 = "AutomationQC9_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName9);
		map.put("redeemableName", redeemableName9);
		map.put("start_time", "2027-12-06T23:59:59");
		map.put("end_time", "2025-00-31T23:59:59");
		Response response9 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response9.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage9 = response9.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage9, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName9 + " redeemable is not created");
		String errorMsg15 = response9.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg15, "End time Invalid", "Error message doesn't match");
		String errorMsg16 = response9.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg16,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable having end time with month less than 1");

		// seconds missing from start time
		String redeemableName10 = "AutomationRedeemableExistingQC10_API_" + CreateDateTime.getTimeDateString();
		String QCName10 = "AutomationQC10_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName10);
		map.put("redeemableName", redeemableName10);
		map.put("start_time", "2027-12-06T23:59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response10 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response10.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage10 = response10.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage10, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName10 + " redeemable is not created");
		String errorMsg17 = response10.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg17, "Start time Invalid", "Error message doesn't match");
		String errorMsg18 = response10.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg18, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when second is missing from start_time time format");

		// minutes missing from start time
		String redeemableName11 = "AutomationRedeemableExistingQC11_API_" + CreateDateTime.getTimeDateString();
		String QCName11 = "AutomationQC11_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName11);
		map.put("redeemableName", redeemableName11);
		map.put("start_time", "2027-12-06T23::59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response11 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response11.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage11 = response11.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage11, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName11 + " redeemable is not created");
		String errorMsg19 = response11.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg19, "Start time Invalid", "Error message doesn't match");
		String errorMsg20 = response11.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg18, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when minute is missing from start_time time format");

		// minutes missing from start time
		String redeemableName12 = "AutomationRedeemableExistingQC12_API_" + CreateDateTime.getTimeDateString();
		String QCName12 = "AutomationQC12_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName12);
		map.put("redeemableName", redeemableName12);
		map.put("start_time", "2027-12-06T23::59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response12 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response12.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage12 = response12.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage12, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName12 + " redeemable is not created");
		String errorMsg21 = response12.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg21, "Start time Invalid", "Error message doesn't match");
		String errorMsg22 = response12.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg22, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when minute is missing from start_time time format");

		// hours missing from start time
		String redeemableName13 = "AutomationRedeemableExistingQC13_API_" + CreateDateTime.getTimeDateString();
		String QCName13 = "AutomationQC13_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName13);
		map.put("redeemableName", redeemableName13);
		map.put("start_time", "2027-12-06T:59:59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response13 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response13.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage13 = response13.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage13, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName13 + " redeemable is not created");
		String errorMsg23 = response13.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg23, "Start time Invalid", "Error message doesn't match");
		String errorMsg24 = response13.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg24, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when hours is missing from start_time time format");

		// negative hour value in start time
		String redeemableName14 = "AutomationRedeemableExistingQC14_API_" + CreateDateTime.getTimeDateString();
		String QCName14 = "AutomationQC14_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName14);
		map.put("redeemableName", redeemableName14);
		map.put("start_time", "2027-12-06T-23:59:59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response14 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response14.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage14 = response14.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage14, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName14 + " redeemable is not created");
		String errorMsg25 = response14.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg25, "Start time Invalid", "Error message doesn't match");
		String errorMsg26 = response14.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg24, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when hours is missing from start_time time format");

		// negative minute value in start time
		String redeemableName15 = "AutomationRedeemableExistingQC15_API_" + CreateDateTime.getTimeDateString();
		String QCName15 = "AutomationQC15_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName15);
		map.put("redeemableName", redeemableName15);
		map.put("start_time", "2027-12-06T23:-59:59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response15 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response15.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage15 = response15.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage15, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName15 + " redeemable is not created");
		String errorMsg27 = response15.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg27, "Start time Invalid", "Error message doesn't match");
		String errorMsg28 = response15.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg28, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when hours is missing from start_time time format");

		// negative seconds value in start time
		String redeemableName16 = "AutomationRedeemableExistingQC16_API_" + CreateDateTime.getTimeDateString();
		String QCName16 = "AutomationQC16_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName16);
		map.put("redeemableName", redeemableName16);
		map.put("start_time", "2027-12-06T23:59:-59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response16 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response16.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage16 = response16.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage16, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName16 + " redeemable is not created");
		String errorMsg29 = response16.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg29, "Start time Invalid", "Error message doesn't match");
		String errorMsg30 = response16.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg30, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when hours is missing from start_time time format");
	}

	@Test(description = "SQ-T5493 Create active deal with end time", groups = { "regression", "dailyrun" })
	public void T5493_ValidateStartTimeAndEndTimeInValidDateTimeFormatPart2() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
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
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "false");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName17 = "AutomationRedeemableExistingQC17_API_" + CreateDateTime.getTimeDateString();
		String QCName17 = "AutomationQC17_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName17);
		map.put("redeemableName", redeemableName17);
		map.put("start_time", "2026-02-30T23:59:59");
		map.put("end_time", "2027-06-25T23:59:59");
		Response response17 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response17.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage17 = response17.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage17, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName17 + " redeemable is not created");
		String errorMsg31 = response17.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg31, "Start time Invalid", "Error message doesn't match");
		String errorMsg32 = response17.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg32, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		utils.logPass(
				"user is not able to create the redeemable when invalid leap year date is provided for start_time time format");

		// hours out of range
		String redeemableName18 = "AutomationRedeemableExistingQC18_API_" + CreateDateTime.getTimeDateString();
		String QCName18 = "AutomationQC18_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName18);
		map.put("redeemableName", redeemableName18);
		map.put("start_time", "2027-12-12T25:59:59");
		map.put("end_time", "2027-12-13T25:59:59");
		Response response18 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response18.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableInvalidHoursSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateQcErrorSchema, response18.asString());
		Assert.assertTrue(isCreateRedeemableInvalidHoursSchemaValidated,
				"Create Redeemable with hours out of range Schema Validation failed");
		boolean successMessage18 = response18.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage18, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName18 + " redeemable is not created");
		String errorMsg33 = response18.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg33, "Start time Invalid", "Error message doesn't match");
		String errorMsg34 = response18.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg34, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsg35 = response18.jsonPath().get("results[0].errors[3]").toString();
		Assert.assertEquals(errorMsg35,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs35 = response18.jsonPath().get("results[0].errors[2]").toString();
		Assert.assertEquals(errorMsgs35, "End time Invalid", "Error message doesn't match");
		utils.logPass(
				"user is not able to create the redeemable when hours is out of range from start_time and end_time time format");

		// minutes out of range
		String redeemableName19 = "AutomationRedeemableExistingQC19_API_" + CreateDateTime.getTimeDateString();
		String QCName19 = "AutomationQC19_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName19);
		map.put("redeemableName", redeemableName19);
		map.put("start_time", "2027-12-12T23:60:59");
		map.put("end_time", "2027-12-13T23:60:59");
		Response response19 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response19.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage19 = response19.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage19, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName19 + " redeemable is not created");
		String errorMsg36 = response19.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg36, "Start time Invalid", "Error message doesn't match");
		String errorMsg37 = response19.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg37, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsg38 = response19.jsonPath().get("results[0].errors[3]").toString();
		Assert.assertEquals(errorMsg38,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs36 = response18.jsonPath().get("results[0].errors[2]").toString();
		Assert.assertEquals(errorMsgs36, "End time Invalid", "Error message doesn't match");
		utils.logPass(
				"user is not able to create the redeemable when minutes is out of range from start_time and end_time time format");

		// Edge of day boundary
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		String QCName20 = "AutomationQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName20);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName20 + " redeemable is created successfully");
		utils.logPass("Deal with edge of day boundary for start time has been created");
		Response resp20 = pageObj.endpoints().getLisOfRedeemableUsingApiWithPagePerItem(dataSet.get("apiKey"), "query",
				redeemableName20, "1", "5");
		deleteRedeemable(resp20);

		// leap year end date
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
		String QCName21 = "AutomationQC21_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName21);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2028-02-26T23:59:59");
		map.put("end_time", "2028-02-30T23:59:59");
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName21 + " redeemable is not created");
		String errorMsg39 = response21.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg39, "End time Invalid", "Error message doesn't match");
		String errorMsg40 = response21.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg40,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		utils.logPass(
				"user is not able to create the redeemable when invalid leap year date is provided for end_time time format");

		// without T
		String redeemableName22 = "AutomationRedeemableExistingQC22_API_" + CreateDateTime.getTimeDateString();
		String QCName22 = "AutomationQC22_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName22);
		map.put("redeemableName", redeemableName22);
		map.put("start_time", "2028-12-1223:59:59");
		map.put("end_time", "2028-12-1323:59:59");
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName22 + " redeemable is not created");
		String errorMsg41 = response22.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg41, "Start time Invalid", "Error message doesn't match");
		String errorMsg42 = response22.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg42, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsg43 = response22.jsonPath().get("results[0].errors[3]").toString();
		Assert.assertEquals(errorMsg43,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs44 = response22.jsonPath().get("results[0].errors[2]").toString();
		Assert.assertEquals(errorMsgs44, "End time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when t is missing from time format");

		// missing time from start date and end date
		String redeemableName23 = "AutomationRedeemableExistingQC23_API_" + CreateDateTime.getTimeDateString();
		String QCName23 = "AutomationQC23_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName23);
		map.put("redeemableName", redeemableName23);
		map.put("start_time", "2028-12-12");
		map.put("end_time", "2028-12-13");
		Response response23 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response23.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage23 = response23.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage23, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName23 + " redeemable is not created");
		String errorMsg44 = response23.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsg44, "Start time Invalid", "Error message doesn't match");
		String errorMsg45 = response23.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg45, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsg46 = response23.jsonPath().get("results[0].errors[3]").toString();
		Assert.assertEquals(errorMsg46,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs45 = response23.jsonPath().get("results[0].errors[2]").toString();
		Assert.assertEquals(errorMsgs45, "End time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when time is missing from time format");

		// invalid day - end_time>31
		String redeemableName24 = "AutomationRedeemableExistingQC24_API_" + CreateDateTime.getTimeDateString();
		String QCName24 = "AutomationQC24_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName24);
		map.put("redeemableName", redeemableName24);
		map.put("start_time", "2027-12-12T23:59:59");
		map.put("end_time", "2028-12-33T23:59:59");
		Response response24 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response24.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage24 = response24.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage24, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName24 + " redeemable is not created");
		String errorMsg47 = response24.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg47,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs48 = response24.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs48, "End time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when end date is greater than 31");

		// out of maximum year range
		String redeemableName25 = "AutomationRedeemableExistingQC25_API_" + CreateDateTime.getTimeDateString();
		String QCName25 = "AutomationQC25_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName25);
		map.put("redeemableName", redeemableName25);
		map.put("start_time", "2027-12-12T23:59:59");
		map.put("end_time", "10999-03-30T23:59:59");
		Response response25 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response25.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage25 = response25.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage25, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName25 + " redeemable is not created");
		String errorMsg49 = response25.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg49,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs50 = response25.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs50, "End time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when end_time is out of maximum year range");

		// SQL injection
		String redeemableName26 = "AutomationRedeemableExistingQC26_API_" + CreateDateTime.getTimeDateString();
		String QCName26 = "AutomationQC26_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName26);
		map.put("redeemableName", redeemableName26);
		map.put("start_time", "2027-12-12T23:59:59");
		map.put("end_time", "10999-03-30T23:59:59 OR '1'='1'");
		Response response26 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response26.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage26 = response26.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage26, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName26 + " redeemable is not created");
		String errorMsg51 = response26.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg51,
				"Either set 'End Time' or 'Expiry Days' for the redeemable or allow it to run indefinitely",
				"Error message doesn't match");
		String errorMsgs52 = response26.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs52, "End time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable when sql injection is passed in end_time.");

		// invalid timezone offset
		String redeemableName27 = "AutomationRedeemableExistingQC27_API_" + CreateDateTime.getTimeDateString();
		String QCName27 = "AutomationQC27_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName27);
		map.put("redeemableName", redeemableName27);
		map.put("start_time", "2027-03-31T00:59:00+25:00");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response27 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response27.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage27 = response27.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage27, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName27 + " redeemable is not created");
		String errorMsg53 = response27.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg53, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs53 = response27.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs53, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with Invalid Time Zone Offset.");
	}

	@Test(description = "SQ-T5493 Create active deal with end time", groups = { "regression", "dailyrun" })
    @Owner(name = "Vansham Mishra")
	public void T5493_ValidateStartTimeAndEndTimeInValidDateTimeFormatPart3() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
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
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "false");
		map.put("expiry_days", null);
		// invalid timezone format
		String redeemableName28 = "AutomationRedeemableExistingQC28_API_" + CreateDateTime.getTimeDateString();
		String QCName28 = "AutomationQC28_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName28);
		map.put("redeemableName", redeemableName28);
		map.put("start_time", "2025-12-12T23:59:59+AB:CD");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response28 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response28.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isCreateRedeemableInvalidTimeZoneSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiUpdateQcErrorSchema, response28.asString());
		Assert.assertTrue(isCreateRedeemableInvalidTimeZoneSchemaValidated,
				"Create Redeemable with invalid timezone Schema Validation failed");
		boolean successMessage28 = response28.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage28, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName28 + " redeemable is not created");
		String errorMsg54 = response28.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg54, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs55 = response28.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs55, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with Invalid Time Zone format.");

		// special character in date
		String redeemableName29 = "AutomationRedeemableExistingQC29_API_" + CreateDateTime.getTimeDateString();
		String QCName29 = "AutomationQC29_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName29);
		map.put("redeemableName", redeemableName29);
		map.put("start_time", "2025-12-12T23:59:59@");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response29 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response29.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage29 = response29.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage29, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName29 + " redeemable is not created");
		String errorMsg55 = response29.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg55, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs56 = response29.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs56, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with special character in start date.");

		// special character in time
		String redeemableName30 = "AutomationRedeemableExistingQC30_API_" + CreateDateTime.getTimeDateString();
		String QCName30 = "AutomationQC30_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName30);
		map.put("redeemableName", redeemableName30);
		map.put("start_time", "2025-12-12T23:59:5$");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response30 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response30.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage30 = response30.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage30, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName30 + " redeemable is not created");
		String errorMsg56 = response30.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg56, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs57 = response30.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs57, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with special character in start time.");

		// End time in past
		String redeemableName31 = "AutomationRedeemableExistingQC31_API_" + CreateDateTime.getTimeDateString();
		String QCName31 = "AutomationQC31_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName31);
		map.put("redeemableName", redeemableName31);
		map.put("start_time", "2028-12-12T23:59:59");
		map.put("end_time", "1000-03-30T23:59:59");
		Response response31 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response31.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage31 = response31.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage31, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName31 + " redeemable is not created");
		String errorMsg58 = response31.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg58, "End time cannot be in the past.", "Error message doesn't match");
		String errorMsgs59 = response31.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs59, "Start time must be before the end time.", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with end time from past");

		// Empty start time
		String redeemableName32 = "AutomationRedeemableExistingQC32_API_" + CreateDateTime.getTimeDateString();
		String QCName32 = "AutomationQC32_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName32);
		map.put("redeemableName", redeemableName32);
		map.put("start_time", "");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response32 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response32.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage32 = response32.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage32, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName32 + " redeemable is not created");
		String errorMsg59 = response32.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg59, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs60 = response32.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs60, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with empty start time.");

		// null start time
		String redeemableName33 = "AutomationRedeemableExistingQC33_API_" + CreateDateTime.getTimeDateString();
		String QCName33 = "AutomationQC33_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName33);
		map.put("redeemableName", redeemableName33);
		map.put("start_time", "null");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response33 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response33.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage33 = response33.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage33, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName33 + " redeemable is not created");
		String errorMsg61 = response33.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg61, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs62 = response33.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs62, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with null start time.");

		// boolean start time
		String redeemableName34 = "AutomationRedeemableExistingQC34_API_" + CreateDateTime.getTimeDateString();
		String QCName34 = "AutomationQC34_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName34);
		map.put("redeemableName", redeemableName34);
		map.put("start_time", "true");
		map.put("end_time", "2028-12-28T23:59:59");
		Response response34 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response34.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage34 = response34.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage34, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		utils.logPass(redeemableName34 + " redeemable is not created");
		String errorMsg62 = response34.jsonPath().get("results[0].errors[1]").toString();
		Assert.assertEquals(errorMsg62, "Either set an start date for the redeemable or allow it to active now.",
				"Error message doesn't match");
		String errorMsgs63 = response34.jsonPath().get("results[0].errors[0]").toString();
		Assert.assertEquals(errorMsgs63, "Start time Invalid", "Error message doesn't match");
		utils.logPass("user is not able to create the redeemable with boolean value in start time.");
	}

	// deleting the created redeemables
	public void deleteRedeemable(Response response) throws Exception {
		String redeemableExternalID = response.jsonPath().getString("data[0].external_id").replace("[", "").replace("]",
				"");
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalID)
				.replace("$businessID", "1043");

		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		utils.logPass(redeemableExternalID + " external redeemable is deleted successfully");
	}

	@Test(description = "SQ-T5594/OMM-T2674 Validate the validations for Hit Target Menu Item Price processing function", groups = { "regression", "dailyrun" }, priority = 2)
	public void T5594_ValidateValidationsForHitTargetMenuItemPriceProcessingFunction() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName4 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName5 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName6 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction3 = "hit_target_price";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\">=\",\"value\":\"10\"}]");
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
		utils.logPass("User is able to create QC successfully");

		// LIF gets created with sum of amounts(i.e in get qc api response processing
		// method of line item filter should be blank)
		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName3, "2", "13");
		Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		String actualProcessingMethod = getQCDetailsResponse.jsonPath()
				.getString("data[0].line_item_filters[0].processing_method");
		Assert.assertEquals(actualProcessingMethod, "", "Processing method is not null in get Qc response");
		utils.logPass("Verified that LIF is created with null processing method");

		// creating QC with "target_price": -1,
		map.put("target_price", "-1");
		Response qcCreateResponse4 = pageObj.endpoints().createQCUsingApi(adminKey, QCName4, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName4 + " QC is not created and status code is not 200");
		String errorMsg4 = qcCreateResponse4.jsonPath().getString("results[0].errors[0]");
		Assert.assertEquals(errorMsg4, "Target Unit Price must be greater than or equal to 0",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC4 = qcCreateResponse4.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC4,
				"QC success message is True on creating QC with  target_price: -1,");
		utils.logPass("User is not able to create QC, expected");

		// creating QC with "target_price": null,
		map.put("target_price", "null");
		Response qcCreateResponse5 = pageObj.endpoints().createQCUsingApi(adminKey, QCName5, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName5 + " QC is not created and status code is not 200");
		String errorMsg5 = qcCreateResponse5.jsonPath().getString("results[0].errors[0]");
		Assert.assertEquals(errorMsg5, "Target Unit Price is not a number",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC5 = qcCreateResponse5.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC5,
				"QC success message is True on creating QC with  target_price: null,");
		utils.logPass("User is not able to create QC, expected");

		// creating QC with "target_price": "ABC",
		map.put("target_price", "ABC");
		Response qcCreateResponse6 = pageObj.endpoints().createQCUsingApi(adminKey, QCName6, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", dataSet.get("locationID"), map);
		Assert.assertEquals(qcCreateResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName5 + " QC is not created and status code is not 200");
		String errorMsg6 = qcCreateResponse6.jsonPath().getString("results[0].errors[0]");
		Assert.assertEquals(errorMsg6, "Target Unit Price is not a number",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC6 = qcCreateResponse6.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC6,
				"QC success message is True on creating QC with  target_price: -1,");
		utils.logPass("User is not able to create QC, expected");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName3 + " LIS is not deleted successfully");
		utils.logPass(lisName3 + " LIS is deleted successfully");

		// Delete QC --  commenting as it is not required to delete the QC , QC not created
//		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
//				.replace("${businessID}", businessID);
//		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
//		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
//				qcID_DB2);
//		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
//				deleteQCFromQualifying_expressions);
//		Assert.assertTrue(statusQualifying_expressionsQuery, QCName3 + " QC is not deleted successfully");
//		TestListeners.extentTest.get().pass(QCName3 + " QC is deleted successfully");
//		logger.info(QCName3 + " QC is deleted successfully");

	}
	@Test(description = "SQ-T5942 Create QC>Target Bundle Price>Validate that If user has provided optional parameter-processing method for line item filter then also line item filter gets created", groups = { "regression", "dailyrun" }, priority = 2)
	public void T5942_ValidateLineItemFilterCreationWithOptionalProcessingMethod() throws Exception {

		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target";
		String processingFunction2 = "sum_amounts";
		String processingFunction3 = "hit_target_price_max_price_once";
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
				+ "\",\"processing_method\":\"max\",\"quantity\":\"1\"}]");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		// commenting below as the fix of OMM-1265
//		Response getQcResponse1 =  pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "2", "13");
//		String externalId = getQcResponse1.jsonPath().getString("data[0].external_id");
//		Assert.assertEquals(externalId, actualExternalIdQC, "LIS external id is not matching");
//		String lifProcessingMethod = getQcResponse1.jsonPath().getString("data[0].line_item_filters[0].processing_method");
//		Assert.assertEquals(lifProcessingMethod, "max", "LIF processing method is not matching");
//		logger.info("Verified that Line item filter gets created as Parameter-processing method is not required for line item filter in target bundle price");
//		TestListeners.extentTest.get().pass("Verified that Line item filter gets created as Parameter-processing method is not required for line item filter in target bundle price");
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
				+ "\",\"processing_method\":\"max\",\"quantity\":\"1\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		String lifWarningMessage = qcCreateResponse2.jsonPath().getString("results[0].warnings.line_item_filters[0].message");
		Assert.assertEquals(lifWarningMessage, "Required parameters missing or invalid: processing_method",
				"Warning message for qc creation with invalid LIF processing function doesn't match");
		String Line_item_selector_id = qcCreateResponse2.jsonPath().getString("results[0].warnings.line_item_filters[0].item.line_item_selector_id");
		Assert.assertEquals(Line_item_selector_id, actualExternalIdLIS2,
				"Line item selector id for LIF doesn't match");
		String processingMethod = qcCreateResponse2.jsonPath().getString("results[0].warnings.line_item_filters[0].item.processing_method");
		Assert.assertEquals(processingMethod, "max", "Processing method for LIF doesn't match");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		utils.logPass(
				"Verified that Line item filter does not created as Parameter-processing method is required for line item filter in sum of amounts");

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
				+ "\",\"processing_method\":\"max\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC3, "QC success message is not True");
		utils.logPass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
//		Response getQcResponse2 =  pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName3, "2", "13");
//		String externalId2 = getQcResponse2.jsonPath().getString("data[0].external_id");
//		Assert.assertEquals(externalId2, actualExternalIdQC3, "LIS external id is not matching");
//		String lifProcessingMethod2 = getQcResponse2.jsonPath().getString("data[0].line_item_filters[0].processing_method");
//		Assert.assertEquals(lifProcessingMethod2, "max", "LIF processing method is not matching");
//		logger.info("Line item filter gets created as Parameter-processing method is not required for line item filter in Hit Target Menu for Maximum Price unit");
//		TestListeners.extentTest.get().pass("Line item filter gets created as Parameter-processing method is not required for line item filter in Hit Target Menu for Maximum Price unit");


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
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertFalse(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertFalse(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		utils.logPass(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		utils.logPass(QCName2 + " QC is deleted successfully");

		//QCName3 is not created in this test case, so not deleting it
//		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
//				.replace("${businessID}", businessID);
//		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
//		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
//				qcID_DB3);
//		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
//				deleteQCFromQualifying_expressions3);
//		Assert.assertFalse(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
//		TestListeners.extentTest.get().pass(QCName3 + " QC is deleted successfully");
//		logger.info(QCName3 + " QC is deleted successfully");


	}
	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
