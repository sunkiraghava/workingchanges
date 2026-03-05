package com.punchh.server.Test;

// as dicussed with rohit doraya no need to run these in regular regression - not a regression class/tests
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

@Listeners(TestListeners.class)
public class RedeemableImageTest {
	private static Logger logger = LogManager.getLogger(RedeemableImageTest.class);
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

	@Test(description = "SQ-T2578 Verify the redeemable_image_url with currency redemption", groups = "Regression", priority = 0)
	public void T2578_verifyRedeemableImageUrlwithCurrencyRedemption() throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString(); // to be used in auth api

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

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response.jsonPath().get("active_redemptions[0].redeemable_image_url").toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// move to instance and update base redeemable image

		// pageObj.menupage().clickSettingsMenu();
		// Thread.sleep(1000);
		// pageObj.menupage().clickredeemablesLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		pageObj.redeemablesPage().searchAndselectRedeemable("Base Redeemable");
		Thread.sleep(2000);

		String msg = pageObj.redeemablesPage().uploadRedeemableimage();
		Assert.assertTrue(msg.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image uploaded successfully");

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response1 = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response1 = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse1 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse1.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse1 = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse1 = pageObj.endpoints().authApiFetchUserBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// generate redemption code using mobile api
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionRedeemed_Points(token, "1.5",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Codee = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse1.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Codee);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Set default Redeemable image
		pageObj.redeemablesPage().searchAndselectRedeemable("Base Redeemable");
		Thread.sleep(2000);

		String msg1 = pageObj.redeemablesPage().setDefaultRedeemableImage();
		Assert.assertTrue(msg1.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image set to default successfully");

	}

	@Test(description = "SQ-T2581 Verify the redeemable_image_url with Redeemable redemption", groups = "Regression", priority = 1)
	public void T2581_verifyRedeemableImageUrlWithRedeemableRedemption() throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString(); // to be used in auth api

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

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response.jsonPath().get("active_redemptions[0].redeemable_image_url").toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
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

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response1 = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response1 = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse1 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse1.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse1 = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse1 = pageObj.endpoints().authApiFetchUserBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Perform redemption of reward through api (/api/mobile/redemptions) generate
		// redemption code using mobile api with redeemable_id
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionRedeemable_id(token, "9126",
				dataSet.get("client").trim(), dataSet.get("secret").trim());
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		@SuppressWarnings("unused")
		String redemption_Code1 = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse1.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Code);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Set default Redeemable image
		pageObj.redeemablesPage().searchAndselectRedeemable("Simplicitea");
		Thread.sleep(2000);

		String msg1 = pageObj.redeemablesPage().setDefaultRedeemableImage();
		Assert.assertTrue(msg1.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image set to default successfully");

	}

	@Test(description = "SQ-T2579 Verify the redeemable_image_url with reward redemption", groups = "Regression", priority = 2)
	public void T2579_verifyRedeemableImageUrlWithRewardRedemption() throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString(); // to be used in auth api

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

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response.jsonPath().get("active_redemptions[0].redeemable_image_url").toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// move to instance and update base redeemable image

		// pageObj.menupage().clickSettingsMenu();
		// Thread.sleep(1000);
		// pageObj.menupage().clickredeemablesLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		pageObj.redeemablesPage().searchAndselectRedeemable("$2.0 OFF");
		Thread.sleep(2000);

