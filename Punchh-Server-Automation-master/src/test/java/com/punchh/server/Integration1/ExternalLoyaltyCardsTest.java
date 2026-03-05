package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
//import com.punchh.server.utilities.TestData;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ExternalLoyaltyCardsTest {
	static Logger logger = LogManager.getLogger(ExternalLoyaltyCardsTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static final String[] ZIPLINE_CARDS = { "639471358071818402", "639471358071818410", "639471358071818428" };
	private static final String[] EXTERNAL_CARDS = { "639471358071818404", "639471358071818405" };
	private Properties prop;

	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5702 Zipline card ingestion by webhook and perform POS lookup/checkin with ingested card_number", priority = 0)

	public void T5702_verifyZiplineLoyaltyCardWebhookIngestion() throws Exception {

		// Delete existing vendor location data
		String deleteLoyaltyCardQuery = "DELETE FROM loyalty_cards WHERE business_id=" + dataSet.get("business_id")
				+ " AND card_number IN ('639471358071818402','639471358071818410','639471358071818428','639471358071818404','639471358071818405');";

		try {
			DBUtils.executeUpdateQuery(env, deleteLoyaltyCardQuery);
			logger.info("All vendor location data has been deleted for business_id: " + dataSet.get("business_id"));
			utils.logPass("Successfully deleted Vendor location Data from DB");
		} catch (Exception e) {
			logger.error("Error during deletion of loyalty cards", e);
			Assert.fail("Failed to delete loyalty cards from DB");
			utils.logPass("Unable to Delete loyalty cards from DB");
		}

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Physical Cards");
		pageObj.cockpitPhysicalCardPage().selectLoyaltyCardAdapter("Zipline Loyalty Cards", "18");

		// Generate user email and perform API1 signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API1 signup");
		String authToken = signUpResponse.jsonPath().get("auth_token.token").toString();
		utils.logPass(userEmail + " API1 user signup is successful");

		// Loyalty Card Ingestion through Zipline Webhook
		Response ziplineWebhookResp = pageObj.endpoints().webhookZiplineAPI(dataSet.get("client"),
				dataSet.get("secret"), userEmail, ZIPLINE_CARDS[0], "ACTIVE", "create", dataSet.get("business_uuid"));
		Assert.assertEquals(ziplineWebhookResp.getStatusCode(), 202,
				"Status code 202 did not match for Zipline webhook");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickLoyaltyCards();

		// Card Number and status verification on Loyalty Card tab available on user
		// timeline
		pageObj.guestTimelinePage().scrollToLoyaltyCard();
		boolean matchedCardNumber = pageObj.guestTimelinePage().matchLoyaltyCardNumber(ZIPLINE_CARDS[0]);
		Assert.assertTrue(matchedCardNumber, "Loyalty card numberdid not match with card number: " + ZIPLINE_CARDS[0]);
		boolean matchedCardStatus = pageObj.guestTimelinePage().matchLoyaltyCardStatus("ACTIVE");
		Assert.assertTrue(matchedCardNumber, "Loyalty card status did not match with status: ACTIVE");
		logger.info("Loyalty card created successfully with card_number: " + ZIPLINE_CARDS[0] + " and status: ACTIVE");
		utils.logPass(
				"Loyalty card created successfully with card_number: " + ZIPLINE_CARDS[0] + " and status: ACTIVE");

		// Update status of the loyalty card
		Response ziplineWebhookResp1 = pageObj.endpoints().webhookZiplineAPI(dataSet.get("client"),
				dataSet.get("secret"), userEmail, ZIPLINE_CARDS[0], "PENDING_ENROLLMENT", "update",
				dataSet.get("business_uuid"));
		Assert.assertEquals(ziplineWebhookResp1.getStatusCode(), 202,
				"Status code 202 did not match for Zipline webhook");
		utils.longWaitInSeconds(4);
		pageObj.guestTimelinePage().refreshTimeline();

		// Card Number and status verification on Loyalty Card tab available on user
		// timeline after webhook update
		pageObj.guestTimelinePage().scrollToLoyaltyCard();
		boolean matchedCardNumber1 = pageObj.guestTimelinePage().matchLoyaltyCardNumber(ZIPLINE_CARDS[0]);
		Assert.assertTrue(matchedCardNumber1, "Loyalty card numberdid not match with card number: " + ZIPLINE_CARDS[0]);
		boolean matchedCardStatus1 = pageObj.guestTimelinePage().matchLoyaltyCardStatus("PENDING_ENROLLMENT");
		Assert.assertTrue(matchedCardStatus1, "Loyalty card status did not match with status: PENDING_ENROLLMENT");
		logger.info(
				"Loyalty card status updated successfully to PENDING_ENROLLMENT for card_number: " + ZIPLINE_CARDS[0]);
		utils.logPass(
				"Loyalty card status updated successfully to PENDING_ENROLLMENT for card_number: " + ZIPLINE_CARDS[0]);

		// Perform Lookup API to get the user details
		Response posLookupResponse = pageObj.endpoints().posUserLookupWithLoyaltyCard(ZIPLINE_CARDS[0],
				dataSet.get("burgerMongerLocationKey"));
		Assert.assertEquals(posLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS lookup");
		String cardUUID = posLookupResponse.jsonPath().getString("loyalty_cards[0].card_uuid");
		Assert.assertEquals(posLookupResponse.jsonPath().getString("email"), userEmail,
				"Email did not match in POS lookup response");
		Assert.assertEquals(posLookupResponse.jsonPath().getString("loyalty_cards[0].card_number"), ZIPLINE_CARDS[0],
				"Loyalty card number did not match in POS lookup response");
		logger.info("User email and loyalty card number matched in POS lookup response");
		utils.logPass("User email and loyalty card number matched in POS lookup response");

		// Perform checkin with loyalty card number
		Response posCheckinResp = pageObj.endpoints().posCheckinWithLoyaltyCard(ZIPLINE_CARDS[0], "10",
				dataSet.get("burgerMongerLocationKey"));
		Assert.assertEquals(posCheckinResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS checkin");
		logger.info("POS checkin with loyalty card number is successful");
		utils.logPass("POS checkin with loyalty card number is successful");

		// Delete Loyalty card
		Response loyaltyCardDeleteResp = pageObj.endpoints().api1LoyaltyCardDelete(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardDeleteResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Cards Delete API");
		logger.info("Loyalty card deleted successfully with card_number: " + ZIPLINE_CARDS[0]);
		utils.logPass("Loyalty card deleted successfully with card_number: " + ZIPLINE_CARDS[0]);
	}

	@Test(description = "SQ-T2948 Zipline Loyalty Card Ingestion through Loyalty Cards API", priority = 1)

	public void T2948_verifyZiplineCardIngestionByLoyaltyCardsAPI() throws Exception {
		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Physical Cards");
		pageObj.cockpitPhysicalCardPage().selectLoyaltyCardAdapter("Zipline Loyalty Cards", "18");

		// Generate user email and perform API1 signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API1 signup");
		String authToken = signUpResponse.jsonPath().get("auth_token.token").toString();
		logger.info("User signed up successfully with email: " + userEmail);
		utils.logPass(userEmail + " API1 user signup is successful");

		// Zipline Card create through Loyalty Cards API
		Response loyaltyCardResp = pageObj.endpoints().api1LoyaltyCardCreateAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, ZIPLINE_CARDS[1], true);
		Assert.assertEquals(loyaltyCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Create API");
		String cardUUID = loyaltyCardResp.jsonPath().getString("card_uuid");
		Assert.assertEquals(loyaltyCardResp.jsonPath().getString("card_number"), ZIPLINE_CARDS[1],
				"Loyalty card number did not match in Loyalty Card Create response");
		Assert.assertEquals(loyaltyCardResp.jsonPath().getString("status"), "UNENROLLED",
				"Loyalty card status did not match in Loyalty Card Create response");
		logger.info("Loyalty card created successfully with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalty card created successfully with card_number: " + ZIPLINE_CARDS[1]);

		// Fetch loyalty card info
		Response loyaltyCardInfoResp = pageObj.endpoints().api1LoyaltyCardInfoAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardInfoResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Info API");
		Assert.assertEquals(loyaltyCardInfoResp.jsonPath().getString("card_number"), ZIPLINE_CARDS[1],
				"Loyalty card number did not match in Loyalty Card Info response");
		logger.info("Loyalty card info fetched successfully with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalty card info fetched successfully with card_number: " + ZIPLINE_CARDS[1]);

		// Update Loyalty card number with new card number (Due to unavailability of
		// real enrolled card, we are just checking error response for update)
		Response loyaltyCardUpdateResp = pageObj.endpoints().api1LoyaltyCardUpdateAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID, ZIPLINE_CARDS[2], false);
		Assert.assertEquals(loyaltyCardUpdateResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Loyalty Card Update API");
		Assert.assertEquals(loyaltyCardUpdateResp.jsonPath().get("[0]"),
				"The card to be replaced or user account was not found.",
				"Error message did not match in Loyalty Card Update response");
		logger.info("Loyalty card update failed with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalty card update failed with card_number: " + ZIPLINE_CARDS[1]);

		// Delete Loyalty card
		Response loyaltyCardDeleteResp = pageObj.endpoints().api1LoyaltyCardDelete(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardDeleteResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Cards Delete API");
		logger.info("Loyalty card deleted successfully with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalty card deleted successfully with card_number: " + ZIPLINE_CARDS[1]);

		// Delete the Loyalty card again
		Response loyaltyCardDeleteResp1 = pageObj.endpoints().api1LoyaltyCardDelete(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardDeleteResp1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Loyalty Cards Delete API");
		Assert.assertEquals(loyaltyCardDeleteResp1.jsonPath().getString("error"), "Card Number not found",
				"Error message did not match in Loyalty Card Delete response");
		logger.info("Loyalty card not found with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalty card not found with card_number: " + ZIPLINE_CARDS[1]);
	}

	@Test(description = "SQ-T2949 Zipline Card Ingestion through Signup and Update user API", priority = 2)

	public void T2949_verifyZiplineCardIngestionWithSignupAndUserUpdate() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Physical Cards");
		pageObj.cockpitPhysicalCardPage().selectLoyaltyCardAdapter("Zipline Loyalty Cards", "18");

		// Create new user with new Zipline loyalty card number
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUpWithLoyaltyCard(dataSet.get("client"),
				dataSet.get("secret"), userEmail, ZIPLINE_CARDS[0]);
		String userId = signUpResponse.jsonPath().getString("id");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API1 signup");
		String authToken = signUpResponse.jsonPath().get("auth_token.token").toString();
		Assert.assertEquals(signUpResponse.jsonPath().getString("loyalty_cards[0].card_number"), ZIPLINE_CARDS[0],
				"Loyalty card number did not match in Signup response");
		logger.info("User signed up successfully with email: " + userEmail + " and loyalty card number: "
				+ ZIPLINE_CARDS[0]);
		utils.logPass(userEmail + " API1 user signup is successful with loyalty card number: " + ZIPLINE_CARDS[0]);

		// Update user with new Zipline loyalty card number
		Response userUpdateResp = pageObj.endpoints().api1UpdateUserWithLoyaltyCard(dataSet.get("client"),
				dataSet.get("secret"), authToken, ZIPLINE_CARDS[1]);
		Assert.assertEquals(userUpdateResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Create API");
		Assert.assertEquals(userUpdateResp.jsonPath().getString("loyalty_cards[0].card_number"), ZIPLINE_CARDS[0],
				"Loyalty card 0 did not match in user update response");
		Assert.assertEquals(userUpdateResp.jsonPath().getString("loyalty_cards[1].card_number"), ZIPLINE_CARDS[1],
				"Loyalty card 1 did not match in user update response");
		logger.info("Loaylty card updated successfully with card_number: " + ZIPLINE_CARDS[1]);
		utils.logPass("Loyalrty card updated successfully with card_number: " + ZIPLINE_CARDS[1]);

		String deleteQuery = "DELETE FROM loyalty_cards WHERE user_id=\"" + userId + "\";";
		DBUtils.executeUpdateQuery(env, deleteQuery);
		utils.logPass("Loyalty card deleted successfully with card_number");
	}

	@Test(description = "SQ-T5701 External Vendor Loyalty Card Ingestion through Loyalty Cards API", priority = 3)

	public void T5701_verifyExternalVendorCardIngestionByLoyaltyCardsAPI() throws Exception {
		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Physical Cards");
		pageObj.cockpitPhysicalCardPage().selectLoyaltyCardAdapter("External Vendor Loyalty Cards", "18");

		// Generate user email and perform API1 signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for API1 signup");
		String authToken = signUpResponse.jsonPath().get("auth_token.token").toString();
		logger.info("User signed up successfully with email: " + userEmail);
		utils.logPass(userEmail + " API1 user signup is successful");

		// External Vendor Card create through Loyalty Cards API
		Response loyaltyCardResp = pageObj.endpoints().api1LoyaltyCardCreateAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, EXTERNAL_CARDS[0], true);
		Assert.assertEquals(loyaltyCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Create API");
		String cardUUID = loyaltyCardResp.jsonPath().getString("card_uuid");
		Assert.assertEquals(loyaltyCardResp.jsonPath().getString("card_number"), EXTERNAL_CARDS[0],
				"Loyalty card number did not match in Loyalty Card Create response");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickLoyaltyCards();
		pageObj.guestTimelinePage().scrollToLoyaltyCard();
		boolean matchedCardNumber = pageObj.guestTimelinePage().matchLoyaltyCardNumber(EXTERNAL_CARDS[0]);
		Assert.assertTrue(matchedCardNumber, "Loyalty card did not match with card number: " + EXTERNAL_CARDS[0]);
		logger.info("Loyalty card created successfully with card_number: " + EXTERNAL_CARDS[0]);
		utils.logPass("Loyalty card created successfully with card_number: " + EXTERNAL_CARDS[0]);

		// Fetch External Vendor Card Info
		Response loyaltyCardInfoResp = pageObj.endpoints().api1LoyaltyCardInfoAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardInfoResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Info API");
		Assert.assertEquals(loyaltyCardInfoResp.jsonPath().getString("card_number"), EXTERNAL_CARDS[0],
				"Loyalty card number did not match in Loyalty Card Info response");
		logger.info("Loyalty card info fetched successfully with card_number: " + EXTERNAL_CARDS[0]);
		utils.logPass("Loyalty card info fetched successfully with card_number: " + EXTERNAL_CARDS[0]);

		// Update External Vendor Loyalty card number to new card number
		Response loyaltyCardUpdateResp = pageObj.endpoints().api1LoyaltyCardUpdateAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID, EXTERNAL_CARDS[1], false);
		Assert.assertEquals(loyaltyCardUpdateResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Card Update API");
		Assert.assertEquals(loyaltyCardUpdateResp.jsonPath().getString("card_number"), EXTERNAL_CARDS[1],
				"Loyalty card number did not match in Loyalty Card Update response");
		logger.info("Loyalty card updated successfully with card_number: " + EXTERNAL_CARDS[1]);
		utils.logPass("Loyalty card updated successfully with card_number: " + EXTERNAL_CARDS[1]);

		// Delete External Vendor Loyalty card
		Response loyaltyCardDeleteResp = pageObj.endpoints().api1LoyaltyCardDelete(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardDeleteResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for Loyalty Cards Delete API");
		logger.info("Loyalty card deleted successfully with card_number: " + EXTERNAL_CARDS[1]);
		utils.logPass("Loyalty card deleted successfully with card_number: " + EXTERNAL_CARDS[1]);

		// Delete the External Vendor Loyalty card again
		Response loyaltyCardDeleteResp1 = pageObj.endpoints().api1LoyaltyCardDelete(dataSet.get("client"),
				dataSet.get("secret"), authToken, cardUUID);
		Assert.assertEquals(loyaltyCardDeleteResp1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for Loyalty Cards Delete API");
		Assert.assertEquals(loyaltyCardDeleteResp1.jsonPath().getString("error"), "Card Number not found",
				"Error message did not match in Loyalty Card Delete response");
		logger.info("Loyalty card not found with card_number: " + EXTERNAL_CARDS[1]);
		utils.logPass("Loyalty card not found with card_number: " + EXTERNAL_CARDS[1]);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}