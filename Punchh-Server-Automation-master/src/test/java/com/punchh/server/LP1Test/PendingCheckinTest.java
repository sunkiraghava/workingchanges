package com.punchh.server.LP1Test;

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
public class PendingCheckinTest {

	private static Logger logger = LogManager.getLogger(PendingCheckinTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
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
		utils = new Utilities(driver);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2276 Verify POS Pending Checkin through valid details", groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2276_verifyPOS_PendingCheckinthroughvaliddetails() throws InterruptedException {
		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickEarningLink();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		// set Pending Checkin Strategy
		pageObj.earningPage().selectPendingCheckinStrategy("Automatic after a configured time delay", "60");
		utils.logPass("checkin strategy updated successfully to create pending checkin");
		// User register/signup using POS Signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		utils.logPass("user signup is done using pos signup api");
		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T23:44:00-08:00"; // Time for pending loyalty
		Response response = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for pos checkin api");
		utils.logPass("checkin is done using pos checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String pendingLoyalty = pageObj.guestTimelinePage().getPendingLoyalty();
		@SuppressWarnings("unused")
		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		pageObj.guestTimelinePage().clickAccountHistory();
		String pointsPending = pageObj.guestTimelinePage().getPendingPoints();
		Assert.assertTrue(pendingLoyalty.contains(key),
				"loyalty checkin pending referesh loyalty did not matched or displayed on timeline");
		Assert.assertTrue(pointsPending.contains("Points Pending"),
				"Points pending did not appeared in account history");
		utils.logPass("pending checkin verified on user time line and account history");
	}

	@Test(description = "SQ-T2232 [Online Order Pending Checkin] >>User is able to perform pending checkin and void by OLO", groups = {"regression", "dailyrun"}, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2232_VerifyOnlineOrderPendingCheckinByOlo() throws InterruptedException {

		// Navigate to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickEarningLink();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		// set Pending Checkin Strategy
		pageObj.earningPage().selectPendingCheckinStrategy("Automatic after a configured time delay", "60");
		utils.logPass("checkin strategy updated successfully to create pending checkin");

		// User register/signup using auth Signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String authToken = signUpResponse.jsonPath().get("authentication_token");
		utils.logPass("user signup is done using auth signup api");
		// auth online order checkin
		String externalUid = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T23:44:00-08:00";
		Response checkinResponse = pageObj.endpoints().authOnlineOrderCheckin(authToken, dataSet.get("amount"),
				dataSet.get("client"), dataSet.get("secret"), txn, externalUid, date);
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online checkin api");
		utils.logPass("pending checkin is done using auth checkin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String pendingLoyalty = pageObj.guestTimelinePage().getPendingLoyalty();
		@SuppressWarnings("unused")
		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		pageObj.guestTimelinePage().clickAccountHistory();
		String pointsPending = pageObj.guestTimelinePage().getPendingPoints();
		Assert.assertTrue(pendingLoyalty.contains(externalUid),
				"loyalty checkin pending referesh loyalty did not matched or displayed on timeline");
		Assert.assertTrue(pointsPending.contains("Points Pending"),
				"Points pending did not appeared in account history");
		utils.logPass("pending checkin is verified on user timeline and in account history");
		// auth online void checkin
		Response voidCheckinResponse = pageObj.endpoints().authOnlineVoidCheckin(authToken, dataSet.get("client"),
				dataSet.get("secret"), externalUid);
		Assert.assertEquals(voidCheckinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 did not matched for auth online void checkin api");
		utils.logPass("void pending checkin is done using auth void checkin api");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String disapprovedLoyalty = pageObj.guestTimelinePage().getDisapprovedLoyalty();
		Assert.assertTrue(disapprovedLoyalty.contains(externalUid),
				"Dsiapproved loyalty did not matched or displayed on timeline");
		utils.logPass("void pending checkin verified on use timeline");

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
