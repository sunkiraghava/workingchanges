package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class UpdateRewardExpiryCampaignTest {

	private static Logger logger = LogManager.getLogger(UpdateRewardExpiryCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-6852 Update expiry date for rewards from Anniversary campaign", priority = 1)
	@Owner(name = "Shubham Gupta")
	public void T6852_updateRewardExpiryDateFromAnniversaryCampaign() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to classic view
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(dataSet.get("anniversayCampaignName"));

		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("anniversayCampaignName"));

		String date = "" + ThreadLocalRandom.current().nextInt(1, 31);
		String dateWithMonthYear = CreateDateTime.getDateFromDay(date);

		pageObj.campaignspage().clikOnMoreOptionAndUpdateRewardExpiryForPostRedemption(date);

		String query = "select distinct(end_time), count(*) from rewards where gifted_for_id = " + campaignID;
		boolean isExpActValueSame = pageObj.campaignspage().pollQueryForExpectedValues(env, query,
				new String[] { "end_time", "count(*)" }, 30,
				new String[] { dateWithMonthYear, dataSet.get("rewardCount") });

		Assert.assertTrue(isExpActValueSame, "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");

	}

	@Test(description = "SQ-6853 Update expiry date for rewards from Recall campaign", priority = 2)
	@Owner(name = "Shubham Gupta")
	public void T6853_updateRewardExpiryDateFromRecallCampaign() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to classic view
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(dataSet.get("recallCampaignName"));

		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("recallCampaignName"));

		String date = "" + ThreadLocalRandom.current().nextInt(1, 31);
		String dateWithMonthYear = CreateDateTime.getDateFromDay(date);

		pageObj.campaignspage().clikOnMoreOptionAndUpdateRewardExpiryForPostRedemption(date);

		String query = "select distinct(end_time), count(*) from rewards where gifted_for_id = " + campaignID;
		boolean isExpActValueSame = pageObj.campaignspage().pollQueryForExpectedValues(env, query,
				new String[] { "end_time", "count(*)" }, 30,
				new String[] { dateWithMonthYear, dataSet.get("rewardCount") });

		Assert.assertTrue(isExpActValueSame, "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");

	}

	@Test(description = "SQ-T6832 Update expiry date for rewards from Post Redemption campaign", priority = 3)
	@Owner(name = "Shubham Gupta")
	public void T6832_updateRewardExpiryDateFromPostRedemptionCampaign() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to classic view
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(dataSet.get("PostRedemptionCampaignName"));

		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("PostRedemptionCampaignName"));

		String date = "" + ThreadLocalRandom.current().nextInt(1, 31);
		String dateWithMonthYear = CreateDateTime.getDateFromDay(date);

		pageObj.campaignspage().clikOnMoreOptionAndUpdateRewardExpiryForPostRedemption(date);

		String query = "select distinct(end_time), count(*) from rewards where gifted_for_id = " + campaignID;
		boolean isExpActValueSame = pageObj.campaignspage().pollQueryForExpectedValues(env, query,
				new String[] { "end_time", "count(*)" }, 30,
				new String[] { dateWithMonthYear, dataSet.get("rewardCount") });

		Assert.assertTrue(isExpActValueSame, "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");

	}

	@Test(description = "SQ-T6832 Update expiry date for rewards from Post checkin campaign", priority = 4)
	@Owner(name = "Shubham Gupta")
	public void T6830_updateRewardExpiryDateFromPostCheckinCampaign() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// Switch to classic view
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(dataSet.get("PostCheckinCampaignName"));

		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("PostCheckinCampaignName"));

		String date = "" + ThreadLocalRandom.current().nextInt(1, 31);
		String dateWithMonthYear = CreateDateTime.getDateFromDay(date);

		pageObj.campaignspage().clikOnMoreOptionAndUpdateRewardExpiryForPostRedemption(date);

		String query = "select distinct(end_time), count(*) from rewards where free_punchh_campaign_id = " + campaignID;
		boolean isExpActValueSame = pageObj.campaignspage().pollQueryForExpectedValues(env, query,
				new String[] { "end_time", "count(*)" }, 30,
				new String[] { dateWithMonthYear, dataSet.get("rewardCount") });

		Assert.assertTrue(isExpActValueSame, "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");

	}

	// Anant
	@Test(description = "SQ-T4808 Verify update expiry feature in mass gifting campaign", priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T4808_verifyUpdateExpiryFeature() throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		String campaignId = dataSet.get("campId");
		String query1 = "SELECT `mass_giftings`.preferences FROM `mass_giftings` WHERE `mass_giftings`.id='"
				+ campaignId + "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query1, "preferences");

		DBUtils.updatePreference(env, expColValue, "", campaignId, dataSet.get("dbFlag1"), "mass_giftings");

		// Instance select business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();
		String campaignID = pageObj.campaignspage().searchAndGetCampaignID(dataSet.get("campaignName"));

//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//		pageObj.newCamHomePage().clickSwitchToClassicBtn();
//		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("campaignName"));
		pageObj.campaignspage().selectSearchedCamapign(dataSet.get("campaignName"));
		String url = driver.getCurrentUrl();
		url = url.replace("new", "dark");
		driver.navigate().to(url);
		utils.waitTillPagePaceDone();

		String date = CreateDateTime.getFutureDate(2);
		String formattedDate = CreateDateTime.convertDateFormat(date);
		pageObj.campaignspage().updateRewardExpiry(formattedDate);

		utils.longWaitInSeconds(1);
		String query = "select end_time from rewards where gifted_for_id=" + campaignID + " order by id desc LIMIT 1 ;";
		pageObj.singletonDBUtilsObj();
		String colVal = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "end_time", 10);

		Assert.assertTrue(colVal.contains(date), "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");
	}

	@Test(description = "SQ-T6951 Update expiry date for rewards from Signup campaign "
			+ "+ SQ-T6952 Update expiry date for rewards from Profile Update campaign", groups = "regression", dataProvider = "TestDataProvider", priority = 5)
	@Owner(name = "Shubham Gupta")
	public void T6951_updateRewardExpiryDateFromSignupCampaign(String campaignName, String rewardCount)
			throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		pageObj.campaignspage().searchAndSelectCamapign(campaignName);
		String url = driver.getCurrentUrl();
		String campaignID = utils.getCamIdFromUrl(url);
		url = url.replace("new", "dark");
		driver.navigate().to(url);
		utils.waitTillPagePaceDone();

		String date = "" + ThreadLocalRandom.current().nextInt(1, 31);
		String dateWithMonthYear = CreateDateTime.getDateFromDay(date);
		String formattedDate = CreateDateTime.convertDateFormat(dateWithMonthYear);
		pageObj.campaignspage().updateRewardExpiry(formattedDate);

		String query = "select end_time, count(*) from rewards where gifted_for_id = " + campaignID
				+ " group by end_time";
		boolean isExpActValueSame = pageObj.campaignspage().pollQueryForExpectedValues(env, query,
				new String[] { "end_time", "count(*)" }, 30, new String[] { date, rewardCount });

		Assert.assertTrue(isExpActValueSame, "reward expiry date did not update");
		utils.logPass("Verified reward expiry date is updated");

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {

				// {"campaignName","rewardCount"},
				{ "Signup Campaign_20Nov2025_Do_Not_delete", "5" },
				{ "Profile Update Campaign_20Nov2025_Do_Not_delete", "5" }, };
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
