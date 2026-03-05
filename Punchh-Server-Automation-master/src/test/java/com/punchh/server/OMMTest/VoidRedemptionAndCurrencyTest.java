package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class VoidRedemptionAndCurrencyTest {
	private static Logger logger = LogManager.getLogger(VoidRedemptionAndCurrencyTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5411 [Point to Manual]Verify void currency redemption either associated checkin is expired or not", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T5411_PTMVerifyVoidCurrencyRedemptionEitherAssociatedCheckinExpiredOrNot() throws InterruptedException {
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();

		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		String token = signUpResponse1.jsonPath().get("access_token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "", dataSet.get("fuelPoints"), dataSet.get("amount"));

		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().info("Api2  send reward amount to user is successful");

		// send reward amount to user Amount
		Response sendRewardResponse1 = pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// Pos redemption of amount
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redemptionResponse.getStatusCode(),
				"Status code 200 did not matched for pos redemption api");
		String redemption_id1 = redemptionResponse.jsonPath().get("redemption_id").toString();
		utils.logit("Currency Redeemed successfully");

		// currency reward redemption displayed on the user timeline or not
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String redemptionAmountDisplayed3 = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg"));
		Assert.assertEquals(redemptionAmountDisplayed3, dataSet.get("expectedMsg"),
				"Currency redemption message did not matched");
		utils.logit("Reward redemption displayed on the user timeline");

		// Pos redemption API for fuel
		String txn5 = "123456" + CreateDateTime.getTimeDateString();
		String date5 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key5 = CreateDateTime.getTimeDateString();
		int posRedemptionOfFuelRespo_attempts = 0;
		Response posRedemptionOfFuelRespo = null;
		while (posRedemptionOfFuelRespo_attempts < 10) {
			utils.longWait(1500);
			posRedemptionOfFuelRespo = pageObj.endpoints().posRedemptionOfFuel(iFrameEmail, date5, key5, txn5,
					dataSet.get("locationKey"));
			if (posRedemptionOfFuelRespo.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption API for fuel is " + posRedemptionOfFuelRespo_attempts);
			posRedemptionOfFuelRespo_attempts++;
		}
		Assert.assertEquals(posRedemptionOfFuelRespo.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemption_id2 = posRedemptionOfFuelRespo.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for fuel is successful with redemption code " + redemption_id2);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for fuel is successful with redemption code " + redemption_id2);

		// fuel redemption displayed on the user timeline or not
		String redemptionAmountDisplayed6 = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg2"));
		Assert.assertEquals(redemptionAmountDisplayed6, dataSet.get("expectedMsg2"),
				"Fuel redemption message did not matched");
		utils.logit("Fuel redemption displayed on the user timeline");

		// Void currency redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		utils.logit("Currency Redemption void successfully");

		// validate from the guesttimelime that the currency redemption has been voided
		// or not
		boolean redemptionAmountDisplayed4 = pageObj.guestTimelinePage().verifyVoidRedemption();
		Assert.assertTrue(redemptionAmountDisplayed4, "Redemption of amount is not successful");
		logger.info("Currency void redemption successful, Redeemable is not displayed on the user timeline");
		TestListeners.extentTest.get()
				.info("Currency void redemption successful, Redeemable is not displayed on the user timeline.");

		// Void fuel redemption using API
		Response voidResponse2 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id2,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		utils.logit("Fuel Redemption void successfully");

		// validate from the guesttimelime that the fuel redemption has been voided or
		// not
		boolean redemptionAmountDisplayed7 = pageObj.guestTimelinePage().verifyVoidRedemption();
		Assert.assertTrue(redemptionAmountDisplayed7, "Redepmtion of amount is not successful");
		logger.info("Fuel void redemption successful, Redeemable is not displayed on the user timeline");
		TestListeners.extentTest.get()
				.info("Fuel void redemption successful, Redeemable is not displayed on the user timeline.");
		// running the rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Rolling Expiry Schedule");
		pageObj.schedulespage().runSchedule();
		utils.logit("rolling expiry schedule ran successfully");

		// Pos redemption of amount
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		Response redemptionResponse2 = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date2, key2, txn2,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemption_id3 = redemptionResponse2.jsonPath().get("redemption_id").toString();
		utils.logit("Currency Redeemed successfully");

		// currency reward redemption displayed on the user timeline or not
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String redemptionAmountDisplayed5 = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg"));
		Assert.assertEquals(redemptionAmountDisplayed5, dataSet.get("expectedMsg"),
				"Currency redemption message did not matched");
		utils.logit("Currency Redeemption displayed on the user timeline");

		// Pos redemption API for fuel
		String txn4 = "123456" + CreateDateTime.getTimeDateString();
		String date4 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key4 = CreateDateTime.getTimeDateString();
		int posRedemptionOfFuelRespo_attempts2 = 0;
		Response posRedemptionOfFuelRespo2 = null;
		while (posRedemptionOfFuelRespo_attempts2 < 10) {
			utils.longWait(1500);
			posRedemptionOfFuelRespo2 = pageObj.endpoints().posRedemptionOfFuel(iFrameEmail, date4, key4, txn4,
					dataSet.get("locationKey"));
			if (posRedemptionOfFuelRespo2.getStatusCode() == ApiConstants.HTTP_STATUS_OK) {
				break;
			}
			logger.info("Attempt no for Pos redemption API for fuel is " + posRedemptionOfFuelRespo_attempts2);
			posRedemptionOfFuelRespo_attempts2++;
		}
		Assert.assertEquals(posRedemptionOfFuelRespo2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemption_id4 = posRedemptionOfFuelRespo2.jsonPath().get("redemption_id").toString();
		logger.info("Pos redemption api for fuel is successful with redemption code " + redemption_id4);
		TestListeners.extentTest.get()
				.pass("Pos redemption api for fuel is successful with redemption code " + redemption_id4);

		// fuel redemption displayed on the user timeline or not
		utils.refreshPage();
		String redemptionAmountDisplayed8 = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg2"));
		Assert.assertEquals(redemptionAmountDisplayed8, dataSet.get("expectedMsg2"),
				"Fuel redemption message did not matched");
		utils.logit("Fuel redemption displayed on the user timeline");

		// Auth void redemption of currency
		Response voidRedemptionResponse1 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id3);
		Assert.assertEquals(voidRedemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED);
		logger.info("Currency auth void redemption is successful");
		TestListeners.extentTest.get().info("Currency auth void redemption is successful");

		// entry of currency redemption should be deleted from the timeline
		Boolean isDeleted = pageObj.guestTimelinePage().redeemedRewardDeleted(dataSet.get("expectedMsg"));
		Assert.assertFalse(isDeleted, "Redemtion Entry is deleted from the timeline");
		logger.info("Redemption Entry is deleted from the timeline");

		// Auth void redemption of fuel
		Response voidRedemptionResponse2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id4);
		Assert.assertEquals(voidRedemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED);
		logger.info("Fuel auth void redemption is successful");
		TestListeners.extentTest.get().info("Fuel auth void redemption is successful");

		// entry of fuel redemption should be deleted from the timeline
		Boolean isDeleted2 = pageObj.guestTimelinePage().redeemedRewardDeleted(dataSet.get("expectedMsg2"));
		Assert.assertFalse(isDeleted2, "Redemption Entry is deleted from the timeline");
		logger.info("Redemption Entry is deleted from the timeline");
		TestListeners.extentTest.get().info("Redemption Entry is deleted from the timeline");

	}

	@Test(description = "SQ-T5410 [Point to Currency]Verify void currency redemption either associated checkin is expired or not", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Vansham Mishra")
	public void T5410_PTCVerifyVoidCurrencyRedemptionEitherAssociatedCheckinExpiredOrNot() throws InterruptedException {
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// pos checkin of 110 points
		Response resp1 = pageObj.endpoints().posCheckin(iFrameEmail, dataSet.get("locationKey"), dataSet.get("amount"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post chekin api");
		logger.info("Checkin of 110 points is successful");
		TestListeners.extentTest.get().info("Checkin of 110 points is successful");

		// Pos redemption of amount
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response redemptionResponse = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Currency redemption is successful");
		TestListeners.extentTest.get().info("Currency redemption is successful");
		String redemption_id1 = redemptionResponse.jsonPath().get("redemption_id").toString();

		// reward redemption displayed on the user timeline or not
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String redemptionAmountDisplayed = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg"));
		Assert.assertEquals(redemptionAmountDisplayed, dataSet.get("expectedMsg"),
				"Redemption message did not matched");
		logger.info("Redeemable displayed on the user timeline");
		TestListeners.extentTest.get().info("Redeemable displayed on the user timeline");

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 200 did not matched for pos redemption api");
		logger.info("Void redemption via pos is successful");
		TestListeners.extentTest.get().info("Void redemption via pos is successful");

		// validate from the guesttimelime that the redemption has been voided or not
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		boolean redemptionAmountDisplayed2 = pageObj.guestTimelinePage().verifyVoidRedemption();
		Assert.assertTrue(redemptionAmountDisplayed2, "Redemption of amount is not successful");
		logger.info("Void redemption successful, Redeemable is not displayed on the user timeline");
		TestListeners.extentTest.get()
				.info("Void redemption successful, Redeemable is not displayed on the user timeline.");

		// running the rolling expiry schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Rolling Expiry Schedule");
		pageObj.schedulespage().runSchedule();
		logger.info("Rolling expiry schedule ran successfully");
		TestListeners.extentTest.get().info("Rolling expiry schedule ran successfully");

		// pos checkin of 110 points
		Response resp2 = pageObj.endpoints().posCheckin(iFrameEmail, dataSet.get("locationKey"), dataSet.get("amount"));
		Assert.assertEquals(resp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for post checkin api");
		logger.info("Checkin of 110 points is successful");
		TestListeners.extentTest.get().info("Checkin of 110 points is successful");
		// Pos redemption of amount
		String txn2 = "123456" + CreateDateTime.getTimeDateString();
		String date2 = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key2 = CreateDateTime.getTimeDateString();
		Response redemptionResponse2 = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date2, key2, txn2,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(redemptionResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for pos redemption api");
		String redemption_id2 = redemptionResponse2.jsonPath().get("redemption_id").toString();
		logger.info("Currency redemption is successful");
		TestListeners.extentTest.get().info("Currency redemption is successful");

		// reward redemption displayed on the user timeline or not
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		String redemptionAmountDisplayed4 = pageObj.guestTimelinePage().rewardDisplayed(dataSet.get("expectedMsg"));
		Assert.assertEquals(redemptionAmountDisplayed4, dataSet.get("expectedMsg"),
				"Redemption message did not matched");
		logger.info("Redeemable displayed on the user timeline");
		TestListeners.extentTest.get().info("Redeemable displayed on the user timeline");

		// Auth void redemption of currency
		Response voidRedemptionResponse1 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidRedemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED);
		logger.info("Currency redemption is successful");
		TestListeners.extentTest.get().info("Currency redemption is successful");

		// entry of currency redemption should be deleted from the timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		Boolean isDeleted = pageObj.guestTimelinePage().redeemedRewardDeleted(dataSet.get("expectedMsg"));
		Assert.assertFalse(isDeleted, "Redemption Entry is deleted from the timeline");
		logger.info("Redemption Entry is deleted from the timeline");
		TestListeners.extentTest.get().info("Redemption Entry is deleted from the timeline");

	}

	@Test(description = "SQ-T5413 [Point Unlock None]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 5)
	@Owner(name = "Vansham Mishra")
	public void T5413_PUNVerifyForceRedemptionShouldNotBeVoidAndValidatePosAndAuthVoidRedemptionApiResponse()
			throws Exception {
		String b_id = dataSet.get("business_id");

		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// pos checkin of 20 points
		Response resp1 = pageObj.endpoints().posCheckin(iFrameEmail, dataSet.get("locationKey"), dataSet.get("amount"));
		Assert.assertEquals(resp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 201 did not matched for post chekin api");
		logger.info("Checkin of 20 points is successful");
		TestListeners.extentTest.get().info("Checkin of 20 points is successful");

//		Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().pointForceRedeem(dataSet.get("apiKey"), userIDUser1, "10");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id1 = forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// validate the force redemption of points in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValuePoints"));

		// Void redemption using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of points via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of points via pos cannot be voided, Expected");

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
//		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
//				dataSet.get("redeemable"), dataSet.get("giftReason"));
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("Redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Force Redemption of offer
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), reward_id, userIDUser1);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for force redemption api for offers");
		logger.info("Offer force redeemed successfully");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id2 = forceRedeemResponse.jsonPath().get("redemption_id").toString();

		// validate the force redemption in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueOffer"));

		// Auth void redemption of offers
		Response voidResponse2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of offers via auth cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of offers via auth cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Void redemption using API
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of points via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of points via pos is successful");

		// validate on the guesttimeline points redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		TestListeners.extentTest.get().pass("Void redemption of Points is validated in account history");

		// Auth void redemption of offers
		Response voidResponse3 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void redemption of offers via auth is successful");
		TestListeners.extentTest.get().info("Void redemption of offers via auth is successful");

		// validate on the guesttimeline offer redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of reward is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of reward is validated in account history");
	}

	@Test(description = "SQ-T5415 [Point to Currency]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 6)
	@Owner(name = "Vansham Mishra")
	public void T5415_PTCVerifyForceRedemptionShouldNotBeVoidAndValidatePosAndAuthVoidRedemptionApiResponseForPoints()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

//		Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// pos checkin of 120 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		logger.info("Checkin of 120 points is successful");
		TestListeners.extentTest.get().info("Checkin of 120 points is successful");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"unbanked_points_redemption", "requested_punches", "10", "reward");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id1 = forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// validate the force redemption of points in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValuePoints"));

		// Void redemption of points using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of points via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of points via pos cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

//		// Void redemption of points using API
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of points via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of points via pos is successful");

//		// validate on the guesttimeline points redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of Points is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of Points is validated in account history");
	}

	@Test(description = "SQ-T5415 [Point to Currency]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 7)
	@Owner(name = "Vansham Mishra")
	public void T5415_PTCVerifyForceRedemptionShouldNotBeVoidAndValidatePosAndAuthVoidRedemptionApiResponseForOffer()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

//		Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("Redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Gift reward to user is successful");
		TestListeners.extentTest.get().info("Gift reward to user is successful");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Force Redemption of offer
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), reward_id, userIDUser1);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for force redemption api for offers");
		logger.info("Offer force redeemed successfully");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id2 = forceRedeemResponse.jsonPath().get("redemption_id").toString();

		// validate the force redemption in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueOffer"));

		// Auth void redemption of offers
		Response voidResponse2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		boolean isAuthVoidRedemptionSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema2, voidResponse2.asString());
		Assert.assertTrue(isAuthVoidRedemptionSchemaValidated, "Auth API Void Redemption Schema Validation failed");
		logger.info("Void force Redemption of offers via auth cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of offers via auth cannot be voided, Expected");

//		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

//		// Auth void redemption of offers
		Response voidResponse5 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void redemption of offers via auth is successful");
		TestListeners.extentTest.get().info("Void redemption of offers via auth is successful");

//		// validate on the guesttimeline offer redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of reward is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of reward is validated in account history");
	}

	@Test(description = "SQ-T5415 [Point to Currency]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 8)
	@Owner(name = "Vansham Mishra")
	public void T5415_PTCVerifyForceRedemptionShouldNotBeVoidAndValidatePosAndAuthVoidRedemptionApiResponseForCurrency()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

//		Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// pos checkin of 120 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		logger.info("Checkin of 120 points is successful");
		TestListeners.extentTest.get().info("Checkin of 120 points is successful");

		// do force redemption of currency
		Response forceRedeem_Response5 = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"amount_redemption", "requested_punches", "1", "reward");
		Assert.assertEquals(forceRedeem_Response5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of $1 is successful");
		TestListeners.extentTest.get().info("Force redemption of $1 is successful");
		String redemption_id5 = forceRedeem_Response5.jsonPath().get("redemption_id").toString();

		// validate the entry of force redemption of currency on the timeline
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueCurrency"));

		// Void redemption of currency using API
		Response voidResponse3 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id5,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of currency via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of currency via pos cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Void redemption of currency using API
		Response voidResponse7 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id5,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of currency via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of currency via pos is successful");

		// validate on the guesttimeline currency redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voidDataCurrency = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voidDataCurrency.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of currency is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of currency is validated in account history");
	}

	@Test(description = "SQ-T5416 [Point to Manual]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 9)
	@Owner(name = "Vansham Mishra")
	public void T5416_PTMVerifyForceRedemptionShouldNotBeVoidAlsoValidatePosAndAuthVoidRedemptionApiResponseForPoints()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// pos checkin of 120 points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftPoints"), dataSet.get("giftReason"));
		logger.info("Checkin of 120 points is successful");
		TestListeners.extentTest.get().info("Checkin of 120 points is successful");

		// force redemption of 10 points using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"unbanked_points_redemption", "requested_punches", "10", "reward");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id1 = forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// validate the force redemption of points in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValuePoints"));

		// Void redemption of points using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of points via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of points via pos cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Void redemption of points using API
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of points via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of points via pos is successful");

		// validate on the guesttimeline points redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of Points is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of Points is validated in account history");
	}

	@Test(description = "SQ-T5416 [Point to Manual]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 10)
	@Owner(name = "Vansham Mishra")
	public void T5416_PTMVerifyForceRedemptionShouldNotBeVoidAlsoValidatePosAndAuthVoidRedemptionApiResponseForFuel()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift fuel discount
		pageObj.guestTimelinePage().messageFuelDiscountToUser(dataSet.get("subject"), dataSet.get("giftTypesFuel"),
				dataSet.get("fuelPoints"), dataSet.get("giftReason"));
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		boolean fuelGiftedNotification = pageObj.guestTimelinePage().verifyfuelGiftedNotification();
		Assert.assertTrue(status2, "Message sent did not displayed on timeline");
		Assert.assertTrue(fuelGiftedNotification, "Fuel gifted notification did not displayed on timeline");
		logger.info("Fuel gifted to the user successfully");
		TestListeners.extentTest.get().info("Fuel gifted to the user successfully");

		// force redemption of fuel
		Response forceFuelRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"fuel_redemption", "requested_punches", "1", "fuel");
		Assert.assertEquals(forceFuelRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of 10 points is successful");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemptionFuel_id1 = forceFuelRedeem_Response.jsonPath().get("redemption_id").toString();

		// validate the force redemption of fuel in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueFuel"));

		// auth Void redemption of fuel
		Response voidFuelResponse = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionFuel_id1);
		Assert.assertEquals(voidFuelResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of Fuel via auth cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of Fuel via auth cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// auth Void redemption of fuel
		Response voidFuelRedemption2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemptionFuel_id1);
		Assert.assertEquals(voidFuelRedemption2.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void redemption of fuel via auth is successful");
		TestListeners.extentTest.get().info("Void redemption of fuel via auth is successful");

		// validate on guest timeline fuel redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voidfueldata2 = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voidfueldata2.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of fuel is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of fuel is validated in account history");
	}

	@Test(description = "SQ-T5416 [Point to Manual]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 11)
	@Owner(name = "Vansham Mishra")
	public void T5416_PTMVerifyForceRedemptionShouldNotBeVoidAlsoValidatePosAndAuthVoidRedemptionApiResponseForOffer()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("Redeemable"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// Force Redemption of offer
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), reward_id, userIDUser1);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for force redemption api for offers");
		logger.info("Offer force redeemed successfully");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id2 = forceRedeemResponse.jsonPath().get("redemption_id").toString();

		// validate the force redemption in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueOffer"));

		// Auth void redemption of offers
		Response voidResponse2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of offers via auth cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of offers via auth cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Auth void redemption of offers
		Response voidResponse5 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void redemption of offers via auth is successful");
		TestListeners.extentTest.get().info("Void redemption of offers via auth is successful");

		// validate on the guesttimeline offer redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in account history");
		TestListeners.extentTest.get().pass("Void redemption of reward is validated in account history");
	}

	@Test(description = "SQ-T5416 [Point to Manual]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 12)
	@Owner(name = "Vansham Mishra")
	public void T5416_PTMVerifyForceRedemptionShouldNotBeVoidAlsoValidatePosAndAuthVoidRedemptionApiResponseForCurrency()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift currency
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().messageRewardAmountToUser(dataSet.get("subject"), dataSet.get("location"),
				dataSet.get("giftTypeCurrency"), dataSet.get("amount"), dataSet.get("giftReason"));
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Message sent did not displayed on timeline");
		logger.info("Currency gifted to the user successfully");
		TestListeners.extentTest.get().info("Currency gifted to the user successfully");

		// do force redemption of currency
		String forceRedemptionValue = dataSet.get("forceRedeemValue");
		Response forceRedeem_Response5 = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"amount_redemption", "requested_punches", forceRedemptionValue, "reward");
		Assert.assertEquals(forceRedeem_Response5.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		utils.logInfo("Force redemption of $" + forceRedemptionValue + " is successful");
		String redemption_id5 = forceRedeem_Response5.jsonPath().get("redemption_id").toString();

		// validate the entry of force redemption of currency on the timeline
		pageObj.guestTimelinePage().clickAccountHistory();
		String actualValue = pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueCurrency"));
		Assert.assertEquals(actualValue, dataSet.get("rewardValueCurrency"), "Force redemption value is not correct in account history");
		utils.logPass("Validated the entry of force redemption of currency on the timeline");

		// Void redemption of currency using API
		Response voidResponse3 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id5,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of currency via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of currency via pos cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Void redemption of currency using API
		Response voidResponse7 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id5,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of currency via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of currency via pos is successful");

		// validate on the guesttimeline currency redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voidDataCurrency = pageObj.accounthistoryPage().getAccountDetailsforPointsRedeemed();
		Assert.assertTrue(voidDataCurrency.size() == 0, "void redemption did not appeared in account history");
		logger.info("Void redemption of currency is validated in account history");
		TestListeners.extentTest.get().pass("Void redemption of currency is validated in account history");

	}

	@Test(description = "SQ-T5417 [Visit Based]Verify force redemption should not be void & also validate pos/auth void redemption api response.", priority = 13)
	@Owner(name = "Vansham Mishra")
	public void T5417_VBVerifyForceRedemptionShouldNotBeVoidAndAlsoValidatePosAndAuthVoidRedemptionApiResponse()
			throws Exception {
		String b_id = dataSet.get("business_id");
		// enable flag from the db
		String query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to true");
		logger.info(dataSet.get("dbFlag") + " value is updated to true");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to true");

		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse1.jsonPath().get("auth_token.token").toString();
		String authToken = signUpResponse1.jsonPath().get("authentication_token").toString();
		String userIDUser1 = signUpResponse1.jsonPath().get("user_id").toString();
		TestListeners.extentTest.get().pass("API v1 user #1 signup is successful");
		logger.info("API v1 user #1 signup is successful");

		// Navigating to the guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift orders visits
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftType"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		logger.info("Gift orders visits is successful");
		TestListeners.extentTest.get().info("Gift orders visits is successful");

		// click message gift and gift reward to user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser(dataSet.get("reward"), dataSet.get("Redeemable"));
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Message sent did not displayed on timeline");

		// fetch user offers/ reward_id using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(offerResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward id is ==>" + reward_id);
		TestListeners.extentTest.get().pass("Api2 user fetch user offers is successful");

		// force redemption of cards using api
		Response forceRedeem_Response = pageObj.endpoints().forceRedeemption(dataSet.get("apiKey"), userIDUser1,
				"card_redemption", "requested_punches", "4", "reward");
		Assert.assertEquals(forceRedeem_Response.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for pos redemption api");
		logger.info("Force redemption of cards is successful");
		TestListeners.extentTest.get().info("Force redemption of cards is successful");
		String redemption_id1 = forceRedeem_Response.jsonPath().get("redemption_id").toString();

		// validate the force redemption of cards in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforCardRedeemed();
		Assert.assertTrue(Itemdata.stream().anyMatch(item -> item.contains("Card Redeemed")),
				"Redemption did not redeemed in account history");
		TestListeners.extentTest.get().pass("Redemption of card is validated in account history");

		// Force Redemption of offer
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), reward_id, userIDUser1);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 200 did not matched for force redemption api for offers");
		logger.info("Offer force redeemed successfully");
		TestListeners.extentTest.get().info("Force redemption of 10 points is successful");
		String redemption_id2 = forceRedeemResponse.jsonPath().get("redemption_id").toString();

		// validate the force of offers redemption in the account history
		pageObj.guestTimelinePage().clickAccountHistory();
		pageObj.forceredemptionPage().verifyTheForceRedemptionPoints(dataSet.get("rewardValueOffer"));

		// Void redemption of cards using API
		Response voidResponse = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of cards via pos cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of cards via pos cannot be voided, Expected");

		// Auth void redemption of offers
		Response voidResponse2 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for pos redemption api");
		logger.info("Void force Redemption of offers via auth cannot be voided, Expected");
		TestListeners.extentTest.get().info("Void force Redemption of offers via auth cannot be voided, Expected");

		// disable the flag from db
		query = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id + "'";
		expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		flag = DBUtils.updateBusinessesPreference(env, expColValue, "false", dataSet.get("dbFlag"), b_id);
		Assert.assertTrue(flag, dataSet.get("dbFlag") + " value is not updated to false");
		logger.info(dataSet.get("dbFlag") + " value is updated to false");
		TestListeners.extentTest.get().info(dataSet.get("dbFlag") + " value is updated to false");

		// Void redemption of cards using API
		Response voidResponse4 = pageObj.endpoints().posVoidRedemption(iFrameEmail, redemption_id1,
				dataSet.get("locationKey"));
		Assert.assertEquals(voidResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void Redemption of cards via pos is successful");
		TestListeners.extentTest.get().info("Void Redemption of cards via pos is successful");

		// validate on the guesttimeline cards redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata = pageObj.accounthistoryPage().getAccountDetailsforCardsRedeemed();
		Assert.assertTrue(voiddata.size() == 0, "void redemption did not appeared in account history");
		TestListeners.extentTest.get().pass("Void redemption of cards is validated in account history");

		// Auth void redemption of offers
		Response voidResponse5 = pageObj.endpoints().authApiVoidRedemptions(authToken, dataSet.get("client"),
				dataSet.get("secret"), redemption_id2);
		Assert.assertEquals(voidResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for pos redemption api");
		logger.info("Void redemption of offers via auth is successful");
		TestListeners.extentTest.get().info("Void redemption of offers via auth is successful");

		// validate on the guesttimeline offer redemption entry should be deleted
		pageObj.guestTimelinePage().clickAccountHistory();
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		// Item redeemed entry must be deleted from history
		List<String> voiddata2 = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		Assert.assertTrue(voiddata2.size() == 0, "void redemption did not appeared in account history");
		TestListeners.extentTest.get().pass("Void redemption of reward is validated in account history");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
