package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class NewCampaignPageTest {
	private static Logger logger = LogManager.getLogger(NewCampaignPageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		utils = new Utilities(driver);
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
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// As discussed with campaigns team location presence and bounce back campaigns
	// deprecated test case
	// Anant
	@Test(description = "SQ-T4390 Verify creation of Locations Presence campaign from new CHP"
			+ "SQ-T4509 Verify location group in more filters option on new campaign home page"
			+ "SQ-T4572 Verify URL when Location filter is selected"
			+ "SQ-T4573 Verify URL when text is searched from search field", priority = 1, groups = { "regression",
					"dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4390_verifyLocationPresenceCampaignNewCHP() throws InterruptedException {
		String campaignName = "LocationPresenceCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// switch to new cam page and select location presence
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().createOtherCampaignCHP(dataSet.get("campaignCategory"), dataSet.get("campaignType"));

		// set campaign name
		pageObj.signupcampaignpage().presenceCampaignWhatPage(campaignName, dataSet.get("giftType"),
				dataSet.get("redeemableName"), dataSet.get("giftReason"));
		pageObj.signupcampaignpage().clickNextBtn();

		// whom page
		pageObj.signupcampaignpage().presenceCampaignWhomPage(dataSet.get("locationGroupName"));
		pageObj.signupcampaignpage().activateCampaign();

		utils.logPass("create a new location Presence campaign -->" + campaignName);

		// select the location from the side panel
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().sidePanelDrpDownClick(dataSet.get("drpDownSelect"));
		pageObj.newCamHomePage().selectFilter(dataSet.get("drpDownSelect"), dataSet.get("locationGroupName"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		// System.out.println(campaignName);
		boolean visible = pageObj.newCamHomePage().selectedFilterVisible(dataSet.get("drpDownSelect"),
				dataSet.get("locationGroupName"));
		Assert.assertTrue(visible, "selected filter not visible");
		utils.logit("verified selected filter is visible");

		pageObj.newCamHomePage().viewCampaignSidePanel();
		String location = pageObj.newCamHomePage().checklocationGrpInCampaignInSidePanel();
		Assert.assertEquals(dataSet.get("locationGroupName"), location, "location filter is not working");
		utils.logit("Verified location filter is working properly");

		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("locationRE"));
		itemVal = itemVal.replace("+", " ");
		Assert.assertEquals(itemVal, dataSet.get("locationGroupName"),
				"on selecting the location filter url did not change");
		utils.logPass("Verify on selecting the location filter url change");
		pageObj.newCamHomePage().closeSidePanel();

		// search campaign
		pageObj.newCamHomePage().searchCampaignNCHP(campaignName);
		String searchVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("searchRE"));
		Assert.assertEquals(searchVal, campaignName, "on searching the campaign url did not change");
		utils.logPass("Verify on searching the campaign url change");

		pageObj.newCamHomePage().deactivateCampaign();
		pageObj.newCamHomePage().deleteCampaign(campaignName);

		utils.logit("deactivate the campaign " + campaignName + " successfullly");

	}

	// As discussed with campaigns team location presence and bounce back campaigns
	// deprecated test case
	// Anant
	@Test(description = "SQ-T4388 Verify creation of Bounce Back campaign from new CHP", priority = 2, groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4388_verifyBounceBackCampaignNewCHP() throws InterruptedException, ParseException {
		String campaignName = "AutomationBounceCampaign" + CreateDateTime.getTimeDateString();
		String starterCampaignName = "AutomationStartCampaign" + CreateDateTime.getTimeDateString();
		String finishCampaignName = "AutomtionFinishCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

//						//create start coupon campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(starterCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaign(dataSet.get("noOfGuests"), dataSet.get("usagePerGuest"),
				dataSet.get("giftType2"), dataSet.get("amount"), "", "", false, dataSet.get("PosOrMobile"), "");
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// create finish Coupon Campaign
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(finishCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaign(dataSet.get("noOfGuests2"), dataSet.get("usagePerGuest"),
				dataSet.get("giftType"), dataSet.get("amount2"), "", "", false, "", dataSet.get("codeGeneration"));
		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		// go to new cam page and select location presence
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().createOtherCampaignCHP(dataSet.get("campaignCategory"), dataSet.get("campaignType"));

		// set bounce back campaign name
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().bounceBackConversionDrpDown(dataSet.get("conversionToValue"));
		pageObj.signupcampaignpage().clickNextBtn();

		// set starter campaign and finish campaign
		pageObj.signupcampaignpage().startCampaignDrpDown(starterCampaignName);
		pageObj.signupcampaignpage().selectFinishCampaignDrpdown(finishCampaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		// active the campaign
		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		utils.logPass("create a new bounce back campaign -->" + campaignName);

		// search bounce back campaign
		pageObj.newCamHomePage().searchCampaignNCHP(campaignName);
		pageObj.newCamHomePage().deactivateCampaign();

		utils.logit("deactivate the campaign " + campaignName + " successfullly");

	}

	// this campaign is planned to be deprecated from campaigns SQ-1959
	// @Test(description = "SQ-T4412, SQ-T4589 Verify creation of A/B campaign from
	// new CHP", priority = 8)
	@Owner(name = "Amit Kumar")
	public void T4412_verifyCreationOfABCampaignFromNewCHP() throws InterruptedException, ParseException {

		String campaignName = "Automation-A/B_Campaign" + CreateDateTime.getTimeDateString();
		logger.info("AB campaign name is created :" + campaignName);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// switch to new cam page and set
		pageObj.newCamHomePage().createOtherCampaignCHP("Other", "A/B Testing");

		// create AB campaign
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setAudienceType(dataSet.get("audiance"));
		pageObj.signupcampaignpage().setSegmentType(dataSet.get("segment"));
		pageObj.utils().waitTillPagePaceDone();
		utils.waitTillElementDisappear(utils.getLocator("signupCampaignsPage.segmentErrorMsg"));
		// pageObj.signupcampaignpage().setSampleSize();
		pageObj.signupcampaignpage().setWinningCombination("Highest unique email opens");
		pageObj.signupcampaignpage().setPushNotification(dataSet.get("pushNotification"));
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().subjectTemplateGroup();
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.signupcampaignpage().setStartDateforABCampaign();
		pageObj.signupcampaignpage().clickScheduleBtn();
		pageObj.newCamHomePage().searchCampaign(campaignName);
		pageObj.newCamHomePage().viewCampaignSummary();
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
