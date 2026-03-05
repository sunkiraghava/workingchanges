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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileAPIv1Test {

	private static Logger logger = LogManager.getLogger(MobileAPIv1Test.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private Utilities utils;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "Verify Mobile API v1:- SQ-T4942: Signup; SQ-T4956: Login; SQ-T4958: Update Guest details; SQ-T4961: Update User; "
			+ "SQ-T4964: Forgot Password; SQ-T4944: Messages; SQ-T4946: Feedback; SQ-T4948: Braintree token; SQ-T4966: Create Passcode; "
			+ "SQ-T4968: Update Passcode; SQ-T4962: Logout", groups = "api", priority = 0)
	public void verifyMobileAPIv1UserSignup() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		boolean isApi1UserSignUpSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1UserSignUpLogInUpdateDetailsSchema, signUpResponse.asString());
		Assert.assertTrue(isApi1UserSignUpSchemaValidated, "API1 User Signup Schema Validation failed");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String fName = signUpResponse.jsonPath().get("first_name").toString();
		String lName = signUpResponse.jsonPath().get("last_name").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		String signupUserEmail = signUpResponse.jsonPath().getString("email");
		Assert.assertEquals(signupUserEmail, userEmail, "email did not match");
		utils.logPass("API v1 user signup is successful");

		// User login
		utils.logit("== Mobile API v1: User login ==");
		Response loginResponse = pageObj.endpoints().Api1UserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v1 user login");
		boolean isApi1UserLogInSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1UserSignUpLogInUpdateDetailsSchema, loginResponse.asString());
		Assert.assertTrue(isApi1UserLogInSchemaValidated, "API1 User Login Schema Validation failed");
		String logInUserEmail = loginResponse.jsonPath().get("email").toString();
		String logInUserFName = loginResponse.jsonPath().get("first_name").toString();
		String logInUserLName = loginResponse.jsonPath().get("last_name").toString();
		String logInUserID = loginResponse.jsonPath().get("id").toString();
		Assert.assertEquals(logInUserEmail, userEmail, "User email did not match");
		Assert.assertEquals(logInUserFName, fName, "User first name did not match");
		Assert.assertEquals(logInUserLName, lName, "User last name did not match");
		Assert.assertEquals(logInUserID, userID, "User Id did not match");
		utils.logPass("API v1 user login is successful");

		// Update guest details (/api/mobile/customers/)
		utils.logit("== Mobile API v1: Update guest details (/api/mobile/customers/) ==");
		String userNewEmail = pageObj.iframeSingUpPage().generateEmail();
		Response updateGuestResponse = pageObj.endpoints().Api1MobileUpdateGuestDetails("New" + fName, "New" + lName,
				"New" + dataSet.get("passcode"), dataSet.get("client"), dataSet.get("secret"), token, userNewEmail);
		Assert.assertEquals(updateGuestResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user update profile");
		boolean isApi1UpdateGuestDetailsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1UserSignUpLogInUpdateDetailsSchema, updateGuestResponse.asString());
		Assert.assertTrue(isApi1UpdateGuestDetailsSchemaValidated,
				"API1 Update Guest Details Schema Validation failed");
		String updateGuestFname = updateGuestResponse.jsonPath().get("first_name").toString();
		String updateGuestLname = updateGuestResponse.jsonPath().get("last_name").toString();
		Assert.assertNotEquals(fName, updateGuestFname);
		Assert.assertNotEquals(lName, updateGuestLname);
		utils.logPass("API v1 guest details update is successful");

		// Update user (/api/mobile/users/)
		utils.logit("== Mobile API v1: Update user (/api/mobile/users/) ==");
		Response updateUserResponse = pageObj.endpoints().api1UpdateUser(dataSet.get("signupChannel"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(updateUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Update user");
		boolean api1UpdateUserSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api1UpdateUserSchema, updateUserResponse.asString());
		Assert.assertTrue(api1UpdateUserSchemaValidated, "API1 Update User Schema Validation failed");
		String updateUserID = updateUserResponse.jsonPath().getString("user_id");
		String updateUserEmail = updateUserResponse.jsonPath().getString("email");
		String updateUserFName = updateUserResponse.jsonPath().get("first_name").toString();
		String updateUserLName = updateUserResponse.jsonPath().get("last_name").toString();
		Assert.assertEquals(updateUserID, userID, "user_id did not match");
		Assert.assertEquals(updateUserEmail, userNewEmail, "email did not match");
		Assert.assertEquals(updateUserFName, updateGuestFname, "User first name did not match");
		Assert.assertEquals(updateUserLName, updateGuestLname, "User last name did not match");
		utils.logPass("API v1 Update user is successful");

		// Forgot password
		utils.logit("== Mobile API v1: Forgot password ==");
		Response forgotPasswordResponse = pageObj.endpoints().Api1MobileForgotPassword(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(forgotPasswordResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Forgot Password");
		boolean isForgotPasswordSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, forgotPasswordResponse.asString());
		Assert.assertTrue(isForgotPasswordSchemaValidated, "API1 Forgot Password Schema Validation failed");
		String passwordResetMessage = forgotPasswordResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(passwordResetMessage,
				"If an account currently exists within our system, an email will be sent to the associated address with instructions on resetting the password.",
				"Password reset message did not match");
		utils.logPass("API v1 forgot password is successful");

		// Create Guest feedback
		utils.logit("== Mobile API v1: Create Guest feedback ==");
		Response createFeedbackResponse = pageObj.endpoints().api1CreateFeedback(dataSet.get("feedbackRating"),
				dataSet.get("feedbackMessage"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createFeedbackResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Create Guest feedback");
		boolean isCreateFeedbackSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1CreateFeedbackSchema, createFeedbackResponse.asString());
		Assert.assertTrue(isCreateFeedbackSchemaValidated, "API1 Create Feedback Schema Validation failed");
		String feedbackID = createFeedbackResponse.jsonPath().getString("id");
		String feedbackMsg = createFeedbackResponse.jsonPath().getString("message");
		Assert.assertNotNull(feedbackID, "Feedback id is null");
		Assert.assertEquals(feedbackMsg, dataSet.get("feedbackMessage"), "Feedback Message did not match");
		utils.logPass("API v1 Create Guest feedback is successful");

		// Fetch Rich messages
		utils.logit("== Mobile API v1: Fetch Rich messages ==");
		String messageId = "";
		int counter = 0;
		while (counter < 20) {
			try {
				utils.logit("API hit count is : " + counter);
				Response richMessagesResponse = pageObj.endpoints().api1FetchMessages(dataSet.get("client"),
						dataSet.get("secret"), token);
				messageId = richMessagesResponse.jsonPath().get("messages[0].message_id").toString();
				if (messageId != null) {
					Assert.assertEquals(richMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 Fetch Messages");
					boolean isApi1FetchRichMessagesSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api1FetchMessagesSchema, richMessagesResponse.asString());
					Assert.assertTrue(isApi1FetchRichMessagesSchemaValidated,
							"API1 Fetch Rich Messages Schema Validation failed");
					utils.logPass("API v1 Rich Message is fetched successfuly with id: " + messageId);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longwait(5000);
		}

		// Get Braintree token
		utils.logit("== Mobile API v1: Get Braintree token ==");
		Response braintreeTokenResponse = pageObj.endpoints().api1BraintreeToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(braintreeTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Get Braintree Token");
		boolean isGetBraintreeTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1GetBraintreeTokenSchema, braintreeTokenResponse.asString());
		Assert.assertTrue(isGetBraintreeTokenSchemaValidated, "API1 Get Braintree Token Schema Validation failed");
		String braintreeToken = braintreeTokenResponse.jsonPath().getString("token");
		Assert.assertNotNull(braintreeToken, "Token is null");
		utils.logPass("API v1 Get Braintree Token is successful");

		// Create Passcode
		utils.logit("== Mobile API v1: Create Passcode ==");
		Response createPasscodeResponse = pageObj.endpoints().api1CreatePasscode(dataSet.get("passcode"),
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(createPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 Create Passcode");
		boolean isCreatePasscodeSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createPasscodeResponse.asString());
		Assert.assertTrue(isCreatePasscodeSchemaValidated, "API1 Create Passcode Schema Validation failed");
		String passcodeSuccessMsg = createPasscodeResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(passcodeSuccessMsg, "Passcode has been successfully created.",
				"Passcode create message did not match");
		utils.logPass("API v1 Create Passcode is successful");

		// Update Passcode
		utils.logit("== Mobile API v1: Update Passcode ==");
		Response updatePasscodeResponse = pageObj.endpoints().api1UpdatePasscode(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(updatePasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Update Passcode");
		boolean isUpdatePasscodeSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, updatePasscodeResponse.asString());
		Assert.assertTrue(isUpdatePasscodeSchemaValidated, "API1 Update Passcode Schema Validation failed");
		String passcodeResetMsg = updatePasscodeResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(passcodeResetMsg,
				"An email has been sent to your registered email address to reset your Passcode.",
				"Passcode reset message did not match");
		utils.logPass("API v1 Update Passcode is successful");

		// User logout
		utils.logit("== Mobile API v1: User logout ==");
		Response logoutResponse = pageObj.endpoints().api1UserLogout(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(logoutResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user logout");
		utils.logPass("API v1 user logout is successful");
	}

	@Test(description = "Verify Mobile API v1:- SQ-T4981: Generate Redemption; SQ-T4983: Cancel Redemption; SQ-T4987: Fetch Notifications", groups = "api", priority = 1)
	public void verifyAPIv1Redemptions() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("API v1 user signup is successful");

		// API2 Send reward amount to user
		utils.logit("== Mobile API v2: Send Reward amount to user ==");
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		utils.logPass("API2 send reward amount to user is successful");

		// Generate Redemption with redeemed points
		utils.logit("== Mobile API v1: Generate Redemption code with redeemed points ==");
		Response generateRedemptionResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "2",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(generateRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API v1 Generate Redemption with redeemed points");
		boolean isApi1GenerateRedemptionRedeemedPointsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1GenerateRedemptionRedeemedPointsSchema,
				generateRedemptionResponse.asString());
		Assert.assertTrue(isApi1GenerateRedemptionRedeemedPointsSchemaValidated,
				"API1 Generate Redemption with redeemed points Schema Validation failed");
		String redemptionId = generateRedemptionResponse.jsonPath().get("id").toString();
		Assert.assertNotNull(redemptionId, "Redemption Id is null");
		utils.logPass(
				"API v1 Generate Redemption with redeemed points is successful for redemption id: " + redemptionId);

		// Cancel Redemption
		utils.logit("== Mobile API v1: Cancel Redemption ==");
		Response cancelRedemptionResponse = pageObj.endpoints().api1CancelRedemption(redemptionId, token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cancelRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Cancel Redemption");
		boolean isApi1CancelRedemptionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiMessageObjectSchema, cancelRedemptionResponse.asString());
		Assert.assertTrue(isApi1CancelRedemptionSchemaValidated, "API1 Cancel Redemption Schema Validation failed");
		String cancelRedemptionMsg = cancelRedemptionResponse.jsonPath().get("message").toString();
		Assert.assertEquals(cancelRedemptionMsg, "Redemption successfully cancelled.", "Message did not match");
		utils.logPass("API v1 Cancel Redemption is successful for redemption id: " + redemptionId
				+ " with message: " + cancelRedemptionMsg);

		// Fetch User Notifications
		utils.logit("== Mobile API v1: Fetch User Notifications ==");
		String notificationId = "";
		int counter = 0;
		while (counter < 20) {
			try {
				utils.logit("API hit count is : " + counter);
				Response fetchNotificationsResponse = pageObj.endpoints().api1FetchNotifications(dataSet.get("client"),
						dataSet.get("secret"), token);
				notificationId = fetchNotificationsResponse.jsonPath().get("[0].id").toString();
				if (notificationId != null) {
					Assert.assertEquals(fetchNotificationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 Fetch User Notifications");
					boolean isApi1FetchNotificationsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.api1FetchNotificationsSchema, fetchNotificationsResponse.asString());
					Assert.assertTrue(isApi1FetchNotificationsSchemaValidated,
							"API1 Fetch Notifications Schema Validation failed");
					utils.logPass("API v1 User Notification is fetched successfully with Id: " + notificationId);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longwait(5000);
		}

	}

	@Test(description = "Verify Mobile API v1:- SQ-T4971: Reward Transfer; SQ-T4978: Currency Transfer; SQ-T4973: Account History; "
			+ "SQ-T4974: User Balance; SQ-T4976: Checkins Balance; SQ-T4989: Fetch Offers", groups = "api", priority = 2)
	public void verifyAPIv1Checkins() throws InterruptedException {

		// User sign-up for user #1
		utils.logit("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String fName1 = signUpResponse1.jsonPath().get("first_name").toString();
		String lName1 = signUpResponse1.jsonPath().get("last_name").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		utils.logPass("API v1 user #1 signup is successful with user id: " + userID1);

		// User sign-up for user #2
		utils.logit("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token2 = signUpResponse2.jsonPath().get("auth_token.token").toString();
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		String userID2 = signUpResponse2.jsonPath().get("id").toString();
		utils.logPass("API v1 user #2 signup is successful with user id: " + userID2);

		// Send reward amount to user #1
		utils.logit("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableID"), "", "");
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		utils.logPass("Send reward amount to user #1 is successful");

		// Get Reward Id for user #1
		utils.logit("== Auth API: Get Reward Id for user #1 ==");
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableID"));
		utils.logit("Reward Id for user #1 is fetched: " + rewardId);

		// Reward Transfer to user #2
		utils.logit("== Mobile API v1: Reward Transfer to user #2 ==");
		Response rewardsTransferResponse = pageObj.endpoints().Api1GiftRewardToOtherUser(dataSet.get("client"),
				dataSet.get("secret"), token1, userEmail2, rewardId);
		Assert.assertEquals(rewardsTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Reward Transfer to other user");
		boolean isApi1RewardTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, rewardsTransferResponse.asString());
		Assert.assertTrue(isApi1RewardTransferSchemaValidated, "API1 Reward Transfer Schema Validation failed");
		String actualRewardTransferMsg = rewardsTransferResponse.jsonPath().getString("[0]");
		String expectedRewardTransferMsg = dataSet.get("rewardName") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualRewardTransferMsg.toLowerCase(), expectedRewardTransferMsg.toLowerCase(),
				"Message did not match");
		utils.logPass("API v1 Reward Transfer to user #2 is successful");

		// Currency Transfer to user #2
		utils.logit("== Mobile API v1: Currency Transfer to user #2 ==");
		Response currencyTransferResponse = pageObj.endpoints().api1CurrencyTransferToOtherUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(currencyTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Currency Transfer to other user");
		boolean isApi1CurrencyTransferSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, currencyTransferResponse.asString());
		Assert.assertTrue(isApi1CurrencyTransferSchemaValidated, "API1 Currency Transfer Schema Validation failed");
		String actualCurrencyTransferMsg = currencyTransferResponse.jsonPath().getString("[0]");
		String expectedCurrencyTransferMsg = "$" + dataSet.get("amount") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualCurrencyTransferMsg.toLowerCase(), expectedCurrencyTransferMsg.toLowerCase(),
				"Message did not match");
		utils.logPass("API v1 Currency Transfer to user #2 is successful");

		// Fetch User Offers of user #2
		utils.logit("== Mobile API v1: Fetch User Offers of user #2 ==");
		Response fetchUserOffersResponse = pageObj.endpoints().api1FetchUserOffers(dataSet.get("client"),
				dataSet.get("secret"), token2);
		Assert.assertEquals(fetchUserOffersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Fetch User Offers");
		boolean isApi1FetchUserOffersSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1FetchUserOffersSchema, fetchUserOffersResponse.asString());
		Assert.assertTrue(isApi1FetchUserOffersSchemaValidated, "API1 Fetch User Offers Schema Validation failed");
		String redeemableId = utils.getJsonReponseKeyValueFromJsonArray(fetchUserOffersResponse, "rewards",
				"redeemable_id", dataSet.get("redeemableID"));
		String redeemableRewardName = utils.getJsonReponseKeyValueFromJsonArray(fetchUserOffersResponse, "redeemables",
				"name", dataSet.get("rewardName"));
		Assert.assertEquals(redeemableId, dataSet.get("redeemableID"), "Redeemable Id did not match");
		Assert.assertEquals(redeemableRewardName, dataSet.get("rewardName"), "Reward name did not match");
		utils.logPass("API v1 Fetch User Offers of user #2 is successful");

		// Fetch User Account history (/api/mobile/accounts) for user #2
		utils.logit("== Mobile API v1: Fetch User Account history (/api/mobile/accounts) for user #2 ==");
		Response accountsResponse = pageObj.endpoints().Api1MobileAccounts(token2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Fetch User account history");
		boolean isApi1FetchUserAccountHistorySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api1FetchUserAccountHistorySchema, accountsResponse.asString());
		Assert.assertTrue(isApi1FetchUserAccountHistorySchemaValidated,
				"API1 Fetch User Account History Schema Validation failed");
		String expectedAmountRecievedDescription = "$" + dataSet.get("amount")
				+ " received through Loyalty Transfer from " + fName1 + " " + lName1;
		String actualAmountRecievedDescription = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(
				accountsResponse, "description", expectedAmountRecievedDescription);
		Assert.assertEquals(actualAmountRecievedDescription.toLowerCase(),
				expectedAmountRecievedDescription.toLowerCase(), "Description did not match");

		String expectedItemGiftedDescription = "You were gifted: " + dataSet.get("rewardName") + " (Transferred from "
				+ fName1 + " " + lName1 + ")";
		String actualItemGiftedDescription = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(accountsResponse,
				"description", expectedItemGiftedDescription);
		Assert.assertEquals(actualItemGiftedDescription.toLowerCase(), expectedItemGiftedDescription.toLowerCase(),
				"Description did not match");
		utils.logPass("API v1 Fetch User account history for user #2 is successful");

		// Fetch User balance (/api/mobile/users/balance) for user #2
		utils.logit("== Mobile API v1: Fetch User balance (/api/mobile/users/balance) for user #2 ==");
		Response balanceResponse = pageObj.endpoints().Api1MobileUsersbalance(token2, dataSet.get("client"),
				dataSet.get("secret"));
		String rewardName = utils.getJsonReponseKeyValueFromJsonArray(balanceResponse, "rewards", "name",
				dataSet.get("rewardName"));
		Assert.assertEquals(balanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch users balance");
		boolean isApi1FetchUserBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1FetchUserBalanceSchema, balanceResponse.asString());
		Assert.assertTrue(isApi1FetchUserBalanceSchemaValidated, "API1 Fetch User Balance Schema Validation failed");
		String userBalance = balanceResponse.jsonPath().getString("account_balance.banked_rewards");
		Assert.assertEquals(userBalance, dataSet.get("amount"), "Balance did not match");
		Assert.assertEquals(rewardName, dataSet.get("rewardName"), "Reward name did not match");
		utils.logPass("API v1 fetch User balance for user #2 is successful");

		// Fetch User Checkins Balance for user #2
		utils.logit("== Mobile API v1: Fetch User Checkins Balance for user #2 ==");
		Response userCheckinsBalanceResponse = pageObj.endpoints().Api1MobileCheckinsBalance(token2,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(userCheckinsBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 fetch user Checkins Balance");
		boolean isApi1FetchUserCheckinsBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1FetchUserCheckinsBalanceSchema, userCheckinsBalanceResponse.asString());
		Assert.assertTrue(isApi1FetchUserCheckinsBalanceSchemaValidated,
				"API1 Fetch User Checkins Balance Schema Validation failed");
		String checkinsBalance = userCheckinsBalanceResponse.jsonPath().getString("banked_rewards");
		Assert.assertEquals(checkinsBalance, dataSet.get("amount"), "Balance did not match");
		utils.logPass("API v1 Fetch User Checkins Balance for user #2 is successful");
	}

	@Test(description = "Verify Mobile API v1:- SQ-T4985: Version Notes; SQ-T4950: Meta API; SQ-T4991: Migration User Look-up; "
			+ "SQ-T4993: Generate OTP token; SQ-T5002: Beacon Entry; SQ-T5004: Beacon Exit", groups = "api", priority = 3)
	public void verifyAPIv1VersionNotes() {

		// Create Business Migration User
		utils.logit("== Platform Functions: Create Business Migration User ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUse(userEmail,
				dataSet.get("apiKey"));
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions Create Business Migration User API");
		String card_number = createMigrationUserResponse.jsonPath().get("original_membership_no");
		String email = createMigrationUserResponse.jsonPath().get("email").toString();
		utils.logPass("Platform Functions Create Business Migration User API call is successful");

		// Migration user look-up
		utils.logit("== Mobile API v1: Migration user look-up ==");
		Response migrationLookupResponse = pageObj.endpoints().api1UserMigrationLookup(card_number, email,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(migrationLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Migration user look-up");
		boolean isApi1MigrationUserLookUpSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1MigrationUserLookupSchema, migrationLookupResponse.asString());
		Assert.assertTrue(isApi1MigrationUserLookUpSchemaValidated,
				"API v1 Migration User Look-up Schema Validation failed");
		String migrationUserEmail = migrationLookupResponse.jsonPath().get("email").toString();
		Assert.assertEquals(migrationUserEmail, email, "User email did not match for Migration user look-up");
		utils.logPass("API v1 Migration user look-up call is successful");

		// Meta API
		utils.logit("== Mobile API v1: Meta API ==");
		Response metaApiResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(metaApiResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API v1 Meta API");
		boolean isApi1MetaApiSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.api1MetaApiSchema, metaApiResponse.asString());
		Assert.assertTrue(isApi1MetaApiSchemaValidated, "API v1 Meta API Schema Validation failed");
		String metaBusinessId = metaApiResponse.jsonPath().getString("[0].business_id");
		Assert.assertEquals(metaBusinessId, dataSet.get("businessId"), "business_id did not match");
		utils.logPass("API v1 Meta API call is successful");

		// Version Notes
		utils.logit("== Mobile API v1: Version Notes ==");
		Response versionNotesResponse = pageObj.endpoints().api1VersionNotes(dataSet.get("version"), dataSet.get("os"),
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Version Notes");
		boolean isApi1VersionNotesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesSchema, versionNotesResponse.asString());
		Assert.assertTrue(isApi1VersionNotesSchemaValidated, "API v1 Version Notes Schema Validation failed");
		boolean versionForceUpgradeFlag = versionNotesResponse.jsonPath().get("force_upgrade");
		String versionNote = versionNotesResponse.jsonPath().get("note").toString();
		String versionNotificationStyle = versionNotesResponse.jsonPath().get("notification_style").toString();
		Assert.assertEquals(versionForceUpgradeFlag, false, "Force Upgrade flag did not match");
		Assert.assertEquals(versionNote, "test", "Note did not match");
		Assert.assertEquals(versionNotificationStyle, "local_notification", "Notification Style did not match");
		utils.logPass("API v1 Version Notes call is successful");

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		utils.logPass("API v1 user signup is successful");

		// Generate OTP Token
		utils.logit("== Mobile API v1: Generate OTP Token ==");
		Response generateOtpTokenResponse = pageObj.endpoints().api1GenerateOtpToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(generateOtpTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Generate OTP Token");
		utils.logPass("API v1 Generate OTP Token is successful");

		// Beacon Entry
		utils.logit("== Mobile API v1: Beacon Entry ==");
		Response beaconEntryResponse = pageObj.endpoints().api1BeaconEntry(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("beaconEntryIDs"));
		Assert.assertEquals(beaconEntryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for Beacon Entry");
		utils.logPass("API v1 Beacon Entry is successful");

		// Beacon Exit
		utils.logit("== Mobile API v1: Beacon Exit ==");
		Response beaconExitResponse = pageObj.endpoints().api1BeaconExit(dataSet.get("client"), dataSet.get("secret"),
				token, dataSet.get("beaconExitIDs"));
		Assert.assertEquals(beaconExitResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for Beacon Exit");
		utils.logPass("API v1 Beacon Exit is successful");

	}

	@Test(description = "Verify Mobile API v1:- SQ-T5006: Gaming Achievements; SQ-T5023: Get Scratch Board", groups = "api", priority = 4)
	public void verifyAPIv1GamingAchievements() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		utils.logPass("API v1 user signup is successful");

		// Gaming Achievements
		utils.logit("== Mobile API v1: Gaming Achievements ==");
		Response gamingAchievementsResponse = pageObj.endpoints().APi1GamingAchievements(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("kind"), dataSet.get("level"), dataSet.get("score"),
				dataSet.get("gamingLevelId"));
		Assert.assertEquals(gamingAchievementsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Gaming Achievements");
		boolean isApi1GamingAchievementsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiMessageObjectSchema, gamingAchievementsResponse.asString());
		Assert.assertTrue(isApi1GamingAchievementsSchemaValidated, "API1 Gaming Achievements Schema Validation failed");
		String gamingAchievementsMsg = gamingAchievementsResponse.jsonPath().get("message").toString();
		Assert.assertEquals(gamingAchievementsMsg, "You have earned one FREE punch!", "Message did not match");
		utils.logPass("API v1 Gaming Achievements is successful");

		// Get Scratch Board
		utils.logit("== Mobile API v1: Get Scratch Board ==");
		Response getScratchBoardResponse = pageObj.endpoints().api1GetScratchBoard(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(getScratchBoardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Get Scratch Board");
		boolean isApi1GetScratchBoardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api1GetScratchBoardSchema, getScratchBoardResponse.asString());
		Assert.assertTrue(isApi1GetScratchBoardSchemaValidated, "API1 Get Scratch Board Schema Validation failed");
		String getScratchBoardId = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(getScratchBoardResponse,
				"scratch_id", "127");
		Assert.assertNotNull(getScratchBoardId, "Scratch Board Id is null");
		utils.logPass("API v1 Get Scratch Board is successful");

	}

	@Test(description = "Verify Mobile API v1:- SQ-T5010: Transfer Loyalty Points to User", groups = "api", priority = 5)
	public void verifyAPIv1TransferLoyaltyPointsToUser() {

		// User sign-up for user #1
		utils.logit("== Mobile API v1: User #1 sign-up ==");
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token1 = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String userID1 = signUpResponse1.jsonPath().get("id").toString();
		utils.logPass("API v1 user #1 signup is successful with user id: " + userID1);

		// User sign-up for user #2
		utils.logit("== Mobile API v1: User #2 sign-up ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		String userID2 = signUpResponse2.jsonPath().get("id").toString();
		utils.logPass("API v1 user #2 signup is successful with user id: " + userID2);

		// Send reward amount to user #1
		utils.logit("== Platform Functions: Send reward amount to user #1 ==");
		Response sendMessageToUserResponse = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey"), "",
				"", "", dataSet.get("amount"));
		Assert.assertEquals(sendMessageToUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Send reward amount to user");
		utils.logPass("Send reward amount to user #1 is successful");

		// Loyalty points transfer to user #2
		utils.logit("== Mobile API v1: Loyalty points transfer to user #2 ==");
		Response transferPointsResponse = pageObj.endpoints().Api1TransferLoyaltyPointsToUser(userEmail2,
				dataSet.get("amount"), dataSet.get("client"), dataSet.get("secret"), token1);
		Assert.assertEquals(transferPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 transfer loyalty points to other user");
		boolean isApi1TransferPointsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferPointsResponse.asString());
		Assert.assertTrue(isApi1TransferPointsSchemaValidated,
				"API v1 Transfer Loyalty Points Schema Validation failed");
		String actualTransferPointsMsg = transferPointsResponse.jsonPath().get("[0]").toString();
		String expectedTransferPointsMsg = dataSet.get("amount") + " transferred to " + fName2 + " " + lName2;
		Assert.assertEquals(actualTransferPointsMsg.toLowerCase(), expectedTransferPointsMsg.toLowerCase(),
				"Message did not match");
		utils.logPass("API v1 Loyalty points transfer to user #2 is successful");

	}

	@Test(description = "Verify Mobile API v1:- SQ-T5008: Social Cause Campaign", groups = "api", priority = 6)
	public void verifyAPIv1SocialCauseCampaign() {

		// Create Social Cause Campaigns
		utils.logit("== Platform Functions: Create Social Cause Campaign ==");
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("apiKey"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for Platform Functions API Create Social Cause Campaign");
		String socialCauseId = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();
		String socialCauseName = socialCauseCampaignResponse.jsonPath().get("name").toString();
		utils.logPass("Platform Functions API Create Social Cause Campaigns is successful");

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		utils.logPass("API v1 user signup is successful");

		// Send reward amount to user
		utils.logit("== Platform Functions: Send reward amount to user ==");
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send reward amount to user");
		utils.logPass("Send reward amount to user is successful");

		// Social Cause Create Donation
		utils.logit("== Mobile API v1: Social Cause Create Donation ==");
		Response createDonationResponse = pageObj.endpoints().api1SocialCauseDonation(dataSet.get("client"),
				dataSet.get("secret"), token, socialCauseId, dataSet.get("donationType"), dataSet.get("itemToDonate"));
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Social Cause Create Donation");
		boolean isApi1SocialCauseCreateDonationSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationResponse.asString());
		Assert.assertTrue(isApi1SocialCauseCreateDonationSchemaValidated,
				"API v1 Social Cause Create Donation Schema Validation failed");
		String expectedDonationMessage = "$" + dataSet.get("itemToDonate") + " donated to " + socialCauseName + ".";
		String actualDonationMessage = createDonationResponse.jsonPath().get("[0]").toString();
		Assert.assertEquals(actualDonationMessage, expectedDonationMessage, "Message did not match");
		utils.logPass("API v1 Social Cause Create Donation is successful");

		// Deactivate Social Cause Campaign
		utils.logit("== Platform Functions: Deactivate Social Cause Campaign ==");
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(socialCauseId,
				dataSet.get("apiKey"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Platform Functions API Deactivate Social Cause Campaign");
		utils.logPass("Platform Functions API Deactivate Social Cause Campaign is successful");

	}

	@Test(description = "Verify Mobile API v1 Gift Card:- SQ-T5012: Purchase; SQ-T5014: Update; SQ-T5016: Reload; SQ-T5025: Fetch Balance; "
			+ "SQ-T5043: POST Tip; SQ-T5047: GET Tip; SQ-T5058: Gift; SQ-T5071: Share; SQ-T5077: Transfer; SQ-T5079: Fetch Transaction History", groups = "api", priority = 7)
	public void verifyAPIv1GiftCard() {

		// User sign-up
		utils.logit("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		utils.logPass("API v1 user signup is successful");

		// Purchase Gift Card
		utils.logit("== Mobile API v1: Purchase Gift Card ==");
		String giftCardUuid = "";
		int counter = 0;
		while (counter < 15) {
			try {
				utils.logit("API v1 Purchase gift card hit count is: " + counter);
				Response purchaseGiftCardResponse = pageObj.endpoints().api1PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("designId"),
						dataSet.get("transactionToken"), dataSet.get("expDate"));
				TestListeners.extentTest.get()
						.info("API v1 Purchase gift card call response is: " + purchaseGiftCardResponse.asString());
				giftCardUuid = purchaseGiftCardResponse.jsonPath().get("uuid").toString();
				if (giftCardUuid != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 purchase gift card");
					boolean isApi1PurchaseGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api1GiftCardSchema, purchaseGiftCardResponse.asString());
					Assert.assertTrue(isApi1PurchaseGiftCardSchemaValidated,
							"API v1 Purchase Gift Card Schema Validation failed");
					utils.logPass(
							"API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longwait(5000);
		}

		// Update Gift Card
		utils.logit("== Mobile API v1: Update Gift Card ==");
		Response updateGiftCardResponse = pageObj.endpoints().api1UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("preferred"), giftCardUuid);
		Assert.assertEquals(updateGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Update gift card");
		boolean isApi1UpdateGiftCardSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api1GiftCardSchema, updateGiftCardResponse.asString());
		Assert.assertTrue(isApi1UpdateGiftCardSchemaValidated, "API v1 Update Gift Card Schema Validation failed");
		String preferredValue = updateGiftCardResponse.jsonPath().get("preferred").toString();
		Assert.assertEquals(preferredValue, dataSet.get("preferred"), "Updated Preferred value did not match");
		utils.logPass("API v1 Update gift card is successful");

		// Reload Gift Card
		utils.logit("== Mobile API v1: Reload Gift Card ==");
		String totalAmount = "";
		int attempt = 0;
		while (attempt < 20) {
			try {
				utils.logit("API v1 Reload gift card hit count is: " + attempt);
				Response reloadGiftCardResponse = pageObj.endpoints().api1ReloadGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("amount"), dataSet.get("designId"),
						dataSet.get("transactionToken"), giftCardUuid);
				TestListeners.extentTest.get()
						.info("API v1 Reload gift card call response is: " + reloadGiftCardResponse.asString());
				totalAmount = reloadGiftCardResponse.jsonPath().get("last_fetched_amount").toString();
				if (totalAmount != null) {
					Assert.assertEquals(reloadGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 Reload gift card call.");
					boolean isApi1ReloadGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api1GiftCardSchema, reloadGiftCardResponse.asString());
					Assert.assertTrue(isApi1ReloadGiftCardSchemaValidated,
							"API v1 Reload Gift Card Schema Validation failed");
					utils.logPass(
							"API v1 Reload gift card call is successful with Total amount fetched as: " + totalAmount);
					break;
				}
			} catch (Exception e) {

			}
			attempt++;
			utils.longwait(5000);
		}

		// Fetch Gift Card Balance
		utils.logit("== Mobile API v1: Fetch Gift Card Balance ==");
		Response giftCardBalanceResponse = pageObj.endpoints().api1FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(giftCardBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Fetch Gift Card Balance");
		boolean isApi1FetchGiftCardBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1GiftCardSchema, giftCardBalanceResponse.asString());
		Assert.assertTrue(isApi1FetchGiftCardBalanceSchemaValidated,
				"API v1 Fetch Gift Card Balance Schema Validation failed");
		String balance = giftCardBalanceResponse.jsonPath().get("last_fetched_amount").toString();
		Assert.assertEquals(balance, totalAmount, "Balance did not match");
		utils.logPass("API v1 Fetch Gift Card Balance is successful");

		// Create Loyalty Checkin by Receipt Image
		utils.logit("== Mobile API2: Create Loyalty Checkin by Receipt Image ==");
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("locationId"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 Loyalty Checkin by Receipt Image");
		String checkinId = receiptCheckinResponse.jsonPath().getString("checkin_id").toString();
		utils.logPass("API2 Create Loyalty Checkin by Receipt Image is successful with checkin id: " + checkinId);

		// POST Tip via Gift Card
		utils.logit("== Mobile API v1: POST Tip via Gift Card ==");
		Response postTipGiftCardResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "POST");
		Assert.assertEquals(postTipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 POST Tip via Gift Card");
		boolean isApi1PostTipGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1GiftCardTipSchema, postTipGiftCardResponse.asString());
		Assert.assertTrue(isApi1PostTipGiftCardSchemaValidated,
				"API v1 POST Tip via Gift Card Schema Validation failed");
		String approvedAmount = postTipGiftCardResponse.jsonPath().get("approved_amount").toString();
		Assert.assertEquals(approvedAmount, dataSet.get("tip"), "Approved amount did not match");
		utils.logPass("API v1 POST Tip via Gift Card is successful");

		// GET Tip via Gift Card
		utils.logit("== Mobile API v1: GET Tip via Gift Card ==");
		Response getTipGiftCardResponse = pageObj.endpoints().api1GiftCardTip(dataSet.get("client"),
				dataSet.get("secret"), token, checkinId, giftCardUuid, dataSet.get("tip"), "GET");
		Assert.assertEquals(getTipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 GET Tip via Gift Card");
		boolean isApi1GetTipGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1GiftCardTipSchema, getTipGiftCardResponse.asString());
		Assert.assertTrue(isApi1GetTipGiftCardSchemaValidated, "API v1 GET Tip via Gift Card Schema Validation failed");
		String locationId = getTipGiftCardResponse.jsonPath().get("location_id").toString();
		Assert.assertEquals(locationId, dataSet.get("locationId"), "Location Id did not match");
		utils.logPass("API v1 GET Tip via Gift Card is successful");

		// User sign-up #2
		utils.logit("== Mobile API v1: User sign-up #2 ==");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api1UserSignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String fName2 = signUpResponse2.jsonPath().get("first_name").toString();
		String lName2 = signUpResponse2.jsonPath().get("last_name").toString();
		utils.logPass("API v1 user signup #2 is successful");

		// Gift a Gift Card to user #2
		utils.logit("== Mobile API v1: Gift a Gift Card to user #2 ==");
		String actualGiftaCardSuccessMsg = "";
		int count = 0;
		while (count < 10) {
			try {
				utils.logit("API v1 Gift a gift card hit count is: " + count);
				Response giftaCardResponse = pageObj.endpoints().api1GiftaCardWithRandomAmount(dataSet.get("client"),
						dataSet.get("secret"), token, userEmail2, dataSet.get("designId"),
						dataSet.get("transactionToken"));
				TestListeners.extentTest.get()
						.info("API v1 Gift a gift card call response is: " + giftaCardResponse.asString());
				actualGiftaCardSuccessMsg = giftaCardResponse.jsonPath().get("[0]").toString();
				if (actualGiftaCardSuccessMsg.contains("You have successfully gifted a Gift Card")) {
					Assert.assertEquals(giftaCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
							"Status code 201 did not match for API v1 Gift a Gift Card call.");
					boolean isApi1GiftCardGiftSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.apiStringArraySchema, giftaCardResponse.asString());
					Assert.assertTrue(isApi1GiftCardGiftSchemaValidated,
							"API v1 Gift a Gift Card Schema Validation failed");
					utils.logPass("API v1 Gift a gift card to user #2 is successful.");
					break;
				}
			} catch (Exception e) {

			}
			count++;
			utils.longWaitInSeconds(5);
		}

		// Share Gift Card to user #2
		utils.logit("== Mobile API v1: Share Gift Card to user #2 ==");
		Response shareGiftCardResponse = pageObj.endpoints().Api1ShareGiftCard(userEmail2, dataSet.get("client"),
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(shareGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Share gift card");
		boolean isApi1ShareGiftCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardResponse.asString());
		Assert.assertTrue(isApi1ShareGiftCardSchemaValidated, "API v1 Share Gift Card Schema Validation failed");
		String actualShareGiftCardSuccessMsg = shareGiftCardResponse.jsonPath().get("[0]").toString();
		String expectedShareGiftCardSuccessMsg = "Gift Card shared with " + fName2 + " " + lName2 + " successfully.";
		Assert.assertEquals(actualShareGiftCardSuccessMsg.toLowerCase(), expectedShareGiftCardSuccessMsg.toLowerCase(),
				"Message did not match");
		utils.logPass("API v1 Share Gift Card to user #2 is successful");

		// Purchase Gift Card #2 (new gift card to transfer to user #2)
		utils.logit("== Mobile API v1: Purchase Gift Card #2 (new gift card to transfer to user #2) ==");
		String giftCardUuid2 = "";
		int attempts = 0;
		while (attempts < 10) {
			try {
				utils.logit("API v1 Purchase gift card hit count is: " + attempts);
				Response purchaseGiftCardResponse = pageObj.endpoints().api1PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("designId"),
						dataSet.get("transactionToken"), dataSet.get("expDate"));
				TestListeners.extentTest.get()
						.info("API v1 Purchase gift card call response is: " + purchaseGiftCardResponse.asString());
				giftCardUuid2 = purchaseGiftCardResponse.jsonPath().get("uuid").toString();
				if (giftCardUuid2 != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v1 purchase gift card");
					utils.logPass(
							"API v1 Purchase gift card call is successful with Gift Card UUID as: " + giftCardUuid2);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
			utils.longwait(5000);
		}

		// Transfer Gift Card #2 to user #2
		utils.logit("== Mobile API v1: Transfer Gift Card to user #2 ==");
		Response transferGiftCardResponse = pageObj.endpoints().Api1TransferGiftCard(userEmail2, dataSet.get("client"),
				dataSet.get("secret"), "8", token, giftCardUuid2);
		Assert.assertEquals(transferGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Transfer gift card due to "
						+ transferGiftCardResponse.asString());
		boolean isApi1TransferGiftCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferGiftCardResponse.asString());
		Assert.assertTrue(isApi1TransferGiftCardSchemaValidated, "API v1 Transfer Gift Card Schema Validation failed");
		String actualTransferGiftCardSuccessMsg = transferGiftCardResponse.jsonPath().get("[0]").toString();
		String expectedTransferGiftCardSuccessMsg = "You have successfully transferred $8.00 to " + fName2 + " "
				+ lName2 + ".";
		Assert.assertEquals(actualTransferGiftCardSuccessMsg.toLowerCase(),
				expectedTransferGiftCardSuccessMsg.toLowerCase(), "Message did not match");
		utils.logPass("API v1 Transfer Gift Card to user #2 is successful");

		// Fetch Gift Card Transaction History
		utils.logit("== Mobile API v1: Fetch Gift Card Transaction History ==");
		Response giftCardHistoryResponse = pageObj.endpoints().api1GiftCardTransactionHistory(dataSet.get("client"),
				dataSet.get("secret"), token, giftCardUuid);
		Assert.assertEquals(giftCardHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Fetch Gift Card Transaction History");
		boolean isApi1GiftCardHistorySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api1GiftCardHistorySchema, giftCardHistoryResponse.asString());
		Assert.assertTrue(isApi1GiftCardHistorySchemaValidated,
				"API v1 Fetch Gift Card Transaction History Schema Validation failed");
		String eventName = utils.getJsonReponseKeyValueFromJsonArrayWithoutArrayName(giftCardHistoryResponse, "event",
				"Reloaded");
		Assert.assertEquals(eventName, "Reloaded", "Event Name did not match");
		utils.logPass("API v1 Fetch Gift Card Transaction History is successful");

	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
