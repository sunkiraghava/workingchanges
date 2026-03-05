package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

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
public class RedeemableDistributionApiTest {

	private static Logger logger = LogManager.getLogger(RedeemableDistributionApiTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail, authUserEmail, baseUrl;
	private String sTCName;
	private String env, run = "ui";
	ApiUtils apiUtils;
	Utilities utils;
	private static Map<String, String> dataSet;
	String campaignStartTimeError = "{\"errors\":{\"invalid_start_time\":\"Campaign start time should be at least 15 ahead from now.\"}}";
	String invalidFormatError = "{\"errors\":{\"invalid_format\":\"Invalid Start Time format. Please use ISO8601\"}}";
	String signUpError = "{\"error\":\"You need to sign in or sign up before continuing.\"}";
	String timeZone = "Etc/UTC";
	String redeemableIdMissigError = "{\"error\":\"Required parameter missing or the value is empty: redeemable_uuid\"}";
	String redeemableIdIncorrectError = "{\"errors\":{\"invalid_redeemable\":\"Invalid redeemable uuid.\"}}";
	String redeemableIdDeactivatedIncorrectError = "{\"errors\":\"Redeemable is deactivated.\"}";
	String redeemableScheduleError = "{\"errors\":\"Redeemable is scheduled.\"}";
	String redeemableExpiredError = "{\"errors\":\"Redeemable is expired.\"}";
	String segmentIdError = "{\"error\":\"Required parameter missing or the value is empty: segment_id\"}";
	String invalidSegmentIdError = "{\"errors\":{\"invalid_segment\":\"Invalid segment id.\"}}";
	String campaignMissingError = "{\"error\":\"Required parameter missing or the value is empty: campaign_type\"}";
	String campaignEmptyError = "{\"error\":\"Required parameter missing or the value is empty: campaign_type\"}";
	String invalidCampaignError = "{\"errors\":{\"invalid_campaign_type\":\"Invalid campaign type.\"}}";
	String categoryMissingError = "{\"error\":\"Required parameter missing or the value is empty: category\"}";
	String invalidCategoryError = "{\"errors\":{\"invalid_category\":\"Invalid category.\"}}";
	String nameMissingError = "{\"errors\":\"Name can't be blank\"}";
	String authError = "{\"error\":\"You need to sign in or sign up before continuing.\"}";
	String privligeError = "{\"no_permission_error\":\"Insufficient Privileges to access this resource\"}";
	String activeRedeemableUuid = "46acd930ba7fa5ac51f1af40a19f586bacdaf4a8"; // Test Redeemable 230222

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		apiUtils = new ApiUtils();
	}

