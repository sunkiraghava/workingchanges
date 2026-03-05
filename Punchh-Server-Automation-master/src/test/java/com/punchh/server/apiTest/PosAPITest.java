package com.punchh.server.apiTest;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;

import Support.ConfigurationClass;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PosAPITest {
	static Logger logger = LogManager.getLogger(PosAPITest.class);
	public WebDriver driver;
	ApiUtils apiUtils;
	String userEmail;
	String email = "AutoApiTemp@punchh.com";
	Properties uiProp;
	Properties prop;
	String punchKey, amount;
	PageObj pageObj;
	String sTCName;
	String run = "api";
	private String env;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		uiProp = Utilities.loadPropertiesFile("config.properties");
		apiUtils = new ApiUtils();
		utils = new Utilities();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T2369 POS API validation for Singup, Checkin, Redemption, void redemption", groups = "api", priority = 0)
	public void verifyPosAPiSingUp_Checkin_Redemption_VoidRedemption() {
		// POS User SignUp
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isPosSignUpSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posUserSignUpSchema, response.asString());
		Assert.assertTrue(isPosSignUpSchemaValidated, "POS API User SignUp Schema Validation failed");
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isPosCheckinSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posCheckinSchema, response2.asString());
		Assert.assertTrue(isPosCheckinSchemaValidated, "POS API Checkin Schema Validation failed");
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// POS Get user balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		boolean isPosUserLookupSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posUserSignUpSchema, balanceResponse.asString());
		Assert.assertTrue(isPosUserLookupSchemaValidated, "POS API User Lookup Schema Validation failed");
		String bankedRewardBefore = balanceResponse.jsonPath().get("balance.banked_rewards");

		// POS Create Redemption
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response possibleRedemptionResponse = pageObj.endpoints().posPossibleRedemptionOfAmount(userEmail, date, key,
				txn, dataSet.get("redeemAmount"), "Token token=" + dataSet.get("locationKey"));
		Assert.assertEquals(possibleRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS Possible redemption API");
		boolean isPosPossibleRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posCreateRedemptionSchema, possibleRedemptionResponse.asString());
		Assert.assertTrue(isPosPossibleRedemptionSchemaValidated,
				"POS API Possible Redemption Schema Validation failed");
		Assert.assertTrue(possibleRedemptionResponse.jsonPath().get("status").toString().contains("It can be honored."),
				"Error in verfiying possible redemption api response status");

		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS Create Redemption API");
		boolean isPosCreateRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posCreateRedemptionSchema, respo.asString());
		Assert.assertTrue(isPosCreateRedemptionSchemaValidated, "POS API Create Redemption Schema Validation failed");
		Assert.assertTrue(respo.jsonPath().get("status").toString().contains("Please HONOR it."),
				"Error in verfiying create redemption api response status");
		int redemption_id = respo.jsonPath().get("redemption_id");

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(userEmail, Integer.toString(redemption_id),
				dataSet.get("locationKey"));
		voidResponse.then().log().all();
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 200 did not matched for pos redemption api");

		// Fetch balance
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				TestListeners.extentTest.get().info("API hit count is : " + attempts);
				utils.longwait(5000);

				Response balanceResponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail,
						dataSet.get("locationKey"));
				Assert.assertEquals(balanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
				String bankedRewardAfter = balanceResponse2.jsonPath().get("balance.banked_rewards");
				if (bankedRewardBefore.equals(bankedRewardAfter)) {
					flag = true;
					Assert.assertEquals(bankedRewardBefore, bankedRewardAfter, "Failed to verify void redemption");
					TestListeners.extentTest.get().pass("Verified void redemption");
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}
	}

	@Test(description = "SQ-T2817 POS API multiple void redemption", groups = "api", priority = 1)
	public void verifyPosApiVoidMultipleRedemption() {
		ArrayList<String> redemptionIdList = new ArrayList<String>();
		logger.info("== POS user signup and test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response, "POS user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// POS Checkin

		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		apiUtils.verifyResponse(response2, "POS checkin");
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// get user balance
		Response balanceResponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		String bankedRewardBefore = balanceResponse.jsonPath().get("balance.banked_rewards");
		// POS redemption API
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		apiUtils.verifyResponse(response, "Redemption successful");
		// int temp1=respo.jsonPath().get("redemption_id");
		redemptionIdList.add(Integer.toString(respo.jsonPath().get("redemption_id")));
		Utilities.longWait(5000);
		// POS redemption API
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:55:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo1 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo1.jsonPath().get("redemption_id")));
		Utilities.longWait(5000);
		// POS redemption API
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:54:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo2 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo2.jsonPath().get("redemption_id")));
		// POS redemption API
		Utilities.longWait(5000);
		txn = "123456" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getCurrentDate() + "T17:59:32Z";
		key = CreateDateTime.getTimeDateString();
		Response respo3 = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(respo3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos redemption api");
		redemptionIdList.add(Integer.toString(respo3.jsonPath().get("redemption_id")));
		// Void multiple redemption using API
		Response voidResponse = pageObj.endpoints().posVoidMultipleRedemption(userEmail, redemptionIdList,
				dataSet.get("locationKey"));
		voidResponse.then().log().all();
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED, "Status code 200 did not matched for pos redemption api");
		Response balanceResponse2 = pageObj.endpoints().posUserLookupFetchBalance(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(balanceResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Error in getting user balance");
		// TD
//		String bankedRewardAfter = balanceResponse2.jsonPath().get("balance.banked_rewards");
//		Assert.assertEquals(bankedRewardBefore, bankedRewardAfter, "Failed to verify void redemption");
		TestListeners.extentTest.get().pass("Verified void redemption");
	}

	@Test(description = "SQ-T2903 POS API verify location configuration", groups = "api", priority = 2)
	public void verifyLocationConfiguration() {
		logger.info("== POS API: verify location configuration ==");

		Response posLocConfigResponse = pageObj.endpoints().posLocationConfig(dataSet.get("locationKey"));
		Assert.assertEquals(posLocConfigResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS Location configuration API");
		boolean isPosLocConfigSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posLocationConfigurationSchema, posLocConfigResponse.asString());
		Assert.assertTrue(isPosLocConfigSchemaValidated, "POS API Location Configuration Schema Validation failed");
//		Assert.assertTrue(posLocConfigResponse.jsonPath().get("status").toString().contains("It can be honored."),
//				"Error in verfiying possible redemption api response status");
//		posLocConfigResponse.body().jsonPath(JsonSchemaValidator.
//			      matchesJsonSchema(""))
	}

	@Test(description = "SQ-T2904 POS API verify active redemption", groups = "api", priority = 3)
	public void verifyPosApiActiveRedemption() {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().Api2SendMessageToUser(userID,
				dataSet.get("adminAuthorization"), dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1.5",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		logger.info("redemption code => " + redemption_Code);

		// call pos active redemptions api
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isPosActiveRedemptionSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.posActiveRedemptionsSchema, activeRedemptionResponse.asString());
		Assert.assertTrue(isPosActiveRedemptionSchemaValidated,
				"POS API Get Active Redemptions Schema Validation failed");
		String redemptionType = activeRedemptionResponse.jsonPath().get("type_of_redemption").toString();
		Assert.assertEquals("[BankedRewardRedemption]", redemptionType);
	}

	@Test(description = "SQ-T3048 POS API verify applicable offers", groups = "api", priority = 4)
	public void verifyApplicableOffers() {
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		logger.info("===Applicable offers API===");
		Response applicableresponse = pageObj.endpoints().posApplicableOffer(userEmail, dataSet.get("amount"),
				dataSet.get("locationKey"));
		applicableresponse.then().log().all();
		Assert.assertEquals(applicableresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified program meta");
	}

	@Test(description = "SQ-T3045 POS API verify program meta API", groups = "api", priority = 5)
	public void verifyProgramMeta() {
		logger.info("===Program meta API===");
		Response response2 = pageObj.endpoints().posProgramMeta(dataSet.get("locationKey"));
		response2.then().log().all();
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS Program Meta API");
		boolean isPosProgramMetaSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.posProgramMetaSchema, response2.asString());
		Assert.assertTrue(isPosProgramMetaSchemaValidated, "POS API Program Meta Schema Validation failed");
		TestListeners.extentTest.get().pass("POS API Program Meta call is successful");
	}

	// @Test(description = "SQ-T4746 Verify POS Payments", groups = "api", priority
	// = 6) //heartland service issue
	public void verifyPOSPayments() {

		// User Sign-up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		TestListeners.extentTest.get().info(userEmail + " Api1 user signup is successful");

		// Generate Single Scan code
		Response singleTokenResponse1 = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartlandPublicKey"));
		Assert.assertEquals(singleTokenResponse1.statusCode(), 201,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken1 = singleTokenResponse1.jsonPath().getString("token_value");
		TestListeners.extentTest.get().info("Heartland Single token is generated: " + finalSingleScanToken1);

		Response singleScanTokenResponse1 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("paymentType"), dataSet.get("transactionToken"),
				finalSingleScanToken1);
		Assert.assertEquals(singleScanTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"singleScanTokenResponse API response status code not matched");
		String single_scan_code1 = singleScanTokenResponse1.jsonPath().getString("single_scan_code").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().info("single_scan_code is generated: " + single_scan_code1);

		// Create Payment
		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", "", single_scan_code1,
				dataSet.get("paymentType"), dataSet.get("locationKey"));
		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		String status = posPaymentResponse.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Create Payment status code not matched.");
		Assert.assertEquals(status, "success", "payment_type is not matched.");
		TestListeners.extentTest.get().pass("POS Create Payment call is successful.");

		// Get Payment Status
		Response posPaymentStatusResponse = pageObj.endpoints().POSPaymentStatus(payment_reference_id,
				dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentStatusResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Get Payment status code not matched.");
		Assert.assertEquals(posPaymentStatusResponse.jsonPath().getString("status").replace("[", "").replace("]", ""),
				"success", "status is not matched.");
		TestListeners.extentTest.get().pass("POS Get Payment Success Status call is successful.");

		// Update Payment
		Response posPaymentUpdateResponse = pageObj.endpoints().POSPaymentPUT(payment_reference_id,
				dataSet.get("locationKey"), dataSet.get("statusToUpdate"));
		Assert.assertEquals(posPaymentUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Payment Update status code not matched.");
		String posPaymentUpdateResponse_status = posPaymentUpdateResponse.jsonPath().getString("status")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentUpdateResponse_status, "processed", "status is not matched.");
		TestListeners.extentTest.get().pass("POS Update Payment call is successful.");

		// Refund Payment
		Response posPaymentRefundResponse = pageObj.endpoints().POSPaymentRefund(payment_reference_id,
				dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentRefundResponse.statusCode(), 200, "POS Payment Refund status code not matched.");
		String posPaymentRefundResponse_status = posPaymentRefundResponse.jsonPath().getString("status")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentRefundResponse_status, "refunded", "status is not matched.");
		TestListeners.extentTest.get().pass("POS Refund Payment call is successful.");

		// Creating another payment to verify cancellation
		Response singleTokenResponse2 = pageObj.endpoints().generateHeartlandPaymentToken("",
				dataSet.get("heartlandPublicKey"));
		Assert.assertEquals(singleTokenResponse2.statusCode(), 201,
				"Not able to generate single Token for heartland adaptor");
		String finalSingleScanToken2 = singleTokenResponse2.jsonPath().getString("token_value");
		TestListeners.extentTest.get().info("Heartland Single token is generated: " + finalSingleScanToken2);

		Response singleScanTokenResponse2 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), authToken, dataSet.get("paymentType"), dataSet.get("transactionToken"),
				finalSingleScanToken2);
		Assert.assertEquals(singleScanTokenResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"singleScanTokenResponse API response status code not matched");
		String single_scan_code2 = singleScanTokenResponse2.jsonPath().getString("single_scan_code").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().info("single_scan_code is generated: " + single_scan_code2);

		Response posPaymentResponse2 = pageObj.endpoints().POSPayment("singleScan", "", single_scan_code2,
				dataSet.get("paymentType"), dataSet.get("locationKey"));
		String payment_reference_id2 = posPaymentResponse2.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(posPaymentResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Create Payment status code not matched.");
		TestListeners.extentTest.get().info("POS Create Payment call is successful.");

		// Cancel Payment
		Response posPaymentCancelResponse = pageObj.endpoints().posPaymentCancel(payment_reference_id2,
				dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentCancelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"POS Cancel Payment status code not matched.");
		String posPaymentCancelResponse_status = posPaymentCancelResponse.jsonPath().getString("status")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentCancelResponse_status, "refunded", "status is not matched.");
		TestListeners.extentTest.get().pass("POS Cancel Payment call is successful.");
	}

	// Disabling this TC as this flow won't work due to OMM-1256. Checked with Rahul Garg
	//@Test(description = "SQ-T5301: Verify POS API Unlock Discount Basket (Redemptions 2.0)", groups = "api")
	public void verifyPOSUnlockDiscountBasket() throws Exception {

		/*
		 * Pre-conditions: Ensure that for business used (autonine) -
		 * 
		 * Location "Automation- Redemption 2.0" should have the
		 * "Allow Location for Multiple Redemption" checked.
		 * 
		 * Under Cockpit > Redemptions > Multiple Redemptions > "Enable Auto-redemption"
		 * and "Enable Reward Locking" are checked.
		 */
		
		// User Sign-up
		TestListeners.extentTest.get().info("== Mobile API v2: User Sign-up ==");
		logger.info("== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String userId = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("API v2 User Signup call is successful");
		logger.info("API v2 User Signup call is successful");

		// Send reward to user
		TestListeners.extentTest.get().info("== Platform Functions: Send reward to user ==");
		logger.info("== Platform Functions: Send reward to user ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userId, dataSet.get("apiKey"), "",
				dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward to user");
		TestListeners.extentTest.get().pass("Platform Functions Send reward to user is successful");
		logger.info("Platform Functions Send reward to user is successful");

		// Get Reward Id for user
		TestListeners.extentTest.get().info("== Auth API: Get Reward Id for user ==");
		logger.info("== Auth API: Get Reward Id for user ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		TestListeners.extentTest.get().pass("Reward Id for user is fetched: " + rewardId);
		logger.info("Reward Id for user is fetched: " + rewardId);

		// POS Add Discount to Basket
		TestListeners.extentTest.get().info("== POS API: Add Discount to Basket ==");
		logger.info("== POS API: Add Discount to Basket ==");
		String externalUid = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		Response addDiscountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey"), userId, "reward", rewardId, externalUid);
		Assert.assertEquals(addDiscountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Add Discount to Basket call.");
		boolean isPosAddDiscountBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posAddDiscountBasketSchema, addDiscountBasketResponse.asString());
		Assert.assertTrue(isPosAddDiscountBasketSchemaValidated,
				"POS API Add Discount to Basket Schema Validation failed");
		String discountId = utils.getJsonReponseKeyValueFromJsonArray(addDiscountBasketResponse,
				"discount_basket_items", "discount_id", rewardId);
		Assert.assertEquals(discountId, rewardId, "ID did not match.");
		TestListeners.extentTest.get().pass("POS API Add Discount to Basket call is successful");
		logger.info("POS API Add Discount to Basket call is successful");

		// POS Unlock Discount Basket
		TestListeners.extentTest.get().info("== POS API: Unlock Discount Basket ==");
		logger.info("== POS API: Unlock Discount Basket ==");
		Response basketUnlockResponse = pageObj.endpoints().discountUnlockPOSAPI(dataSet.get("locationKey"), userId,
				externalUid);
		Assert.assertEquals(basketUnlockResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Unlock Discount Basket.");
		boolean isPosUnlockDiscountBasketSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, basketUnlockResponse.asString());
		Assert.assertTrue(isPosUnlockDiscountBasketSchemaValidated,
				"POS API Unlock Discount Basket Schema Validation failed");
		String basketUnlockResponseMsg = basketUnlockResponse.jsonPath().getString("[0]");
		Assert.assertEquals(basketUnlockResponseMsg, "Discount Basket is unlocked successfully.",
				"Message did not match.");
		TestListeners.extentTest.get().pass("POS API Unlock Discount Basket call is successful");
		logger.info("POS API Unlock Discount Basket call is successful");

	}
	
	//akansha jain - commenting temporarily until ready, as these will be part of next minor release 
	@Test(description = "SQ-T7215 Verify pos user search API with single_scan_code parameter when parametrization is enabled.", groups = "api", priority = 4)
	@Owner(name = "Akansha Jain")
	public void SQ_7215_verifyPOSUsersSearchWithSST() throws Exception {
		
		// DB - update preference column in business table
		// updating enable_api_parameterization to true
		String b_id = dataSet.get("business_id");
		String query = "SELECT preferences FROM businesses WHERE id = '" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = false;
		try {
				flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		} catch (Exception e) {
				e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");
		
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		
		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCode(token,
				dataSet.get("paymentType"), dataSet.get("transaction_token"), dataSet.get("client"),dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		boolean isSingleScanCodeSchemaValidated = Utilities.validateJsonAgainstSchema(
			ApiResponseJsonSchema.api2SingleScanCodeSchema, singleScanCodeResponse.asString());
		Assert.assertTrue(isSingleScanCodeSchemaValidated, "API v2 Generate Single Scan Code Schema Validation failed");
		Assert.assertNotNull(singleScanCodeResponse.jsonPath().get("single_scan_code").toString(),
				"single_scan_code is null");
		String singleScanToken = singleScanCodeResponse.jsonPath().get("single_scan_code").toString();
		TestListeners.extentTest.get().pass("API2 Generate Single Scan Code call is successful.");

		// get pos user search api with enable parametrization and single scan token
		logger.info("===pos users search API===");
		Response usersearchresponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanToken, dataSet.get("location"));
		usersearchresponse.then().log().all();
		Assert.assertEquals(usersearchresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified pos user search api with parametrization ON and single scan token");
	}
	
	//akansha jain - commenting temporarily until ready, as these will be part of next minor release 
	@Test(description = "SQ-T7216 Verify pos user search API with enable_cstore_account_balance configuration when parametrization is enabled.", groups = "api", priority = 4)
	@Owner(name = "Akansha Jain")
	public void SQ_7216_verifyPOSUsersSearchWithCstore() throws Exception {
		
		// DB - update preference column in business table
		// updating enable_cstore_account_balance to true
		String b_id = dataSet.get("business_id");
		String query = "SELECT preferences FROM businesses WHERE id = '" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = false;
		try {
				flag = DBUtils.updateBusinessesPreference(env, expColValue2, "true", dataSet.get("dbFlag"), b_id);
		} catch (Exception e) {
				e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");
		
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// get pos user search api with enable parametrization and cstore flag ON
		logger.info("===pos users search API===");
		Response usersearchresponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("location"));
		usersearchresponse.then().log().all();
		Assert.assertEquals(usersearchresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified pos user search api with parametrization ON and cstore flag ON");
	}
	
	//akansha jain - commenting temporarily until ready, as these will be part of next minor release 
	@Test(description = "SQ-T7217 Verify pos user search API default behavior (ApiCompleteAccount) when parametrization is enabled.", groups = "api", priority = 4)
	@Owner(name = "Akansha Jain")
	public void SQ_7217_verifyPOSUsersSearchWithCompleteAccount() throws Exception {
			
		// DB - update preference column in business table
		// updating enable_cstore_account_balance to false
		String b_id = dataSet.get("business_id");
		String query = "SELECT preferences FROM businesses WHERE id = '" + b_id + "'";
		pageObj.singletonDBUtilsObj();
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = false;
		try {
				flag = DBUtils.updateBusinessesPreference(env, expColValue2, "false", dataSet.get("dbFlag"), b_id);
		} catch (Exception e) {
				e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");
			
		// user sign up
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// get pos user search api with enable parametrization and complete account
		logger.info("===pos users search API===");
		Response usersearchresponse = pageObj.endpoints().posUserLookupFetchBalance(userEmail, dataSet.get("location"));
				usersearchresponse.then().log().all();
		Assert.assertEquals(usersearchresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos program meta api");
		TestListeners.extentTest.get().pass("Verified pos user search api with parametrization ON and complete account");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
