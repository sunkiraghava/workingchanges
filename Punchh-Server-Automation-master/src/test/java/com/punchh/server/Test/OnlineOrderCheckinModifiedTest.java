package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class OnlineOrderCheckinModifiedTest {
	static Logger logger = LogManager.getLogger(OnlineOrderCheckinModifiedTest.class);
	public WebDriver driver;
	private ApiUtils apiUtils;
	private String userEmail;
	private String externalUid;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	SeleniumUtilities SelUtils;
	String order_modeArray[] = { "pickup", "delivery", "dispatch", "curb_side", "drive_thru", "dine_in", "more" };
	String client_platformArray[] = { "web", "call_center", "mobile_web", "kiosk", "android_app", "ios_app", "other" };
	String order_mode = "";
	String client_platform = "";
	String item_qty, item_type, item_family, item_group = "";

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==> " + dataSet);
		apiUtils = new ApiUtils();
		SelUtils = new SeleniumUtilities(driver);
		item_qty = "1";
		item_type = "M";
		item_family = "10";
		item_group = "999";
		externalUid = "TestUid" + CreateDateTime.getTimeDateString();
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2805 Test API - Online Ordering Checkin API with order_mode and client_platform", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void T2805_authOnlineOrderCheckinModifiedTest() throws InterruptedException {
		logger.info("== Online order checkin test ==");
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");

		Response loginResponse = pageObj.endpoints().authApiUserLogin(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		apiUtils.verifyCreateResponse(loginResponse, "Auth API user login");
		Assert.assertEquals(loginResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Failed - Auth API user login");
		String amount = "110";
		Response checkinResponse = pageObj.endpoints().onlineOrderCheckinModified(dataSet.get("order_mode"),
				dataSet.get("client_platform"), authToken, amount, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(checkinResponse, "Online order checkin");
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		externalUid = pageObj.endpoints().externalUid;
		// transactionNumber = pageObj.endpoints().transactionNumber;

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String orderMode = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String clientPlatform = pageObj.guestTimelinePage().getClientPlatformDeatils();
		Assert.assertEquals(orderMode, "Mode delivery", "Oredr Mode did not matched on timeline");
		Assert.assertEquals(clientPlatform, "Client mobile_web", "client plateform did not mnatched on reciept");
		pageObj.utils().logPass("Online Order checkin reciept displayed successfully. order mode : "
				+ orderMode + "and client platform is : " + clientPlatform);
	}

	@Test(description = "SQ-T2806 CCA2-534 | CCA2-543 | Test API - Online Ordering Redemption API with order_mode and client_platform", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2806_authOnlineOrderwithOrder_modeAndclient_platformTest() throws InterruptedException {

		// Reward
		int ranOrderMode = Utilities.getRandomNoFromRange(0, order_modeArray.length - 1);
		int ranClienPlatform = Utilities.getRandomNoFromRange(0, client_platformArray.length - 1);
		order_mode = order_modeArray[ranOrderMode];
		client_platform = client_platformArray[ranClienPlatform];

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String userID = response.jsonPath().get("user_id").toString();
		String token = response.jsonPath().get("access_token").toString();

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id-1
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), externalUid, authToken, "reward", "reward_id", rewardId,
				order_mode, client_platform, "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String order_modeVal = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String client_platformVal = pageObj.guestTimelinePage().getClientPlatformDeatils();

		Assert.assertEquals(client_platform, client_platformVal.replaceAll("Client ", ""));
		Assert.assertEquals(order_mode, order_modeVal.replaceAll("Mode ", ""));
		logger.info(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
		pageObj.utils().logPass(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
	}

	@Test(description = "SQ-T2806 CCA2-534 | CCA2-543 | Test API - Online Ordering Redemption API with order_mode and client_platform", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2806_authOnlineOrderwithOrder_modeAndclient_platformTestPartTwo() throws InterruptedException {
		// redeemable
		int ranOrderMode = Utilities.getRandomNoFromRange(0, order_modeArray.length - 1);
		int ranClienPlatform = Utilities.getRandomNoFromRange(0, client_platformArray.length - 1);
		order_mode = order_modeArray[ranOrderMode];
		client_platform = client_platformArray[ranClienPlatform];

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String userID = response.jsonPath().get("user_id").toString();

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), externalUid, authToken, "redeemable", "redeemable_id",
				dataSet.get("redeemable_id"), order_mode, client_platform, "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String order_modeVal = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String client_platformVal = pageObj.guestTimelinePage().getClientPlatformDeatils();

		Assert.assertEquals(client_platform, client_platformVal.replaceAll("Client ", ""));
		Assert.assertEquals(order_mode, order_modeVal.replaceAll("Mode ", ""));
		logger.info(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
		pageObj.utils().logPass(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
	}

	@Test(description = "SQ-T2806 CCA2-534 | CCA2-543 | Test API - Online Ordering Redemption API with order_mode and client_platform", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2806_authOnlineOrderwithOrder_modeAndclient_platformTestPartThree() throws InterruptedException {
//		discount_amount
		int ranOrderMode = Utilities.getRandomNoFromRange(0, order_modeArray.length - 1);
		int ranClienPlatform = Utilities.getRandomNoFromRange(0, client_platformArray.length - 1);
		order_mode = order_modeArray[ranOrderMode];
		client_platform = client_platformArray[ranClienPlatform];

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String userID = response.jsonPath().get("user_id").toString();

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60", "",
				"", "");
		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), externalUid, authToken, "discount_amount",
				"redeemed_points", "10", order_mode, client_platform, "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String order_modeVal = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String client_platformVal = pageObj.guestTimelinePage().getClientPlatformDeatils();

		Assert.assertEquals(client_platform, client_platformVal.replaceAll("Client ", ""));
		Assert.assertEquals(order_mode, order_modeVal.replaceAll("Mode ", ""));
		logger.info(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
		pageObj.utils().logPass(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
	}

	@Test(description = "SQ-T2806 CCA2-534 | CCA2-543 | Test API - Online Ordering Redemption API with order_mode and client_platform", groups = {
			"regression", "dailyrun" ,"nonNightly"})
	@Owner(name = "Hardik Bhardwaj")
	public void T2806_authOnlineOrderwithOrder_modeAndclient_platformTestPartFour() throws InterruptedException {
		// redemption_code
		int ranOrderMode = Utilities.getRandomNoFromRange(0, order_modeArray.length - 1);
		int ranClienPlatform = Utilities.getRandomNoFromRange(0, client_platformArray.length - 1);
		order_mode = order_modeArray[ranOrderMode];
		client_platform = client_platformArray[ranClienPlatform];

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String userID = response.jsonPath().get("user_id").toString();
		String token = response.jsonPath().get("access_token").toString();

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");
		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");

		// get reward id-1
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// Fetch Redemption Code for reward id
		Response fetchRedemptionCodeResponse = pageObj.endpoints().authApiFetchRedemptionCode(authToken,
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("location_id"), "", rewardId);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, fetchRedemptionCodeResponse.getStatusCode(),
				"Status code 201 did not matched for auth Api Fetch Redemption Code");
		pageObj.utils().logPass("auth Api Fetch Redemption Code is successful");
		String redemption_code = fetchRedemptionCodeResponse.jsonPath().get("internal_tracking_code").toString();

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), externalUid, authToken, "redemption_code",
				"redemption_code", redemption_code, order_mode, client_platform, "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String order_modeVal = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String client_platformVal = pageObj.guestTimelinePage().getClientPlatformDeatils();

		Assert.assertEquals(client_platform, client_platformVal.replaceAll("Client ", ""));
		Assert.assertEquals(order_mode, order_modeVal.replaceAll("Mode ", ""));
		logger.info(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
		pageObj.utils().logPass(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
	}

	@Test(description = "SQ-T2806 CCA2-534 | CCA2-543 | Test API - Online Ordering Redemption API with order_mode and client_platform", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T2806_authOnlineOrderwithOrder_modeAndclient_platformTestCardCompletion() throws InterruptedException {

		// card_completion
		int ranOrderMode = Utilities.getRandomNoFromRange(0, order_modeArray.length - 1);
		int ranClienPlatform = Utilities.getRandomNoFromRange(0, client_platformArray.length - 1);
		order_mode = order_modeArray[ranOrderMode];
		client_platform = client_platformArray[ranClienPlatform];

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		Map<String, String> detailsMap2 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", item_qty, "1", item_type, item_family,
				item_group, "1", dataSet.get("item_id"));
		parentMap.put("Pizza1", detailsMap1);
		detailsMap2 = pageObj.endpoints().getRecieptDetailsMap("Pizza2", item_qty, "4", item_type, item_family,
				item_group, "2", dataSet.get("item_id"));
		parentMap.put("Pizza2", detailsMap2);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String userID = response.jsonPath().get("user_id").toString();

		// send reward amount to user Reedemable-1
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "60", "",
				"", "100");
		logger.info("Send redeemable to the user successfully");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		Response redemptionResponse = pageObj.endpoints().onlineOrderWithOrderModeAndClientPlatformTest(
				dataSet.get("client"), dataSet.get("secret"), externalUid, authToken, "card_completion", "", "",
				order_mode, client_platform, "15", parentMap);
		Assert.assertEquals(redemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String order_modeVal = pageObj.guestTimelinePage().getOnlineOrderModeDeatils();
		String client_platformVal = pageObj.guestTimelinePage().getClientPlatformDeatils();

		Assert.assertEquals(client_platform, client_platformVal.replaceAll("Client ", ""));
		Assert.assertEquals(order_mode, order_modeVal.replaceAll("Mode ", ""));
		logger.info(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
		pageObj.utils().logPass(
				"Given client_platform and order_mode matched with the client_platform and order_mode present on the receipt");
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
