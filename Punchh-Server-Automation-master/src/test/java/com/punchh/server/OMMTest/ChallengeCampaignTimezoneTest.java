package com.punchh.server.OMMTest;

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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ChallengeCampaignTimezoneTest {

	private static Logger logger = LogManager.getLogger(ChallengeCampaignTimezoneTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	String userEmail, campaignName;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		campaignName = null;
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T5826 Dashboard GIFT API>Verify that user is gifting challenge steps using API if start and end date is set to past date in HST timezone", groups = "Regression", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T5826_VerifyChallengeCampaignInHSTtimezone() throws Exception {

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create challenge campaign with every x point
		campaignName = "AutomationChallengeCampaign" + CreateDateTime.getTimeDateString();
		String oneDayOldDate = CreateDateTime.getPreviousDate(1);

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setPostCheckinGiftReason(campaignName);
		pageObj.signupcampaignpage().setGiftPoints(dataSet.get("giftPoints"));
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.newCamHomePage().uploadAllImagesInChallengeCampaign();

		pageObj.newCamHomePage().challengeDrpDown(dataSet.get("challengeDrpDown"));
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("challenge_campaign_global", "check");
		pageObj.newCamHomePage().noOfStepsPoints(dataSet.get("points"));
		pageObj.newCamHomePage().pnForChallengeProgress(campaignName);
		pageObj.newCamHomePage().pnForChallengeComplete(campaignName);
		pageObj.newCamHomePage().selectChallengeCampaignTimeZone(dataSet.get("timeZone"));
		pageObj.newCamHomePage().activateChallengeCampaign();

		// search and get campaign id
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(campaignName);

		// update one day old start date and end date in db
		String query = "UPDATE campaigns SET start_date='" + oneDayOldDate + "' WHERE `id` = " + campaignID;
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1, "Update query to make start date one day old is not working");

		String query2 = "UPDATE campaigns SET end_date='" + oneDayOldDate + "' WHERE `id` = " + campaignID;
		int rs2 = DBUtils.executeUpdateQuery(env, query2);
		Assert.assertEquals(rs2, 1, "Update query to make end date one day old is not working");

		@SuppressWarnings("unused")
		String query3 = "UPDATE `campaigns` SET `timezone` = 'Pacific/Honolulu' WHERE `id` = " + campaignID;
		int rs3 = DBUtils.executeUpdateQuery(env, query3);
		Assert.assertEquals(rs3, 1, "Update query to make timezone HST is not working");

		// create User
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		String currentTime = CreateDateTime.getCurrentTimeInIST();
		utils.logInfo("Current Time in IST: " + currentTime);

		// navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		if (CreateDateTime.isBeforeGivenTime(15, 30)) {
			utils.logInfo(
					"As test case is executed before 3:30 PM IST, we can confirm the campaign's visibility in the Gifting dropdown menu");
			// gift challenge progress to user from guest timeline
			boolean flag = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
					campaignName);
			Assert.assertTrue(flag, campaignName + "is not appearing in Challenge Campaigns dropdown");
			utils.logPass("Verified that " + campaignName + " is appearing in Challenge Campaigns dropdown");

			Response sendRewardResponse = pageObj.endpoints().API2SendMessageToUserChallengeCampaign(
					"challengeCampaign", userID, dataSet.get("apiKey"), campaignID, "10");
			Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
					"Status code 201 did not matched for api2 send challenge campaign to user");
			utils.logPass("verified that user is able to gift steps as in HST timezone");

		} else {
			utils.logInfo(
					"As test case is executed after 3:30 PM IST, we can confirm the campaign's is not visible in the Gifting dropdown menu");

			// gift challenge progress to user from guest timeline
			boolean flag = pageObj.guestTimelinePage().verifyChallengeCampaignAppearedInDrpDwn("Challenge Progress",
					campaignName);
			Assert.assertFalse(flag, campaignName + " appeared in Challenge Campaigns dropdown");
			utils.logPass("Verified that " + campaignName + " did not appeared in Challenge Campaigns dropdown");
		}

		/*pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// delete camapigns
		pageObj.campaignspage().removeSearchedCampaign(campaignName);*/

	}


	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
