package com.punchh.server.Test;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class RedeemableTimezoneTest {
	private static Logger logger = LogManager.getLogger(RedeemableTimezoneTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	private Utilities utils;
	String redeemableName = "AutomationRedeemable";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single Login to instance
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
		utils = new Utilities(driver);
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T4204 Validate that timezone is mandatory field in case user selects expiry_days or end time for redeemable", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T4204_TimezoneAsMandatory() throws Exception {
		redeemableName += CreateDateTime.getTimeDateString();

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemable(redeemableName);
		pageObj.redeemablePage().selectRecieptRule("1");

		pageObj.redeemablePage().expiryInDays("", "1");
		pageObj.redeemablePage().timezoneWithOrWithoutTimezone("without timezone", "");
		pageObj.redeemablePage().clickOnFinishButton();
		boolean flag = pageObj.redeemablePage().successOrErrorConfirmationMessage("Timezone can't be blank");
		Assert.assertTrue(flag, "Timezone can't be blank error is not visible");
		logger.info("error is verified Timezone can't be blank for Redeemable with Expiry in Days");
		utils.logPass("error is verified Timezone can't be blank for Redeemable with Expiry in Days");

		pageObj.redeemablePage().expiryInDays("clear expiry", "");
		String enddateTime = CreateDateTime.getFutureDate(4) + " 11:00 PM";
		pageObj.redeemablePage().endTime(enddateTime);
		pageObj.redeemablePage().clickOnFinishButton();
		boolean flag1 = pageObj.redeemablePage()
				.successOrErrorConfirmationMessage("Timezone can't be blank, Timezone is not included in the list");
		Assert.assertTrue(flag1, "Timezone can't be blank error is not visible");
		logger.info("error is verified Timezone can't be blank for Redeemable with Expiry in Days");

		pageObj.redeemablePage().allowRedeemableToRunIndefinitely();
		pageObj.redeemablePage().clickOnFinishButton();
		boolean flag2 = pageObj.guestTimelinePage().successOrErrorConfirmationMessage("Redeemable successfully saved.");
		Assert.assertTrue(flag2, "Timezone can't be blank error is not visible");
		logger.info("error is verified Timezone can't be blank for Redeemable with Expiry in Days");

		pageObj.redeemablePage().searchRedeemable(redeemableName);
		pageObj.redeemablePage().deleteRedeemable(redeemableName);
	}

	@Test(description = "SQ-T4250 Validate the end date of reward by gifting redeemable and end date selected in 'Valid Until' on 'Message/Gift' screen", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T4250_RedeemableValidUntill() throws Exception {
		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("Api2 user signup is successful");

		String expEndTime = dataSet.get("expectedEndTime");
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"), "", "", expEndTime);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		logger.info("Send redeemable to the user successfully");
		utils.logPass("Send redeemable to the user successfully");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(rewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2");
		String endTime = rewardResponse.jsonPath().get("end_time").toString();
		boolean verification1 = utils.textContains(endTime, expEndTime);
		Assert.assertTrue(verification1, "Expected end time is not equal to actual end time");
		logger.info("Reward end date/time should be same as date selected in 'Valid until' field");
		utils.logPass("Reward end date/time should be same as date selected in 'Valid until' field");
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
