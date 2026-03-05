package com.punchh.server.Test;

// as discussed with rohit doraya no need to run these in regular regression, not a regression class/test
import java.awt.AWTException;
import java.lang.reflect.Method;
import java.util.Map;
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
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedeemableImageActiveRedemptionsApiTest {

	private static Logger logger = LogManager.getLogger(RedeemableImageActiveRedemptionsApiTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	String redeemable_image_url;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2582 Verify the redeemable_image_url in active_redemptions api part one", groups = "Regression", priority = 0)
	public void T2582_verifyRedeemableImageUrlInActiveRedemptionsApiPartOne()
			throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api

		// navigate to user timeline
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// Perform redemption of reward through api (/api/mobile/redemptions) generate
		// redemption code using mobile api with redeemable_id
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token, "9126",
				dataSet.get("client").trim(), dataSet.get("secret").trim());
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Code);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call pos active redemptions api
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemable_image_url = activeRedemptionResponse.jsonPath().get("[0].redeemable.redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// move to instance and update base redeemable image

		// pageObj.menupage().clickSettingsMenu();
		// Thread.sleep(1000);
		// pageObj.menupage().clickredeemablesLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		pageObj.redeemablesPage().searchAndselectRedeemable("Simplicitea");
		Thread.sleep(2000);

		String msg = pageObj.redeemablesPage().uploadRedeemableimage();
		Assert.assertTrue(msg.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image uploaded successfully");

		Response activeRedemptionResponse1 = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemable_image_url = activeRedemptionResponse1.jsonPath().get("[0].redeemable.redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Set default Redeemable image
		pageObj.redeemablesPage().searchAndselectRedeemable("Simplicitea");
		Thread.sleep(2000);

		String msg1 = pageObj.redeemablesPage().setDefaultRedeemableImage();
		Assert.assertTrue(msg1.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image set to default successfully");

	}

	@Test(description = "SQ-T2582 Verify the redeemable_image_url in active_redemptions api part two", groups = "Regression", priority = 1)
	public void T2582_verifyRedeemableImageUrlInActiveRedemptionsApiPartTwo()
			throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		// String authToken =
		// signUpResponse.jsonPath().get("authentication_token").toString(); // to be
		// used in auth api

		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// Perform redemption of card through api (/api/mobile/redemptions) with
		// card_completion :1
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionCardCompletion(token, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		redeemable_image_url = redemption_codeResponse.jsonPath().get("redeemable_image_url").toString();
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call pos active redemptions api
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemable_image_url = pageObj.redeemablesPage().checkRedeemableImageUrl(activeRedemptionResponse);
		Assert.assertEquals(redeemable_image_url, null);

	}

	@Test(description = "SQ-T2582 Verify the redeemable_image_url in active_redemptions api part three", groups = "Regression", priority = 2)
	public void T2582_verifyRedeemableImageUrlInActiveRedemptionsApiPartThree()
			throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		// String authToken =
		// signUpResponse.jsonPath().get("authentication_token").toString(); // to be
		// used in auth api

		// navigate to user timeline
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// click message gift and gift reward
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// fetch user offers(reward_id) using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward_id is => " + reward_id);

		// Perform redemption of reward through api (/api/mobile/redemptions) with
		// reward_id as 1234

		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionReward_id(token, reward_id,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Code);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call pos active redemptions api
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemable_image_url = activeRedemptionResponse.jsonPath().get("[0].redeemable.redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

	}

	@Test(description = "SQ-T2582 Verify the redeemable_image_url in active_redemptions api part four", groups = "Regression", priority = 3)
	public void T2582_verifyRedeemableImageUrlInActiveRedemptionsApiPartFour()
			throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		// String authToken =
		// signUpResponse.jsonPath().get("authentication_token").toString(); // to be
		// used in auth api

		// navigate to user timeline
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// gift currency amount $10 to user
		pageObj.guestTimelinePage().messageRewardAmountToUser(dataSet.get("subject"), dataSet.get("location"),
				dataSet.get("giftTypes"), dataSet.get("amount"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// generate redemption code using mobile api
		Response redemption_codeResponse = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1.5",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code = redemption_codeResponse.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Code);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call pos active redemptions api
		Response activeRedemptionResponse = pageObj.endpoints().posActiveRedemptions(userEmail,
				dataSet.get("locationKey"));
		Assert.assertEquals(activeRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		redeemable_image_url = pageObj.redeemablesPage().checkRedeemableImageUrl(activeRedemptionResponse);
		Assert.assertEquals(redeemable_image_url, null);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
