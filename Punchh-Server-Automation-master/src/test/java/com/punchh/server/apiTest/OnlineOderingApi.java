package com.punchh.server.apiTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.apiConfig.AuthHeaders;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OnlineOderingApi {
	static Logger logger = LogManager.getLogger(OnlineOderingApi.class);
	public WebDriver driver;
	AuthHeaders authHeaders;
	ApiUtils apiUtils;
	String userEmail;
	Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	private String env;
	private Utilities utils;
	private static Map<String, String> dataSet;
	// String adminAuthorization = "19Ez5ypiztii6J6we3J1"; // pp Super Admin

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		authHeaders = new AuthHeaders();
		apiUtils = new ApiUtils();
		utils = new Utilities();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T3004 verifying auth online Create Loyalty Checkin || SQ-T3013 verifying auth online Estimate Loyalty Points Earning || SQ-T3021 verifying auth online Estimate Points Earning "
			+ "|| SQ-T3027 verifying auth online Balance Timelines || SQ-T3034 verifying auth online Fetch available offers of the user "
			+ "|| SQ-T3005 verifying auth online Fetch Redemption Code (Redemptions 1.0) || SQ-T3007 verifying auth online Get reset_password_token of the user "
			+ "|| SQ-T3006 verifying auth online Change Password || SQ-T3031 verifying auth online User Enrollment || SQ-T3033 verifying auth online User Disenrollment", groups = "api", priority = 0)
	public void verifyFetchRedemptionCodeAuthApi() {
		String BlankSpace = "";

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token");
		int userId = signUpResponse.jsonPath().get("user_id");
		String userID = Integer.toString(userId);

		// Estimate Loyalty Points Earning
		Response estimateLoyaltyPointsEarningResponse = pageObj.endpoints().authApiEstimateLoyaltyPointsEarning(
				authToken, dataSet.get("client"), dataSet.get("secret"), dataSet.get("subtotal_amount"));
		Assert.assertEquals(estimateLoyaltyPointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Estimate Loyalty Points Earning");
		boolean isAuthEstimateLoyaltyPointsEarningSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authEstimateLoyaltyPointsEarningSchema,
				estimateLoyaltyPointsEarningResponse.asString());
		Assert.assertTrue(isAuthEstimateLoyaltyPointsEarningSchemaValidated,
				"Auth estimate loyalty points earning schema validation failed");
		pageObj.utils().logPass("auth Api Estimate Loyalty Points Earning is successful");

		// Estimate Points Earning
		Response estimatePointsEarningResponse = pageObj.endpoints().authApiEstimatePointsEarning(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("receipt_amount"),
				dataSet.get("subtotal_amount"), dataSet.get("item_amount"));
		Assert.assertEquals(estimatePointsEarningResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Estimate Points Earning");
		boolean isAuthEstimatePointsEarningSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authEstimateLoyaltyPointsEarningSchema, estimatePointsEarningResponse.asString());
		Assert.assertTrue(isAuthEstimatePointsEarningSchemaValidated,
				"Auth estimate points earning schema validation failed");
		pageObj.utils().logPass("auth Api Estimate Points Earning is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				"", dataSet.get("redeemable_id"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");

		// Fetch available offers of the user
		Response fetchAvailableOffersOfTheUserResponse = pageObj.endpoints()
				.authApiFetchAvailableOffersOfTheUser(authToken, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchAvailableOffersOfTheUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Fetch available offers of the user");
		boolean isAuthFetchAvailableOffersSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchAvailableOffersSchema, fetchAvailableOffersOfTheUserResponse.asString());
		Assert.assertTrue(isAuthFetchAvailableOffersSchemaValidated,
				"Auth fetch available offers of the user schema validation failed");
		pageObj.utils().logPass("auth Api Fetch available offers of the user is successful");

		// Balance Timelines
		Response balanceTimelinesResponse = pageObj.endpoints().authApiBalanceTimelines(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(balanceTimelinesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Balance Timelines");
		boolean isAuthBalanceTimelinesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authBalanceTimelineSchema, balanceTimelinesResponse.asString());
		Assert.assertTrue(isAuthBalanceTimelinesSchemaValidated, "Auth balance timelines schema validation failed");
		pageObj.utils().logPass("auth Api Balance Timelines is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		pageObj.utils().logPass("Api2 user fetch user offers is successful");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2 send reward reedemable to user is successful");
		logger.info("Api2 send reward reedemable to user is successful");

		// Fetch Redemption Code for reward id
		Response fetchRedemptionCodeResponse = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"), BlankSpace, reward_id);
		Assert.assertEquals(fetchRedemptionCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for auth Api Fetch Redemption Code");
		boolean isAuthFetchRedemptionCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchRedemptionCodeSchema, fetchRedemptionCodeResponse.asString());
		Assert.assertTrue(isAuthFetchRedemptionCodeSchemaValidated,
				"Auth fetch redemption code schema validation failed");
		pageObj.utils().logPass("auth Api Fetch Redemption Code is successful");

		// Fetch Redemption Code for redeemed_points
		Response fetchRedemptionCodeResponse1 = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"),
				dataSet.get("redeemed_points"), BlankSpace);
		Assert.assertEquals(fetchRedemptionCodeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for auth Api Fetch Redemption Code");
		pageObj.utils().logPass("auth Api Fetch Redemption Code is successful");

		// list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth API list all deals");
		boolean isAuthListAllDealsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authListAllDealsSchema, listAuthDealsResponse.asString());
		Assert.assertTrue(isAuthListAllDealsSchemaValidated, "Auth list all deals schema validation failed");
		String redeemable_uuid = listAuthDealsResponse.jsonPath().get("redeemable_uuid[0]");

		// Get the deal detail
		Response getTheDealDetailResponse = pageObj.endpoints().authApiGetTheDealDetail(authToken,
				dataSet.get("client"), dataSet.get("secret"), redeemable_uuid);
		Assert.assertEquals(getTheDealDetailResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Get the deal detail");
		boolean isAuthGetDealDetailSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authGetDealDetailsSchema, getTheDealDetailResponse.asString());
		Assert.assertTrue(isAuthGetDealDetailSchemaValidated, "Auth get the deal detail schema validation failed");
		pageObj.utils().logPass("auth Api Get the deal detail is successful");

		// Get reset_password_token of the user
		Response resetPasswordTokenOfTheUserResponse = pageObj.endpoints()
				.authApiResetPasswordTokenOfTheUser(dataSet.get("client"), dataSet.get("secret"), userEmail);
		Assert.assertEquals(resetPasswordTokenOfTheUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Auth Api Get reset_password_token of the user");
		boolean isAuthResetPasswordTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authResetPasswordTokenSchema, resetPasswordTokenOfTheUserResponse.asString());
		Assert.assertTrue(isAuthResetPasswordTokenSchemaValidated,
				"Auth reset password token schema validation failed");
		pageObj.utils().logPass("Auth Api Get reset_password_token of the user");

		// Change Password
		Response changePasswordResponse = pageObj.endpoints().authApiChangePassword(authToken, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("newPassword"));
		Assert.assertEquals(changePasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Change Password");
		boolean isAuthChangePasswordSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authUserSignUpLogInUpdateInfoSchema, changePasswordResponse.asString());
		Assert.assertTrue(isAuthChangePasswordSchemaValidated, "Auth change password schema validation failed");
		pageObj.utils().logPass("auth Api Change Password is successful");

		// User SignUp from Auth Api
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().authApiSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken1 = signUpResponse1.jsonPath().get("authentication_token");

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// Activate Social Cause Campaign
		Response activateSocialCampaignResponse = pageObj.endpoints().activateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(activateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Activate Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Activate Social Cause Campaign is successful");

		// User Enrollment
		Response userEnrollmentResponse = pageObj.endpoints().authApiUserEnrollment(authToken1, dataSet.get("client"),
				dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userEnrollmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api User Enrollment");
		boolean isAuthUserEnrollmentSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userEnrollmentResponse.asString());
		Assert.assertTrue(isAuthUserEnrollmentSchemaValidated, "Auth user enrollment schema validation failed");
		pageObj.utils().logPass("auth Api User Enrollment is successful");

		// User Disenrollment
		Response userDisenrollmentResponse = pageObj.endpoints().authApiUserDisenrollment(authToken1,
				dataSet.get("client"), dataSet.get("secret"), social_cause_id);
		Assert.assertEquals(userDisenrollmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api User Disenrollment");
		boolean isAuthUserDisenrollmentSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, userDisenrollmentResponse.asString());
		Assert.assertTrue(isAuthUserDisenrollmentSchemaValidated, "Auth user disenrollment schema validation failed");
		pageObj.utils().logPass("auth Api User Disenrollment is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		pageObj.utils().logPass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

	}

	@Test(description = "SQ-T5205 Auth Api || Auth Api create Access Token for Single Sign On from SSO Token", groups = "api", priority = 0)
	public void T5205_SSO_token() {
		String BlankSpace = "";

		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token");

		// Fetch Client Token
		Response fetchClientTokenResponse = pageObj.endpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchClientTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch client token");
		pageObj.utils().logPass("Api2 Fetch User Notifications successful ");
		String security_token = fetchClientTokenResponse.jsonPath().get("token");
		String securityToken = null;

		// Regex to extract the security_token
		String pattern = "security_token=([^&]+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(security_token);

		if (m.find()) {
			securityToken = m.group(1);
			logger.info("Security Token: " + securityToken);
			pageObj.utils().logit("Security Token: " + securityToken);
		} else {
			logger.info("Security Token not found");
			pageObj.utils().logit("Security Token not found");
		}

		// Create Access token for Single Sign On from SSO Token
		Response createAccessTokenResponse = pageObj.endpoints().authApiCreateAccessToken(dataSet.get("client"),
				dataSet.get("secret"), securityToken);
		Assert.assertEquals(createAccessTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matche for auth Api create Access Token for Single Sign On from SSO Token");
		boolean isAuthCreateAccessTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authCreateSsoAccessTokenSchema, createAccessTokenResponse.asString());
		Assert.assertTrue(isAuthCreateAccessTokenSchemaValidated,
				"Auth create SSO access token schema validation failed");
		TestListeners.extentTest.get()
				.pass("auth Api create Access Token for Single Sign On from SSO Token is successful");

		// Create SSO Access token with invalid signature
		pageObj.utils().logit("== Create SSO Access token with invalid signature ==");
		logger.info("== Create SSO Access token with invalid signature ==");
		Response createAccessTokenInvalidSignatureResponse = pageObj.endpoints().authApiCreateAccessToken("1",
				dataSet.get("secret"), securityToken);
		Assert.assertEquals(createAccessTokenInvalidSignatureResponse.getStatusCode(), ApiConstants.HTTP_STATUS_PRECONDITION_FAILED);
		boolean isAuthCreateAccessTokenInvalidSignatureSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createAccessTokenInvalidSignatureResponse.asString());
		Assert.assertTrue(isAuthCreateAccessTokenInvalidSignatureSchemaValidated,
				"Auth create SSO access token with invalid signature schema validation failed");
		String createAccessTokenInvalidSignatureMsg = createAccessTokenInvalidSignatureResponse.jsonPath().get("[0]");
		Assert.assertEquals(createAccessTokenInvalidSignatureMsg, "Invalid Signature");
		pageObj.utils().logPass("Auth API create SSO Access token with invalid signature is unsuccessful");
		logger.info("Auth API create SSO Access token with invalid signature is unsuccessful");

		// Create SSO Access token with invalid security token
		pageObj.utils().logit("== Create SSO Access token with invalid security token ==");
		logger.info("== Create SSO Access token with invalid security token ==");
		Response createAccessTokenInvalidSecurityTokenResponse = pageObj.endpoints()
				.authApiCreateAccessToken(dataSet.get("client"), dataSet.get("secret"), "1");
		Assert.assertEquals(createAccessTokenInvalidSecurityTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isAuthCreateAccessTokenInvalidSecurityTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema,
				createAccessTokenInvalidSecurityTokenResponse.asString());
		Assert.assertTrue(isAuthCreateAccessTokenInvalidSecurityTokenSchemaValidated,
				"Auth create SSO access token with invalid security token schema validation failed");
		String createAccessTokenInvalidSecurityTokenMsg = createAccessTokenInvalidSecurityTokenResponse.jsonPath()
				.get("error.message");
		Assert.assertEquals(createAccessTokenInvalidSecurityTokenMsg, "Invalid/Expired security token.");
		TestListeners.extentTest.get()
				.pass("Auth API create SSO Access token with invalid security token is unsuccessful");
		logger.info("Auth API create SSO Access token with invalid security token is unsuccessful");

		// Create SSO Access token with missing security token
		pageObj.utils().logit("== Create SSO Access token with missing security token ==");
		logger.info("== Create SSO Access token with missing security token ==");
		Response createAccessTokenMissingSecurityTokenResponse = pageObj.endpoints()
				.authApiCreateAccessToken(dataSet.get("client"), dataSet.get("secret"), "");
		Assert.assertEquals(createAccessTokenMissingSecurityTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isAuthCreateAccessTokenMissingSecurityTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, createAccessTokenMissingSecurityTokenResponse.asString());
		Assert.assertTrue(isAuthCreateAccessTokenMissingSecurityTokenSchemaValidated,
				"Auth create SSO access token with missing security token schema validation failed");
		String createAccessTokenMissingSecurityTokenMsg = createAccessTokenMissingSecurityTokenResponse.jsonPath()
				.get("error");
		Assert.assertEquals(createAccessTokenMissingSecurityTokenMsg,
				"Required parameter missing or the value is empty: security_token");
		TestListeners.extentTest.get()
				.pass("Auth API create SSO Access token with missing security token is unsuccessful");
		logger.info("Auth API create SSO Access token with missing security token is unsuccessful");

	}

	@Test(description = "SQ-T5204 Auth API || Fetch checkin by external id", groups = "api", priority = 0)
	public void T5204_verifyFetchCheckinByExternalId() {
		// User SignUp from Auth Api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		String token = signUpResponse.jsonPath().get("access_token");

		// Fetch Client Token
		Response fetchClientTokenResponse = pageObj.endpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchClientTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch client token");
		pageObj.utils().logPass("Api2 Fetch User Notifications successful ");
		// Checkin via auth API
		String amount = "210.0";
		Response checkinResponse = pageObj.endpoints().authGrantLoyaltyCheckinAgainstReciept(authToken, amount,
				dataSet.get("client"), dataSet.get("secret"));

		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(
				checkinResponse.jsonPath().get("checkin.points_earned").toString().equals("630.0")
						| checkinResponse.jsonPath().get("checkin.points_earned").toString().equals("210"),
				"total points not matched");
		Assert.assertEquals(checkinResponse.jsonPath().get("checkins").toString(), "1");
		pageObj.utils().logPass("Successfully verfied auth checkin against reciept");
		String externalUid = checkinResponse.jsonPath().get("checkin.external_uid").toString();

		// Fetch a Checkin By External_uid
		Response fetchACheckinByExternal_uidResponse = pageObj.endpoints().authApiFetchACheckinByExternal_uid(authToken,
				dataSet.get("client"), dataSet.get("secret"), externalUid);
		Assert.assertEquals(fetchACheckinByExternal_uidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for auth Api Fetch a Checkin by external_uid");
		boolean isAuthFetchCheckinByExternalUidSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authFetchCheckinByExternalUidSchema,
				fetchACheckinByExternal_uidResponse.asString());
		Assert.assertTrue(isAuthFetchCheckinByExternalUidSchemaValidated,
				"Auth Fetch Checkin By External_uid schema validation failed");
		pageObj.utils().logPass("auth Api Fetch a Checkin by external_uid is successful");
	}
	
	//khushbu
	@Test(description = "SQ-T6726 Enrich Online order Api", groups = "Regression", priority = 6)
	@Owner(name = "Khushbu Soni")
	public void Tc_6726_enrichOrderOnlineTest() throws Exception {

		// user creation using api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// send reward to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
					"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
					dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// perform redemption first time with reward type
		Response posRedeem = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"), rewardId);
		Assert.assertEquals(posRedeem.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified able to redeem the redeemable having the redeemable id --" + rewardId);
		TestListeners.extentTest.get()
					.pass("Verified able to redeem the redeemable having the redeemable id --" + rewardId);

		// sql query to get transaction no
		String redemptioncode = posRedeem.jsonPath().get("redemption_code").toString();
		String getTransactionNo = "select transaction_no from redemption_codes where business_id="
					+ dataSet.get("business_id") + " and redemption_token=" + redemptioncode + "";
		pageObj.singletonDBUtilsObj();
		// Execute the query and retrieve the result
		String result = DBUtils.executeQueryAndGetColumnValue(env, getTransactionNo, "transaction_no");
		logger.info("transaction no is--" + result);
		pageObj.utils().logPass("transaction no is --" + result);

		// enrich online order api with reward type
		Response enrich = pageObj.endpoints().enrichOnlineOrderApi(result, dataSet.get("adminKey"));
		Assert.assertEquals(enrich.statusCode(), ApiConstants.HTTP_STATUS_OK, "api status code did not match");
		logger.info("Verified enrich order api successfully");
		pageObj.utils().logPass("Verified enrich order api successfully");
		
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}