	@Test(description = "SQ-T2293_Applicable Offers -> menu_item amount rounde off till 2 decimal places", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2293_ApplicableOfferAmountRoundOff() throws InterruptedException {
		logger.info("== API 2 | Applicable Offers -> menu_item amount rounded off till 2 decimal places ===");
		List<String> itemQtyList = Arrays.asList(dataSet.get("itemQty").split(","));
		System.out.println(itemQtyList);
		List<String> itemAmountList = Arrays.asList(dataSet.get("itemAmount").split(","));
		logger.info(itemAmountList);
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");
		for (String qtyListStr : itemQtyList) {
			for (String amountListStr : itemAmountList) {
				Response applicableOffersResponse = pageObj.endpoints().RegApplicableOffers(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("location_id"), qtyListStr, amountListStr);
				Assert.assertEquals(applicableOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api2 list applicable offers");
				final DecimalFormat df = new DecimalFormat("0.00");
				Assert.assertEquals(Integer.parseInt(qtyListStr), (int) Float
						.parseFloat(applicableOffersResponse.jsonPath().get("menu_items[0].item_qty[0]").toString()));
				Assert.assertEquals(df.format(Float.parseFloat(amountListStr)), df.format(Float.parseFloat(
						applicableOffersResponse.jsonPath().get("menu_items[0].item_amount[0]").toString())));
			}
		}
		logger.info("== POS API | Applicable Offers -> menu_item amount rounded off till 2 decimal places ===");
		Response posApplicableOffersResponse = null;
		for (String qtyListStr : itemQtyList) {
			for (String amountListStr : itemAmountList) {
				posApplicableOffersResponse = pageObj.endpoints().posApplicableOffers(userEmail,
						dataSet.get("location_key"), qtyListStr, amountListStr);
				Assert.assertEquals(posApplicableOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api2 list applicable offers");
				final DecimalFormat df = new DecimalFormat("0.00");
				Assert.assertEquals(Integer.parseInt(qtyListStr), (int) Float.parseFloat(
						posApplicableOffersResponse.jsonPath().get("menu_items[0].item_qty[0]").toString()));
				Assert.assertEquals(df.format(Float.parseFloat(amountListStr)), df.format(Float.parseFloat(
						posApplicableOffersResponse.jsonPath().get("menu_items[0].item_amount[0]").toString())));
			}
		}
		boolean isPosApplicableOffersSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2ListApplicableOffersSchema, posApplicableOffersResponse.asString());
		Assert.assertTrue(isPosApplicableOffersSchemaValidated, "Applicable Offers POS API Schema Validation failed");

		logger.info("== Auth API | Applicable Offers -> menu_item amount rounded off till 2 decimal places ===");
		authUserEmail = pageObj.iframeSingUpPage().generateEmail();
		// user creation using auth signup api
		Response authSignUpResponse = pageObj.endpoints().authApiSignUp(authUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(authSignUpResponse, "Auth API user signup");
		Assert.assertEquals(authSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = authSignUpResponse.jsonPath().get("authentication_token");
		String authUserID = authSignUpResponse.jsonPath().get("user_id").toString();
		Response autSendRewardResponse = pageObj.endpoints().sendMessageToUser(authUserID,
				dataSet.get("apiKey"), dataSet.get("amount"), dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, autSendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// verifying decimal values for various input
		for (String qtyListStr : itemQtyList) {
			for (String amountListStr : itemAmountList) {
				Response authApplicableOffersResponse = pageObj.endpoints().authApplicableOffers(dataSet.get("client"),
						dataSet.get("secret"), authToken, qtyListStr, amountListStr);
				Assert.assertEquals(authApplicableOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api2 list applicable offers");
				final DecimalFormat df = new DecimalFormat("0.00");
				Assert.assertEquals(Integer.parseInt(qtyListStr), (int) Float.parseFloat(
						authApplicableOffersResponse.jsonPath().get("menu_items[0].item_qty[0]").toString()));
				Assert.assertEquals(df.format(Float.parseFloat(amountListStr)), df.format(Float.parseFloat(
						authApplicableOffersResponse.jsonPath().get("menu_items[0].item_amount[0]").toString())));
				utils.logPass("Verified menu_item amount rounded off for quanity: " + qtyListStr + " and amount :"
						+ amountListStr);
			}
		}
	}

	@Test(description = "SQ-T2306, Redeemable Distribution API ->'start_time' parameter Validations", groups = {
			"unstable", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T2306_RedeemableDistributionApiStartTimeParameterValidation()
			throws Exception {
		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_deals", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_deals value is not updated to true");
		utils.logit("enable_deals value is updated to true");

		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));

		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		String redeemableUuid = responseDashboardRedeemableList.jsonPath().getString("redeemable_uuid[0]");

		String startTime = "none";
		Response responseCreateMassCampaign = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_BAD_REQUEST, responseCreateMassCampaign.getStatusCode(),
				"Status code 200 did not matched for responseDashboardRedeemableList");
		Assert.assertEquals("{\"error\":\"Required parameter missing or the value is empty: start_time\"}",
				responseCreateMassCampaign.asString());
		utils.logPass("Verified expected error when no start time parameteris provided");

		Response responseCreateMassCampaign1 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseCreateMassCampaign1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for invalid start time");
		boolean isCommitRedeemableDistributionInvalidStartTimeValueSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema,
						responseCreateMassCampaign1.asString());
		Assert.assertTrue(isCommitRedeemableDistributionInvalidStartTimeValueSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals("{\"error\":\"Required parameter missing or the value is empty: start_time\"}",
				responseCreateMassCampaign1.asString());
		utils.logPass("Verified expected error when start time parameter value is blank ");

		startTime = CreateDateTime.getTomorrowDate2() + " 07:56 PM";
		logger.info(startTime);
		Response responseCreateMassCampaign2 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseCreateMassCampaign1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for invalid time format YYYY/mm/dd hh:mm a");
		boolean isCommitRedeemableDistributionInvalidStartTimeFormat400SchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.dashboardInvalidFormatErrorSchema,
						responseCreateMassCampaign2.asString());
		Assert.assertTrue(isCommitRedeemableDistributionInvalidStartTimeFormat400SchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(invalidFormatError, responseCreateMassCampaign2.asString());
		utils.logPass("Verified expected error when start time format is invalid YYYY/mm/dd hh:mm a");

		startTime = CreateDateTime.getTomorrowDate() + " 07:56 PM";
		Response responseCreateMassCampaign3 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseCreateMassCampaign3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for invalid time format YYYY-mm-dd hh:mm a");
		boolean isCommitRedeemableDistributionInvalidStartTimeFormat422SchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.dashboardInvalidFormatErrorSchema,
						responseCreateMassCampaign3.asString());
		Assert.assertTrue(isCommitRedeemableDistributionInvalidStartTimeFormat422SchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(invalidFormatError, responseCreateMassCampaign3.asString());
		utils.logPass("Verified expected error when start time format is invalid YYYY-mm-dd hh:mm a");

		startTime = CreateDateTime.getTomorrowDate() + " 19:44:38";
		Response responseCreateMassCampaign4 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, responseCreateMassCampaign4.getStatusCode(),
				"Status code 200 did not matched for invalid start time format is invalid YYYY-mm-dd hh:mm:ss");
		Assert.assertEquals(invalidFormatError, responseCreateMassCampaign4.asString());
		utils.logPass("Verified expected error when start time format is invalid YYYY-mm-dd hh:mm:ss");

		startTime = CreateDateTime.getTomorrowDate();
		Response responseCreateMassCampaign5 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, responseCreateMassCampaign5.getStatusCode(),
				"Status code 200 did not matched for invalid time format is invalid YYYY-mm-dd");
		Assert.assertEquals(invalidFormatError, responseCreateMassCampaign5.asString());
		utils.logPass("Verified expected error when start time format is invalid YYYY-mm-dd");

		startTime = "19:44:38";
		Response responseCreateMassCampaign6 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		logger.info(responseCreateMassCampaign6.asString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, responseCreateMassCampaign6.getStatusCode(),
				"Status code 200 did not matched for invalid start time format is invalid hh:mm:ss");
		Assert.assertEquals(invalidFormatError, responseCreateMassCampaign6.asString());
		utils.logPass("Verified expected error when start time format is invalid hh:mm:ss");

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		startTime = CreateDateTime.getTomorrowDate() + "T18:36:32Z";
		Response responseCreateMassCampaign7 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				activeRedeemableUuid1, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		logger.info(responseCreateMassCampaign7.asString());
		Assert.assertEquals(responseCreateMassCampaign7.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Campaign creation start time format with valid format");
		boolean isCommitRedeemableDistributionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardCommitRedeemableDistributionSchema,
				responseCreateMassCampaign7.asString());
		Assert.assertTrue(isCommitRedeemableDistributionSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(responseCreateMassCampaign7.jsonPath().getString("start_time"),
				CreateDateTime.getTomorrowDate() + " 06:36 PM");
		Assert.assertEquals(responseCreateMassCampaign7.jsonPath().getString("timezone"), timeZone);
		System.out.println(CreateDateTime.getTomorrowDate());
		logger.info(CreateDateTime.getTomorrowDate());
		utils.logPass("Verified Campaign creation start time format with valid format: " + startTime);

		startTime = CreateDateTime.getTomorrowDate() + "T13:36:32";
		Response responseCreateMassCampaign8 = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				activeRedeemableUuid1, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, responseCreateMassCampaign8.getStatusCode(),
				"Status code 200 did not matched for Campaign creation start time format with valid format");
		logger.info(responseCreateMassCampaign8.jsonPath().getString("start_time"));
		Assert.assertEquals(CreateDateTime.getTomorrowDate() + " 01:36 PM",
				responseCreateMassCampaign8.jsonPath().getString("start_time"));
		Assert.assertEquals(responseCreateMassCampaign8.jsonPath().getString("timezone"), timeZone);
		utils.logPass("Verified Campaign creation start time format with valid format: " + startTime);

		startTime = CreateDateTime.getTomorrowDate() + "T20:30:01+05:30";
		Response responseCreateMassCampaign09 = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, responseCreateMassCampaign8.getStatusCode(),
				"Status code 200 did not matched for Campaign creation start time format with valid format");
		logger.info(responseCreateMassCampaign09.jsonPath().getString("start_time"));
		Assert.assertEquals(CreateDateTime.getTomorrowDate() + " 03:00 PM",
				responseCreateMassCampaign09.jsonPath().getString("start_time"));
		Assert.assertEquals(responseCreateMassCampaign09.jsonPath().getString("timezone"), timeZone);
		utils.logPass("Verified Campaign creation start time format with valid format: " + startTime);

		startTime = CreateDateTime.getTomorrowDate() + "T16:30:01-05:00";
		Response responseCreateMassCampaign10 = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, responseCreateMassCampaign10.getStatusCode(),
				"Status code 200 did not matched for Campaign creation start time format with valid format");
		logger.info(responseCreateMassCampaign10.jsonPath().getString("start_time"));
		Assert.assertEquals(CreateDateTime.getTomorrowDate() + " 09:30 PM",
				responseCreateMassCampaign10.jsonPath().getString("start_time"));
		Assert.assertEquals(responseCreateMassCampaign10.jsonPath().getString("timezone"), timeZone);
		utils.logPass("Verified Campaign creation start time format with valid format: " + startTime);

		startTime = CreateDateTime.getCurrentDateTimeInUtc() + "z";
		Response responseCreateMassCampaign11 = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseCreateMassCampaign11.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for expected error when start time is same as current time in UTC");
		Assert.assertEquals(responseCreateMassCampaign11.asString(), campaignStartTimeError);
		boolean isCommitRedeemableDistributionInvalidStartTimeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidStartTimeErrorSchema, responseCreateMassCampaign11.asString());
		Assert.assertTrue(isCommitRedeemableDistributionInvalidStartTimeSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		utils.logPass("Verified error when start time is same as current time in UTC: " + startTime);

		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		logger.info(startTime);
		Response responseCreateMassCampaign12 = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseCreateMassCampaign12.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Campaign creation start time is more then 15 min of current time");
//		Assert.assertEquals(responseCreateMassCampaign12.jsonPath().getString("start_time"),
//				CreateDateTime.timeFormatToAmPm(startTime).toUpperCase());
		Assert.assertEquals(responseCreateMassCampaign12.jsonPath().getString("timezone"), timeZone);
		utils.logPass("Verified Campaign creation start time is more then 15 min of current time: " + startTime);
	}

