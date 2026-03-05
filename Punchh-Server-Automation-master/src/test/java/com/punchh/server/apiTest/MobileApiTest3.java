package com.punchh.server.apiTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileApiTest3 {

	private static Logger logger = LogManager.getLogger(MobileApiTest3.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "api";
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		utils = new Utilities();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T2811 Verify Api2 giftcard api", groups = "api", priority = 0)
	public void Api2GiftCardApi() throws InterruptedException {

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(token, "Access token should not be null");
		Assert.assertFalse(token.isEmpty(), "Access token should not be empty");
		String userId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(userId, "User ID should not be null");
		String responseEmail = signUpResponse.jsonPath().getString("user.email");
		Assert.assertEquals(responseEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");
		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userId);

		// Purchase Gift Card API2
		String uuid = "";
		String cardNumber = "";
		int designId = 0;
		double lastFetchedAmount = 0;
		int attempts = 0;
		while (attempts < 20) {
			try {
				pageObj.utils().logit("info", "API hit count is : " + attempts);
				utils.longwait(5000);
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("design_id"));
				Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
						"Status code 200 did not matched for api2 purchase gift card");
				uuid = purchaseGiftCardResponse.jsonPath().getString("uuid");
				if (uuid != null) {
					boolean isPurchaseGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api2GiftCardSchema, purchaseGiftCardResponse.asString());
					Assert.assertTrue(isPurchaseGiftCardSchemaValidated,
							"API v2 Purchase Gift Card Schema Validation failed");
					Assert.assertFalse(uuid.isEmpty(), "Gift card UUID should not be empty");
					cardNumber = purchaseGiftCardResponse.jsonPath().getString("card_number");
					Assert.assertNotNull(cardNumber, "Card number should not be null");
					designId = purchaseGiftCardResponse.jsonPath().getInt("design_id");
					Assert.assertTrue(designId > 0, "Design ID should be greater than 0");
					lastFetchedAmount = purchaseGiftCardResponse.jsonPath().getDouble("last_fetched_amount");
					Assert.assertTrue(lastFetchedAmount > 0, "Last fetched amount should be greater than 0");
					String status = purchaseGiftCardResponse.jsonPath().getString("status");
					Assert.assertEquals(status, "active", "Gift card status should be active");
					pageObj.utils().logit("pass", "Api2 Purchase Gift Card is successful. UUID: " + uuid + ", Card Number: " + cardNumber + ", Amount: " + lastFetchedAmount);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}

		// Update Gift Cards
		Response updateGiftCardResponse = pageObj.endpoints().Api2UpdateGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(updateGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 update gift card");
		boolean isUpdateGiftCardSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2GiftCardSchema, updateGiftCardResponse.asString());
		Assert.assertTrue(isUpdateGiftCardSchemaValidated, "API v2 Update Gift Card Schema Validation failed");
		String updatedUuid = updateGiftCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(updatedUuid, uuid, "Updated UUID should match original UUID");
		String updatedStatus = updateGiftCardResponse.jsonPath().getString("status");
		Assert.assertEquals(updatedStatus, "active", "Gift card status should remain active after update");
		pageObj.utils().logit("pass", "Api2 Update Gift Card is successful. UUID: " + updatedUuid);

		// Reload Gift Card API2
		double reloadedAmount = 0;
		int counter = 0;
		while (counter < 10) {
			try {
				pageObj.utils().logit("info", "API v2 Reload gift card hit count is: " + counter);
				Response giftCardReloadResponse = pageObj.endpoints().api2ReloadGiftCardWithRandomAmount(
						dataSet.get("client"), dataSet.get("secret"), token, uuid, userEmail, dataSet.get("design_id"),
						"fake-valid-nonce", "Test Name", "825", "VISA");
				Object lastFetchedAmountObj = giftCardReloadResponse.jsonPath().get("last_fetched_amount");
				if (lastFetchedAmountObj != null) {
					Assert.assertEquals(giftCardReloadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API v2 Reload Gift Card call.");
					boolean isReloadGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api2GiftCardSchema, giftCardReloadResponse.asString());
					Assert.assertTrue(isReloadGiftCardSchemaValidated,
							"API v2 Reload Gift Card Schema Validation failed");
					reloadedAmount = giftCardReloadResponse.jsonPath().getDouble("last_fetched_amount");
					Assert.assertTrue(reloadedAmount > 0, "Reloaded amount should be greater than 0");
					String reloadedUuid = giftCardReloadResponse.jsonPath().getString("uuid");
					Assert.assertEquals(reloadedUuid, uuid, "UUID should match after reload");
					pageObj.utils().logit("pass", "API v2 Reload gift card call is successful. Amount: " + reloadedAmount);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longWaitInSeconds(5);
		}

		// Fetch Gift Card
		Response fetchGiftCardResponse = pageObj.endpoints().Api2FetchGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch gift card");
		boolean isFetchGiftCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2FetchGiftCardsSchema, fetchGiftCardResponse.asString());
		Assert.assertTrue(isFetchGiftCardSchemaValidated, "API v2 Fetch Gift Card Schema Validation failed");
		int giftCardsCount = fetchGiftCardResponse.jsonPath().getList("$").size();
		Assert.assertTrue(giftCardsCount > 0, "Fetched gift cards count should be greater than 0");
		String fetchedUuid = fetchGiftCardResponse.jsonPath().getString("[0].uuid");
		Assert.assertNotNull(fetchedUuid, "Fetched gift card UUID should not be null");
		pageObj.utils().logit("pass", "Api2 Fetch Gift Card is successful. Cards Count: " + giftCardsCount);

		// Fetch Gift Card Balance
		Response fetchGiftCardBalanceResponse = pageObj.endpoints().Api2FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(fetchGiftCardBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch gift card balance");
		boolean isFetchGiftCardBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GiftCardSchema, fetchGiftCardBalanceResponse.asString());
		Assert.assertTrue(isFetchGiftCardBalanceSchemaValidated,
				"API v2 Fetch Gift Card Balance Schema Validation failed");
		double balanceAmount = fetchGiftCardBalanceResponse.jsonPath().getDouble("last_fetched_amount");
		Assert.assertTrue(balanceAmount >= 0, "Gift card balance should be non-negative");
		String balanceUuid = fetchGiftCardBalanceResponse.jsonPath().getString("uuid");
		Assert.assertEquals(balanceUuid, uuid, "UUID should match for balance fetch");
		pageObj.utils().logit("pass", "Api2 Fetch Gift Card Balance is successful. Balance: " + balanceAmount);

		// Fetch Gift Card Transaction History
		Response giftCardtransactionResponse = pageObj.endpoints().Api2GiftCardTransactionHistory(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(giftCardtransactionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch gift card transaction history");
		boolean isGiftCardTransactionHistorySchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api1GiftCardHistorySchema, giftCardtransactionResponse.asString());
		Assert.assertTrue(isGiftCardTransactionHistorySchemaValidated,
				"API v2 Fetch Gift Card Transaction History Schema Validation failed");
		int historyCount = giftCardtransactionResponse.jsonPath().getList("$").size();
		Assert.assertTrue(historyCount > 0, "Transaction history should have at least one entry");
		String firstEventType = giftCardtransactionResponse.jsonPath().getString("[0].event_type");
		Assert.assertNotNull(firstEventType, "Event type should not be null");
		pageObj.utils().logit("pass", "Api2 Fetch Gift Card Transaction History is successful. History Count: " + historyCount);

		// User register/signup using API2 Signup (receiver user)
		String reciverUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpReciverResponse = pageObj.endpoints().Api2SignUp(reciverUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpReciverResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String receiverUserId = signUpReciverResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(receiverUserId, "Receiver User ID should not be null");
		pageObj.utils().logit("pass", "Api2 receiver user signup is successful. User ID: " + receiverUserId);

		// Transfer GiftCard Balance
		Response transferGiftCardResponse = pageObj.endpoints().Api2TransferGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(transferGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 transfer gift card balance");
		boolean isTransferGiftCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, transferGiftCardResponse.asString());
		Assert.assertTrue(isTransferGiftCardSchemaValidated, "API v2 Transfer Gift Card Schema Validation failed");
		String transferMessage = transferGiftCardResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(transferMessage, "Transfer message should not be null");
		Assert.assertTrue(transferMessage.contains("successfully transferred"), "Transfer message should contain 'successfully transferred'");
		pageObj.utils().logit("pass", "Api2 Transfer Gift Card is successful. Message: " + transferMessage);

		// Share Gift Card
		Response shareGiftCardResponse = pageObj.endpoints().Api2ShareGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, reciverUserEmail);
		Assert.assertEquals(shareGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 share gift card ");
		boolean isShareGiftCardSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, shareGiftCardResponse.asString());
		Assert.assertTrue(isShareGiftCardSchemaValidated, "API v2 Share Gift Card Schema Validation failed");
		String shareMessage = shareGiftCardResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(shareMessage, "Share message should not be null");
		Assert.assertTrue(shareMessage.contains("shared") && shareMessage.contains("successfully"), "Share message should contain 'shared' and 'successfully'");
		pageObj.utils().logit("pass", "Api2 Share Gift Card is successful. Message: " + shareMessage);

		// Create Loyalty Checkin by Receipt Image
		Response receiptCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinReceiptImage(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("locationid"));
		Assert.assertEquals(receiptCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Loyalty Checkin by Receipt Image");
		boolean isReceiptCheckinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateLoyaltyCheckinSchema, receiptCheckinResponse.asString());
		Assert.assertTrue(isReceiptCheckinSchemaValidated,
				"API v2 Loyalty Checkin by Receipt Image Schema Validation failed");
		long checkinIdLong = receiptCheckinResponse.jsonPath().getLong("checkin_id");
		Assert.assertTrue(checkinIdLong > 0, "Checkin ID should be greater than 0");
		String checkin_id = String.valueOf(checkinIdLong);
		int checkinLocationId = receiptCheckinResponse.jsonPath().getInt("location_id");
		Assert.assertTrue(checkinLocationId > 0, "Location ID should be greater than 0");
		String membershipLevel = receiptCheckinResponse.jsonPath().getString("current_membership_level");
		Assert.assertNotNull(membershipLevel, "Membership level should not be null");
		pageObj.utils().logit("pass", "Api2 Loyalty Checkin by Receipt Image is successful. Checkin ID: " + checkin_id + ", Membership Level: " + membershipLevel);

		// Tip via giftcard
		Response tipGiftCardResponse = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"), dataSet.get("secret"),
				token, uuid, checkin_id);
		Assert.assertEquals(tipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 tip via gift card ");
		boolean isTipGiftCardSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api1GiftCardTipSchema, tipGiftCardResponse.asString());
		Assert.assertTrue(isTipGiftCardSchemaValidated, "API v2 Tip Via Gift Card Schema Validation failed");
		long tipCheckinId = tipGiftCardResponse.jsonPath().getLong("checkin_id");
		Assert.assertEquals(String.valueOf(tipCheckinId), checkin_id, "Tip checkin ID should match original checkin ID");
		double approvedAmount = tipGiftCardResponse.jsonPath().getDouble("approved_amount");
		Assert.assertTrue(approvedAmount > 0, "Approved tip amount should be greater than 0");
		String tipGiftCardUuid = tipGiftCardResponse.jsonPath().getString("gift_card_uuid");
		Assert.assertEquals(tipGiftCardUuid, uuid, "Tip gift card UUID should match");
		pageObj.utils().logit("pass", "Api2 Tip Via Gift Card is successful. Approved Amount: " + approvedAmount);

		// Delete a card
		Response deleteGiftCard = pageObj.endpoints().Api2DeleteGiftCard(dataSet.get("client"), dataSet.get("secret"),
				token, uuid);
		Assert.assertEquals(deleteGiftCard.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 delete a gift card");
		boolean isDeleteGiftCardSchemaValidated = Utilities
				.validateJsonArrayAgainstSchema(ApiResponseJsonSchema.apiStringArraySchema, deleteGiftCard.asString());
		Assert.assertTrue(isDeleteGiftCardSchemaValidated, "API v2 Delete Gift Card Schema Validation failed");
		String deleteMessage = deleteGiftCard.jsonPath().getString("[0]");
		Assert.assertNotNull(deleteMessage, "Delete message should not be null");
		Assert.assertTrue(deleteMessage.contains("successfully deleted"), "Delete message should contain 'successfully deleted'");
		pageObj.utils().logit("pass", "Api2 delete Gift Card is successful. Message: " + deleteMessage);

		// Gift a card
		String actualGiftCardGiftedMsg = "";
		String expectedGiftCardGiftedMsg = "You have successfully gifted a Gift Card";
		int count = 0;
		while (count < 10) {
			try {
				pageObj.utils().logit("info", "API v2 Gift a gift card hit count is: " + count);
				Response giftCardGiftedResponse = pageObj.endpoints().api2GiftCardGiftedWithRandomAmount(
						dataSet.get("client"), dataSet.get("secret"), token, userEmail, dataSet.get("design_id"),
						"fake-valid-nonce", "Test Name", "825", "VISA");
				actualGiftCardGiftedMsg = giftCardGiftedResponse.jsonPath().getString("[0]");
				if (actualGiftCardGiftedMsg != null && actualGiftCardGiftedMsg.contains(expectedGiftCardGiftedMsg)) {
					Assert.assertEquals(giftCardGiftedResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
							"Status code 201 did not match for API v2 Gift a Gift Card call.");
					boolean isGiftCardGiftedSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.apiStringArraySchema, giftCardGiftedResponse.asString());
					Assert.assertTrue(isGiftCardGiftedSchemaValidated,
							"API v2 Gift a Gift Card Schema Validation failed");
					Assert.assertTrue(actualGiftCardGiftedMsg.contains("$"), "Gift card message should contain amount");
					pageObj.utils().logit("pass", "API v2 Gift a gift card call is successful. Message: " + actualGiftCardGiftedMsg);
					break;
				}
			} catch (Exception e) {

			}
			count++;
			utils.longWaitInSeconds(5);
		}

	}

	@Test(description = "SQ-T2810 Verify Api2 old user signup login and logout", groups = "api", priority = 1)
	public void Api2OldUserLoginLogout() {

		// User login using API2 Signin
		pageObj.utils().logit("info", "== Mobile API2 login with existing user ==");
		userEmail = "automation02@punchh.com";
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 login");
		boolean isUserLoginExistingUserSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UserSignUpLogInFetchUpdateSchema, loginResponse.asString());
		Assert.assertTrue(isUserLoginExistingUserSchemaValidated,
				"API2 Login with Existing User Schema Validation failed");
		String token = loginResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(token, "Login token should not be null");
		Assert.assertFalse(token.isEmpty(), "Login token should not be empty");
		String loginUserId = loginResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(loginUserId, "User ID should not be null");
		String loginEmail = loginResponse.jsonPath().getString("user.email");
		Assert.assertEquals(loginEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");
		pageObj.utils().logit("pass", "Mobile API2 login with existing user '" + userEmail + "' is successful. User ID: " + loginUserId);

		// User logout using API2 Logout
		pageObj.utils().logit("info", "== Mobile API2 logout with existing user ==");
		Response logoutResponse = pageObj.endpoints().Api2Logout(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(logoutResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 logout");
		pageObj.utils().logit("pass", "Mobile API2 logout with existing user '" + userEmail + "' is successful");

	}

	@Test(description = "SQ-T2813 Verify Api2 Social Cause Campaign and Notifications API", groups = "api", priority = 2)
	public void Api2SocialCauseCampaignAndNotificationsApi() throws InterruptedException {

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		boolean isSocialCauseCampaignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SocialCauseCampaignSchema, socialCauseCampaignResponse.asString());
		Assert.assertTrue(isSocialCauseCampaignSchemaValidated,
				"Dashboard API Create Social Cause Campaign Schema Validation failed");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().getString("social_cause_id");
		Assert.assertNotNull(social_cause_id, "Social cause ID should not be null");
		String createdCampaignName = socialCauseCampaignResponse.jsonPath().getString("name");
		Assert.assertEquals(createdCampaignName, campaignName, "Campaign name should match");
		boolean activated = socialCauseCampaignResponse.jsonPath().getBoolean("activated");
		Assert.assertTrue(activated, "Campaign should be activated");
		pageObj.utils().logit("pass", "PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful. ID: " + social_cause_id);

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(token, "Token should not be null");
		String userID = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(userID, "User ID should not be null");
		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + userID);

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", dataSet.get("amount"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		pageObj.utils().logit("pass", "Api2 send reward amount to user is successful. Amount: " + dataSet.get("amount"));

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		boolean isGetSocialCauseCampaignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GetSocialCauseCampaignSchema, getsocialCauseCampaignResponse.asString());
		Assert.assertTrue(isGetSocialCauseCampaignSchemaValidated,
				"API v2 Social Cause Campaign Schema Validation failed");
		String getsocial_cause_id = getsocialCauseCampaignResponse.jsonPath().getString("social_cause_id");
		Assert.assertEquals(getsocial_cause_id, social_cause_id, "Social cause ID should match");
		String getCampaignName = getsocialCauseCampaignResponse.jsonPath().getString("name");
		Assert.assertNotNull(getCampaignName, "Campaign name should not be null");
		pageObj.utils().logit("pass", "Api2 Social Cause Campaign Details is successful. Name: " + getCampaignName);

		// Create Donation
		Response createDonationResponse = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Create Donation");
		boolean isCreateDonationSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createDonationResponse.asString());
		Assert.assertTrue(isCreateDonationSchemaValidated, "API v2 Create Donation Schema Validation failed");
		String donationMessage = createDonationResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(donationMessage, "Donation message should not be null");
		Assert.assertTrue(donationMessage.contains("donated"), "Donation message should contain 'donated'");
		pageObj.utils().logit("pass", "Api2 Create Donation is successful. Message: " + donationMessage);

		// Social Cause Campaign Details
		Response socialCauseCampaignDetailsResponse = pageObj.endpoints().Api2SocialCausecampaigndetails(
				dataSet.get("client"), dataSet.get("secret"), token, getsocial_cause_id);
		Assert.assertEquals(socialCauseCampaignDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		boolean isSocialCauseCampaignDetailsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GetSocialCauseCampaignSchema, socialCauseCampaignDetailsResponse.asString());
		Assert.assertTrue(isSocialCauseCampaignDetailsSchemaValidated,
				"API v2 Social Cause Campaign Details Schema Validation failed");
		String detailsSocialCauseId = socialCauseCampaignDetailsResponse.jsonPath().getString("social_cause_id");
		Assert.assertEquals(detailsSocialCauseId, getsocial_cause_id, "Social cause ID should match in details");
		pageObj.utils().logit("pass", "Api2 Social Cause Campaign Details is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		boolean isDeactivateSocialCampaignSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2SocialCauseCampaignSchema, deactivateSocialCampaignResponse.asString());
		Assert.assertTrue(isDeactivateSocialCampaignSchemaValidated,
				"Dashboard API Deactivate Social Cause Campaign Schema Validation failed");
		boolean deactivated = deactivateSocialCampaignResponse.jsonPath().getBoolean("activated");
		Assert.assertFalse(deactivated, "Campaign should be deactivated");
		pageObj.utils().logit("pass", "PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");

		// Create Feedback
		Response createfeedbackResponse = pageObj.endpoints().Api2CreateFeedback(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(createfeedbackResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create feedback");
		boolean isCreateFeedbackSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FeedbackSchema, createfeedbackResponse.asString());
		Assert.assertTrue(isCreateFeedbackSchemaValidated, "API v2 Create Feedback Schema Validation failed");
		int feedbackIdInt = createfeedbackResponse.jsonPath().getInt("feedback_id");
		Assert.assertTrue(feedbackIdInt > 0, "Feedback ID should be greater than 0");
		String feedback_id = String.valueOf(feedbackIdInt);
		pageObj.utils().logit("pass", "Api2 Create Feedback is successful. Feedback ID: " + feedback_id);

		// Update Feedback
		Response updatefeedbackResponse = pageObj.endpoints().Api2UpdateFeedback(dataSet.get("client"),
				dataSet.get("secret"), token, feedback_id);
		Assert.assertEquals(updatefeedbackResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 update feedback");
		boolean isUpdateFeedbackSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api2FeedbackSchema, updatefeedbackResponse.asString());
		Assert.assertTrue(isUpdateFeedbackSchemaValidated, "API v2 Update Feedback Schema Validation failed");
		int updatedFeedbackId = updatefeedbackResponse.jsonPath().getInt("feedback_id");
		Assert.assertTrue(updatedFeedbackId > 0, "Updated feedback ID should be greater than 0");
		pageObj.utils().logit("pass", "Api2 Update Feedback is successful. Feedback ID: " + updatedFeedbackId);

		// Fetch Client Token
		Response fetchClientTokenResponse = pageObj.endpoints().Api2FetchClientToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(fetchClientTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch client token");
		boolean isFetchClientTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2FetchTokenSchema, fetchClientTokenResponse.asString());
		Assert.assertTrue(isFetchClientTokenSchemaValidated, "API v2 Fetch Client Token Schema Validation failed");
		String clientToken = fetchClientTokenResponse.jsonPath().getString("token");
		Assert.assertNotNull(clientToken, "Client token should not be null");
		Assert.assertFalse(clientToken.isEmpty(), "Client token should not be empty");
		pageObj.utils().logit("pass", "Api2 Fetch Client Token is successful");

		// Fetch User Notifications
		String notification_id = "";
		int attempts = 0;
		while (attempts < 20) {
			try {
				pageObj.utils().logit("info", "Mobile API2 Fetch Notifications API hit count is : " + attempts);
				Response fetchNotificationsResponse = pageObj.endpoints().Api2FetchNotifications(dataSet.get("client"),
						dataSet.get("secret"), token);

				int statusCode = fetchNotificationsResponse.getStatusCode();
				Object notificationIdObj = fetchNotificationsResponse.jsonPath().get("[0].id");
				if (statusCode == 200 && notificationIdObj != null) {
					notification_id = notificationIdObj.toString();
					boolean isFetchNotificationsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
							ApiResponseJsonSchema.api2FetchNotificationsSchema, fetchNotificationsResponse.asString());
					Assert.assertTrue(isFetchNotificationsSchemaValidated,
							"API v2 Fetch Notifications Schema Validation failed");
					String notificationKind = fetchNotificationsResponse.jsonPath().getString("[0].kind");
					Assert.assertNotNull(notificationKind, "Notification kind should not be null");
					pageObj.utils().logit("pass", "Api2 Fetch User Notifications successful. Notification ID: " + notification_id + ", Kind: " + notificationKind);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
			utils.longwait(5000);
		}
		if (notification_id.isEmpty()) {
			Assert.fail("Notification id is empty");
		}

		// Delete User Notification Response
		Response deleteNotificationsResponse = pageObj.endpoints().Api2DeletehNotifications(dataSet.get("client"),
				dataSet.get("secret"), token, notification_id);
		Assert.assertEquals(deleteNotificationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 delete notifications");
		pageObj.utils().logit("pass", "Api2 Delete User Notification is successful. Notification ID: " + notification_id);

		// Fetch Messages
		String message_id = "";
		int counter = 0;
		while (counter < 20) {
			try {
				pageObj.utils().logit("info", "Mobile API2 Fetch Messages API hit count is : " + counter);
				Response fetchMessagesResponse = pageObj.endpoints().Api2FetchMessages(dataSet.get("client"),
						dataSet.get("secret"), token);
				Object messageIdObj = fetchMessagesResponse.jsonPath().get("messages[0].message_id");

				if (messageIdObj != null) {
					message_id = messageIdObj.toString();
					Assert.assertEquals(fetchMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not matched for api2 Fetch Messages");
					boolean isFetchMessagesSchemaValidated = Utilities.validateJsonAgainstSchema(
							ApiResponseJsonSchema.api1FetchMessagesSchema, fetchMessagesResponse.asString());
					Assert.assertTrue(isFetchMessagesSchemaValidated, "API v2 Fetch Messages Schema Validation failed");
					String messageBody = fetchMessagesResponse.jsonPath().getString("messages[0].body");
					Assert.assertNotNull(messageBody, "Message body should not be null");
					pageObj.utils().logit("pass", "Api2 Fetch Messages successful. Message ID: " + message_id);
					break;
				}
			} catch (Exception e) {

			}
			counter++;
			utils.longwait(5000);
		}

		// Mark message read
		Response markMessagesReadResponse = pageObj.endpoints().Api2MarkMessagesRead(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(markMessagesReadResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for api2 Mark Messages Read");
		pageObj.utils().logit("pass", "Api2 Mark Messages Read successful");

		// delete message
		Response deleteMessagesResponse = pageObj.endpoints().Api2DeleteMessages(dataSet.get("client"), message_id,
				dataSet.get("secret"), token);
		Assert.assertEquals(deleteMessagesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Delete Messages");
		pageObj.utils().logit("pass", "Api2 Delete Messages successful. Message ID: " + message_id);

		// Generate OTP token
		Response getOTPTokenResponse = pageObj.endpoints().Api2GenerateOtpToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(getOTPTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Generate Otp Token");
		pageObj.utils().logit("pass", "Api2 Generate Otp Token is successful");
	}

	@Test(description = "SQ-T2853 Verify Api2 Challenges_Passcodes And Invitations api", groups = "api", priority = 3)
	public void Api2ChallengesPasscodesAndInvitationsApi() throws InterruptedException {
		String passcode = "123456";
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(token, "Token should not be null");
		String signupUserId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		pageObj.utils().logit("pass", "Api2 user signup is successful. User ID: " + signupUserId);

		// Purchase Gift Card API2
		String uuid = "";
		String giftCardNumber = "";
		int attempts = 0;
		while (attempts < 10) {
			try {
				pageObj.utils().logit("info", "API2 Purchase gift card hit count is: " + attempts);
				Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
						dataSet.get("secret"), token, dataSet.get("design_id"));
				uuid = purchaseGiftCardResponse.jsonPath().getString("uuid");
				if (uuid != null) {
					Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
							"Status code 200 did not match for API2 Purchase gift card");
					Assert.assertFalse(uuid.isEmpty(), "Gift card UUID should not be empty");
					giftCardNumber = purchaseGiftCardResponse.jsonPath().getString("card_number");
					Assert.assertNotNull(giftCardNumber, "Card number should not be null");
					double amount = purchaseGiftCardResponse.jsonPath().getDouble("last_fetched_amount");
					Assert.assertTrue(amount > 0, "Gift card amount should be greater than 0");
					pageObj.utils().logit("pass", "API2 Purchase gift card call is successful. UUID: " + uuid + ", Card Number: " + giftCardNumber);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
			utils.longWaitInSeconds(5);
		}

		// List Challenges
		Response listChallengesResponse = pageObj.endpoints().Api2ListChallenges(dataSet.get("client"),
				dataSet.get("secret"), "es");
		Assert.assertEquals(listChallengesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 List Challenges");
		Assert.assertNotNull(listChallengesResponse.jsonPath().get("challenges"), "Challenges array should not be null");
		pageObj.utils().logit("pass", "Api2 List Challenges successful. Response: " + listChallengesResponse.asString());

		// Create Passcode
		Response createPasscodeResponse = pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"),
				dataSet.get("secret"), token, passcode);
		Assert.assertEquals(createPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 create passcode");
		boolean isCreatePasscodeSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, createPasscodeResponse.asString());
		Assert.assertTrue(isCreatePasscodeSchemaValidated, "API v2 Create Passcode Schema Validation failed");
		String createPasscodeMsg = createPasscodeResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(createPasscodeMsg, "Create passcode message should not be null");
		Assert.assertTrue(createPasscodeMsg.contains("successfully created"), "Message should contain 'successfully created'");
		pageObj.utils().logit("pass", "Api2 create passcode is successful. Message: " + createPasscodeMsg);

		// Forgot Passcode
		Response forgotPasscodeResponse = pageObj.endpoints().Api2ForgotPasscode(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(forgotPasscodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 forgot passcode");
		boolean isForgotPasscodeSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, forgotPasscodeResponse.asString());
		Assert.assertTrue(isForgotPasscodeSchemaValidated, "API v2 Forgot Passcode Schema Validation failed");
		String forgotPasscodeMsg = forgotPasscodeResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(forgotPasscodeMsg, "Forgot passcode message should not be null");
		Assert.assertTrue(forgotPasscodeMsg.contains("email has been sent"), "Message should contain 'email has been sent'");
		pageObj.utils().logit("pass", "Api2 forgot passcode is successful. Message: " + forgotPasscodeMsg);

		// Generate Epin
		Response generateEpinResponse = pageObj.endpoints().Api2GenerateEpin(dataSet.get("client"),
				dataSet.get("secret"), token, passcode, uuid);
		Assert.assertEquals(generateEpinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 generate epin :" + generateEpinResponse.asString());
		boolean isGenerateEpinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GenerateEpinSchema, generateEpinResponse.asString());
		Assert.assertTrue(isGenerateEpinSchemaValidated, "API v2 Generate Epin Schema Validation failed");
		String epin = generateEpinResponse.jsonPath().getString("epin");
		Assert.assertNotNull(epin, "Epin should not be null");
		Assert.assertFalse(epin.isEmpty(), "Epin should not be empty");
		pageObj.utils().logit("pass", "Api2 generate epin is successful. Epin: " + epin);

		// Create Gift Card Claim Token
		Response gcClaimTokenResponse1 = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(gcClaimTokenResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Create Gift Card Claim Token");
		boolean isCreateGiftCardClaimTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CreateGiftCardClaimTokenSchema, gcClaimTokenResponse1.asString());
		Assert.assertTrue(isCreateGiftCardClaimTokenSchemaValidated,
				"API v2 Create Gift Card Claim Token Schema Validation failed");
		String claim_token1 = gcClaimTokenResponse1.jsonPath().getString("claim_token");
		Assert.assertNotNull(claim_token1, "Claim token should not be null");
		Assert.assertFalse(claim_token1.isEmpty(), "Claim token should not be empty");
		String invitation_id1 = gcClaimTokenResponse1.jsonPath().getString("invitation_id");
		Assert.assertNotNull(invitation_id1, "Invitation ID should not be null");
		String invitationType1 = gcClaimTokenResponse1.jsonPath().getString("invitation_type");
		Assert.assertEquals(invitationType1, "claim_token", "Invitation type should be 'claim_token'");
		String invitationStatus1 = gcClaimTokenResponse1.jsonPath().getString("status");
		Assert.assertEquals(invitationStatus1, "pending", "Invitation status should be 'pending'");
		pageObj.utils().logit("pass", "Api2 Create Gift Card Claim Token is successful. Invitation ID: " + invitation_id1);

		// Delete An Invitation Claim Token
		Response deleteClaimTokenResponse = pageObj.endpoints().Api2DeleteClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, invitation_id1);
		Assert.assertEquals(deleteClaimTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Delete An Invitation Claim Token");
		boolean isDeleteClaimTokenSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, deleteClaimTokenResponse.asString());
		Assert.assertTrue(isDeleteClaimTokenSchemaValidated,
				"API v2 Delete An Invitation Claim Token Schema Validation failed");
		String deleteClaimMsg = deleteClaimTokenResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(deleteClaimMsg, "Delete claim message should not be null");
		Assert.assertTrue(deleteClaimMsg.contains("successfully cancelled"), "Message should contain 'successfully cancelled'");
		pageObj.utils().logit("pass", "Api2 Delete An Invitation Claim Token is successful. Message: " + deleteClaimMsg);

		// Create Gift Card Claim Token (second time)
		Response gcClaimTokenResponse = pageObj.endpoints().Api2CreateGiftCardClaimToken(dataSet.get("client"),
				dataSet.get("secret"), token, uuid);
		Assert.assertEquals(gcClaimTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 Create Gift Card Claim Token");
		String claim_token = gcClaimTokenResponse.jsonPath().getString("claim_token");
		Assert.assertNotNull(claim_token, "Claim token should not be null");
		String invitation_id = gcClaimTokenResponse.jsonPath().getString("invitation_id");
		Assert.assertNotNull(invitation_id, "Invitation ID should not be null");
		pageObj.utils().logit("pass", "Api2 Create Gift Card Claim Token is successful. Invitation ID: " + invitation_id);

		// Get Invitations
		Response getInvitationsResponse = pageObj.endpoints().Api2GetInvitations(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(getInvitationsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Get Invitations");
		boolean isGetInvitationsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.api2GetInvitationsSchema, getInvitationsResponse.asString());
		Assert.assertTrue(isGetInvitationsSchemaValidated, "API v2 Get Invitations Schema Validation failed");
		int invitationsCount = getInvitationsResponse.jsonPath().getList("$").size();
		Assert.assertTrue(invitationsCount > 0, "Invitations count should be greater than 0");
		String fetchedInvitationId = getInvitationsResponse.jsonPath().getString("[0].invitation_id");
		Assert.assertEquals(fetchedInvitationId, invitation_id, "Invitation ID should match");
		pageObj.utils().logit("pass", "Api2 Get Invitations is successful. Count: " + invitationsCount);

		// Check Status of the claim token
		Response claimTokenResponse = pageObj.endpoints().Api2CheckStatusofclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), token, claim_token);
		Assert.assertEquals(claimTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Check Status of the claim token");
		boolean isCheckClaimTokenStatusSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.apiStringArraySchema, claimTokenResponse.asString());
		Assert.assertTrue(isCheckClaimTokenStatusSchemaValidated,
				"API v2 Check Status of the claim token Schema Validation failed");
		String claimTokenStatusMsg = claimTokenResponse.jsonPath().getString("[0]");
		Assert.assertNotNull(claimTokenStatusMsg, "Claim token status message should not be null");
		Assert.assertTrue(claimTokenStatusMsg.contains("valid claim token"), "Message should indicate valid claim token");
		pageObj.utils().logit("pass", "Api2 Check Status of the claim token is successful. Message: " + claimTokenStatusMsg);

		// Signup new user for transfer
		String userEmailNew = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseNew = pageObj.endpoints().Api2SignUp(userEmailNew, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponseNew.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String tokenNew = signUpResponseNew.jsonPath().getString("access_token.token");
		Assert.assertNotNull(tokenNew, "New user token should not be null");
		String newUserId = signUpResponseNew.jsonPath().getString("user.user_id");
		Assert.assertNotNull(newUserId, "New User ID should not be null");
		pageObj.utils().logit("pass", "Api2 new user signup is successful. User ID: " + newUserId);

		// Transfer a Gift Card Using Invitation Claim Token
		Response claimTokenTransferResponse = pageObj.endpoints().Api2Transferclaimtoken(dataSet.get("client"),
				dataSet.get("secret"), tokenNew, claim_token);
		Assert.assertEquals(claimTokenTransferResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Transfer a Gift Card Using Invitation Claim Token");
		boolean isClaimTokenTransferSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GiftCardSchema, claimTokenTransferResponse.asString());
		Assert.assertTrue(isClaimTokenTransferSchemaValidated,
				"API v2 Transfer a Gift Card Using Invitation Claim Token Schema Validation failed");
		String transferredUuid = claimTokenTransferResponse.jsonPath().getString("uuid");
		Assert.assertEquals(transferredUuid, uuid, "Transferred gift card UUID should match original UUID");
		String transferredCardNumber = claimTokenTransferResponse.jsonPath().getString("card_number");
		Assert.assertNotNull(transferredCardNumber, "Transferred card number should not be null");
		pageObj.utils().logit("pass", "Api2 Transfer a Gift Card Using Invitation Claim Token is successful. UUID: " + transferredUuid);

	}

	@Test(description = "Verify Mobile API v2:- SQ-T5137: Version Notes; SQ-T5141: Request for User Account Deletion; "
			+ "SQ-T5225: Beacon Entry; SQ-T5238: Beacon Exit", groups = "api", priority = 4)
	public void verifyAPIv2VersionNotes() {

		// Version Notes
		pageObj.utils().logit("info", "== Mobile API v2: Version Notes ==");
		Response versionNotesResponse = pageObj.endpoints().api2VersionNotes(dataSet.get("version"), dataSet.get("os"),
				dataSet.get("model"), dataSet.get("client"));
		Assert.assertEquals(versionNotesResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Version Notes");
		boolean isVersionNotesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api1VersionNotesSchema, versionNotesResponse.asString());
		Assert.assertTrue(isVersionNotesSchemaValidated, "API v2 Version Notes Schema Validation failed");
		String versionNote = versionNotesResponse.jsonPath().getString("note");
		Assert.assertEquals(versionNote, "test", "Note did not match");
		boolean forceUpgrade = versionNotesResponse.jsonPath().getBoolean("force_upgrade");
		Assert.assertFalse(forceUpgrade, "Force upgrade should be false");
		String notificationStyle = versionNotesResponse.jsonPath().getString("notification_style");
		Assert.assertNotNull(notificationStyle, "Notification style should not be null");
		pageObj.utils().logit("pass", "API v2 Version Notes call is successful. Note: " + versionNote + ", Notification Style: " + notificationStyle);

		// User Sign-up
		pageObj.utils().logit("info", "== Mobile API v2: User Sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 User Signup");
		String authToken = signUpResponse.jsonPath().getString("access_token.token");
		Assert.assertNotNull(authToken, "Auth token should not be null");
		Assert.assertFalse(authToken.isEmpty(), "Auth token should not be empty");
		String signupUserId = signUpResponse.jsonPath().getString("user.user_id");
		Assert.assertNotNull(signupUserId, "User ID should not be null");
		String signupEmail = signUpResponse.jsonPath().getString("user.email");
		Assert.assertEquals(signupEmail.toLowerCase(), userEmail.toLowerCase(), "Email should match");
		pageObj.utils().logit("pass", "API v2 User Signup call is successful. User ID: " + signupUserId);

		// Beacon Entry
		pageObj.utils().logit("info", "== Mobile API v2: Beacon Entry ==");
		Response beaconEntryResponse = pageObj.endpoints().api2BeaconEntry(dataSet.get("client"), dataSet.get("secret"),
				authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconEntryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Beacon Entry call due to " + beaconEntryResponse.asString());
		Assert.assertEquals(beaconEntryResponse.asString(), "", "Response body is not empty string.");
		pageObj.utils().logit("pass", "API v2 Beacon Entry call is successful");

		// Beacon Exit
		pageObj.utils().logit("info", "== Mobile API v2: Beacon Exit ==");
		Response beaconExitResponse = pageObj.endpoints().api2BeaconExit(dataSet.get("client"), dataSet.get("secret"),
				authToken, dataSet.get("beaconMajor"), dataSet.get("beaconMinor"));
		Assert.assertEquals(beaconExitResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Beacon Exit call due to " + beaconExitResponse.asString());
		Assert.assertEquals(beaconExitResponse.asString(), "", "Response body is not empty string.");
		pageObj.utils().logit("pass", "API v2 Beacon Exit call is successful");

		// Request for User Account Deletion
		pageObj.utils().logit("info", "== Mobile API v2: Request for User Account Deletion ==");
		Response accountDeletionResponse = pageObj.endpoints().api2UserAccountDeletion(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountDeletionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v2 Request for User Account Deletion");
		boolean isUserAccountDeletionSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiMessageObjectSchema, accountDeletionResponse.asString());
		Assert.assertTrue(isUserAccountDeletionSchemaValidated,
				"API v2 User Account Deletion Schema Validation failed");
		String message = accountDeletionResponse.jsonPath().getString("message");
		Assert.assertNotNull(message, "Deletion message should not be null");
		String expectedSubString = "Guest marked for deletion. Data will be automatically deleted";
		Assert.assertTrue(message.contains(expectedSubString), "Message did not match");
		pageObj.utils().logit("pass", "API v2 Request for User Account Deletion call is successful. Message: " + message);

	}

	// Will run these cases once PR merged on pre-prod
	// @Test(description = "SQ-T7102 Verify that the API returns all attributes when
	// parameterization is enabled", groups = "api", priority = 2)
	@Owner(name = "Akansha Jain")
	public void SQ_T7102_FetchSocialCauseCampaignDetailsWithAllParametrization() throws Exception {

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

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// DB- update api_configurations table for business with all keys
		String query1 = "UPDATE api_configurations\n" + "SET response_keys = CAST(\n" + "  '{\n"
				+ "	\"zip\": true,\n" + "	\"city\": true,\n" + "	\"name\": true,\n" + "	\"email\": true,\n"
				+ "	\"phone_number\": true,\n" + "	\"state\": true,\n" + "	\"street\": true,\n"
				+ "	\"address\": true,\n" + "	\"image_url\": true,\n" + "	 \"donations\": {\n"
				+ "		\"currency\": true\n" + "	},\n" + "	\"social_cause_id\": true,\n"
				+ "	\"disclaimer\": true,\n" + "	\"description\": true,\n" + "	\"miscellaneous\": true\n"
				+ "}' AS JSON) where business_id = '" + b_id + "'\n" + "";
		DBUtils.executeUpdateQuery(env, query1);

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// User register or sign up using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", dataSet.get("amount"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward amount to user is successful");

		// Create Donation
		Response createDonationResponse = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Create Donation");
		TestListeners.extentTest.get().pass("Api2 Create Donation is successful");

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		Assert.assertNotNull(getsocialCauseCampaignResponse.jsonPath().get("social_cause_id"),
				"Social cause id not found");
		TestListeners.extentTest.get().pass("Fetch api2 Social Cause Campaign Details is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");
	}

	// Will run these cases once PR merged on pre-prod
	// @Test(description = "SQ-T7103 Verify that the API returns only selected
	// attributes when parameterization is enabled", groups = "api", priority = 2)
	@Owner(name = "Akansha Jain")
	public void SQ_T7103_FetchSocialCauseCampaignDetailsWithSelectedParametrization() throws Exception {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// DB- update api_configurations table for business with all keys
		String query1 = "UPDATE api_configurations\n" + "SET response_keys = CAST(\n" + "  '{\n"
				+ "	\"zip\": true,\n" + "	\"city\": true,\n" + "	\"name\": true,\n" + "	\"email\": true,\n"
				+ "	\"phone_number\": true,\n" + "	\"state\": true,\n" + "	\"street\": true,\n"
				+ "	\"address\": true,\n" + "	\"image_url\": true,\n" + "	\"social_cause_id\": true,\n"
				+ "	\"disclaimer\": true,\n" + "	\"description\": true,\n" + "	\"miscellaneous\": true\n"
				+ "}' AS JSON) where business_id = '" + b_id + "'\n" + "";
		DBUtils.executeUpdateQuery(env, query1);

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", dataSet.get("amount"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward amount to user is successful");

		// Create Donation
		Response createDonationResponse = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Create Donation");
		TestListeners.extentTest.get().pass("Api2 Create Donation is successful");

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		Assert.assertNull(getsocialCauseCampaignResponse.jsonPath().get("donations"), "donations key value found");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");
	}

	// Will run these cases once PR merged on pre-prod
	// @Test(description = "SQ-T7104 Verify the API's behavior when invalid keys are
	// specified in the configuration", groups = "api", priority = 2) @Owner(name =
	// "Akansha Jain")
	@Owner(name = "Akansha Jain")
	public void SQ_T7104_FetchSocialCauseCampaignDetailsWithInvalidParametrization() throws Exception {

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		// DB- update api_configurations table for business with all keys
		String query1 = "UPDATE api_configurations\n" + "SET response_keys = CAST(\n" + "  '{\n"
				+ "	\"zip\": true,\n" + "	\"city\": true,\n" + "	\"name\": true,\n" + "	\"email\": true,\n"
				+ "	\"phone\": true,\n" + "	\"state\": true,\n" + "	\"street\": true,\n" + "	\"address\": true,\n"
				+ "	\"image_url\": true,\n" + "	\"social_cause_id\": true,\n" + "	\"disclaimer\": true,\n"
				+ "	\"description\": true,\n" + "	\"miscellaneous\": true\n" + "}' AS JSON) where business_id = '"
				+ b_id + "'\n" + "";
		DBUtils.executeUpdateQuery(env, query1);

		// Create Social Cause Campaigns
		String campaignName = "SocialCauseCampaign" + CreateDateTime.getTimeDateString();
		Response socialCauseCampaignResponse = pageObj.endpoints().cReateSocialcauseCampaign(campaignName,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(socialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Social Cause Campaigns is successful");
		String social_cause_id = socialCauseCampaignResponse.jsonPath().get("social_cause_id").toString();

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// send reward amount to user Amount
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("adminAuthorization"),
				dataSet.get("amount"), "", "", dataSet.get("amount"));
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2 send reward amount to user is successful");

		// Create Donation
		Response createDonationResponse = pageObj.endpoints().Api2CreateDonation(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(createDonationResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Create Donation");
		TestListeners.extentTest.get().pass("Api2 Create Donation is successful");

		// Get Social Cause Campaigns
		Response getsocialCauseCampaignResponse = pageObj.endpoints().Api2SocialCauseCampaign(dataSet.get("client"),
				dataSet.get("secret"), token, social_cause_id);
		Assert.assertEquals(getsocialCauseCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 Social Cause Campaign Details");
		Assert.assertNull(getsocialCauseCampaignResponse.jsonPath().get("phone_number"),
				"phone_number key value found");
		TestListeners.extentTest.get().pass("Api2 Social Cause Campaign Details is successful");

		// Deactivate Social Cause Campaign
		Response deactivateSocialCampaignResponse = pageObj.endpoints().deactivateSocialCampaign(social_cause_id,
				dataSet.get("adminAuthorization"));
		Assert.assertEquals(deactivateSocialCampaignResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Deactivate Social Cause Campaign");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Deactivate Social Cause Campaign is successful");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}
