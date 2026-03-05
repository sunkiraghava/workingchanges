package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Test
@Listeners(TestListeners.class)
public class CampaignPerformancePageTest {
	private static Logger logger = LogManager.getLogger(CampaignPerformancePageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";
	ApiUtils apiUtils;

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
		utils = new Utilities(driver);
	}

	// Rakhi
	@Test(description = "SQ-T6422 Verify Split Test Ellipsis options For Campaign Performance Page.")
	@Owner(name = "Rakhi Rawat")
	public void T6422_VerifySplitTestCppEllipsis() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campId"));
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().clickOnFirstCampaign();
		utils.longWaitInSeconds(2);
		// Verify Split Test Ellipsis options
		List<String> expected = Arrays.asList("Export Report", "Clear Notifications", "Audit Log");
		logger.info("Expected Split Test Ellipsis options: " + expected);
		List<String> options = pageObj.campaignPerformancePage().verifySplitTestCPPOptions();
		logger.info("Split Test Ellipsis options: " + options);
		Assert.assertEquals(options, expected,
				"Split Test Ellipsis options not matched for Campaign Performance Page.");
		logger.info(
				"Verified Export Report, Clear Notifications , Audit logs only these options visible for split test campaign");
		TestListeners.extentTest.get().pass(
				"Verified Export Report, Clear Notifications , Audit logs only these options visible for split test campaign");
	}

	// Rakhi
	@Test(description = "SQ-T6443 Verify Combined Engagement Metrics (Email and Push Notifications"
			+ "SQ-T6582 Verify Push Notification for Split Campaign")
	@Owner(name = "Rakhi Rawat")
	public void T6443_VerifyCombinedEngagementMetrics() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// navigate to Campaign Performance Page
		for (int i = 1; i <= 2; i++) {
			pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
			pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignId" + i));
			pageObj.newCamHomePage().clickOnFirstCampaign();

			String sentValueOverall = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"Overall", "Sent");
			String openedValueOverall = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"Overall", "Opened");

			String sentValueVarintA = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"A", "Sent");
			String openedValueVarintA = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"A", "Opened");

			String sentValueVarintB = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"B", "Sent");
			String openedValueVarintB = pageObj.campaignPerformancePage().getMessageEngagementValue("Push Notification",
					"B", "Opened");

			String expectedSentValueOverall = String
					.valueOf(Integer.parseInt(sentValueVarintA) + Integer.parseInt(sentValueVarintB));
			String expectedOpenedValueOverall = String
					.valueOf(Integer.parseInt(openedValueVarintA) + Integer.parseInt(openedValueVarintB));
			Assert.assertEquals(sentValueOverall, expectedSentValueOverall,
					"Overall Sent value is not equal to sum of Variant A and Variant B Sent values");
			Assert.assertEquals(openedValueOverall, expectedOpenedValueOverall,
					"Overall Opened value is not equal to sum of Variant A and Variant B Opened values");
			logger.info("Verified Combined Engagement Metrics for Split Test Campaign Id : "
					+ dataSet.get("campaignId" + i));
			TestListeners.extentTest.get().pass("Verified Combined Engagement Metrics for Split Test Campaign Id : "
					+ dataSet.get("campaignId" + i));
		}
	}

	// Rakhi
	@Test(description = "SQ-T6444 Verify CPP Logic for Split Test Overall KPI's"
			+ "SQ-T6453 Verify Combined Key Metrics for split test campaign"
			+ "SQ-T6478 Verify key metrics for split test long campaign", dataProvider = "TestDataProvider2")
	@Owner(name = "Rakhi Rawat")
	public void T6444_VerifySplitTestKeyMetrics(String campaignId) throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().searchCampaign(campaignId);
		pageObj.newCamHomePage().clickOnFirstCampaign();

		List<String> expectedKeyMetricsList = Arrays.asList("Customers Targeted", "Redeemed Net Sales",
				"Redeemed Net AOV", "Redeemed Rate", "Discount");
		// verify key metrics for Variants Combined
		List<String> combinedKeyMetricsList = pageObj.campaignPerformancePage()
				.getSplitCppKeyMetrics("Variants Combined");
		Assert.assertEquals(combinedKeyMetricsList, expectedKeyMetricsList,
				"Key metrics section not matched for Variants Combined");
		logger.info("Verified Key metrics section for Variants Combined");
		TestListeners.extentTest.get().pass("Verified Key metrics section for Variants Combined");

		// verify key metrics for variant A
		List<String> variantAkeyMetrics = pageObj.campaignPerformancePage().getSplitCppKeyMetrics("A variant");
		Assert.assertEquals(variantAkeyMetrics, expectedKeyMetricsList,
				"Key metrics section not matched for Variant A");
		logger.info("Verified Key metrics section for Variant A");
		TestListeners.extentTest.get().pass("Verified Key metrics section for Variant A");

		// verify key metrics for variant B
		List<String> variantBkeyMetrics = pageObj.campaignPerformancePage().getSplitCppKeyMetrics("B variant");
		Assert.assertEquals(variantBkeyMetrics, expectedKeyMetricsList,
				"Key metrics section not matched for Variant B");
		logger.info("Verified Key metrics section for Variant B");
		TestListeners.extentTest.get().pass("Verified Key metrics section for Variant B");

		// get key metrics values for Variants Combined Top level
		List<String> keyMetricValuesCombinedTop = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("Variants Combined",
					dataSet.get("keyMetrics" + i), "Top");
			keyMetricValuesCombinedTop.add(values);
		}
		logger.info(
				"Values for Customers Targeted, Redeemed Net Sales, Redeemed Net AOV, Redeemed Rate, and Discount under 'Variants Combined' (Top) are: "
						+ keyMetricValuesCombinedTop);

		// get key metrics values for Variant A Top level
		List<String> keyMetricValuesVariantATop = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("A variant",
					dataSet.get("keyMetrics" + i), "Top");
			keyMetricValuesVariantATop.add(values);
		}
		logger.info(
				"Values for Customers Targeted, Redeemed Net Sales, Redeemed Net AOV, Redeemed Rate, and Discount under 'Variant A' (Top) are: "
						+ keyMetricValuesVariantATop);

		// get key metrics values for Variant B Top level
		List<String> keyMetricValuesVariantBTop = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("B variant",
					dataSet.get("keyMetrics" + i), "Top");
			keyMetricValuesVariantBTop.add(values);
		}
		logger.info(
				"Values for Customers Targeted, Redeemed Net Sales, Redeemed Net AOV, Redeemed Rate, and Discount under 'Variant B' (Top) are: "
						+ keyMetricValuesVariantBTop);

		// get key metrics values for Variants Combined bottom level
		List<String> keyMetricValuesCombinedBottom = new ArrayList<>();
		for (int i = 2; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("Variants Combined",
					dataSet.get("keyMetrics" + i), "Bottom");
			keyMetricValuesCombinedBottom.add(values);
		}
		logger.info(
				"Values for Gross Sales, Redemption AOV, Redemptions, Discount Rate under 'Variants Combined' (Bottom) are: "
						+ keyMetricValuesCombinedBottom);

		// get key metrics values for Variant A bottom level
		List<String> keyMetricValuesVariantABottom = new ArrayList<>();
		for (int i = 2; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("A variant",
					dataSet.get("keyMetrics" + i), "Bottom");
			keyMetricValuesVariantABottom.add(values);
		}
		logger.info(
				"Values for Gross Sales, Redemption AOV, Redemptions, Discount Rate under 'Variant A' (Bottom) are: "
						+ keyMetricValuesVariantABottom);

		// get key metrics values for Variant B bottom level
		List<String> keyMetricValuesVariantBBottom = new ArrayList<>();
		for (int i = 2; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getCppKeyMetricsValues("B variant",
					dataSet.get("keyMetrics" + i), "Bottom");
			keyMetricValuesVariantBBottom.add(values);
		}
		logger.info(
				"Values for Gross Sales, Redemption AOV, Redemptions, Discount Rate under 'Variant B' (Bottom) are: "
						+ keyMetricValuesVariantBBottom);

		// calculate overall Customers Targeted, Redeemed Net Sales, Gross Sales,
		// Redemptions, & Discount
		// Customers Targeted, Redeemed Net Sales, Gross Sales, Redemptions, & Discount
		// overall count should be equal to variant A+B
		int customersTargeted = Integer.parseInt(keyMetricValuesVariantATop.get(0))
				+ Integer.parseInt(keyMetricValuesVariantBTop.get(0));
		Assert.assertEquals(customersTargeted, Integer.parseInt(keyMetricValuesCombinedTop.get(0)),
				"Overall Customers Targeted value not matched");
		logger.info("Verified Overall Customers Targeted value is sum of variant A and B");
		TestListeners.extentTest.get().pass("Verified Overall Customers Targeted value is sum of variant A and B");

		int redeemedNetSales = Integer.parseInt(keyMetricValuesVariantATop.get(1))
				+ Integer.parseInt(keyMetricValuesVariantBTop.get(1));
		Assert.assertEquals(redeemedNetSales, Integer.parseInt(keyMetricValuesCombinedTop.get(1)),
				"Overall Redeemed Net Sales value not matched");
		logger.info("Verified Overall Redeemed Net Sales value is sum of variant A and B");
		TestListeners.extentTest.get().pass("Verified Overall Redeemed Net Sales value is sum of variant A and B");

		int grossSales = Integer.parseInt(keyMetricValuesVariantABottom.get(0))
				+ Integer.parseInt(keyMetricValuesVariantBBottom.get(0));
		Assert.assertEquals(grossSales, Integer.parseInt(keyMetricValuesCombinedBottom.get(0)),
				"Overall Gross Sales value not matched");
		logger.info("Verified Overall Gross Sales value is sum of variant A and B");
		TestListeners.extentTest.get().pass("Verified Overall Gross Sales value is sum of variant A and B");

		int redemptions = Integer.parseInt(keyMetricValuesVariantABottom.get(2))
				+ Integer.parseInt(keyMetricValuesVariantBBottom.get(2));
		Assert.assertEquals(redemptions, Integer.parseInt(keyMetricValuesCombinedBottom.get(2)),
				"Overall Redemptions value not matched");
		logger.info("Verified Overall Redemptions value is sum of variant A and B");
		TestListeners.extentTest.get().pass("Verified Overall Redemptions value is sum of variant A and B");

		int discount = Integer.parseInt(keyMetricValuesVariantATop.get(4))
				+ Integer.parseInt(keyMetricValuesVariantBTop.get(4));
		Assert.assertEquals(discount, Integer.parseInt(keyMetricValuesCombinedTop.get(4)),
				"Overall Discount value not matched");
		logger.info("Verified Overall Discount value is sum of variant A and B");
		TestListeners.extentTest.get().pass("Verified Overall Discount value is sum of variant A and B");

		// calculate Redeemed Net AOV
		// Redeemed Net AOV should be (Gross Sales () - Discount ()) / Redemptions ()

		double redeemedNetAOV = (grossSales - discount) / (double) redemptions;
		int actualValue = (int) redeemedNetAOV;
		int expectedValue = (int) Double.parseDouble(keyMetricValuesCombinedTop.get(2));
		Assert.assertEquals(actualValue, expectedValue, "Overall Redeemed Net AOV value not matched");
		logger.info("Verified Overall Redeemed Net AOV value.");
		TestListeners.extentTest.get().pass("Verified Overall Redeemed Net AOV value.");

		// calculate Redemption AOV
		// (Gross Sales () / Redemptions () = Redeemed Net AOV ()
		double redemptionAOV = grossSales / (double) redemptions;
		double redemptionAOVnew = Math.round(redemptionAOV * 100.0) / 100.0;
		Assert.assertEquals(redemptionAOVnew, Double.parseDouble(keyMetricValuesCombinedBottom.get(1)),
				"Overall Redemption AOV value not matched");
		logger.info("Verified Overall Redemption AOV value matched");
		TestListeners.extentTest.get().pass("Verified Overall Redemption AOV value matched");

		// calculate Discount Rate
		// Discount () / Gross Sales () = Discount Rate %
		double discountRate = ((double) discount / grossSales) * 100;
		double discountRateNew = Math.round(discountRate * 10.0) / 10.0;
		Assert.assertEquals(discountRateNew, Double.parseDouble(keyMetricValuesCombinedBottom.get(3)),
				"Overall Discount Rate value not matched");
		logger.info("Verified Overall Discount Rate value matched");
		TestListeners.extentTest.get().pass("Verified Overall Discount Rate value matched");

		// calculate Redeemed Rate
		// Redemptions / Customers Targeted () = Redeemed Rate%
		double redeemedRate = ((double) redemptions / customersTargeted) * 100;
		double redeemedRateNew = Math.round(redeemedRate * 100.0) / 100.0;

		Assert.assertEquals(redeemedRateNew, Double.parseDouble(keyMetricValuesCombinedTop.get(3)),
				"Overall Redeemed Rate value not matched");
		logger.info("Verified Overall Redeemed Rate value matched");
		TestListeners.extentTest.get().pass("Verified Overall Redeemed Rate value matched");

	}

	// Rakhi
	@Test(description = "SQ-T6448 Verify Campaign Audience vs Control Group showing population size %."
			+ " SQ-T6828 Verify Campaign Audience & Control Group implementation for Split Test Campaigns")
	@Owner(name = "Rakhi Rawat")
	public void T6448_VerifyCampaignAudienceVsControlGroup() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignId"));
		pageObj.newCamHomePage().clickOnFirstCampaign();

		pageObj.campaignPerformancePage().expandCampaignSummary();
		utils.longWaitInSeconds(4);
		// verify combined segment details
		String variantAsegSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Variant A");
		String variantBsegSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Variant B");

		int expCampaignAudiencePopulationSize = Integer.parseInt(variantAsegSize) + Integer.parseInt(variantBsegSize);

		List<String> expectedList = Arrays.asList("CONTROL GROUP", "CAMPAIGN AUDIENCE", "VARIANT A", "VARIANT B");
		List<String> campAudienceColumnList = pageObj.campaignPerformancePage().getCampaignAudienceVsControlGroup();
		Assert.assertEquals(campAudienceColumnList, expectedList,
				"Expected Campaign Audienece Vs Control Group column elements not matched");
		logger.info("Campaign Audienece Vs Control Group column elements matched");
		TestListeners.extentTest.get().pass("Campaign Audienece Vs Control Group column elements matched");

		String controlGroupPopulationValue = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("CONTROL GROUP", "1", "value");
		String campaignAudiencePopulationValue = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("CAMPAIGN AUDIENCE", "2", "value");
		String VariantAPopulationValue = pageObj.campaignPerformancePage().getPopulationSizeAndPercentage("VARIANT A",
				"3", "value");
		String VariantBPopulationSizeValue = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("VARIANT B", "4", "value");

		String controlGroupPopulationPercentage = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("CONTROL GROUP", "1", "percentage");
		String campaignAudiencePopulationPercentage = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("CAMPAIGN AUDIENCE", "2", "percentage");
		String VariantAPopulationPercentage = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("VARIANT A", "3", "percentage");
		String VariantBPopulationSizePercentage = pageObj.campaignPerformancePage()
				.getPopulationSizeAndPercentage("VARIANT B", "4", "percentage");

		// campaign audience should be variant A + variant B
		Assert.assertEquals(Integer.parseInt(campaignAudiencePopulationValue), expCampaignAudiencePopulationSize,
				"CAMPAIGN AUDIENCE size does not matched");

		// Control Group %
		double expControlGroup = Double.parseDouble(controlGroupPopulationValue)
				/ (Double.parseDouble(controlGroupPopulationValue)
						+ Double.parseDouble(campaignAudiencePopulationValue));
		long expControlGroupNew = Math.round(expControlGroup * 100);
		Assert.assertEquals(expControlGroupNew, Double.parseDouble(controlGroupPopulationPercentage),
				"Control Group % did not matched");
		logger.info("Verified Control Group % matched ie : " + expControlGroupNew);
		TestListeners.extentTest.get().pass("Verified Control Group % matched ie : " + expControlGroupNew);

		// Campaign Audience %
		double expCampaignAudience = Double.parseDouble(campaignAudiencePopulationValue)
				/ (Double.parseDouble(controlGroupPopulationValue)
						+ Double.parseDouble(campaignAudiencePopulationValue));
		long expCampaignAudienceNew = Math.round(expCampaignAudience * 100);
		Assert.assertEquals(expCampaignAudienceNew, Double.parseDouble(campaignAudiencePopulationPercentage),
				"Campaign Audience % did not matched");
		logger.info("Verified Campaign Audience % matched ie : " + expCampaignAudienceNew);
		TestListeners.extentTest.get().pass("Verified Campaign Audience % matched ie : " + expCampaignAudienceNew);

		// Variant A %
		double expVariantA = Double.parseDouble(VariantAPopulationValue)
				/ (Double.parseDouble(controlGroupPopulationValue)
						+ Double.parseDouble(campaignAudiencePopulationValue));
		long expVariantANew = Math.round(expVariantA * 100);
		Assert.assertEquals(expVariantANew, Double.parseDouble(VariantAPopulationPercentage),
				"Variant A %  did not matched");
		logger.info("Verified Variant A % matched ie : " + expVariantANew);
		TestListeners.extentTest.get().pass("Verified Variant A % matched ie : " + expVariantANew);

		// Variant B %
		double expVariantB = Double.parseDouble(VariantBPopulationSizeValue)
				/ (Double.parseDouble(controlGroupPopulationValue)
						+ Double.parseDouble(campaignAudiencePopulationValue));
		long expVariantBNew = Math.round(expVariantB * 100);
		Assert.assertEquals(expVariantBNew, Double.parseDouble(VariantBPopulationSizePercentage),
				"Variant B % did not matched");
		logger.info("Verified Variant B %  matched ie : " + expVariantBNew);
		TestListeners.extentTest.get().pass("Verified Variant B %  matched ie : " + expVariantBNew);

	}

	// Rakhi
	@Test(description = "SQ-T6525 Verify Inactive tag on deactivated Campaign")
	@Owner(name = "Rakhi Rawat")
	public void T6525_VerifyInactiveTag() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// apply filter for deactivated campaigns
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel("Inactive", "Status");
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().clickOnFirstCampaign();
		boolean flag = pageObj.campaignPerformancePage().verifyInactiveTagVisibility();
		Assert.assertTrue(flag, "Inactive tag is not displayed for Deactivated campaigns");
		logger.info("Verified Inactive tag is displayed for Deactivated campaigns");
		TestListeners.extentTest.get().pass("Verified Inactive tag is displayed for Deactivated campaigns");

	}

	// Rakhi
	@Test(description = "SQ-T6532 Verify Campaign Sales metrics presence")
	@Owner(name = "Rakhi Rawat")
	public void T6532_VerifyCampaignSalesMetrics() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		for (int i = 1; i < 3; i++) {
			pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl" + i));
			List<String> keyMetrics = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
			Assert.assertFalse(keyMetrics.contains("Campaign Sales"),
					"Campaign Sales metrics is present for recall campaign with incentivize checkin");
		}
		logger.info("Verified Campaign Sales metrics is not present for recall campaign with incentivize checkin");
		TestListeners.extentTest.get()
				.pass("Verified Campaign Sales metrics is not present for recall campaign with incentivize checkin");
	}

	// Rakhi
	@Test(description = "SQ-T6575 Campaign links redirected to new cpp not the classic cpp")
	@Owner(name = "Rakhi Rawat")
	public void T6575_VerifyCampaignLinksRedirectedToNewCpp() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// check for campaign links for Redeemables
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().searchRedeemable(dataSet.get("redeemableName"));
		utils.longWaitInSeconds(3);
		pageObj.redeemablePage().selectRedeemableEllipsisOption(dataSet.get("redeemableName"), "Associations");
		pageObj.campaignPerformancePage().openCampaignLinkFromAssociation();
		// verify that it is redirected to new cpp
		String campaignURL = pageObj.superadminMassCampaignPage().getCampaignURL();
		Assert.assertTrue(campaignURL.contains("mode=new&"),
				"Campaign link did not redirected to new Campaign Performance Page for Redeemables");
		logger.info("Verified Campaign link redirected to new Campaign Performance Page for Redeemables");
		TestListeners.extentTest.get()
				.pass("Verified Campaign link redirected to new Campaign Performance Page for Redeemables");

		// check for campaign links for Surveys
		pageObj.menupage().navigateToSubMenuItem("Settings", "Surveys");
		pageObj.surveysPage().selectSurveys(dataSet.get("surveyName"));
		pageObj.dashboardpage().navigateToTabsNew("Associations");
		pageObj.campaignPerformancePage().openCampaignLinkFromAssociation();
		// verify that it is redirected to new cpp
		String campaignURL1 = pageObj.superadminMassCampaignPage().getCampaignURL();
		Assert.assertTrue(campaignURL1.contains("mode=new&"),
				"Campaign link did not redirected to new Campaign Performance Page for Surveys");
		logger.info("Verified Campaign link redirected to new Campaign Performance Page for Surveys");
		TestListeners.extentTest.get()
				.pass("Verified Campaign link redirected to new Campaign Performance Page for Surveys");

		// check for campaign links for QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(dataSet.get("qcName"));
		pageObj.dashboardpage().navigateToTabs("Associations");
		pageObj.campaignPerformancePage().openCampaignLinkFromAssociation();
		// verify that it is redirected to new cpp
		String campaignURL2 = pageObj.superadminMassCampaignPage().getCampaignURL();
		Assert.assertTrue(campaignURL2.contains("mode=new&"),
				"Campaign link did not redirected to new Campaign Performance Page for Qualification Criteria");
		logger.info("Verified Campaign link redirected to new Campaign Performance Page for Qualification Criteria");
		TestListeners.extentTest.get()
				.pass("Verified Campaign link redirected to new Campaign Performance Page for Qualification Criteria");

	}

	// Rakhi
	@Test(description = "SQ-T6581 When isDataPresent is false then loader is keep loading in the aggregate page")
	@Owner(name = "Rakhi Rawat")
	public void T6581_VerifyAggregatePerformancePageWhenDataIsNotPresent() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		for (int i = 1; i <= 4; i++) {
			pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl" + i));
			String nextRefresh = pageObj.campaignPerformancePage()
					.getLastAndNextRefreshDate(dataSet.get("nextRefresh"));
			String lastRefresh = pageObj.campaignPerformancePage()
					.getLastAndNextRefreshDate(dataSet.get("lastRefresh"));
			Assert.assertTrue(nextRefresh.contains("Pending") && lastRefresh.contains("Pending"),
					"Next and Last refresh date is not showing as Pending");
			// click on Aggregate Performance
			pageObj.campaignPerformancePage().clickAggregatePerformanceLink();
			boolean flag = pageObj.campaignPerformancePage().verifyAggregatePerformancePageMsg();
			Assert.assertTrue(flag, "Aggregate Performance page message is not displayed");
		}
		logger.info(
				"Verified when isDataPresent is false then loader is not loading in the aggregate page and showing expected 'No results found' message");
		TestListeners.extentTest.get().pass(
				"Verified when isDataPresent is false then loader is not loading in the aggregate page and showing expected 'No results found' message");
	}

	// Rakhi
	@Test(description = "SQ-T6583 Verify Campaign Performance data is loading")
	@Owner(name = "Rakhi Rawat")
	public void T6583_VerifyCampaignPerformanceDataLoading() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify for Promo campaign
		for (int i = 1; i <= 4; i++) {
			pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("promoCampaignUrl" + i));
			pageObj.campaignPerformancePage().expandCampaignPerformanceSection();
			boolean flag = pageObj.campaignPerformancePage().verifyCampaignPerformanceSection();
			utils.longWaitInSeconds(1);
			Assert.assertTrue(flag, "Campaign Performance data is not loading for Promo Campaign");
		}
		logger.info("Verified Campaign Performance data is loading for Promo Campaigns");
		TestListeners.extentTest.get().pass("Verified Campaign Performance data is loading for Promo Campaigns");

		// verify for Coupon campaign
		for (int i = 1; i <= 6; i++) {
			pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("couponCampaignUrl" + i));
			pageObj.campaignPerformancePage().expandCampaignPerformanceSection();
			boolean flag = pageObj.campaignPerformancePage().verifyCampaignPerformanceSection();
			utils.longWaitInSeconds(1);
			Assert.assertTrue(flag, "Campaign Performance data is not loading for Coupon Campaigns");
		}
		logger.info("Verified Campaign Performance data is loading for Coupon Campaigns");
		TestListeners.extentTest.get().pass("Verified Campaign Performance data is loading for Coupon Campaigns");

	}

	// Rakhi
	@Test(description = "SQ-T6590 check mass notification campaign aggregate performance page")
	@Owner(name = "Rakhi Rawat")
	public void T6590_VerifyAggregatePerformancePageForMassNotification() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl"));
		// click on Aggregate Performance
		pageObj.campaignPerformancePage().clickAggregatePerformanceLink();
		List<String> expectedColumns = Arrays.asList("Customers Targeted", "Conversion", "Push Noti. Sent",
				"Push Noti. Open");
		List<String> excludedColumns = Arrays.asList("Redeemed Net Sales", "Gross Sales", "Redemption Net AOV",
				"Redemption AOV", "Redemptions", "Discount", "Rewards Disbursed", "Points Disbursed",
				"Currency Disbursed");
		List<String> actualColumns = pageObj.campaignPerformancePage().getAggregatePageKeyMetrics();
		logger.info("Aggregate page key metrics columns: " + actualColumns);

		// verifying only expected columns are present
		for (String col : expectedColumns) {
			Assert.assertTrue(actualColumns.contains(col), "Missing expected column: " + col);
		}
		// verifying excluded columns are not present
		for (String col : excludedColumns) {
			Assert.assertFalse(actualColumns.contains(col),
					"Column should not be visible for mass notification: " + col);
		}
		logger.info(
				"Verified for Mass Notification campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Points Disbursed, Currency Disbursed are not present in Aggregate Performance page");
		TestListeners.extentTest.get().pass(
				"Verified for Mass Notification campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Points Disbursed, Currency Disbursed are not present in Aggregate Performance page");
	}

	// Rakhi
	@Test(description = "SQ-T6591 check mass offer campaign(currency) aggregate performance page")
	@Owner(name = "Rakhi Rawat")
	public void T6591_VerifyAggregatePerformancePageForMassGiftingCurrency() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl"));
		// click on Aggregate Performance
		pageObj.campaignPerformancePage().clickAggregatePerformanceLink();
		List<String> expectedColumns = Arrays.asList("Customers Targeted", "Conversion", "Currency Disbursed",
				"Push Noti. Sent", "Push Noti. Open");
		List<String> excludedColumns = Arrays.asList("Redeemed Net Sales", "Gross Sales", "Redemption Net AOV",
				"Redemption AOV", "Redemptions", "Discount", "Rewards Disbursed", "Points Disbursed");
		List<String> actualColumns = pageObj.campaignPerformancePage().getAggregatePageKeyMetrics();
		logger.info("Aggregate page key metrics: " + actualColumns);

		// verifying only expected columns are present
		for (String col : expectedColumns) {
			Assert.assertTrue(actualColumns.contains(col), "Missing expected column: " + col);
		}
		// verifying excluded columns are not present
		for (String col : excludedColumns) {
			Assert.assertFalse(actualColumns.contains(col),
					"Column should not be visible for mass gifting currency: " + col);
		}
		logger.info(
				"Verified for Mass Gifting Currency campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Points Disbursed are not present in Aggregate Performance page");
		TestListeners.extentTest.get().pass(
				"Verified for Mass Gifting Currency campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Points Disbursed are not present in Aggregate Performance page");
	}

	// Rakhi
	@Test(description = "SQ-T6598 Validate that campaign logs is visible for Mass offer campaign Long with gift type Points"
			+ "SQ-T6600 Validate that campaign logs is visible for Mass offer campaign Short with gift type Redeemable"
			+ "SQ-T6599 Validate that campaign logs is visible for Mass offer campaign Long with gift type Redeemable")
	@Owner(name = "Rakhi Rawat")
	public void T6598_VerifyCampaignLogs() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify campaign logs for mass gifting points Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("massOfferPointsLong"));
		String logParam = pageObj.campaignPerformancePage().verifyCampaignLogsOnNewCpp();
		Assert.assertTrue(logParam.contains("MassGiftingWorker on_complete"),
				"Campaign logs are not present for Mass Gifting Points Long Campaign");
		logger.info("Verified Campaign logs are present for Mass Gifting Points Long Campaign");
		TestListeners.extentTest.get().pass("Verified Campaign logs are present for Mass Gifting Points Long Campaign");

		// verify campaign logs for mass gifting Redeemable Short
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("massOfferRedeemableShort"));
		String logParam1 = pageObj.campaignPerformancePage().verifyCampaignLogsOnNewCpp();
		Assert.assertTrue(logParam1.contains("MassGiftingWorker on_complete"),
				"Campaign logs are not present for Mass Gifting Redeemable Short Campaign");
		logger.info("Verified Campaign logs are present for Mass Gifting Redeemable Short Campaign");
		TestListeners.extentTest.get()
				.pass("Verified Campaign logs are present for Mass Gifting Redeemable Short Campaign");

		// verify campaign logs for mass gifting Redeemable Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("massOfferRedeemableLong"));
		String logParam2 = pageObj.campaignPerformancePage().verifyCampaignLogsOnNewCpp();
		Assert.assertTrue(logParam2.contains("MassGiftingWorker on_complete"),
				"Campaign logs are not present for Mass Gifting Redeemable Long Campaign");
		logger.info("Verified Campaign logs are present for Mass Gifting Redeemable Long Campaign");
		TestListeners.extentTest.get()
				.pass("Verified Campaign logs are present for Mass Gifting Redeemable Long Campaign");
	}

	// Rakhi
	@Test(description = "SQ-T6597 check mass offer campaign(points) aggregate performance page")
	@Owner(name = "Rakhi Rawat")
	public void T6597_VerifyAggregatePerformancePageForMassGiftingPoints() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl"));
		// click on Aggregate Performance
		pageObj.campaignPerformancePage().clickAggregatePerformanceLink();
		List<String> expectedColumns = Arrays.asList("Customers Targeted", "Conversion", "Points Disbursed",
				"Push Noti. Sent", "Push Noti. Open");
		List<String> excludedColumns = Arrays.asList("Redeemed Net Sales", "Gross Sales", "Redemption Net AOV",
				"Redemption AOV", "Redemptions", "Discount", "Rewards Disbursed", "Currency Disbursed");
		List<String> actualColumns = pageObj.campaignPerformancePage().getAggregatePageKeyMetrics();
		logger.info("Aggregate page key metrics: " + actualColumns);

		// verifying only expected columns are present
		for (String col : expectedColumns) {
			Assert.assertTrue(actualColumns.contains(col), "Missing expected column: " + col);
		}
		// verifying excluded columns are not present
		for (String col : excludedColumns) {
			Assert.assertFalse(actualColumns.contains(col),
					"Column should not be visible for mass gifting points : " + col);
		}
		logger.info(
				"Verified for Mass Gifting Points campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Currency Disbursed are not present in Aggregate Performance page");
		TestListeners.extentTest.get().pass(
				"Verified for Mass Gifting Points campaign Redeemed Net Sales, Gross Sales, Redemption Net AOV, Redemption AOV, Redemptions, Discount, Rewards Disbursed, Currency Disbursed are not present in Aggregate Performance page");
	}

	// Rakhi
	@Test(description = "SQ-T6602 Validate campaign logs are visible for Split Test campaigns", dataProvider = "TestDataProvider2")
	@Owner(name = "Rakhi Rawat")
	public void T6602_ValidateCampaignLogsForSplitCampaign(String campaignId) throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// verify campaign logs for Split campaign
		pageObj.newCamHomePage().searchCampaign(campaignId);
		pageObj.newCamHomePage().clickOnFirstCampaign();
		String logParam = pageObj.campaignPerformancePage().verifyCampaignLogsOnNewCpp();
		Assert.assertTrue(logParam.contains("MassGiftingWorker on_complete"),
				"Campaign logs are not present for Split Test Campaign : " + campaignId);
		logger.info("Verified Campaign logs are present for Split Test Campaign : " + campaignId);
		TestListeners.extentTest.get()
				.pass("Verified Campaign logs are present for Split Test Campaign : " + campaignId);

	}

	// Rakhi
	// this campaign is planned to be deprecated from campaigns SQ-1959
	// @Test(description = "SQ-T6606 Validate that campaign logs is visible for AB
	// campaign")
	@Owner(name = "Rakhi Rawat")
	public void T6606_VerifyCampaignLogsForABcampaign() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to Campaign Performance Page
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl"));
		String logParam = pageObj.campaignspage().verifyCampaignLogs();
		Assert.assertTrue(logParam.contains("Parameters"), "Campaign logs are not present for A/B Campaign");
		logger.info("Verified Campaign logs are present for A/B Campaign");
		TestListeners.extentTest.get().pass("Verified Campaign logs are present for A/B Campaign");

	}

	// Rakhi
	@Test(description = "SQ-T6605 Display recent & upcoming date/time stamps in business time zone in CPP")
	@Owner(name = "Rakhi Rawat")
	public void T6605_VerifyDateTimeStampsOnCPP() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to Campaign Performance Page
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl"));
		boolean flag = pageObj.campaignPerformancePage().validateDateTimeFormat(dataSet.get("lastRefresh"));
		Assert.assertTrue(flag, "Date/Time format is not matching On Campaign Performance Page");
		logger.info("Verified Date/Time format is matching On Campaign Performance Page as expected");
		TestListeners.extentTest.get()
				.pass("Verified Date/Time format is matching On Campaign Performance Page as expected");
	}

	// Rakhi
	@Test(description = "SQ-T6608 Signups via Referrals option is not visible for Long Referral Campaign with point gift type"
			+ "SQ-T6607 Signups via Referrals option is not visible for Long Referral Campaign with Surprise Set gift type"
			+ "SQ-T6613 Signups via Referrals option is not visible for Long Referral Campaign with Redeemable gift type"
			+ "SQ-T6817 Signups via Referrals option is not visible for Long Referral Campaign with Currency gift type")
	@Owner(name = "Rakhi Rawat")
	public void T6608_VerifySignupViaReferralsOptionPresence() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify Signups via Referrals option for Referral Campaign with Points Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("referralCampaignPoints"));
		List<String> options1 = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		Assert.assertFalse(options1.contains("Signup via Referrals"),
				"Signups via Referrals option is present for Long Referral Campaign with Points");
		logger.info("Verified Signups via Referrals option is not present for Long Referral Campaign with Points");
		TestListeners.extentTest.get()
				.pass("Verified Signups via Referrals option is not present for Long Referral Campaign with Points");

		// verify Signups via Referrals option for Referral Campaign with Surprise Set
		// Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("referralCampaignSurpriseSet"));
		List<String> options2 = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		Assert.assertFalse(options2.contains("Signup via Referrals"),
				"Signups via Referrals option is present for Long Referral Campaign with Surprise Set");
		logger.info(
				"Verified Signups via Referrals option is not present for Long Referral Campaign with Surprise Set");
		TestListeners.extentTest.get().pass(
				"Verified Signups via Referrals option is not present for Long Referral Campaign with Surprise Set");

		// verify Signups via Referrals option for Referral Campaign with Redeemable
		// Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("referralCampaignRedeemable"));
		List<String> options3 = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		Assert.assertFalse(options3.contains("Signup via Referrals"),
				"Signups via Referrals option is present for Long Referral Campaign with Redeemable");
		logger.info("Verified Signups via Referrals option is not present for Long Referral Campaign with Redeemable");
		TestListeners.extentTest.get().pass(
				"Verified Signups via Referrals option is not present for Long Referral Campaign with Redeemable");

		// verify Signups via Referrals option for Referral Campaign with Currency
		// Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("referralCampaignCurrency"));
		List<String> options4 = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		Assert.assertFalse(options4.contains("Signup via Referrals"),
				"Signups via Referrals option is present for Long Referral Campaign with Currency");
		logger.info("Verified Signups via Referrals option is not present for Long Referral Campaign with Currency");
		TestListeners.extentTest.get()
				.pass("Verified Signups via Referrals option is not present for Long Referral Campaign with Currency");
	}

	// Rakhi
	@Test(description = "SQ-T6614 Label for Long Post Checkin Campaign for Currency gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6617 Label for Long Post Checkin Campaign for Fuel gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6618 Label for Long Post Checkin Campaign for Points gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6619 Label for Long Post Checkin Campaign for Redeemable gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6620 Label for Long Post Checkin Campaign for surprise set gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary")
	@Owner(name = "Rakhi Rawat")
	public void T6614_VerifyLabelForPostCheckinCampaign() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to Campaign Performance Page
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("postCheckinCurrencyLong"));
		String title = pageObj.campaignPerformancePage().getCampaignSummaryTitle();
		Assert.assertTrue(title.contains("Post Checkin Campaign"),
				"Label for Long Post Checkin Campaign for Currency gift type does not change from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		logger.info(
				"Verified Label for Long Post Checkin Campaign for Currency gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		TestListeners.extentTest.get().pass(
				"Verified Label for Long Post Checkin Campaign for Currency gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");

		// verify for Long Post Checkin Campaign with Fuel gift
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("postCheckinFuelLong"));
		String title1 = pageObj.campaignPerformancePage().getCampaignSummaryTitle();
		Assert.assertTrue(title1.contains("Post Checkin Campaign"),
				"Label for Long Post Checkin Campaign with Fuel gift type does not change from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		logger.info(
				"Verified Label for Long Post Checkin Campaign with Fuel gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		TestListeners.extentTest.get().pass(
				"Verified Label for Long Post Checkin Campaign with Fuel gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		TestListeners.extentTest.get().pass(
				"Verified Label for Long Post Checkin Campaign with Fuel gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");

		// verify Long Post Checkin Campaign with Points gift type
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("postCheckinPointsLong"));
		String title2 = pageObj.campaignPerformancePage().getCampaignSummaryTitle();
		Assert.assertTrue(title2.contains("Post Checkin Campaign"),
				"Label for Long Post Checkin Campaign with Points gift type does not change from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		logger.info(
				"Verified Label for Long Post Checkin Campaign with Points gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		TestListeners.extentTest.get().pass(
				"Verified Label for Long Post Checkin Campaign with Points gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");

		// verify Long Post Checkin Campaign with Surprise Set gift type
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("postCheckinSurpriseSetLong"));
		String title3 = pageObj.campaignPerformancePage().getCampaignSummaryTitle();
		Assert.assertTrue(title3.contains("Post Checkin Campaign"),
				"Label for Long Post Checkin Campaign with Surprise Set gift type does not change from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		logger.info(
				"Verified Label for Long Post Checkin Campaign with Surprise Set gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");
		TestListeners.extentTest.get().pass(
				"Verified Label for Long Post Checkin Campaign with Surprise Set gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary");

	}

	// Rakhi
	@Test(description = "SQ-T6625 Label for Short Post Checkin Campaign for Currency gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6626 Label for Short Post Checkin Campaign for Fuel gift type changes from Free Punchh Campaign to Post Checkin Campaign beside Summary"
			+ "SQ-T6627 Label for Short Post Checkin Campaign for surprise set gift type changes from Free Punchh Campaign to Post Checkin Campaign"
			+ "SQ-T6628 The Label for Short Post Checkin Campaign for Points gift type changes from Free Punchh Campaign to Post Checkin Campaign"
			+ "SQ-T6629 The label for the Short Post Checkin Campaign is updated to reflect the change in redeemable gift type from a Free Punchh Campaign to a Post Checkin Campaign")
	@Owner(name = "Rakhi Rawat")
	public void T6625_VerifyLabelForPostCheckinCampaignShort() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// verify for Short Post Checkin Campaign with Currency, Fuel, Points, Surprise
		// Set and Redeemable Gift type Respectively
		for (int i = 1; i < 6; i++) {
			pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("postCheckinShort" + i));
			String title = pageObj.campaignPerformancePage().getCampaignSummaryTitle();
			Assert.assertTrue(title.contains("Post Checkin Campaign"),
					"Label for Short Post Checkin Campaign does not change from Free Punchh Campaign to Post Checkin Campaign beside Summary for gift type: "
							+ dataSet.get("postCheckinShort" + i));
			logger.info(
					"Verified Label for Short Post Checkin Campaign changes from Free Punchh Campaign to Post Checkin Campaign beside Summary for gift type: "
							+ dataSet.get("postCheckinShort" + i));
			TestListeners.extentTest.get().pass(
					"Verified Label for Short Post Checkin Campaign changes from Free Punchh Campaign to Post Checkin Campaign beside Summary for gift type: "
							+ dataSet.get("postCheckinShort" + i));

		}

	}

	// Rakhi
	@Test(description = "SQ-T6787 verify Campaign Performance for Long Split Test Campaigns")
	@Owner(name = "Rakhi Rawat")
	public void T6787_VerifyCampaignPerformanceForLongSplitTestCampaign() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignId"));
		pageObj.newCamHomePage().clickOnFirstCampaign();

		List<String> expectedKeyMetricsList = Arrays.asList("Redemption Sales", "Customers Targeted",
				"Customers Redeemed", "Redemptions", "Visits");

		// verify campaign performance metrics for Variant A
		pageObj.campaignPerformancePage().expandCampaignPerformanceSection();
		List<String> keyMetricsListVariantA = pageObj.campaignPerformancePage().getSplitCampaignPerformanceKeyMetrics();
		logger.info("actual Campaign Performance Key metrics: " + keyMetricsListVariantA);
		Assert.assertEquals(keyMetricsListVariantA, expectedKeyMetricsList,
				"Campaign Performance Key metrics not matched for Variants A");

		// switch to variant B tab
		pageObj.campaignPerformancePage().switchSplitVariantsTab("Campaign Performance", "VariantB");
		// verify campaign performance metrics for Variant B
		List<String> keyMetricsListVariantB = pageObj.campaignPerformancePage().getSplitCampaignPerformanceKeyMetrics();
		Assert.assertEquals(keyMetricsListVariantB, expectedKeyMetricsList,
				"Campaign Performance Key metrics not matched for Variants B");

		logger.info("Verified Campaign Performance Key metrics for Long Split Test Variants A and B");
		TestListeners.extentTest.get()
				.pass("Verified Campaign Performance Key metrics for Long Split Test Variants A and B");

	}

	// Rakhi
	@Test(description = "SQ-T6806 Verify Referral Campaign Key metrics."
			+ "SQ-T6794 Verify Campaign Performance should displayed when toggling for a Referral Currency Long Campaign"
			+ "SQ-T6822 Verify Referral campaign data loading.")
	@Owner(name = "Rakhi Rawat")
	public void T6806_VerifyReferralCampaignKeyMetrics() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignId"));
		pageObj.newCamHomePage().clickOnFirstCampaign();

		// verify data loading for Referral currency campaign long
		List<String> expectedKeyMetrics = Arrays.asList("Total Currency Gifted", "Referrers Qualified",
				"Successful Referrals");
		List<String> excludedKeyMetrics = Arrays.asList("Campaign Sales", "Campaign AOV", "Net Sales", "Gross Sales",
				"Redeemed Net AOV", "Redemption AOV", "Redeemed Rate", "Discount", "Discount Rate", "% of Redemption");
		List<String> actualKeyMetrics = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		logger.info("Actual key metrics: " + actualKeyMetrics);

		// verifying only expected key metrics are present
		for (String col : expectedKeyMetrics) {
			Assert.assertTrue(actualKeyMetrics.contains(col), "Missing expected Key metrics: " + col);
		}
		// verifying excluded key metrics are not present
		for (String col : excludedKeyMetrics) {
			Assert.assertFalse(actualKeyMetrics.contains(col),
					"Key metrics should not be visible for Referral Currency Campaign : " + col);
		}
		logger.info(
				"Verified for Referral Currency Campaign Campaign Sales , Campaign AOV , Net Sales , Gross Sales , Redeemed Net AOV , Redemption AOV, Redeemed Rate , Discount ,Discount Rate , % of Redemption , these metrics are not visible");
		TestListeners.extentTest.get().pass(
				"Verified for Referral Currency Campaign Campaign Sales , Campaign AOV , Net Sales , Gross Sales , Redeemed Net AOV , Redemption AOV, Redeemed Rate , Discount ,Discount Rate , % of Redemption , these metrics are not visible");

		// verify toggling for campaign performance section
		pageObj.campaignPerformancePage().expandCampaignPerformanceSection();
		boolean flag = pageObj.campaignPerformancePage().verifyCampaignPerformanceSection();
		Assert.assertTrue(flag, "Campaign Performance data is not loading for Referral Currency Long Campaign");
		logger.info(
				"Verified Campaign Performance section is displayed when toggling for a Referral Currency Long Campaign");
		TestListeners.extentTest.get().pass(
				"Verified Campaign Performance section is displayed when toggling for a Referral Currency Long Campaign");

		// verify data loading for Referral Redeemable Campaign Long
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("referralRedeemable"));
		List<String> expectedKeyMetrics1 = Arrays.asList("Redeemed Net Sales", "Campaign Sales", "Redeemed Net AOV",
				"Redeemed Rate", "Discount", "Referrers Qualified", "Successful Referrals");
		List<String> actualKeyMetrics1 = pageObj.campaignPerformancePage().getNonSplitCppKeyMetrics();
		logger.info("Actual key metrics: " + actualKeyMetrics1);

		Assert.assertEquals(actualKeyMetrics1, expectedKeyMetrics1,
				"Campaign data is not loading for Referral Redeemable Long Campaign");
		logger.info("Verified Campaign data is loading for Referral Redeemable Long Campaign");
		TestListeners.extentTest.get().pass("Verified Campaign data is loading for Referral Redeemable Long Campaign");

	}

	// Rakhi
	@Test(description = "SQ-T6829 Verify control group for split campaign")
	@Owner(name = "Rakhi Rawat")
	public void T6829_VerifySplitTestControlGroupSection() throws InterruptedException {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignId"));
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().clickOnFirstCampaign();
		pageObj.campaignPerformancePage().expandCampaignSummary();
		utils.longWaitInSeconds(4);
		// get control group size from campaign summary
		String controlGroupSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Control group");

		// verify control group section
		List<String> expectedKeyMetrics = Arrays.asList("Customers Targeted", "Net Sales", "Net AOV");
		List<String> actualKeyMetrics = pageObj.campaignPerformancePage().getSplitTestControlGroupMetrics();
		logger.info("Actual key metrics: " + actualKeyMetrics);
		Assert.assertEquals(actualKeyMetrics, expectedKeyMetrics,
				"Control Group Key metrics not matched for Variants for split test campaign");
		logger.info("Verified control group key metrics for split test campaign");
		TestListeners.extentTest.get().pass("Verified control group key metrics for split test campaign");

		// verify control group size matches with Customers Targeted in Control Group
		// Section
		String value = pageObj.campaignPerformancePage().getSplitTestControlGroupMetricsValue("Customers Targeted");
		Assert.assertEquals(value, controlGroupSize,
				"Control Group size did not match with Customers Targeted in Control Group Section");
		logger.info("Verified Control Group size matched with Customers Targeted in Control Group Section");
		TestListeners.extentTest.get()
				.pass("Verified Control Group size matched with Customers Targeted in Control Group Section");
	}

	// Rakhi
	@Test(description = "SQ-T6421 Verify Campaign summary for segment combined details", dataProvider = "TestDataProvider1")
	@Owner(name = "Rakhi Rawat")
	public void T6421_VerifyCampaignSummaryForSegmentCombinedDetails(String campId, String segmentSize,
			String variantASize, String variantBSize, String controlGroup, String segmentName, String campaignIds,
			String channels) throws InterruptedException {

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(campId);
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().clickOnFirstCampaign();
		pageObj.campaignPerformancePage().expandCampaignSummary();
		utils.longWaitInSeconds(4);
		// verify combined segment details
		String parentSegmentSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Segment Size");
		String variantAsegSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Variant A");
		String variantBsegSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Variant B");
		String controlGroupSize = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Control group");

		int expectedSegmentSize = Integer.parseInt(variantAsegSize) + Integer.parseInt(variantBsegSize)
				+ Integer.parseInt(controlGroupSize);
		Assert.assertEquals(Integer.parseInt(parentSegmentSize), expectedSegmentSize,
				"Segment size in Campaign Summary is not equal to sum of Variant A, Variant B and Control Group sizes");
		logger.info("Verified Campaign summary for segment combined details");
		TestListeners.extentTest.get().pass("Verified Campaign summary for segment combined details");

		// verify campaign summary details
		List<String> campSummaryData = pageObj.campaignPerformancePage().getCampaignSummaryDetails();
		Assert.assertTrue(campSummaryData.get(0).contains(segmentSize),
				"Segment size is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(variantASize),
				"Variant A size is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(variantBSize),
				"Variant B size is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(controlGroup),
				"Control group size is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(segmentName),
				"Campaign ID is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(campaignIds),
				"Campaign ID is not present in Campaign Summary");
		Assert.assertTrue(campSummaryData.get(0).contains(channels), "Channels did not matched in Campaign Summary");
		logger.info("Verified Campaign summary details for Split Test campaign " + campId);
		TestListeners.extentTest.get().pass("Verified Campaign summary details for Split Test campaign " + campId);

	}

	// Rakhi
	@Test(description = "SQ-T6859 Verify Segment Size and Control Group When Navigating Between Campaigns in Same Tab")
	@Owner(name = "Rakhi Rawat")
	public void T6859_VerifySegmentSizeAndControlGroupWhenNavigatingBetweenCampaigns() throws InterruptedException {
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl1"));
		pageObj.campaignPerformancePage().expandCampaignSummary();

		// navigate to another campaign
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl2"));
		pageObj.campaignPerformancePage().expandCampaignSummary();
		// get segment size and control group size for new campaign
		String campTwoSegSize1 = pageObj.campaignPerformancePage().getSegmentSizeFromCampaignSummary("Segment Size");
		String campTwoControlGroupSize1 = pageObj.campaignPerformancePage()
				.getSegmentSizeFromCampaignSummary("Control group");
		logger.info("Campaign 2 Control Group Size: " + campTwoControlGroupSize1);

		Assert.assertEquals(campTwoSegSize1, dataSet.get("actualSegSize"),
				"Segment Size did not match with actual size when navigating between campaigns in same tab");
		Assert.assertEquals(campTwoControlGroupSize1, dataSet.get("actualControlGroupSize"),
				"Control Group Size did not match with actual size when navigating between campaigns in same tab");
		logger.info(
				"Verified Segment Size and Control Group is showing correct data when navigating between campaigns in same tab");
		TestListeners.extentTest.get().pass(
				"Verified Segment Size and Control Group is showing correct data when navigating between campaigns in same tab");
	}

	// Rakhi
	@Test(description = "SQ-T6858 Verify Key Metrics values When Navigating Between Campaigns in Same Tab")
	@Owner(name = "Rakhi Rawat")
	public void T6858_VerifyKeyMetricsWhenNavigatingBetweenCampaigns() throws InterruptedException {

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl1"));
		// navigate to another campaign
		pageObj.campaignPerformancePage().navigateToCampaignUrl(dataSet.get("campaignUrl2"));

		List<String> expectedKeyMetricsList = Arrays.asList("Customers Targeted", "Redeemed Net Sales",
				"Redeemed Net AOV", "Redeemed Rate", "Discount");

		// verify key metrics for variant A
		List<String> variantAkeyMetrics = pageObj.campaignPerformancePage().getSplitCppKeyMetrics("A variant");
		Assert.assertEquals(variantAkeyMetrics, expectedKeyMetricsList,
				"Key metrics section not matched for Variant A");
		logger.info("Verified Key metrics section for Variant A");
		TestListeners.extentTest.get().pass("Verified Key metrics section for Variant A");

		// verify key metrics for variant B
		List<String> variantBkeyMetrics = pageObj.campaignPerformancePage().getSplitCppKeyMetrics("B variant");
		Assert.assertEquals(variantBkeyMetrics, expectedKeyMetricsList,
				"Key metrics section not matched for Variant B");
		logger.info("Verified Key metrics section for Variant B");
		TestListeners.extentTest.get().pass("Verified Key metrics section for Variant B");

		// verify control group key metrics
		List<String> expectedKeyMetrics = Arrays.asList("Customers Targeted", "Net Sales", "Net AOV");
		List<String> actualKeyMetrics = pageObj.campaignPerformancePage().getSplitTestControlGroupMetrics();
		logger.info("Actual key metrics: " + actualKeyMetrics);
		Assert.assertEquals(actualKeyMetrics, expectedKeyMetrics,
				"Control Group Key metrics not matched when navigating between campaigns in same tab");
		logger.info("Verified control group key metrics matched when navigating between campaigns in same tab");
		TestListeners.extentTest.get()
				.pass("Verified control group key metrics matched when navigating between campaigns in same tab");

		// verify control group Customers Targeted size
		String value = pageObj.campaignPerformancePage().getSplitTestControlGroupMetricsValue("Customers Targeted");
		Assert.assertEquals(value, dataSet.get("actualControlGroupSize"),
				"Control Group Customers Targeted Size did not match with actual size when navigating between campaigns in same tab");
		logger.info(
				"Verified Control Group Customers Targeted size matched with actual size when navigating between campaigns in same tab");
		TestListeners.extentTest.get().pass(
				"Verified Control Group Customers Targeted size matched with actual size when navigating between campaigns in same tab");

	}

	// Rakhi
	@Test(description = "SQ-T7046 Verify Key metrics values for non split campaigns", dataProvider = "TestDataProvider3")
	@Owner(name = "Rakhi Rawat")
	public void T7046_VerifyNonSplitCampaignKeyMetrics(String campaignId) throws InterruptedException {

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// navigate to Campaign Performance Page
		pageObj.newCamHomePage().searchCampaign(campaignId);
		pageObj.newCamHomePage().clickOnFirstCampaign();

		// get key metrics values for Top level
		List<String> keyMetricValuesTop = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getNonSplitKeyMetricsValues(dataSet.get("keyMetrics" + i),
					"Top");
			keyMetricValuesTop.add(values);
		}
		logger.info(
				"Values for Customers Targeted, Redeemed Net Sales, Redeemed Net AOV, Redeemed Rate, and Discount under 'Variant A' (Top) are: "
						+ keyMetricValuesTop);
		TestListeners.extentTest.get().pass(
				"Values for Customers Targeted, Redeemed Net Sales, Redeemed Net AOV, Redeemed Rate, and Discount under 'Variant A' (Top) are: "
						+ keyMetricValuesTop);

		// get key metrics values for bottom level
		List<String> keyMetricValuesBottom = new ArrayList<>();
		for (int i = 2; i <= 5; i++) {
			String values = pageObj.campaignPerformancePage().getNonSplitKeyMetricsValues(dataSet.get("keyMetrics" + i),
					"Bottom");
			keyMetricValuesBottom.add(values);
		}
		logger.info(
				"Values for Gross Sales, Redemption AOV, Redemptions, Discount Rate under 'Variant A' (Bottom) are: "
						+ keyMetricValuesBottom);
		TestListeners.extentTest.get().pass(
				"Values for Gross Sales, Redemption AOV, Redemptions, Discount Rate under 'Variant A' (Bottom) are: "
						+ keyMetricValuesBottom);

		// get Customers Targeted, Redeemed Net Sales, Gross Sales,
		// Redemptions, & Discount
		int customersTargeted = Integer.parseInt(keyMetricValuesTop.get(0));
		int redeemedNetSales = Integer.parseInt(keyMetricValuesTop.get(1));
		int grossSales = Integer.parseInt(keyMetricValuesBottom.get(0));
		int redemptions = Integer.parseInt(keyMetricValuesBottom.get(2));
		int discount = Integer.parseInt(keyMetricValuesTop.get(4));

		// calculate Redeemed Net AOV
		// Redeemed Net AOV should be (Gross Sales () - Discount ()) / Redemptions ()
		double redeemedNetAOV = (grossSales - discount) / (double) redemptions;
		int actualValue = (int) redeemedNetAOV;
		int expectedValue = (int) Double.parseDouble(keyMetricValuesTop.get(2));
		Assert.assertEquals(actualValue, expectedValue,
				"Redeemed Net AOV value not matched for campaign id : " + campaignId);
		logger.info("Verified Redeemed Net AOV value for campaign id : " + campaignId);
		TestListeners.extentTest.get().pass("Verified Redeemed Net AOV value for campaign id : " + campaignId);

		// calculate Redemption AOV
		// (Gross Sales () / Redemptions () = Redeemed Net AOV ()
		double redemptionAOV = grossSales / (double) redemptions;
		double redemptionAOVnew = Math.round(redemptionAOV * 100.0) / 100.0;
		Assert.assertEquals(redemptionAOVnew, Double.parseDouble(keyMetricValuesBottom.get(1)),
				"Redemption AOV value not matched for campaign id : " + campaignId);
		logger.info("Verified Redemption AOV value matched for campaign id : " + campaignId);
		TestListeners.extentTest.get().pass("Verified Redemption AOV value matched for campaign id : " + campaignId);

		// calculate Discount Rate
		// Discount () / Gross Sales () = Discount Rate %
		double discountRate = ((double) discount / grossSales) * 100;
		double discountRateNew = Math.round(discountRate * 10.0) / 10.0;
		Assert.assertEquals(discountRateNew, Double.parseDouble(keyMetricValuesBottom.get(3)), 0.1,
				"Discount Rate value not matched for campaign id : " + campaignId);
		logger.info("Verified Discount Rate value matched for campaign id : " + campaignId);
		TestListeners.extentTest.get().pass("Verified Discount Rate value matched for campaign id : " + campaignId);

		// calculate Redeemed Rate
		// Redemptions / Customers Targeted () = Redeemed Rate%
		double redeemedRate = ((double) redemptions / customersTargeted) * 100;
		double redeemedRateNew = Math.round(redeemedRate * 100.0) / 100.0;
		Assert.assertEquals(redeemedRateNew, Double.parseDouble(keyMetricValuesTop.get(3)),
				"Redeemed Rate value not matched for campaign id : " + campaignId);
		logger.info("Verified Redeemed Rate value matched for campaign id : " + campaignId);
		TestListeners.extentTest.get().pass("Verified Redeemed Rate value matched for campaign id : " + campaignId);

	}

	@DataProvider(name = "TestDataProvider1")
	public Object[][] testDataProvider() {

		return new Object[][] {

				{ "627399", "3039", "50% (1521)", "50% (1518)", "0% (0)", "Mobile user only",
						"Parent: 627399, Variant A: 627400, Variant B: 627401", "Push Notification" },

				{ "627220", "3037", "35% (1064)", "35% (1064)", "30% (909)", "Mobile user only",
						"Parent: 627220, Variant A: 627232, Variant B: 627233", "Email, SMS, and Push Notification" },

				{ "628467", "3044", "60% (1826)", "20% (609)", "20% (609)", "Mobile user only",
						"Parent: 628467, Variant A: 628474, Variant B: 628475", "Email and Push Notification" },

				{ "633238", "3057", "40% (1223)", "40% (1223)", "20% (611)", "Mobile user only",
						"Parent: 633238, Variant A: 633239, Variant B: 633240", "Email and Push Notification" }

		};
	}

	@DataProvider(name = "TestDataProvider2")

	public Object[][] testDataProvider2() {

		return new Object[][] { { "627399" }, { "627220" }, { "633238" } };
	}

	@DataProvider(name = "TestDataProvider3")

	public Object[][] testDataProvider3() {

		return new Object[][] { { "694432" }, { "694433" } };
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
