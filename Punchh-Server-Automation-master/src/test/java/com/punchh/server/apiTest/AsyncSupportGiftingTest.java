package com.punchh.server.apiTest;

import java.lang.reflect.Method;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AsyncSupportGiftingTest {
	static Logger logger = LogManager.getLogger(AsyncSupportGiftingTest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	Properties prop;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	private String env;
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		apiUtils = new ApiUtils();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}
	
	// akansha jain - commented this case as flag dependency is removed. This TC is covered in PTC case: T7268_supportGiftingPointToCurrency
	//@Test(description = "SQ-T6474 Validate the point gifting functionality of the API", groups = "api", priority = 7)
	public void SQ_T6474_NewSupportPointsGiftingApiValidation() {
		// :enable_optimised_support_gifting: true
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send points to the user via new support gifting api
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send points to user is successful");

	}

	// akansha jain - commented this case as flag dependency is removed. This TC is covered in PTC case: T7268_supportGiftingPointToCurrency
	//@Test(description = "SQ-T6475 Validate the reward gifting functionality of the API", groups = "api", priority = 7)
	public void SQ_T6475_NewSupportRewardGiftingApiValidation() {
		// :enable_optimised_support_gifting: true
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send offer to the user via new support gifting api
		Response sendOffersResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("redeemable_id"), dataSet.get("end_date"));
		Assert.assertEquals(sendOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send offer to user is successful");

	}

	// akansha jain
	@Test(description = "SQ-T6609 Validate the default behavior of the exclude_from_membership_points parameter when not provided (null or missing) in api.", groups = "api", priority = 7)
	public void SQ_T6609_DefaultValueOfExcludeMembershipPointsInSupportApi() throws Exception {
		/*
		 * Need :exclude_gifted_points_from_tier_progression: true,
		 * :enable_optimised_support_gifting:true and :enable_expiry_unification: true
		 */
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send points to the user via support gifting api without passing
		// exclude_from_membership_point key value pair
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send points to user is successful");

		// verify exclude_from_membership_point value in DB after hitting the api (check
		// by default case)
		String latestCheckinId = "select id from checkins where user_id = '${userID}' ORDER By id DESC limit 1";
		String getlatestCheckinId = latestCheckinId.replace("${userID}", userID);
		logger.info("Latest checkin id:" + getlatestCheckinId);
		String getlatestCheckinIdValue = DBUtils.executeQueryAndGetColumnValue(env,
				getlatestCheckinId, "id");

		String getExcludeFromMembershipPointValueQuery = "select exclude_from_membership_points from checkins where user_id = '${userID}' and id = '${getlatestCheckinIdValue}'";
		getExcludeFromMembershipPointValueQuery = getExcludeFromMembershipPointValueQuery.replace("${userID}", userID)
				.replace("${getlatestCheckinIdValue}", getlatestCheckinIdValue);
		logger.info("exclude_from_membership_points column's value for user's latest checkin:"
				+ getExcludeFromMembershipPointValueQuery);
		String getExcludeFromMembershipPointValue = DBUtils.executeQueryAndGetColumnValue(env,
				getExcludeFromMembershipPointValueQuery, "exclude_from_membership_points");
		Assert.assertEquals(getExcludeFromMembershipPointValue, "",
				"exclude_from_membership_points column value from checkins table not matched");
		utils.logPass(
				"exclude_from_membership_points column value is saved by default blank in checkins table and the gifted points wont be included in tier progression.");

	}

	// akansha jain
	@Test(description = "SQ-T6610 Validate the behavior when exclude_from_membership_points is passed as false in the API request.", groups = "api", priority = 7)
	public void SQ_T6610_ExcludeMembershipPointsValueAsFalseInSupportApi() throws Exception {
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send points to the user via support gifting api with passing
		// exclude_from_membership_point value as false
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserWithExcludeFlagFalse(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send points to user is successful");

		// verify exclude_from_membership_point value in DB after hitting the api with
		// passing false in exclude_from_membership_points key
		String latestCheckinId = "select id from checkins where user_id = '${userID}' ORDER By id DESC limit 1";
		String getlatestCheckinId = latestCheckinId.replace("${userID}", userID);
		logger.info(getlatestCheckinId);
		String getlatestCheckinIdValue = DBUtils.executeQueryAndGetColumnValue(env,
				getlatestCheckinId, "id");
		String getExcludeFromMembershipPointValueQuery = "select exclude_from_membership_points from checkins where user_id = '${userID}' and id = '${getlatestCheckinIdValue}'";
		getExcludeFromMembershipPointValueQuery = getExcludeFromMembershipPointValueQuery.replace("${userID}", userID)
				.replace("${getlatestCheckinIdValue}", getlatestCheckinIdValue);
		logger.info(getExcludeFromMembershipPointValueQuery);
		String getExcludeFromMembershipPointValue = DBUtils.executeQueryAndGetColumnValue(env,
				getExcludeFromMembershipPointValueQuery, "exclude_from_membership_points");
		Assert.assertNull(getExcludeFromMembershipPointValue,
				"exclude_from_membership_points column value from checkins table not matched");
		utils.logPass(
				"exclude_from_membership_points column value is saved as NULL in checkins table and the gifted points will be included in tier progression.");

	}

	// akansha jain
	@Test(description = "SQ-T6611 Validate the behavior when exclude_from_membership_points is passed as true in the API request.", groups = "api", priority = 7)
	public void SQ_T6611_ExcludeMembershipPointsValueAsTrueInSupportApi() throws Exception {
		/*
		 * Need :exclude_gifted_points_from_tier_progression: true,
		 * :enable_optimised_support_gifting:true and :enable_expiry_unification: true
		 */
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send points to the user via support gifting api with passing
		// exclude_from_membership_point value as true
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserWithExcludeFlagTrue(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send points to user is successful");

		// verify exclude_from_membership_point value in DB after hitting the api with
		// passing true in exclude_from_membership_points key
		String latestCheckinId = "select id from checkins where user_id = '${userID}' ORDER By id DESC limit 1";
		String getlatestCheckinId = latestCheckinId.replace("${userID}", userID);
		logger.info(getlatestCheckinId);
		String getlatestCheckinIdValue = DBUtils.executeQueryAndGetColumnValue(env,
				getlatestCheckinId, "id");
		String getExcludeFromMembershipPointValueQuery = "select exclude_from_membership_points from checkins where user_id = '${userID}' and id = '${getlatestCheckinIdValue}'";
		getExcludeFromMembershipPointValueQuery = getExcludeFromMembershipPointValueQuery.replace("${userID}", userID)
				.replace("${getlatestCheckinIdValue}", getlatestCheckinIdValue);
		logger.info(getExcludeFromMembershipPointValueQuery);
		String getExcludeFromMembershipPointValue = DBUtils.executeQueryAndGetColumnValue(env,
				getExcludeFromMembershipPointValueQuery, "exclude_from_membership_points");
		Assert.assertEquals(getExcludeFromMembershipPointValue, "",
				"exclude_from_membership_points column value from checkins table not matched");
		utils.logPass(
				"exclude_from_membership_points column value is saved by default blank in checkins table and the gifted points wont be included in tier progression.");

	}

	// akansha jain
	@Test(description = "SQ-T6612 Validate the behavior when exclude_from_membership_points is passed with offer gifting from api.", groups = "api", priority = 7)
	public void SQ_T6612_SupportRewardGiftingApiWithExcludeFlag() {
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send offer to the user via new support gifting api with passing
		// exclude_from_membership_points key value pair
		Response sendOffersResponse = pageObj.endpoints().sendOfferToUserWithExcludeFlag(userID,
				dataSet.get("apiKey"), dataSet.get("redeemable_id"), dataSet.get("end_date"));
		Assert.assertEquals(sendOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send offer to user with exclude flag is successful");

	}

	// akansha jain - commented this case as flag dependency is removed. This TC is covered in PTC case: T7268_supportGiftingPointToCurrency
	//@Test(description = "SQ-T6855 Validate the currency gifting functionality in the API.", groups = "api", priority = 7)
	public void SQ_T6855_NewSupportCurrencyGiftingApiValidation() {
		// :enable_optimised_support_gifting: true
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send currency to the user via new support gifting api
		Response sendCurrencyResponse = pageObj.endpoints().sendCurrencyToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("adminAuthorization"), dataSet.get("rewardAmount"));
		Assert.assertEquals(sendCurrencyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send amount to user is successful");
	}

	// akansha jain
	@Test(description = "SQ-T6856 Validate the visits gifting functionality in the API.", groups = "api", priority = 7)
	public void SQ_T6856_NewSupportVisitGiftingApiValidation() throws Exception {
		// enable_optimised_support_gifting should be false for Point To Visits
		// business
		String businessId = dataSet.get("business_id");
		String query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optimised_support_gifting", businessId);
		utils.logit("enable_optimised_support_gifting is set as false for Point Unlock Staged business: "
				+ dataSet.get("slug"));
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send visits to the user via new support gifting api
		Response sendVisitsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("visits"));
		Assert.assertEquals(sendVisitsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 202 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send visits to user is successful");

	}

	// akansha jain
	@Test(description = "SQ-T6530 Validate the incorrect json error for the API false", groups = "api", priority = 7)
	public void SQ_T6530_NewSupportGiftingApiIncorrectJsonValidation() {
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send points to the user via new support gifting api with invalid json
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPIInvalidJson(userID,
				dataSet.get("adminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST, "Status code 400 did not matched for dashboard api2 support gifting to user with invalid json");
		TestListeners.extentTest.get().pass("Api2 send offer to user is not successful with invalid json.");
		String sendOffersResponseError = sendPointsResponse.jsonPath().get("error");
		Assert.assertEquals(sendOffersResponseError, "data was not valid JSON");
		utils.logPass("Verified new support gifting api with invalid json");

	}

	// akansha jain
	@Test(description = "SQ-T6531 Validate the incorrect admin key error for the API false", groups = "api", priority = 7)
	public void SQ_T6531_NewSupportGiftingApiIncorrectAdminKeyValidation() {
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Step 1
		// send points to the user via new support gifting api with invalid admin
		// authorization
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("invalidAdminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for dashboard api2 support gifting to user with invalid admin authorization");
		TestListeners.extentTest.get()
				.pass("Api2 send offer to user is not successful with invalid admin authorization.");
		String sendOffersResponseError = sendPointsResponse.jsonPath().get("error");
		Assert.assertEquals(sendOffersResponseError, "You need to sign in or sign up before continuing.");
		utils.logPass("Verified new support gifting api with invalid admin authorization");

		// Step 2
		// send points to the user via new support gifting api with blank admin
		// authorization
		Response sendPointsResponse2 = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("blankAdminAuthorization"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED, "Status code 401 did not matched for dashboard api2 support gifting to user with invalid admin authorization");
		TestListeners.extentTest.get()
				.pass("Api2 send offer to user is not successful with invalid admin authorization.");
		String sendOffersResponseError2 = sendPointsResponse2.jsonPath().get("error");
		Assert.assertEquals(sendOffersResponseError2, "You need to sign in or sign up before continuing.");
		utils.logPass("Verified new support gifting api with invalid admin authorization");

	}
	
	// akansha jain
	@Test(description = "SQ-T6857 Validate the error handling for currency invalid requests in new support gifting api.", groups = "api", priority = 7)
	public void SQ_T6857_NewSupportCurrencyGiftingIncorrectAmountValidation() {
		// :enable_optimised_support_gifting: true
		// User register/sign up using API2 Sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Step 1
		// send currency to the user via new support gifting api with blank rewardAmount
		Response sendCurrencyResponse = pageObj.endpoints().sendCurrencyToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("rewardAmount1"));
		int sendCurrencyResponseCode = sendCurrencyResponse.getStatusCode();
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, sendCurrencyResponseCode,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send amount to user is unsuccessful");
		utils.logPass("Verified new support gifting api with blank reward_amount passing in api.");

		// Step 2
		// send currency to the user via new support gifting api with invalid rewardAmount
		Response sendCurrencyResponse2 = pageObj.endpoints().sendCurrencyToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("rewardAmount2"));
		int sendCurrencyResponseCode2 = sendCurrencyResponse2.getStatusCode();
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, sendCurrencyResponseCode2,
				"Status code 422 did not matched for dashboard api2 support gifting to user");
		TestListeners.extentTest.get().pass("Api2 send amount to user is unsuccessful");
		utils.logPass("Verified new support gifting api with invalid reward_amount passing in api.");
	}

	@Test(description = "[Point To Currency] SQ-T7268 Verify functionality of Async Support Gifting API with all gift types with no flag dependency", groups = "api", priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T7268_supportGiftingPointToCurrency() throws Exception {
		// enable_optimised_support_gifting should be false
		String businessId = dataSet.get("business_id");
		String query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optimised_support_gifting", businessId);
		utils.logit("enable_optimised_support_gifting is set as false");

		// User Sign up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 User signup");
		utils.logit("pass", "API2 user signup is successful for user [" + userID + "]");

		// SQ-T7268 Step 2: send points to the user via support gifting api
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send points to user [" + userID + "] is successful");

		// SQ-T7268 Step 3: send currency to the user via support gifting api
		Response sendCurrencyResponse = pageObj.endpoints().sendCurrencyToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("rewardAmount"));
		Assert.assertEquals(sendCurrencyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "Api2 send reward amount to user [" + userID + "] is successful");

		// SQ-T7268 Step 4: send offer to the user via support gifting api
		Response sendOffersResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("redeemableId"), dataSet.get("endDate"));
		Assert.assertEquals(sendOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send offer to user [" + userID + "] is successful");

	}

	@Test(description = "[Point Unlock Staged & Point To Reward] SQ-T7268 Verify functionality of Async Support Gifting API with all gift types with no flag dependency", groups = "api", priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T7268_supportGiftingPointUnlockStagedAndPointToReward() throws Exception {
		// enable_optimised_support_gifting should be false for Point Unlock Staged
		// business
		String businessId = dataSet.get("business_id");
		String query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optimised_support_gifting", businessId);
		utils.logit("enable_optimised_support_gifting is set as false for Point Unlock Staged business: "
				+ dataSet.get("slug"));

		// User Sign up for Point Unlock Staged
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 User signup");
		utils.logit("pass", "API2 user signup is successful for user [" + userID + "]");

		// SQ-T7268 Step 1: send points to the user via support gifting api
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send points to user [" + userID + "] is successful");

		// enable_optimised_support_gifting should be false for Point To Reward
		// business
		businessId = dataSet.get("businessId2");
		query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optimised_support_gifting", businessId);
		utils.logit("enable_optimised_support_gifting is set as false for Point Unlock Staged business: "
				+ dataSet.get("slug2"));

		// User Sign up for Point To Reward
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client2"),
				dataSet.get("secret2"));
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 User signup");
		utils.logit("pass", "API2 user signup is successful for user [" + userID2 + "]");

		// SQ-T7268 Step 8: send points to the user via support gifting api
		Response sendPointsResponse2 = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID2,
				dataSet.get("adminAuthorization2"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send points to user [" + userID2 + "] is successful");

		// SQ-T7268 Step 9: send offer to the user via support gifting api
		Response sendOffersResponse2 = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID2,
				dataSet.get("adminAuthorization2"), dataSet.get("redeemableId"), dataSet.get("endDate"));
		Assert.assertEquals(sendOffersResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send offer to user [" + userID2 + "] is successful");
	}

	@Test(description = "[Point To Manual] SQ-T7268 Verify functionality of Async Support Gifting API with all gift types with no flag dependency", groups = "api", priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T7268_supportGiftingPointToManual() throws Exception {
		// enable_optimised_support_gifting should be false
		String businessId = dataSet.get("business_id");
		String query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessId);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_optimised_support_gifting", businessId);
		utils.logit("enable_optimised_support_gifting is set as false");

		// User Sign up
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 User signup");
		utils.logit("pass", "API2 user signup is successful for user [" + userID + "]");

		// SQ-T7268 Step 5: send points to the user via support gifting api
		Response sendPointsResponse = pageObj.endpoints().sendPointsToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("points"));
		Assert.assertEquals(sendPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send points to user [" + userID + "] is successful");

		// SQ-T7268 Step 6: send currency to the user via support gifting api
		Response sendCurrencyResponse = pageObj.endpoints().sendCurrencyToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("rewardAmount"));
		Assert.assertEquals(sendCurrencyResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "Api2 send reward amount to user [" + userID + "] is successful");

		// SQ-T7268 Step 7: send offer to the user via support gifting api
		Response sendOffersResponse = pageObj.endpoints().sendOfferToUserViaNewSupportGiftingAPI(userID,
				dataSet.get("apiKey"), dataSet.get("redeemableId"), dataSet.get("endDate"));
		Assert.assertEquals(sendOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not match for dashboard API2 support gifting to user");
		utils.logit("pass", "API2 send offer to user [" + userID + "] is successful");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
