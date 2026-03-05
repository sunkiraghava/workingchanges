package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class EmailSignupTest {
	private static Logger logger = LogManager.getLogger(EmailSignupTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
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
		// move toall business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// hitesh LCM-T175
	@Test(description = "Verify Point Unlock Redeemable businesses future checkins expiring, expired should reflect in API", groups = {
			"regression", "dailyrun" }, priority = 1)

	public void T3202_verifyPointUnlockRedeemableBusinessesFutureCheckinsExpiringExpiredShouldReflectInAPI()
			throws Exception {
		// business live - off, went live - off in db
		// Instance login and goto timeline
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		/* pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl); */

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "false",
				"live", b_id);
		Assert.assertTrue(businessLiveFlag, "live value is not updated to false");
		logger.info("live value is updated to false");
		pageObj.utils().logit("live value is updated to false");

		boolean businessWentLIveflag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue,
				"false", "went_live", b_id);
		Assert.assertTrue(businessWentLIveflag, "went_live value is not updated to false");
		logger.info("went_live value is updated to false");
		pageObj.utils().logit("went_live value is updated to false");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set checkin expiry
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.dashboardpage().navigateToTabs("Checkin Earning");
		pageObj.earningPage().setProgramType("Points Unlock Redeemables");
		pageObj.earningPage().setPointsConvertTo("Staged");
		pageObj.earningPage().updateConfiguration();
		pageObj.cockpitearningPage().setCheckinExpiry("1");

		// User register/signup using API2 Signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful ");

		// User login using API2 Signin
		Response loginResponse = pageObj.endpoints().Api2Login(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, loginResponse.getStatusCode(), "Status code 200 did not matched for api2 login");
		String token = loginResponse.jsonPath().get("access_token.token").toString();
		pageObj.utils().logPass("Api2 user Login is successful ");

		// Gift points to the user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// Fetch user expired/expiring points
		Response userExpiringPointsResponse = pageObj.endpoints().Api2FetchUserExpiringPoints(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(userExpiringPointsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 fetch user expiring points");
		boolean isFetchExpiryPointsSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2FetchExpiryPointsSchema, userExpiringPointsResponse.asString());
		Assert.assertTrue(isFetchExpiryPointsSchemaValidated,
				"API v2 Fetch user expiring points Schema Validation failed");
		String points = userExpiringPointsResponse.jsonPath().get("points_expiry[0].expiring_points").toString();
		String expirydate = userExpiringPointsResponse.jsonPath().get("points_expiry[0].expiring_at").toString();
		Assert.assertEquals(points, "200", "expiring points did not matched in response");
		Assert.assertNotNull(expirydate, "expiry date is null in response");
		pageObj.utils().logPass("Api2 Fetch user expiring points is successful");

		// Set checkin expiry
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		pageObj.cockpitearningPage().setCheckinExpiry("");

	}

	@Test(description = "SQ-T2647 Verify Force Redemption With Null Data", groups = { "regression",
			"dailyrun" }, priority = 2)
	public void T2647_verifyForceRedemptionWithNullValues() throws InterruptedException {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// Force Redemption Reward
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().clickCreateRedemptionBtn();
		boolean forceRedemptionRewardStatus = pageObj.campaignspage().validateErrorsMessage();
		Assert.assertTrue(forceRedemptionRewardStatus, "Force redemption error help message did not displayed");
		pageObj.utils().logPass("Force redemption with null data displaying error help message successfully");

	}

	@Test(description = "SQ-T2643 Validate Points/CheckIns force redemption", groups = { "regression",
			"dailyrun" }, priority = 3)
	public void T2643_verifyPointsCheckInsForceRedemption() throws InterruptedException {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// send gift points to user
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("points"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> pointsdata = pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		Assert.assertTrue(pointsdata.get(0).contains("Rewards Earned"),
				"Points gifted to user did not appeared in account history");
		System.out.println(dataSet.get("points") + "points converted into $20.00 of banked rewards");
		Assert.assertTrue(
				pointsdata.get(0).contains(dataSet.get("points") + " points converted into $20.00 of banked rewards"),
				"Points did not converted to banked rewards");
		pageObj.utils().logPass("Verified Gifted points to account history");

		// Force Redemption Points
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionOfPoints(dataSet.get("comment"),
				dataSet.get("forceRedemptionType"), dataSet.get("redeemAmount"));
		boolean forceRedemptionPointsStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionPointsStatus, "Force redemption success message did not displayed");
		pageObj.utils().logPass("Force redemption of points is done successfully");
		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("$1.00 redeemable as Currency Force Redemption"),
				"Force Redeemed amount did not matched");
	}

	@Test(description = "SQ-T2642 Validate Amount/CheckIns force redemption", groups = { "regression",
			"dailyrun" }, priority = 4)
	public void T2642_verifyAmountCheckInsForceRedemption() throws InterruptedException {
		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// send gift points to user
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("points"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> pointsdata = pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		Assert.assertTrue(pointsdata.get(0).contains("Rewards Earned"),
				"Points gifted to user did not appeared in account history");
		System.out.println(dataSet.get("points") + "points converted into $20.00 of banked rewards");
		Assert.assertTrue(
				pointsdata.get(0).contains(dataSet.get("points") + " points converted into $20.00 of banked rewards"),
				"Points did not converted to banked rewards");
		pageObj.utils().logPass("Verified Gifted points to account history");

		// Force Redemption Points
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionOfPoints(dataSet.get("comment"),
				dataSet.get("forceRedemptionType"), dataSet.get("redeemAmount"));
		boolean forceRedemptionPointsStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(forceRedemptionPointsStatus, "Force redemption success message did not displayed");
		pageObj.utils().logPass("Force redemption of points is done successfully");
		String notify = pageObj.guestTimelinePage().validateForceRedemptiononTimeLine();
		String amount = pageObj.guestTimelinePage().geteForceRedeemedAmount();
		Assert.assertTrue(notify.contains("Forced Redemption by"),
				"Force redemption notification did not appeared on user timeline");
		Assert.assertTrue(amount.contains("$5.00 redeemable as Currency Force Redemption"),
				"Force Redeemed amount did not matched");
	}

	@Test(description = "SQ-T2645 Validate Points/CheckIns more than wallet balance force redemption", groups = {
			"regression", "dailyrun" }, priority = 5)
	public void T2645_verifyPointsCheckInsMoreThanWalletBalanceForceRedemption() throws InterruptedException {

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// send gift points to user
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("points"), dataSet.get("giftReason"));
		boolean pointStatus = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(pointStatus, "Message sent did not displayed on timeline");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> pointsdata = pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		Assert.assertTrue(pointsdata.get(0).contains("Rewards Earned"),
				"Points gifted to user did not appeared in account history");
		System.out.println(dataSet.get("points") + "points converted into $20.00 of banked rewards");
		Assert.assertTrue(
				pointsdata.get(0).contains(dataSet.get("points") + " points converted into $20.00 of banked rewards"),
				"Points did not converted to banked rewards");
		pageObj.utils().logPass("Verified Gifted points to account history");

		// Force Redemption Points
		pageObj.forceredemptionPage().clickForceRedemptionBtn();
		pageObj.forceredemptionPage().forceRedemptionOfPoints(dataSet.get("comment"),
				dataSet.get("forceRedemptionType"), dataSet.get("redeemPoints"));
		String forceRedemptionRewardStatus = pageObj.campaignspage().validateErrorsMessagee();
		Assert.assertTrue(
				forceRedemptionRewardStatus.contains(
						"Error creating the forced redemption: Not enough reward balance available to redeem"),
				"Force redemption error help message did not displayed");
		pageObj.utils().logPass("Force redemption with null data displaying error help message successfully");

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