		String msg = pageObj.redeemablesPage().uploadRedeemableimage();
		Assert.assertTrue(msg.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image uploaded successfully");

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response1 = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response1 = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse1 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");
		redeemable_image_url = accountBalResponse1.jsonPath().get("banked_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse1 = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse1 = pageObj.endpoints().authApiFetchUserBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// click message gift and gift reward
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean statuss = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(statuss, "Message sent did not displayed on timeline");

		// fetch user offers(reward_id) using ap2 mobileOffers
		Response offerResponsee = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponsee.getStatusCode(),
				"Status code 200 did not matched for api2 user offers");
		String reward_idd = offerResponsee.jsonPath().get("rewards[0].reward_id").toString();
		logger.info("reward_id is => " + reward_idd);

		// Perform redemption of reward through api (/api/mobile/redemptions) with
		// reward_id as 1234

		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionReward_id(token, reward_idd,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String redemption_Code1 = redemption_codeResponse1.jsonPath().get("internal_tracking_code").toString();
		redeemable_image_url = redemption_codeResponse1.jsonPath().get("redeemable_image_url").toString();
		logger.info("redemption code => " + redemption_Code1);
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Set default Redeemable image
		// pageObj.menupage().clickSettingsMenu();
		// Thread.sleep(1000);
		// pageObj.menupage().clickredeemablesLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablesPage().searchAndselectRedeemable("$2.0 OFF");
		Thread.sleep(2000);

		String msg1 = pageObj.redeemablesPage().setDefaultRedeemableImage();
		Assert.assertTrue(msg1.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image set to default successfully");

	}

	@Test(description = "SQ-T2580 Verify the redeemable_image_url with card redemption", groups = "Regression", priority = 3)
	public void T2580_verifyRedeemableImageUrlWithCardRedemption() throws InterruptedException, AWTException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Signup using mobile api
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString(); // to be used in auth api

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

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response.jsonPath().get("active_redemptions[0].redeemable_image_url").toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse);
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse = pageObj.endpoints().authApiFetchUserBalance(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// move to instance and update base redeemable image

		// pageObj.menupage().clickSettingsMenu();
		// Thread.sleep(1000);
		// pageObj.menupage().clickredeemablesLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");

		pageObj.redeemablesPage().searchAndselectRedeemable("Base Redeemable");
		Thread.sleep(2000);

		String msg = pageObj.redeemablesPage().uploadRedeemableimage();
		Assert.assertTrue(msg.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image uploaded successfully");

		// Call account balance api of mobile v1 (/api/mobile/accounts)
		Response accounts_Response1 = pageObj.endpoints().Api1MobileAccounts(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accounts_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile accounts");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accounts_Response1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Call user balance api of mobile v1 (/api/mobile/users/balance")
		Response balance_Response1 = pageObj.endpoints().Api1MobileUsersbalance(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(balance_Response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api v1 mobile users balance");
		redeemable_image_url = balance_Response1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of api2 (/api2/mobile/checkins/account_balance)
		Response accountBalResponse1 = pageObj.endpoints().Api2AccountBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(accountBalResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 account balance");

		// call user balance api of api2 (/api2/mobile/users/balance)
		Response userBalanceResponse1 = pageObj.endpoints().Api2FetchUserBalance(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, userBalanceResponse1.getStatusCode(),
				"Status code 200 did not matched for api2 fetch user balance");
		redeemable_image_url = userBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call account balance api of auth (/api/auth/accounts)
		Response accountHistoryResponse1 = pageObj.endpoints().authApiAccountHistory(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(accountHistoryResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account history response");
		redeemable_image_url = pageObj.redeemablesPage().getValueinJsonArray(accountHistoryResponse1);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// call user balance api of auth (/api/auth/users/balance)
		Response authUserBalanceResponse1 = pageObj.endpoints().authApiFetchUserBalance(authToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(authUserBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Auth API Account balance response");
		redeemable_image_url = authUserBalanceResponse1.jsonPath().get("active_redemptions[0].redeemable_image_url")
				.toString();
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Perform redemption of card through api (/api/mobile/redemptions) with
		// card_completion :1
		Response redemption_codeResponse1 = pageObj.endpoints().Api1MobileRedemptionCardCompletion(token, "1",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(redemption_codeResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		redeemable_image_url = redemption_codeResponse1.jsonPath().get("redeemable_image_url").toString();
		logger.info("redeemable image url => " + redeemable_image_url);
		Assert.assertNotEquals(redeemable_image_url,
				"https://res.cloudinary.com/punchh/image/upload/fl_lossy,q_auto/v1/static/punchh-icon-thumb.png");

		// Set default Redeemable image
		pageObj.redeemablesPage().searchAndselectRedeemable("Base Redeemable");
		Thread.sleep(2000);

		String msg1 = pageObj.redeemablesPage().setDefaultRedeemableImage();
		Assert.assertTrue(msg1.contains("Redeemable successfully saved."), "Redeemable image uplod is not successfull");
		logger.info("Redeemable image set to default successfully");

	}

	@SuppressWarnings("rawtypes")
	@Test(description = "SQ-T5466 Validate that Redeemable Image information is getting displayed in GET Redeemable API.", groups = "Regression", priority = 3)
	public void T5466_ValidateRedeemableImageInformationIsDisplayedInGETRedeemableAPI() throws InterruptedException {
		String adminKey = dataSet.get("apiKey");
		Response redeemableListResp = pageObj.endpoints().redeemableListAPi(adminKey,
				dataSet.get("redeemablewithpunchhlogo"), "1", "20");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListResp.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable List Api is successful");
		ResponseBody body = redeemableListResp.getBody();
		String bodyAsString = body.jsonPath().get("data[0].image").toString();
		String name1 = body.jsonPath().get("data[0].name").toString();
		Assert.assertEquals(name1.contains(dataSet.get("redeemablewithpunchhlogo")), true,
				"Redeemable name does not match");
		Assert.assertEquals(bodyAsString.contains("punchh-icon-thumb"), true,
				"Response body does not contains punchh logo");
		Response redeemableListRespwithLogo = pageObj.endpoints().redeemableListAPi(adminKey,
				dataSet.get("redeemablewithuploadedlogo"), "1", "20");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, redeemableListRespwithLogo.getStatusCode(),
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		pageObj.utils().logit("Dashboard Redeemable with punchh logo Api is successful");
		ResponseBody body2 = redeemableListRespwithLogo.getBody();
		String body2AsString = body2.jsonPath().get("data[0].image").toString();
		String name2 = body2.jsonPath().get("data[0].name").toString();
		Assert.assertEquals(name2.contains(dataSet.get("redeemablewithuploadedlogo")), true,
				"Redeemable name does not match");
		Assert.assertEquals(body2AsString.contains("redeemable_images"), true,
				"Response body does not contains the logo uploaded by the user");
		pageObj.utils().logPass("Dashboard Redeemable with uploaded logo is successful");
	}

	@SuppressWarnings("rawtypes")
	@Test(description = "SQ-T5545 Validate that user is able to get redeemable data only if Admin user is having Dashboard API access permission.", groups = "Regression", priority = 3)
	public void T5545_ValidateGetRedeemableData() throws InterruptedException {
		String adminKey = dataSet.get("apiKey");
//		Navigating to the guest timeline
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to Admin users sub menu
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("email"), dataSet.get("role"));
		pageObj.AdminUsersPage().turnPermissionoff(dataSet.get("permission"));
		pageObj.utils().logit("Dashboard API Access permission turned off");
		Response redeemableListResp = pageObj.endpoints().redeemableListAPi(adminKey,
				dataSet.get("redeemablewithpunchhlogo"), "1", "20");
		Assert.assertEquals(redeemableListResp.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN,
				"Status code 403 did not matched for Dashboard Redeemable List Api");
		boolean isGetRedeemableListInsufficientPrivelegesSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.dashboardGetRedeemableList403ErrorSchema, redeemableListResp.asString());
		Assert.assertTrue(isGetRedeemableListInsufficientPrivelegesSchemaValidated,
				"Get Redeemable List (InsufficientPriveleges) Schema Validation failed");
		ResponseBody body = redeemableListResp.getBody();
		String responseWithPermissionTurnedOff = body.jsonPath().get("no_permission_error").toString();
		Assert.assertEquals(responseWithPermissionTurnedOff.contains(dataSet.get("permissionOffResponse")), true,
				"Message does not match when Permission is turned off");
		pageObj.utils().logPass("User is getting no permission error when dashboard API Access permission turned off, expected");
		pageObj.AdminUsersPage().turnPermissionOn(dataSet.get("permission"));
		pageObj.utils().logit("Dashboard API Access permission turned on");
		Response redeemableListResp2 = pageObj.endpoints().redeemableListAPi(adminKey,
				dataSet.get("redeemablewithpunchhlogo"), "1", "20");
		Assert.assertEquals(redeemableListResp2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for Dashboard Redeemable List Api");
		ResponseBody body2 = redeemableListResp2.getBody();
		String responseWithPermissionTurnedOn = body2.jsonPath().get("data[0].name").toString();
		Assert.assertEquals(responseWithPermissionTurnedOn.contains(dataSet.get("redeemablewithpunchhlogo")), true,
				"User is unable to get data on granting dashboard API permission to admin");
		pageObj.utils().logPass("User is getting data successfully when dashboard API Access permission turned on");
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