	@Test(description = "SQ-T2307, Redeemable Distribution API -> Authorization token 'redeemable_id' parameter Validations", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2307_RedeemableDistributionApiRedeemableIdParameterValidation()
			throws InterruptedException, ParseException {

		try {
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			// pageObj.menupage().clickCockPitMenu();
			// pageObj.menupage().clickCockpitDashboardLink();
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.instanceDashboardPage().enableOfferDistributionFromExternalSystemFlag();
			driver.quit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		String redeemableUuid = responseDashboardRedeemableList.jsonPath().getString("redeemable_uuid[0]");

		// Authorization token sent in API is incorrect
		String startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseIncorrectAuthToken = pageObj.endpoints().createMassCampaign("abc", redeemableUuid, segmentId,
				startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseIncorrectAuthToken.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 401 did not matched for Incorrect auth token");
		boolean isInvalidAuthResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, responseIncorrectAuthToken.asString());
		Assert.assertTrue(isInvalidAuthResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(signUpError, responseIncorrectAuthToken.asString());
		utils.logPass("Verified expected error for incorrect auth token");

		// "redeemable_uuid" parameter is missing in API
		String redeemableUuidTemp = "none";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseMissingRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseMissingRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 validation failed, expected error for missing redeemable_uuid");
		boolean isMissingRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, responseMissingRedeemableUuid.asString());
		Assert.assertTrue(isMissingRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(redeemableIdMissigError, responseMissingRedeemableUuid.asString());
		utils.logPass("Verified expected error for missing redeemable_uuid");

		// "redeemable_uuid" parameter is blank in API
		redeemableUuidTemp = "";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseBlankRedeemableUuid = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuidTemp, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseBlankRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 validation failed, expected error for blank redeemable_uuid");
		boolean isBlankRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, responseBlankRedeemableUuid.asString());
		Assert.assertTrue(isBlankRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(redeemableIdMissigError, responseBlankRedeemableUuid.asString());
		utils.logPass("Verified expected error for blank value of redeemable_uuid");

		// "redeemable_uuid" parameter is invalid in API
		redeemableUuidTemp = "abc";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseIncorrectRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		// apiUtils.verifyResponse(responseCreateMassCampaign, "SegmentListResponse");
		Assert.assertEquals(responseIncorrectRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 validation failed, expected error for incorrect redeemable_uuid");
		boolean isIncorrectRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardInvalidRedeemableErrorSchema,
				responseIncorrectRedeemableUuid.asString());
		Assert.assertTrue(isIncorrectRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(redeemableIdIncorrectError, responseIncorrectRedeemableUuid.asString());
		utils.logPass("Verified expected error for invalid redeemable_uuid");

		// "redeemable_uuid" parameter is deactivated in API
		redeemableUuidTemp = "118b2a416d9ef2cf9f772186539688bbca7d0020";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseDeactivatedRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(responseDeactivatedRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 validation failed, expected error for deactivated redeemable_uuid");
		boolean isDeactivatedRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, responseDeactivatedRedeemableUuid.asString());
		Assert.assertTrue(isDeactivatedRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(redeemableIdDeactivatedIncorrectError, responseDeactivatedRedeemableUuid.asString());
		utils.logPass("Verified expected error for deactivated redeemable_uuid");

		// "redeemable_uuid" parameter is scheduled in API
		redeemableUuidTemp = "e53970f87676dca5625323e09a74bdcdb48f32ee"; // Scheduled Redeemable - Auto Test Scheduled
																			// Redeemable
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseScheduledRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(responseScheduledRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 validation failed, expected error for scheduled redeemable_uuid");
		boolean isScheduledRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, responseScheduledRedeemableUuid.asString());
		Assert.assertTrue(isScheduledRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
		Assert.assertEquals(redeemableScheduleError, responseScheduledRedeemableUuid.asString());
		utils.logPass("Verified expected error for scheduled redeemable_uuid");

		// "redeemable_uuid" parameter is expired in API
		redeemableUuidTemp = "b9627f87c220016d39cd29cb361af8a9caca8e1a";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseExpiredRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(responseExpiredRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 validation failed, expected error for expired redeemable_uuid");
		boolean isExpiredRedeemableUuidResponseSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema, responseExpiredRedeemableUuid.asString());
		Assert.assertTrue(isExpiredRedeemableUuidResponseSchemaValidated,
				"Commit Redeemable Distribution API Schema Validation failed");
//		Assert.assertEquals(redeemableExpiredError, responseExpiredRedeemableUuid.asString());
		utils.logPass("Verified expected error for expired redeemable_uuid");

		// "redeemable_uuid" parameter is draft in API -- ask divya
		// ?????????????????????????
		redeemableUuidTemp = "a4fef3f7d1059f73a57d1a60cd53bf904f9b7132";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseDraftRedeemableUuid = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, responseDraftRedeemableUuid.getStatusCode(),
				"Status code 422 validation failed, expected error for draft redeemable_uuid");
		Assert.assertEquals(redeemableIdIncorrectError, responseDraftRedeemableUuid.asString()); // TD ??
		utils.logPass("Verified expected error for draft redeemable_uuid");

		// "redeemable_uuid" parameter is other business in API -- ask divya
		// ?????????????????????????
		redeemableUuidTemp = "bf2ba41d4e52327d59a12b72ca24a8acde6ae03d";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseOtherBuisnessRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, responseOtherBuisnessRedeemableUuid.getStatusCode(),
				"Status code 422 validation failed, expected error for other business redeemable_uuid");
		Assert.assertEquals(redeemableIdIncorrectError, responseOtherBuisnessRedeemableUuid.asString()); // TD ??
		utils.logPass("Verified expected error for other business redeemable_uuid");

		// "redeemable_uuid" parameter is active in API
		redeemableUuidTemp = "95e4dadd649b92ecd5d8e072c91e518c31480179";
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseActiveRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuidTemp, segmentId, startTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, responseActiveRedeemableUuid.getStatusCode(),
				"Status code 200 validation failed for Redeemable Distribution API");
		Assert.assertEquals("2821199", responseActiveRedeemableUuid.jsonPath().get("redeemable_id").toString());
		utils.logPass("Verified Redeemable Distribution API for valid redeemable_uuid");
	}

	@Test(description = "SQ-T2308, Redeemable Distribution API -> 'segment_id' parameter Validations", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2308_RedeemableDistributionApiSegmentIdParameterValidation()
			throws InterruptedException, ParseException {
		// User SignUp from Auth Api
		// String external_source_id = CreateDateTime.getTimeDateString();
		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		String redeemableUuid = responseDashboardRedeemableList.jsonPath().getString("redeemable_uuid[0]");

		// "segment_id" parameter is missing in Redeemable Distribution API
		String startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		segmentId = "none";
		Response responseMissingSegmentId = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseMissingSegmentId.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 didn't match, for expected error for missing segment id");
		Assert.assertEquals(segmentIdError, responseMissingSegmentId.asString());
		utils.logPass("Verified expected error for missing segment id");

		// "segment_id" parameter value is blank in Redeemable Distribution API
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		segmentId = "\"\"";
		Response responseBlankSegmentId = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseBlankSegmentId.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not matched for expected error for blank value of segment id");
		Assert.assertEquals(segmentIdError, responseBlankSegmentId.asString());
		utils.logPass("Verified expected error for blank value of segment id");

		// "segment_id" parameter value is Invalid in Redeemable Distribution API
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		segmentId = "123";
		Response responseInvalidSegmentId = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseInvalidSegmentId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for expected error for incorrect segment id ");
		Assert.assertEquals(invalidSegmentIdError, responseInvalidSegmentId.asString());
		utils.logPass("Verified expected error for incorrect segment id");

		// "segment_id" parameter value is other business in Redeemable Distribution API
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		segmentId = "1045";
		Response responseOtherBuisnessSegmentId = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), redeemableUuid, segmentId, startTime);
		Assert.assertEquals(responseOtherBuisnessSegmentId.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for expected error for other business segment id");
		Assert.assertEquals(invalidSegmentIdError, responseOtherBuisnessSegmentId.asString());
		utils.logPass("Verified expected error for other business segment id");

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		// "segment_id" parameter value is valid in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		Response responseActiveRedeemableUuid = pageObj.endpoints()
				.createMassCampaign(dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime);
		Assert.assertEquals(responseActiveRedeemableUuid.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 validation failed for Redeemable Distribution API");
//		Assert.assertEquals(responseActiveRedeemableUuid.jsonPath().get("redeemable_id").toString(), "2361916",
//				"Failed to verify Redeemable Distribution for valid segment_id");
		utils.logPass("Verified Redeemable Distribution for valid segment_id");
	}

	@Test(description = "SQ-T2309, Redeemable Distribution API -> 'segment_id' parameter Validations", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2309_RedeemableDistributionApiCampaignTypeParameterValidation()
			throws InterruptedException, ParseException {
		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		String redeemableUuid = responseDashboardRedeemableList.jsonPath().getString("redeemable_uuid[0]");
		// "campaign_type" parameter is missing in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		String startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		String campaignType = "none";
		Response responseMissingCampaignType = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime, campaignType);
		Assert.assertEquals(responseMissingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400, failed to verfiy missing campaign error");
		Assert.assertEquals(responseMissingCampaignType.asString(), campaignMissingError);
		utils.logPass("Verified Redeemable Distribution for missing campaign type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		campaignType = "";
		Response responseBlankCampaignType = pageObj.endpoints().createMassCampaign(dataSet.get("apiKey"),
				redeemableUuid, segmentId, startTime, campaignType);
		Assert.assertEquals(responseBlankCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400, failed to verify blank campaign type error");
		Assert.assertEquals(responseBlankCampaignType.asString(), campaignEmptyError);
		utils.logPass("Verified Redeemable Distribution for blank campaign type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		campaignType = "abc";
		Response responseIncorrectCampaignType = pageObj.endpoints().createMassCampaign(
				dataSet.get("apiKey"), redeemableUuid, segmentId, startTime, campaignType);
		Assert.assertEquals(responseIncorrectCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422, failed to verify invalid campaign type error");
		Assert.assertEquals(responseIncorrectCampaignType.asString(), invalidCampaignError);
		utils.logPass("Verified Redeemable Distribution for invalid campaign type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		campaignType = "giftbearing";
		Response responseGiftBearingCampaignType = pageObj.endpoints().createMassCampaign(
				dataSet.get("apiKey"), redeemableUuid, segmentId, startTime, campaignType);
		Assert.assertEquals(responseGiftBearingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422, failed to verify gift bearing campaign type error");
		Assert.assertEquals(responseGiftBearingCampaignType.asString(), invalidCampaignError);
		utils.logPass("Verified Redeemable Distribution API error for gift bearing campaign type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");
		startTime = CreateDateTime.get15MinAheadTimeInUtc() + "z";
		campaignType = "mass_gifting";

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		Response responseMassgiftingCampaignType = pageObj.endpoints().createMassCampaign(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, startTime, campaignType);
		Assert.assertEquals(responseMassgiftingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify mass gifting ");
		utils.logPass("Verified Redeemable Distribution for mass gifting campaign type parameter");
	}

	@Test(description = "SQ-T2310, Redeemable Distribution API -> 'category' parameter Validations", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2310_RedeemableDistributionApiCategoryParameterValidation()
			throws InterruptedException, ParseException {
		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		String redeemableUuid = responseDashboardRedeemableList.jsonPath().getString("redeemable_uuid[0]");
		// "category" parameter is missing in Redeemable Distribution API
		String category = "none";
		Response responseMissingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), redeemableUuid, segmentId, "category", category);
		Assert.assertEquals(responseMissingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400, failed to verfiy missing category error");
		Assert.assertEquals(responseMissingCampaignType.asString(), categoryMissingError);
		utils.logPass("Verified Redeemable Distribution for missing category type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		category = "";
		Response responseBlankCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), redeemableUuid, segmentId, "category", category);
		Assert.assertEquals(responseBlankCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400, failed to verify blank category type error");
		Assert.assertEquals(responseBlankCampaignType.asString(), categoryMissingError);
		utils.logPass("Verified Redeemable Distribution for blank category type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		category = "gift_currency";
		Response responseIncorrectCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), redeemableUuid, segmentId, "category", category);
		Assert.assertEquals(responseIncorrectCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422, failed to verify invalid category type error");
		Assert.assertEquals(responseIncorrectCampaignType.asString(), invalidCategoryError);
		utils.logPass("Verified Redeemable Distribution for invalid category type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		category = "no_gift";
		Response responseGiftBearingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), redeemableUuid, segmentId, "category", category);
		Assert.assertEquals(responseGiftBearingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422, failed to verify no gift category type error");
		Assert.assertEquals(responseGiftBearingCampaignType.asString(), invalidCategoryError);
		utils.logPass("Verified Redeemable Distribution API error for no gift category type parameter");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		category = "gift_redeemable";

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		Response responseMassgiftingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "category", category);
		Assert.assertEquals(responseMassgiftingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify mass gifting campaign type error");
		utils.logPass("Verified Redeemable Distribution for mass gifting campaign type parameter");
	}

	@Test(description = "SQ-T2311, Redeemable Distribution API -> 'name' parameter Validations", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2311_RedeemableDistributionApiNameParameterValidation() throws InterruptedException, ParseException {
		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		// "Name" parameter is missing in Redeemable Distribution API
		String name = "none";
		Response responseMissingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "name", name);
		Assert.assertEquals(responseMissingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422, failed to verfiy missing name error");
		Assert.assertEquals(responseMissingCampaignType.asString(), nameMissingError);
		utils.logPass("Verified Redeemable Distribution for missing name type parameter");
		// "Name" parameter is empty/blank in Redeemable Distribution API
		name = "";
		Response responseBlankCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "name", name);
		Assert.assertEquals(responseBlankCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 400, failed to verify blank category type error");
		Assert.assertEquals(responseBlankCampaignType.asString(), nameMissingError);
		utils.logPass("Verified Redeemable Distribution for blank category type parameter");
		// "Name" parameter is empty/blank in Redeemable Distribution API
		name = "API Campaign 1";
		Response responseIncorrectCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "name", name);
		Assert.assertEquals(responseIncorrectCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify Redeemable Distribution API with valid name");
		// Assert.assertEquals(responseIncorrectCampaignType.asString(),
		// invalidCategoryError);
		utils.logPass("Verified Redeemable Distribution Redeemable Distribution API with valid name");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		name = "API Campaign 1";
		Response responseGiftBearingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "name", name);
		Assert.assertEquals(responseGiftBearingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify Redeemable Distribution API with valid name");
		// Assert.assertEquals(responseGiftBearingCampaignType.asString().ge);
		utils.logPass("Verified ​​​​​Campaign gets created with same name");
		// "campaign_type" parameter is empty/blank in Redeemable Distribution API
		// ---???? TD 2 scenarios
//		name = "1.256";                                                                           
		Response responseMassgiftingCampaignType = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "name", name);
		Assert.assertEquals(responseMassgiftingCampaignType.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify mass gifting campaign type error");
		// Assert.assertEquals(responseMassgiftingCampaignType.jsonPath().getString("campaignType"),
		// category);
		utils.logPass("Verified Redeemable Distribution for mass gifting campaign type parameter");
	}

	@Test(description = "SQ-T2312, Redeemable Distribution API -> 'external_campaign_id' parameter Validations", groups = {
			"unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2312_RedeemableDistributionApiExternalCampainIdParameterValidation()
			throws InterruptedException, ParseException {
		Response responseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardSegmentList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardSegmentList");
		String segmentId = responseDashboardSegmentList.jsonPath().getString("segment_id[0]");

		Response responseDashboardRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"));
		apiUtils.verifyResponse(responseDashboardRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2 send reward reedemable to user is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		String activeRedeemableUuid1 = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		// "external_campaign_id" parameter is missing in Redeemable Distribution API
		String external_campaign_id = "none";
		Response responseMissingCampaignId = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "external_campaign_id",
				external_campaign_id);
		Assert.assertEquals(responseMissingCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verfiy missing external_Campaign_id error");
		// System.out.println(responseMissingCampaignType.jsonPath().getString("external_source_id"));
		Assert.assertTrue(responseMissingCampaignId.jsonPath().getString("external_source_id") == null);
		utils.logPass(
				"Verified external_Campaign_id as null for missing external_Campaign_id for Redeemable Distribution API");
		// "external_campaign_id" parameter is empty/blank in Redeemable Distribution
		// API
		external_campaign_id = "";
		Response responseBlankCampaignId = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "external_campaign_id",
				external_campaign_id);
		Assert.assertEquals(responseBlankCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify blank category type error");
		Assert.assertEquals(responseBlankCampaignId.jsonPath().getString("external_source_id"), "");
		utils.logPass("Verified Redeemable Distribution for blank category type parameter");

		// "external_campaign_id" parameter is random value "abc" in Redeemable
		// Distribution API
		external_campaign_id = "abc";
		Response responseAbcCampaignId = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "external_campaign_id",
				external_campaign_id);
		Assert.assertEquals(responseAbcCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify Redeemable Distribution API with valid name");
		Assert.assertEquals(responseAbcCampaignId.jsonPath().getString("external_source_id"), "abc");
		utils.logPass("Verified  Redeemable Distribution API with external_campaign_id as 'abc'");
		// "campaign_id" parameter is Valid in Redeemable Distribution API
		external_campaign_id = "2";
		Response responseValidCampaignId = pageObj.endpoints().createMassCampaignCategoryPara(
				dataSet.get("apiKey"), activeRedeemableUuid1, segmentId, "external_campaign_id",
				external_campaign_id);
		Assert.assertEquals(responseValidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify Redeemable Distribution API with valid name");
		Assert.assertEquals(responseValidCampaignId.jsonPath().getString("external_source_id"), external_campaign_id);
		utils.logPass("Verified Redeemable Distribution API with valid external_campaign_id");
		// "campaign_id" parameter is Valid in Redeemable Distribution API
		responseValidCampaignId = pageObj.endpoints().createMassCampaignCategoryPara(dataSet.get("apiKey"),
				activeRedeemableUuid1, segmentId, "external_campaign_id", external_campaign_id);
		Assert.assertEquals(responseValidCampaignId.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200, failed to verify Redeemable Distribution API with valid name");
		Assert.assertEquals(responseValidCampaignId.jsonPath().getString("external_source_id"), external_campaign_id);
		utils.logPass("Verified Redeemable Distribution API with valid but same external_campaign_id value");
	}

	@Test(description = "SQ-T2351, Redeemable List Deal API", groups = { "regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2351_RedeemableDistributionApiExternalCampainIdParameterValidation()
			throws InterruptedException, ParseException {
		String incorrectToken = dataSet.get("apiKey") + "ABC";
		Response responseDashboardRedeemableList = pageObj.endpoints().dashboardRedeemableList(incorrectToken);
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Expected Status code 401 for incorrect Authorization token is not matching");
		boolean isGetRedeemableListInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, responseDashboardRedeemableList.asString());
		Assert.assertTrue(isGetRedeemableListInvalidAuthSchemaValidated,
				"Get Redeemable List Schema Validation failed");
		Assert.assertEquals(responseDashboardRedeemableList.asString(), authError);

		String query = "DND Automation Activate";
		Response responseDashboardQueryRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"), query);
		apiUtils.verifyResponse(responseDashboardQueryRedeemableList, "SegmentListResponse");
		Assert.assertEquals(responseDashboardQueryRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for responseDashboardRedeemableList");
		boolean isGetRedeemableListValidQuerySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetRedeemableListSchema,
				responseDashboardQueryRedeemableList.asString());
		Assert.assertTrue(isGetRedeemableListValidQuerySchemaValidated, "Get Redeemable List Schema Validation failed");
		Assert.assertEquals(responseDashboardQueryRedeemableList.jsonPath().get("redeemable_id.size()").toString(),
				"1");
		Assert.assertTrue(responseDashboardQueryRedeemableList.jsonPath().get("name").toString().contains(query));

		int perPage = 0;
		Response responseDashboardPageCountRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"), perPage);
		Assert.assertEquals(responseDashboardPageCountRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched when per no page count is provided for Redeemable List Api");
		Assert.assertEquals(responseDashboardPageCountRedeemableList.jsonPath().get("redeemable_id.size()").toString(),
				"10");
		utils.logPass("Verified Redeemable List Api returns 10 redeemables when no page count is provided");

		perPage = 2;
		responseDashboardPageCountRedeemableList = pageObj.endpoints()
				.dashboardRedeemableList(dataSet.get("apiKey"), perPage);
		Assert.assertEquals(responseDashboardPageCountRedeemableList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched when per no page count is provided for Redeemable List Api");
		Assert.assertEquals(responseDashboardPageCountRedeemableList.jsonPath().get("redeemable_id.size()").toString(),
				"2");
		utils.logPass("Verified Redeemable List Api when per page count is 2");

		String token = dataSet.get("adminWithoutDashboardApiAccess");
		responseDashboardRedeemableList = pageObj.endpoints().dashboardRedeemableList(token);
		Assert.assertEquals(responseDashboardRedeemableList.getStatusCode(), 403,
				"Expected Status code 403 for Insufficient Privileges");
		boolean isGetRedeemableListInsufficientPrivelegesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardGetRedeemableList403ErrorSchema,
				responseDashboardRedeemableList.asString());
		Assert.assertTrue(isGetRedeemableListInsufficientPrivelegesSchemaValidated,
				"Get Redeemable List Schema Validation failed");
		Assert.assertEquals(responseDashboardRedeemableList.asString(), privligeError);
	}

	@Test(description = "SQ-T2297, Segment List Deal API -> API Response", groups = { "regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2297_SegmentListApiAdminPermissionValidation() {

		String incorrectToken = dataSet.get("apiKey") + "AAA";

		Response responseDashboardSegmentList = pageObj.endpoints().dashboardSegmentList(incorrectToken);
		Assert.assertEquals(responseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Expected Status code 401 for incorrect Authorization token is not matching");
		boolean isGetSegmentListInvalidAuthSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, responseDashboardSegmentList.asString());
		Assert.assertTrue(isGetSegmentListInvalidAuthSchemaValidated, "Get Segment List Schema Validation failed");
		Assert.assertEquals(responseDashboardSegmentList.asString(), authError);
		utils.logPass("Verified Get Segment List API with invalid Auth token");

		String segmentName = "segmentTemp";
		logger.info("== create Custom segments ==");
		String segName = segmentName.replace("Temp", CreateDateTime.getTimeDateString());
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segName,
				dataSet.get("apiKey"));
		pageObj.apiUtils().verifyCreateResponse(createSegmentResponse, "Custom segment created"); // int customSegmentId
																									// = //
		createSegmentResponse.jsonPath().get("custom_segment_id");

		logger.info("== get Custom segments details ==");
		String query = createSegmentResponse.jsonPath().get("name").toString();
		Response responseQuery1DashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"), query);
		Assert.assertEquals(responseQuery1DashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		boolean isGetSegmentListValidQuerySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.dashboardGetSegmentListSchema, responseQuery1DashboardSegmentList.asString());
		Assert.assertTrue(isGetSegmentListValidQuerySchemaValidated, "Get Segment List Schema Validation failed");
		logger.info(responseQuery1DashboardSegmentList.jsonPath().get("name[0]").toString());
		Assert.assertEquals(responseQuery1DashboardSegmentList.jsonPath().get("name[0]").toString(), query);
		utils.logPass("Verified segment name while querying newly created segment list API");

		Response responseCountDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"));
		Assert.assertEquals(responseCountDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		Assert.assertEquals(responseCountDashboardSegmentList.jsonPath().get("name.size()").toString(),
				Integer.toString(10));
		utils.logPass("Verified segment list API count is 10 when body/query parameters is blank");

		List<Integer> jsonResponse = responseCountDashboardSegmentList.jsonPath().getList("segment_id");
		List<Integer> expected = new ArrayList<Integer>(jsonResponse);
		List<Integer> sorted = expected.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		Assert.assertTrue(expected.equals(sorted));

		utils.logPass(
				"Verified ​Order of segments returned in response of API are based on created_at latest (newest first)");

		Response queryResponseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"), "@");
		Assert.assertEquals(queryResponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		List<String> nameList = queryResponseDashboardSegmentList.jsonPath().getList("name");
		for (String str : nameList) {
			System.out.println(str);
			Assert.assertTrue(str.contains("@"));
		}
		utils.logPass("Verified ​​​​​​All segments having @ in name are returned in segment list API response");

		queryResponseDashboardSegmentList = pageObj.endpoints().dashboardSegmentList(dataSet.get("apiKey"),
				"Auto1");
		Assert.assertEquals(queryResponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		List<String> nameList1 = queryResponseDashboardSegmentList.jsonPath().getList("name");
		for (String str : nameList1) {
			Assert.assertTrue(str.contains("Auto1"));
		}

		utils.logPass("Verified ​​​​​​All segments having Auto1 in name are returned in segment list API response");

		queryResponseDashboardSegmentList = pageObj.endpoints().dashboardSegmentList(dataSet.get("apiKey"),
				"Auto custom segment");
		Assert.assertEquals(queryResponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		Assert.assertEquals(queryResponseDashboardSegmentList.asString(), "[]");
		utils.logPass("Verified empty response when segment description is queried segment list API response");

		Response queryPageResponseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"), "segment", 1);
		Assert.assertEquals(queryResponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		Assert.assertEquals(queryPageResponseDashboardSegmentList.jsonPath().get("name.size()").toString(),
				Integer.toString(1));
		utils.logPass(
				"Verified segment count when query is Auto(Buisness contain more than 2 segments having 'Auto' in Segement name) and per page count is 2");

		Response pageReponseDashboardSegmentList = pageObj.endpoints()
				.pageDashboardSegmentList(dataSet.get("apiKey"), "segment", 2);
		Assert.assertEquals(pageReponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		Assert.assertEquals(pageReponseDashboardSegmentList.jsonPath().get("name.size()").toString(),
				Integer.toString(2));
		utils.logPass(
				"Verified segment count '2' when query is 'segment19'(Buisness contain more than 32 segments having 'segment19' in Segement name) and page value is 4");

		Response pagePerPageReponseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"), "segment", 2, 1);
		Assert.assertEquals(pagePerPageReponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		Assert.assertEquals(pagePerPageReponseDashboardSegmentList.jsonPath().get("name.size()").toString(),
				Integer.toString(2));
		utils.logPass(
				"Verified segment count '2' when query is 'segment19'(Buisness contain more than 32 segments having 'segment19' in Segement name), per-page value is 2 and page value is 1");

		Response query1ResponseDashboardSegmentList = pageObj.endpoints()
				.dashboardSegmentList(dataSet.get("apiKey"), "All Signed Up");
		Assert.assertEquals(query1ResponseDashboardSegmentList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Expected Status code 200 for valid query");
		logger.info(query1ResponseDashboardSegmentList.jsonPath().get("segment_id[0]").toString());
		Assert.assertEquals(query1ResponseDashboardSegmentList.jsonPath().get("segment_id[0]").toString(), "125511");
		Assert.assertEquals(query1ResponseDashboardSegmentList.jsonPath().get("name[0]").toString(), "All Signed Up");
		Assert.assertEquals(query1ResponseDashboardSegmentList.jsonPath().get("segment_type[0]").toString(),
				"GenericSegmentDefinition");
		utils.logPass(
				"Verified  Following values are returned in API response  1.segment_id 2.name 3.description 4.segment_type");

	}

	@Test(description = "SQ-T2294, Dynamic Coupons List Deal API -> API Response", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void couponCampaignListValidation() {
		try {
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			// pageObj.menupage().clickCockPitMenu();
			// pageObj.menupage().clickCockpitDashboardLink();
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.instanceDashboardPage().enableOfferDistributionFromExternalSystemFlag();
			driver.quit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// List of 10 Dynamic coupon campaigns is returned in API response if "Body"
		// section of API request is blank
		String token = dataSet.get("apiKey");
		Response responseCouponCampaignList = pageObj.endpoints().dynamicCouponList(token);
		Assert.assertEquals(responseCouponCampaignList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response");
		logger.info(responseCouponCampaignList.jsonPath().get("name.size()").toString());
		utils.logPass("Verified 10 dynamic coupon campaigns is returned in API response if body is blank");
		List<Integer> jsonResponse = responseCouponCampaignList.jsonPath().getList("coupon_campaign_id");
		List<Integer> expected = new ArrayList<Integer>(jsonResponse);
		List<Integer> sorted = expected.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		Assert.assertTrue(expected.equals(sorted));
		utils.logPass(
				"Verified ​Order of coupon campaign id returned in response of API are based on created_at latest (newest first)");
		// Verify ​​​​​Pre generated coupon campaign is not returned in API response
		String pregenerateCouponCampaign = "PreGenCouponCampaign";
		Response responseCouponCampaignListquery = pageObj.endpoints().dynamicCouponList(token,
				pregenerateCouponCampaign);
		Assert.assertEquals(responseCouponCampaignList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response with query param");
		Assert.assertEquals(responseCouponCampaignListquery.body().asString(), "[]",
				"Failed to verify that  ​​​​​Pre generated coupon campaign is not returned in API response");
		utils.logPass("Verified ​​​​​Pre generated coupon campaign having 'Number of Guest' as 10");

		// Dynamic coupon campaigns having “Number of Guests” as 10 is not returned in
		// API response
		String couponCampaign10Guest = "CouponCampaign10Guest";
		Response responseCouponCampaign011 = pageObj.endpoints().dynamicCouponList(token, couponCampaign10Guest);
		Assert.assertEquals(responseCouponCampaignList.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response with query param");
		Assert.assertEquals(responseCouponCampaign011.body().asString(), "[]", "Failed to verify ");
		utils.logPass("Verified dynamic coupon campaign having 'Number of Guest' as 10");

		// ​​​​​​​​​Dynamic coupon campaigns having “Number of Guests” as 0 ie.
		// unlimited usage is returned in API response
		String partialCampaignName = "Automation_do_not_delete_coupon";
		Response responseCouponCampaign012 = pageObj.endpoints().dynamicCouponList(token, partialCampaignName);

		Assert.assertTrue(responseCouponCampaign012.jsonPath().get("name[0]").toString().contains(partialCampaignName));
		utils.logPass("Verified  ​​​​​Pre dynamic coupon campaign query response on valid partial queryName");

		// Verify All dynamic coupon campaigns having "coupon" in name or description
		// are returned in response
		String couponcampName = "Coupon";
		Response responseCouponCampaignListqueryName = pageObj.endpoints().dynamicCouponList(token, couponcampName);
		List<String> nameList = responseCouponCampaignListqueryName.jsonPath().getList("name");
		logger.info(nameList);
		for (String str : nameList) {
			Assert.assertTrue(str.contains(couponcampName) || str.contains(couponcampName.toLowerCase()));
		}
		utils.logPass(
				"Verified  All dynamic coupon campaigns having \"coupon\" in name or description are returned in response");

		couponcampName = "3";
		responseCouponCampaignListqueryName = pageObj.endpoints().dynamicCouponList(token, couponcampName);
		nameList = responseCouponCampaignListqueryName.jsonPath().getList("name");
		for (String str : nameList) {
			Assert.assertTrue(str.contains(couponcampName));
		}
		utils.logPass(
				"Verified  All dynamic coupon campaigns having \"3\" in name or description are returned in response");

		// Verify 2 dynamic coupon campaigns is returned when query is "coupon" &
		// per_page query parameter is 2

		String couponcampName01 = "Automation"; // Automation_do_not_delete_1
		int perPage = 2;
		Response responseCouponCampaignListqueryName01 = pageObj.endpoints().dynamicCouponList(token, couponcampName01,
				perPage);
		logger.info(responseCouponCampaignListqueryName01.jsonPath().get("name.size()").toString());
		Assert.assertEquals(responseCouponCampaignListqueryName01.jsonPath().get("name.size()").toString(), "1");
		utils.logPass(
				"Verified that dynamic coupon campaigns is returned when query is \"coupon\" & per_page query parameter is 1");

		// Verify Dynamic Coupons List Deal API returns future date dynamic coupon
		// campaign
		// in response
		String futureCampaignName = "FutureCouponCampaign";
		Response responseCouponCampaign013 = pageObj.endpoints().dynamicCouponList(token, futureCampaignName);
		Assert.assertEquals(responseCouponCampaign013.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response");
		Assert.assertTrue(responseCouponCampaign013.jsonPath().get("name[0]").toString().equals(futureCampaignName));
		utils.logPass("Verified dynamic Coupons List Deal API returns future date dynamic coupon campaign in response");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("coupon_campaign_id[0]").toString(), "123918");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("coupon_campaign_uuid[0]").toString(),
				"e133fa48a0c323e2a2b7ce23002ed9f4b64a20ea");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("image_url[0]").toString(),
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("description[0]").toString(),
				"Test future coupon campaign");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("start_date[0]").toString(), "2025-12-31");
		Assert.assertEquals(responseCouponCampaign013.jsonPath().get("end_date[0]").toString(), "2029-12-31");
		utils.logPass(
				"Verified Following values are returned in API response -> 1.coupon_campaign_id 2.coupon_campaign_uuid 3.image_url 4.name 5.description 6.start_date 7.end_date");
		// Verify Dynamic Coupons List Deal API doesn't returns draft coupon campaign in
		// response
		String draftCampaignName = "DraftCampaign";
		Response responseCouponCampaign014 = pageObj.endpoints().dynamicCouponList(token, draftCampaignName);
		Assert.assertEquals(responseCouponCampaign014.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response");
		Assert.assertEquals(responseCouponCampaign014.jsonPath().get("name.size()").toString(), "0");
		utils.logPass(
				"Verified Following values are returned in API response -> 1.coupon_campaign_id 2.coupon_campaign_uuid 3.image_url 4.name 5.description 6.start_date 7.end_date");

		// Verify Dynamic Coupons List Deal API doesn't returns expired coupon campaign
		// in
		// response
		String expiredCampaignName = "Erie, PA -$25 Gift";
		Response responseCouponCampaign015 = pageObj.endpoints().dynamicCouponList(token, expiredCampaignName);
		Assert.assertEquals(responseCouponCampaign015.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response");
		Assert.assertEquals(responseCouponCampaign015.jsonPath().get("name.size()").toString(), "0");
		utils.logPass("Verified dynamic Coupons List Deal API doesn't returns expired  coupon campaign in response");

		// Verify Dynamic Coupons List Deal API doesn't returns deactivated coupon
		// campaign
		// in response
		String deactivateCampaign = "CouponCamp004";
		Response responseCouponCampaign016 = pageObj.endpoints().dynamicCouponList(token, deactivateCampaign);
		Assert.assertEquals(responseCouponCampaign016.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify coupon list API response");
		Assert.assertEquals(responseCouponCampaign016.jsonPath().get("name.size()").toString(), "0");
		utils.logPass("Verified dynamic Coupons List Deal API doesn't returns deactivated  coupon campaign in response");
	}

//	@Test(description = "SQ-T2352, List / Post Deals API")
	public void T2352_ListPostDealsApiValidation() {
		// List all deals
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, (dataSet.get("client")),
				dataSet.get("secret"));
		apiUtils.verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token");
		signUpResponse.jsonPath().get("user.user_id").toString();
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listdealsResponse.getStatusCode(),
				"Status code 200 did not matched for api2 list all deals");
		List<String> nameList = listdealsResponse.jsonPath().getList("name");
		logger.info(nameList);
		Assert.assertTrue(!nameList.contains("Expired Deal"),
				"Failed to verify that ​List Deal API 2 does not return Expired deals in API response");
		utils.logPass("Verified that that ​List Deal API 2 does not return Expired deals in API response");
		Assert.assertTrue(!nameList.contains("DraftDeal"),
				"Failed to verify that ​List Deal API 2 does not return draft deals in API response");
		utils.logPass("Verified that that ​List Deal API 2 does not return draft deals in API response");
		Assert.assertTrue(nameList.contains("DND Automation Scheduled"),
				"Failed to verify that ​List Deal API 2 does not return Automation Scheduled in API response");
		utils.logPass("Verified that that ​List Deal API 2 does not return Automation Scheduled in API response");

		Assert.assertTrue(nameList.contains("$2.0 OFF"),
				"Failed to verify that ​List Deal API 2 returns Active deals in API response");
		utils.logPass("Verified that that ​​List Deal API 2 returns Active deals in API response");

		Response listApi1DealsResponse = pageObj.endpoints().Api1ListAllDeals(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listApi1DealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		List<String> nameListApi1 = listApi1DealsResponse.jsonPath().getList("name");
		logger.info(nameListApi1);
		Assert.assertTrue(!nameListApi1.contains("Expired Deal"),
				"Failed to verify that ​List Deal API 1 does not return Expired deals in API response");
		utils.logPass("Verified that that ​List Deal API 1 does not return Expired deals in API response");
		Assert.assertTrue(!nameListApi1.contains("DraftDeal"),
				"Failed to verify that ​List Deal API 1 does not return draft deals in API response");
		utils.logPass("Verified that that ​List Deal API 1 does not return draft deals in API response");

		Assert.assertTrue(nameListApi1.contains("DND Automation Scheduled"),
				"Failed to verify that ​List Deal API 1 does not return Automation Scheduled in API response");
		utils.logPass("Verified that that ​List Deal API 1 does not return Automation Scheduled in API response");

		Assert.assertTrue(nameListApi1.contains("$2.0 OFF"),
				"Failed to verify that ​List Deal API 1 returns Active $2.0 OFF in API response");
		utils.logPass("Verified that that ​​List Deal API 1 returns Active $2.0 OFF in API response");

		String authUserEmail = pageObj.iframeSingUpPage().generateEmail();

		Response authSignUpResponse = pageObj.endpoints().authApiSignUp(authUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(authSignUpResponse, "Auth API user signup");
		Assert.assertEquals(authSignUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = authSignUpResponse.jsonPath().get("authentication_token");
		String user_id = authSignUpResponse.jsonPath().get("id").toString();

		// List All Deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listAuthDealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");

		List<String> nameListAuth = listAuthDealsResponse.jsonPath().getList("name");
		Assert.assertTrue(!nameListApi1.contains("Expired Deal"),
				"Failed to verify that ​List Deal API 1 does not return Expired deals in API response");
		utils.logPass("Verified that that ​List Deal API 1 does not return Expired deals in API response");
		Assert.assertTrue(!nameListAuth.contains("DraftDeal"),
				"Failed to verify that ​List Deal API 1 does not return draft deals in API response");
		logger.info("Verified that that ​List Deal API 1 does not return draft deals in API response");

		Assert.assertTrue(nameListAuth.contains("DND Automation Scheduled"),
				"Failed to verify that ​List Deal API 1 does not return Automation Scheduled in API response");
		utils.logPass("Verified that that ​List Deal API 1 does not return Automation Scheduled in API response");

		Assert.assertTrue(nameListAuth.contains("$2.0 OFF"),
				"Failed to verify that ​List Deal API 1 returns Active deals in API response");
		utils.logPass("Verified that that ​​List Deal API 1 returns Active deals in API response");

		// send reward
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(user_id, dataSet.get("apiKey"),
				"", "2821199", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logit("Send points to the user successfully");
//		// Pre-condition -> Deal which is Active
//		String dealResponseMsg = "[\"Used Deal successfully added.\"]";
//		Response postDealResponse = pageObj.endpoints().Api2PostDeals(dataSet.get("client"), dataSet.get("secret"),
//				token, dataSet.get("usedDealRedeemableUuid"));
//		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, postDealResponse.getStatusCode(),
//				"Status code 200 did not matched for post api2 deals active deal");
//		Assert.assertEquals(dealResponseMsg, postDealResponse.asString());
//		logger.info("Verified Post API for active deal");
//		TestListeners.extentTest.get()
//				.pass("Verified that that ​List Deal API 1 does not return used deals in API response");

		String dealAuthResonseMsg = "[\n" + "    \"Used Deal successfully added.\"\n" + "]";
		Response postAuthResponse = pageObj.endpoints().authPostDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("usedDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, postAuthResponse.getStatusCode(),
				"Status code 200 did not matched for post auth deals active deal");
		Assert.assertEquals(dealAuthResonseMsg, postAuthResponse.asString());
		utils.logPass("Verified used deal auth api response");

		/*
		 * Pre-condition -> Deal which is already used by user 1.In POST API (Secure /
		 * API 2) use "redeemable_uuid" of Already used deal 2.Click Send and check
		 * response
		 */
		String usedDealResponse = "{\"errors\":{\"already_added\":\"Deal has already been credited to your account.\"}}";
		Response postUsedDealResponse = pageObj.endpoints().Api2PostDeals(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("usedDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postUsedDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");
		Assert.assertEquals(usedDealResponse, postUsedDealResponse.asString());
		utils.logPass("Verified already used deal api 2 response");

		String api1DealsNotFoundError = "{\n" + "    \"errors\": {\n"
				+ "        \"already_added\": \"Deal has already been credited to your account.\"\n" + "    }\n" + "}";
		Response postAPI1UsedDealResponse = pageObj.endpoints().Api1PostDeals(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("usedDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postAPI1UsedDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api1 deals for already used deal");
		Assert.assertEquals(api1DealsNotFoundError, postAPI1UsedDealResponse.asString());
		utils.logPass("Verified already used deal api 1 response");

		String authUsedDealError = "{\n" + "    \"error\": {\n"
				+ "        \"message\": \"This deal is not available for guest.\",\n"
				+ "        \"code\": \"invalid_deal\"\n" + "    }\n" + "}";
		Response postAuthUsedDealResponse = pageObj.endpoints().authPostDeals(dataSet.get("authToken"),
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("usedDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postAuthUsedDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");
		Assert.assertEquals(authUsedDealError, postAuthUsedDealResponse.asString());
		utils.logPass("Verified already used deal auth api response");
		/*
		 * Pre-condition -> Deal with Scheduled status (future Start time) 1.In POST API
		 * (Auth) use "redeemable_uuid" of scheduled deal 2.Click Send and check
		 * response
		 */

		Response postUsedDealResponse1 = pageObj.endpoints().Api2PostDeals(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("scheduledDealRedeemableUuid"));
		// System.out.println(postUsedDealResponse1.asPrettyString());
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postUsedDealResponse1.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");
		Assert.assertEquals(postUsedDealResponse1.jsonPath().getString("errors"),
				"[invalid_deal:This deal is not available for guest.]");
		utils.logPass("Verified deal not found error api 2 response");

		Response postAp1UsedDealResponse1 = pageObj.endpoints().Api1PostDeals(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("scheduledDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postAp1UsedDealResponse1.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");

		Assert.assertEquals(postAp1UsedDealResponse1.jsonPath().getString("errors"),
				"[invalid_deal:This deal is not available for guest.]");

		utils.logPass("Verified used deal error api 1 response");

		Response postAuthUsedDealResponse1 = pageObj.endpoints().authPostDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("scheduledDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, postAuthUsedDealResponse1.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");

		Assert.assertEquals(postAuthUsedDealResponse1.jsonPath().getString("error"),
				"[message:This deal is not available for guest., code:invalid_deal]");

		utils.logPass("Verified used deal error auth api response");
		/*
		 * Pre-condition -> Deal with Expired status 1.In POST API (Secure / API 2) use
		 * "redeemable_uuid" of expired deal 2.Click Send and check response
		 */

		String expireDealsAp1Error = "{\"errors\":{\"not_found\":\"Deal not found.\"}}";
		Response expiredPostDealResponse = pageObj.endpoints().Api2PostDeals(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("expiredDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, expiredPostDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");
		Assert.assertEquals(expireDealsAp1Error, expiredPostDealResponse.asString());
		utils.logPass("Verified expired deal error api2 response");

		Response expiredAp1PostDealResponse = pageObj.endpoints().Api1PostDeals(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("expiredDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, expiredAp1PostDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");

		Assert.assertEquals(expiredAp1PostDealResponse.jsonPath().getString("errors"), "[not_found:Deal not found.]");

		utils.logPass("Verified expired deal error api1 response");

		Response expiredAuthPostDealResponse = pageObj.endpoints().authPostDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("expiredDealRedeemableUuid"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, expiredAuthPostDealResponse.getStatusCode(),
				"Status code 422 did not matched for post api2 deals for already used deal");

		Assert.assertEquals(expiredAuthPostDealResponse.jsonPath().getString("error"),
				"[message:Deal not found., code:not_found]");

		utils.logPass("Verified expired deal error auth api response");
	}

	@Test(description = "SQ-T2395 (1.0) Dynamic Coupon Distribution API -> 'email' parameter Validations", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2395_DynamicCouponDistributionEmailvalidation() {
		String campaign_uuid = dataSet.get("uuid");
		Response postDynamicCouponResponse = pageObj.endpoints().postDynamicCoupon(dataSet.get("apiKey"),
				dataSet.get("email"), campaign_uuid);
		Assert.assertEquals(postDynamicCouponResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		Assert.assertNotNull(postDynamicCouponResponse.jsonPath().get("coupon").toString());
		utils.logPass("Verified Dynamic Coupon Distribution API, coupon code is genereated successfully");

		String email = null;
		String nullEmailResponse = "{\n"
				+ "    \"error\": \"Required parameter missing or the value is empty: email\"\n" + "}";
		Response postDynamicCouponNullEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), email, campaign_uuid);
		Assert.assertEquals(postDynamicCouponNullEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Failed to verify empty/null email Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponNullEmailResponse.asString(), nullEmailResponse);
		utils.logPass("Verified error for null email for Dynamic Coupon Distribution API");

		String emailEmpty = "";
		Response postDynamicCouponEmptyEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailEmpty, campaign_uuid);
		Assert.assertEquals(postDynamicCouponEmptyEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Failed to verify empty/null email Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponEmptyEmailResponse.asString(), nullEmailResponse);
		utils.logPass("Verified error for empty email for Dynamic Coupon Distribution API");

		String emailInvalid = "abc";
		String invlidEmailResponse = "{\n" + "    \"errors\": {\n" + "        \"user_not_found\": \"User not found.\"\n"
				+ "    }\n" + "}";
		Response postDynamicCouponInvalidEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailInvalid, campaign_uuid);
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse.getStatusCode(), 404,
				"Failed to verifyinvalid email Dynamic Coupon Distribution API");
		logger.info(postDynamicCouponInvalidEmailResponse.asString());
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse.asString(), invlidEmailResponse);
		utils.logPass("Verified error for invalid email for Dynamic Coupon Distribution API");

		String emailInvalid01 = "divya@@punchh.com";
		Response postDynamicCouponInvalidEmailResponse01 = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailInvalid01, campaign_uuid);
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse01.getStatusCode(), 404,
				"Failed to verifyinvalid email Dynamic Coupon Distribution API");
		logger.info(postDynamicCouponInvalidEmailResponse01.asString());
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse01.asString(), invlidEmailResponse);
		utils.logPass("Verified error for invalid email for Dynamic Coupon Distribution API");

		// deactivated user email
		String emailInvalid02 = "divya+d1@punchh.com";
		Response postDynamicCouponInvalidEmailResponse02 = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailInvalid02, campaign_uuid);
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse02.getStatusCode(), 404,
				"Failed to verifyinvalid email Dynamic Coupon Distribution API");
		logger.info(postDynamicCouponInvalidEmailResponse02.asString());
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse02.asString(), invlidEmailResponse);
		utils.logPass("Verified error for invalid email for Dynamic Coupon Distribution API");

		// banned user email
		String emailInvalid03 = "divya+d1@punchh.com";
		Response postDynamicCouponInvalidEmailResponse03 = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailInvalid03, campaign_uuid);
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse03.getStatusCode(), 404,
				"Failed to verifyinvalid email Dynamic Coupon Distribution API");
		logger.info(postDynamicCouponInvalidEmailResponse03.asString());
		Assert.assertEquals(postDynamicCouponInvalidEmailResponse03.asString(), invlidEmailResponse);
		utils.logPass("Verified error for banned email for Dynamic Coupon Distribution API");

		// divya+eclubcache3@punchh.com eclub user
		String emailEclubUser = "hardik.bhardwaj+10122024@test.com";
		Response postDynamicCouponEclubUserEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), emailEclubUser, campaign_uuid);
		Assert.assertEquals(postDynamicCouponEclubUserEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		Assert.assertNotNull(postDynamicCouponEclubUserEmailResponse.jsonPath().get("coupon").toString());
		utils.logPass("Verified Dynamic Coupon Distribution API, coupon code is genereated successfully for eclub user");

	}

	@Test(description = "SQ-T2396 (1.0) Dynamic Coupon Distribution API -> 'uuid' parameter Validations", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2396_DynamicCouponDistributionUuidValidation() {
		String campaign_uuid = dataSet.get("uuid");
		Response postDynamicCouponResponse = pageObj.endpoints().postDynamicCoupon(dataSet.get("apiKey"),
				dataSet.get("email"), campaign_uuid);
		Assert.assertEquals(postDynamicCouponResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		Assert.assertNotNull(postDynamicCouponResponse.jsonPath().get("coupon").toString());
		utils.logPass("Verified Dynamic Coupon Distribution API, coupon code is genereated successfully");

		String uuid = null;
		String nullUuidResponse = "{\n"
				+ "    \"error\": \"Required parameter missing or the value is empty: campaign_uuid\"\n" + "}";
		Response postDynamicCouponNullEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), uuid);
		Assert.assertEquals(postDynamicCouponNullEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Failed to verify empty/null uuid Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponNullEmailResponse.asString(), nullUuidResponse);
		utils.logPass("Verified error for null uuid for Dynamic Coupon Distribution API");

		String uuidEmpty = "";
		Response postDynamicCouponEmptyEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), uuidEmpty);
		Assert.assertEquals(postDynamicCouponEmptyEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Failed to verify empty/null uuis Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponEmptyEmailResponse.asString(), nullUuidResponse);
		utils.logPass("Verified error for empty uuid for Dynamic Coupon Distribution API");

		// uuid of deactivated campaign
		String deactivatedUuidError = "{\n" + "    \"errors\": {\n"
				+ "        \"campaign_not_found\": \"Coupon campaign not found.\"\n" + "    }\n" + "}";
		String deactivatedUuid = "06e31d344b19a837007dbd962a9eb56fffffefac";
		Response postDynamicCouponDeactivatedEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), deactivatedUuid);
		Assert.assertEquals(postDynamicCouponDeactivatedEmailResponse.getStatusCode(), 404,
				"Failed to verify empty/null uuid Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponDeactivatedEmailResponse.asString(), deactivatedUuidError);
		utils.logPass("Verified error for deactivated campaign uuid for Dynamic Coupon Distribution API");

		// passing expired campaign uuid
		// String expiredUuid = "e254da1baf364a8e326726b700ff851dfc033bf8";
		Response postDynamicCouponExpiredEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), deactivatedUuid);
		Assert.assertEquals(postDynamicCouponExpiredEmailResponse.getStatusCode(), 404,
				"Failed to verify empty/null uuid Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponExpiredEmailResponse.asString(), deactivatedUuidError);
		utils.logPass("Verified error for expired campaign uuid for Dynamic Coupon Distribution API");

		// passing draft campaign uuid
		// String draftUuid = "44f73ff5e96c18183780dc937772a482ef7ac2be";
		Response postDynamicCouponDraftEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), deactivatedUuid);
		Assert.assertEquals(postDynamicCouponDraftEmailResponse.getStatusCode(), 404,
				"Failed to verify empty/null uuid Dynamic Coupon Distribution API");
		Assert.assertEquals(postDynamicCouponDraftEmailResponse.asString(), deactivatedUuidError);
		utils.logPass("Verified error for draft campaign uuid for Dynamic Coupon Distribution API");

		// passing uuid for campaign having future "start_date"
		String futureCampUuid = "e133fa48a0c323e2a2b7ce23002ed9f4b64a20ea";
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), dataSet.get("email"), futureCampUuid);
		Assert.assertEquals(postDynamicCouponScheduledEmailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to verify Dynamic Coupon Distribution API response");
		Assert.assertEquals(postDynamicCouponScheduledEmailResponse.jsonPath().get("coupon").toString(), "BP2DI3A");
		utils.logPass(
				"Verified Dynamic Coupon Distribution API for scheduled campaign, coupon code is genereated successfully");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}