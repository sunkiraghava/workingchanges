package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.text.ParseException;
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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DashboardTest {

	private static Logger logger = LogManager.getLogger(DashboardTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private Utilities utils;
	private static Map<String, String> dataSet;
	String run = "ui";

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

	@Test(description = "SQ-T2225 Verify dashboard stats and graph on any business [Part 1]", groups = { "regression",
			"unstable","nonNightly" }, priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T2225_verifyDashboardStatsAndGraphOnBusinessPart1() throws Exception {
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Feedback
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Feedback");
		pageObj.dashboardpage().navigateToTabs("Feedback Rating");
		pageObj.dashboardpage().checkUncheckAnyFlag("Request experience rating on feedback screen on mobile app?",
				"check");

		// Verify Dashboard charts on 'All Locations' and 'Last 365 Days' filter
		utils.logit("=== Verifying with 'All Locations' and 'Last 365 Days' filter ===");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectLocation("All", "All Locations");
		pageObj.dashboardpage().selectDataRange("Last 365 Days");
		verifyAllDashboardCharts("");
		verifyAverageRatingStats("");
		verifyReferralsRevenueStats("");

		// Verify Dashboard charts with 'All Locations' and 'Last Week' filter
		utils.logit("=== Verifying with 'All Locations' and 'Last Week' filter ===");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectLocation("All", "All Locations");
		pageObj.dashboardpage().selectDataRange("Last Week");
		verifyAllDashboardCharts("");
		verifyAverageRatingStats("");
		verifyReferralsRevenueStats("");

		// Verify Dashboard charts with one location and 'Last Month' filter
		utils.logit("=== Verifying with one location and 'Last Month' filter ===");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectLocation(dataSet.get("locationPartialName"), dataSet.get("locationFullName"));
		pageObj.dashboardpage().selectDataRange("Last Month");
		verifyAllDashboardCharts("");
		verifyAverageRatingStats("");
		verifyReferralsRevenueStats("");

	}

	@Test(description = "SQ-T2225 Verify dashboard stats and graph on any business [Part 2]; "
			+ "SQ-T4674 verify dashboard page footer Privacy Link is working properly; "
			+ "SQ-T4675 verify dashboard page footer Security Link is working properly; "
			+ "SQ-T4676 verify dashboard page footer Status Link is working properly; "
			+ "SQ-T4677 verify dashboard page footer Developers Link is working properly; "
			+ "SQ-T4678 verify dashboard page footer Contact Link is working properly; "
			+ "SQ-T4679 verify dashboard page footer About Link is working properly", priority = 0, groups = {
					"regression", "unstable" ,"nonNightly"})
	@Owner(name = "Vaibhav Agnihotri")
	public void T2225_verifyDashboardStatsAndGraphOnBusinessPart2() throws Exception {
		// Select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Verify Dashboard charts with location group and 'Last 90 Days' filter
		utils.logit("=== Verifying with Location Group and 'Last 90 Days' filter ===");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectLocation(dataSet.get("locationGroupPartialName"),
				dataSet.get("locationGroupFullName"));
		pageObj.dashboardpage().selectDataRange("Last 90 Days");
		verifyAllDashboardCharts("");
		verifyAverageRatingStats("");
		verifyReferralsRevenueStats("");

		/*
		 * Verify Dashboard charts with 'All Locations' and 'Custom Range' filter. In
		 * this case, stats are always expected have some data. If they don't, then the
		 * test will fail. Here, we are using custom range of more than 1 year.
		 */
		utils.logit("=== Verifying with 'All Locations' and 'Custom Range' filter ===");
		pageObj.menupage().clickDashboardMenu();
		pageObj.dashboardpage().selectLocation("All", "All Locations");
		pageObj.dashboardpage().enterCustomDateRange(dataSet.get("customDateRange"));
		verifyAllDashboardCharts("Custom Range");
		verifyAverageRatingStats("Custom Range");
		verifyReferralsRevenueStats("Custom Range");

		/*
		 * Verify 'Loyalty Checkins and Sales' & 'Redemptions and Sales' Graphs with All
		 * options dropdown
		 */
		utils.logit("=== Verifying 'Loyalty Checkins and Sales' graph with All options dropdown ===");
		pageObj.dashboardpage().verifyLoyaltyCheckinsandSalesgraphswithAlloption();
		utils.logit("=== Verifying 'Redemptions and Sales' graph with All options dropdown ===");
		pageObj.dashboardpage().verifyRedemptionsandSalesGraphWithAllOptions();
		pageObj.dashboardpage().verifyBrowserLogs();
		utils.longWaitInSeconds(4);

		// Verify Dashboard page footer links are working properly
		boolean status = pageObj.dashboardpage().brokenLinksUi();
		Assert.assertTrue(status);
		utils.logPass("links are working fine");

	}

	// This method verifies the Average Rating stats only
	public void verifyAverageRatingStats(String condition) {

		// Average Rating
		utils.logit("== Average Rating ==");
		boolean isAvgRatingHeaderNameDisplayed = pageObj.dashboardpage().getDashBoardChartsHeaderstatus("Avg. Rating");
		Assert.assertTrue(isAvgRatingHeaderNameDisplayed, "Average Rating header name is not displayed");
		String avgRatingCount = pageObj.dashboardpage().getDashBoardChartsCount("Avg. Rating", "");

		// For Custom Range, verify that count is >0 and <=5
		if (condition.equals("Custom Range")) {
			double avgRatingDoubleCount = Double.parseDouble(avgRatingCount);
			Assert.assertTrue(avgRatingDoubleCount > 0 && avgRatingDoubleCount <= 5,
					"Average Rating count is not within range 0-5");
			utils.logit("Average Rating count is within range 0-5");
		}
		boolean isAvgRatingCountDisplayed = pageObj.dashboardpage().validateDashBoardChartsSVGAndClass("Avg. Rating",
				"", avgRatingCount, "No");
		Assert.assertTrue(isAvgRatingCountDisplayed, "Average Rating count is not displayed");
		utils.logPass("Average Rating header name and count are displayed");

	}

	// This method verifies the Referrals and Referrals Revenue stats only
	public void verifyReferralsRevenueStats(String condition) {

		// Referrals
		utils.logit("== Referrals ==");
		boolean isReferralsHeaderNameDisplayed = pageObj.dashboardpage().getDashBoardChartsHeaderstatus("Referrals");
		Assert.assertTrue(isReferralsHeaderNameDisplayed, "Referrals header name is not displayed");
		String referralsCount = pageObj.dashboardpage().getDashBoardChartsCount("Referrals", "");

		// For custom range, verify that count is always greater than 0
		if (condition.equals("Custom Range")) {
			Assert.assertTrue(Integer.parseInt(referralsCount) > 0, "Referrals count is not greater than 0");
			utils.logit("Referrals count is greater than 0");
		}
		boolean isReferralsCountDisplayed = pageObj.dashboardpage().validateDashBoardChartsSVGAndClass("Referrals", "",
				referralsCount, "No");
		Assert.assertTrue(isReferralsCountDisplayed, "Referrals chart count is not displayed");
		utils.logPass("Referrals header name and chart count are displayed");

		// Referrals Revenue
		utils.logit("== Referrals Revenue ==");
		boolean isReferralsRevenueHeaderNameDisplayed = pageObj.dashboardpage()
				.getDashBoardChartsHeaderstatus("Referrals Revenue");
		Assert.assertTrue(isReferralsRevenueHeaderNameDisplayed, "Referrals Revenue header name is not displayed");
		String referralsRevenueCount = pageObj.dashboardpage().getDashBoardChartsCount("Referrals Revenue", "");

		// For custom range, verify that count contains $ sign and >0
		if (condition.equals("Custom Range")) {
			Assert.assertTrue(Integer.parseInt(referralsRevenueCount.substring(1)) > 0,
					"Referrals Revenue count is not greater than 0");
			Assert.assertTrue(referralsRevenueCount.startsWith("$"),
					"Referrals Revenue count does not starts with $ sign");
			utils.logit("Referrals Revenue count starts with $ sign");
		}
		boolean isReferralsRevenueCountDisplayed = pageObj.dashboardpage()
				.validateDashBoardChartsSVGAndClass("Referrals Revenue", "", referralsRevenueCount, "No");
		Assert.assertTrue(isReferralsRevenueCountDisplayed, "Referrals Revenue chart count is not displayed");
		utils.logPass("Referrals Revenue header name and chart count are displayed");

	}

	/*
	 * This method verifies all the Dashboard stats and charts, except Average
	 * Rating, Referrals and Referrals Revenue
	 */
	public void verifyAllDashboardCharts(String condition) throws Exception {
		//utils.waitTillPagePaceDone();
		// Checks for system busy message and if displayed, then wait for it to
		// disappear
		boolean isSystemBusyMessageDisplayed = pageObj.dashboardpage()
				.isDashboardStatsNotLoadingMessageDisplayed(utils.getLocator("dashboardPage.systemBusyMessage"), 5);
		Assert.assertFalse(isSystemBusyMessageDisplayed, "System busy message is displayed.");

		// Top stats tiles names
		utils.logit("== Top stats tiles names ==");
		List<String> topStatsTilesNames = pageObj.dashboardpage().getTopStatsCards();
		Assert.assertTrue(topStatsTilesNames.contains("AvgVisits"));
		Assert.assertTrue(topStatsTilesNames.contains("LoyaltySales"));
		Assert.assertTrue(topStatsTilesNames.contains("ParticipationRate"));
		Assert.assertTrue(topStatsTilesNames.contains("SpendLift"));
		utils.logPass("Top stats tiles name are displayed");

		// Signups pie chart
		utils.logit("== Signups pie chart ==");
		boolean isSignupChartHeaderNameDisplayed = pageObj.dashboardpage().getDashBoardChartsHeaderstatus("Signups");
		Assert.assertTrue(isSignupChartHeaderNameDisplayed, "Signup chart header name is not displayed");
		String signUpChartHeaderCount = pageObj.dashboardpage().getDashBoardChartsCount("Signups", "");

		// For custom range, verify that count is always greater than 0
		if (condition.equals("Custom Range")) {
			Assert.assertTrue(Integer.parseInt(signUpChartHeaderCount) > 0, "Signup chart count is not greater than 0");
			utils.logit("Signup chart count is greater than 0");
		}
		boolean isSignupChartSvgClassDisplayed = pageObj.dashboardpage().validateDashBoardChartsSVGAndClass("Signups",
				"signup_container", signUpChartHeaderCount, "Yes");
		/*
		 * For custom range, if SVG is displayed, then verify that chart specific
		 * legends are also displayed
		 */
		if (condition.equals("Custom Range") && isSignupChartSvgClassDisplayed) {
			boolean isSignupChartLegendsDisplayed = pageObj.dashboardpage()
					.isDashboardChartLegendsDisplayed("signup_container", dataSet.get("signUpChartLegends"));
			Assert.assertTrue(isSignupChartLegendsDisplayed, "Signup chart legends are not displayed");
			utils.logit("Signup chart legends are displayed");
		}
		Assert.assertTrue(isSignupChartSvgClassDisplayed, "Signup chart svg and chart class is not displayed");
		utils.logPass("Signups chart header name, count, svg and class are displayed");

		// Loyalty Checkins pie chart
		utils.logit("== Loyalty Checkins pie chart ==");
		boolean isLoyaltyCheckinsChartHeaderNameDisplayed = pageObj.dashboardpage()
				.getDashBoardChartsHeaderstatus("Loyalty Checkins");
		Assert.assertTrue(isLoyaltyCheckinsChartHeaderNameDisplayed,
				"Loyalty Checkins chart header name is not displayed");
		String loyaltyCheckinsChartHeaderCount = pageObj.dashboardpage().getDashBoardChartsCount("Loyalty Checkins",
				"");
		// For custom range, verify that count is always greater than 0
		if (condition.equals("Custom Range")) {
			Assert.assertTrue(Integer.parseInt(loyaltyCheckinsChartHeaderCount) > 0,
					"Loyalty Checkins chart count is not greater than 0");
			utils.logit("Loyalty Checkins chart count is greater than 0");
		}
		boolean isLoyaltyCheckinsChartSvgClassDisplayed = pageObj.dashboardpage().validateDashBoardChartsSVGAndClass(
				"Loyalty Checkins", "checkin_stats", loyaltyCheckinsChartHeaderCount, "Yes");
		/*
		 * For custom range, if SVG is displayed, then verify that chart specific
		 * legends are also displayed
		 */
		if (condition.equals("Custom Range") && isLoyaltyCheckinsChartSvgClassDisplayed) {
			boolean isLoyaltyCheckinsChartLegendsDisplayed = pageObj.dashboardpage()
					.isDashboardChartLegendsDisplayed("checkin_stats", dataSet.get("loyaltyCheckinsChartLegends"));
			Assert.assertTrue(isLoyaltyCheckinsChartLegendsDisplayed,
					"Loyalty Checkins chart legends are not displayed");
			utils.logit("Loyalty Checkins pie chart legends are displayed");
		}
		Assert.assertTrue(isLoyaltyCheckinsChartSvgClassDisplayed,
				"Loyalty Checkins chart svg and chart class is not displayed");
		utils.logPass("Loyalty Checkins chart header name, count, svg and class are displayed");

		// Loyalty Checkins and Sales bar chart
		utils.logit("== Loyalty Checkins and Sales bar chart ==");
		boolean isLoyaltyCheckinsAndSalesChartHeaderNameDisplayed = pageObj.dashboardpage()
				.getDashBoardChartsHeaderstatus("Loyalty Checkins and Sales");
		Assert.assertTrue(isLoyaltyCheckinsAndSalesChartHeaderNameDisplayed,
				"Loyalty Checkins and Sales chart header name is not displayed");
		boolean isLoyaltyCheckinsAndSalesChartSvgClassDisplayed = pageObj.dashboardpage()
				.validateDashBoardChartsSVGAndClass("Loyalty Checkins and Sales", "dashboard-graph", "", "Yes");
		/*
		 * For custom range, if SVG is displayed, then verify that chart specific
		 * legends are also displayed
		 */
		if (condition.equals("Custom Range") && isLoyaltyCheckinsAndSalesChartSvgClassDisplayed) {
			boolean isLoyaltyCheckinsAndSalesChartLegendsDisplayed = pageObj.dashboardpage()
					.isDashboardChartLegendsDisplayed("dashboard-graph",
							dataSet.get("loyaltyCheckinsAndSalesChartLegends"));
			Assert.assertTrue(isLoyaltyCheckinsAndSalesChartLegendsDisplayed,
					"Loyalty Checkins And Sales chart legends are not displayed");
			utils.logit("Loyalty Checkins and Sales bar chart legends are displayed");
		}
		Assert.assertTrue(isLoyaltyCheckinsAndSalesChartSvgClassDisplayed,
				"Loyalty Checkins and Sales chart svg and chart class is not displayed");

		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("dashboard-graph", "Unique Guests"));
		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("dashboard-graph", "Loyalty Checkins"));
		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("dashboard-graph", "Loyalty Sales"));
		Assert.assertTrue(
				pageObj.dashboardpage().isSubHeaderBelowChartPresent("dashboard-graph", "Loyalty Avg. Check"));
		utils.logPass("Loyalty Checkins and Sales chart header name, svg class and headers below chart are displayed");

		// Redemptions & Sales chart
		utils.logit("== Redemptions & Sales chart ==");
		boolean isRedemptionsSalesChartHeaderNameDisplayed = pageObj.dashboardpage()
				.getDashBoardChartsHeaderstatus("Redemptions & Sales");
		Assert.assertTrue(isRedemptionsSalesChartHeaderNameDisplayed,
				"Redemptions & Sales chart header name is not displayed");
		boolean isRedemptionsSalesChartSvgClassDisplayed = pageObj.dashboardpage()
				.validateDashBoardChartsSVGAndClass("Redemptions & Sales", "redemption-sales", "", "Yes");
		/*
		 * For custom range, if SVG is displayed, then verify that chart specific
		 * legends are also displayed
		 */
		if (condition.equals("Custom Range") && isRedemptionsSalesChartSvgClassDisplayed) {
			boolean isRedemptionsSalesChartLegendsDisplayed = pageObj.dashboardpage()
					.isDashboardChartLegendsDisplayed("redemption-sales", dataSet.get("redemptionsSalesChartLegends"));
			Assert.assertTrue(isRedemptionsSalesChartLegendsDisplayed,
					"Redemptions & Sales chart legends are not displayed");
			utils.logit("Redemptions & Sales chart legends are displayed");
		}
		Assert.assertTrue(isRedemptionsSalesChartSvgClassDisplayed,
				"Redemptions & Sales chart svg and chart class is not displayed");

		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("redemption-sales", "Redemptions"));
		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("redemption-sales", "Discount"));
		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("redemption-sales", "Gross Sales"));

		utils.logPass("Redemptions & Sales chart header name, svg class and headers below chart are displayed");

		// App Downloads chart
		utils.logit("== App Downloads chart ==");
		boolean isAppDownloadsChartHeaderNameDisplayed = pageObj.dashboardpage()
				.getDashBoardChartsHeaderstatus("App Downloads");
		Assert.assertTrue(isAppDownloadsChartHeaderNameDisplayed, "App Downloads chart header name is not displayed");
		boolean isAppDownloadsChartSvgClassDisplayed = pageObj.dashboardpage()
				.validateDashBoardChartsSVGAndClass("App Downloads", "download-stats", "", "Yes");
		/*
		 * For custom range, if SVG is displayed, then verify that chart specific
		 * legends are also displayed
		 */
		if (condition.equals("Custom Range") && isAppDownloadsChartSvgClassDisplayed) {
			boolean isAppDownloadsChartLegendsDisplayed = pageObj.dashboardpage()
					.isDashboardChartLegendsDisplayed("download-stats", dataSet.get("appDownloadsChartLegends"));
			Assert.assertTrue(isAppDownloadsChartLegendsDisplayed, "App Downloads chart legends are not displayed");
			utils.logit("App Downloads chart legends are displayed");
		}
		Assert.assertTrue(isAppDownloadsChartSvgClassDisplayed,
				"App Downloads chart svg and chart class is not displayed");

		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("download-stats", "iOS"));
		Assert.assertTrue(pageObj.dashboardpage().isSubHeaderBelowChartPresent("download-stats", "Android"));

		utils.logPass("App Downloads chart header name, svg class and headers below chart are displayed");

		// Reviews
		utils.logit("== Reviews ==");
		boolean isReviewsHeaderNameDisplayed = pageObj.dashboardpage().getDashBoardChartsHeaderstatus("Reviews");
		Assert.assertTrue(isReviewsHeaderNameDisplayed, "Reviews header name is not displayed");
		String reviewsCount = pageObj.dashboardpage().getDashBoardChartsCount("Reviews", "");

		// For custom range, verify that count is always greater than 0
		if (condition.equalsIgnoreCase("Custom Range")) {
			Assert.assertTrue(Integer.parseInt(reviewsCount) > 0, "Reviews count is not greater than 0");
			utils.logit("Reviews count is greater than 0");
		}
		boolean isReviewsCountDisplayed = pageObj.dashboardpage().validateDashBoardChartsSVGAndClass("Reviews", "",
				reviewsCount, "No");
		Assert.assertTrue(isReviewsCountDisplayed, "Reviews count is not displayed");
		utils.logPass("Reviews header name and count are displayed");

		// Most Frequent, Top Spenders & Highest Averages Carousel
		utils.logit("== Most Frequent, Top Spenders & Highest Averages Carousel ==");
		boolean isCarouselNamesDisplayed1 = pageObj.dashboardpage().validateCarouselContents("Guests",
				dataSet.get("guestsCarouselHeaders"));
		Assert.assertTrue(isCarouselNamesDisplayed1, "Expected carousel header with subheader are not displayed");
		utils.logPass("Most Frequent, Top Spenders & Highest Averages Carousel are displayed");

		// Checkins, Redemptions & Coupons Carousel
		utils.logit("== Checkins, Redemptions & Coupons Carousel ==");
		boolean isCarouselNamesDisplayed2 = pageObj.dashboardpage().validateCarouselContents("Activity",
				dataSet.get("activityCarouselHeaders"));
		Assert.assertTrue(isCarouselNamesDisplayed2, "Expected carousel header with subheader are not displayed");
		utils.logPass("Checkins, Redemptions & Coupons Carousel are displayed");

		// Campaigns Carousel
		utils.logit("== Campaigns Carousel ==");
		boolean isCarouselNamesDisplayed3 = pageObj.dashboardpage().validateCarouselContents("Recent", "Campaigns");
		Assert.assertTrue(isCarouselNamesDisplayed3, "Expected carousel header with subheader are not displayed");
		boolean isCarouselNamesDisplayed4 = pageObj.dashboardpage().validateCarouselContents("Revenue", "Campaigns");
		Assert.assertTrue(isCarouselNamesDisplayed4, "Expected carousel header with subheader are not displayed");
		utils.logPass("Campaigns Carousel are displayed");

		/*
		 * For custom range, verify the count values for specific stats. These stats are
		 * present in top tiles and below graphs
		 */
		if (condition.equals("Custom Range")) {
			// Loyalty Sales
			String loyaltySalesCount = pageObj.dashboardpage().getDashBoardChartsCount("Loyalty Sales", "Yes");
			Assert.assertTrue(loyaltySalesCount.startsWith("$"));
			Assert.assertTrue(Integer.parseInt(loyaltySalesCount.substring(1)) > 0);

			// Spend Lift (only have data where value is 0.00%)
			String spendLiftCount = pageObj.dashboardpage().getDashBoardChartsCount("Spend Lift", "Yes");
			Assert.assertTrue(spendLiftCount.endsWith("%"));
			Assert.assertTrue(Double.parseDouble(spendLiftCount.substring(0, 2)) >= 0);

			// Participation Rate (only have data where value is 0.00%)
			String participationRateCount = pageObj.dashboardpage().getDashBoardChartsCount("Participation Rate",
					"Yes");
			Assert.assertTrue(participationRateCount.endsWith("%"));
			Assert.assertTrue(Double.parseDouble(participationRateCount.substring(0, 2)) >= 0);

			// Average Visits
			String averageVisitsCount = pageObj.dashboardpage().getDashBoardChartsCount("Avg. Visits", "Yes");
			Assert.assertTrue(Double.parseDouble(averageVisitsCount) > 0);

			// Unique Guests
			String uniqueGuestsCount = pageObj.dashboardpage().getDashBoardChartsCount("Unique Guests", "Yes");
			Assert.assertTrue(Integer.parseInt(uniqueGuestsCount) > 0);

			// Loyalty Checkins
			String loyaltyCheckinsCount = pageObj.dashboardpage().getDashBoardChartsCount("Loyalty Checkins", "Yes");
			Assert.assertTrue(Integer.parseInt(loyaltyCheckinsCount) > 0);

			// Loyalty Avg. Check
			String loyaltyAvgCheckCount = pageObj.dashboardpage().getDashBoardChartsCount("Loyalty Avg. Check", "Yes");
			Assert.assertTrue(loyaltyAvgCheckCount.startsWith("$"));
			Assert.assertTrue(Double.parseDouble(loyaltyAvgCheckCount.substring(1)) > 0);

			// Redemptions
			String redemptionsCount = pageObj.dashboardpage().getDashBoardChartsCount("Redemptions", "Yes");
			Assert.assertTrue(Integer.parseInt(redemptionsCount) > 0);

			// Discount
			String discountCount = pageObj.dashboardpage().getDashBoardChartsCount("Discount", "Yes");
			Assert.assertTrue(discountCount.startsWith("$"));
			Assert.assertTrue(Integer.parseInt(discountCount.substring(1)) > 0);

			// Gross Sales
			String grossSalesCount = pageObj.dashboardpage().getDashBoardChartsCount("Gross Sales", "Yes");
			Assert.assertTrue(grossSalesCount.startsWith("$"));
			Assert.assertTrue(Integer.parseInt(grossSalesCount.substring(1)) > 0);

			// iOS Downloads (only have data where value is 0)
			String iOSDownloadsCount = pageObj.dashboardpage().getDashBoardChartsCount("iOS", "Yes");
			Assert.assertTrue(Integer.parseInt(iOSDownloadsCount) >= 0);

			// Android Downloads (only have data where value is 0)
			String androidDownloadsCount = pageObj.dashboardpage().getDashBoardChartsCount("Android", "Yes");
			Assert.assertTrue(Integer.parseInt(androidDownloadsCount) >= 0);

			utils.logit("For Custom Range, values for following stats are verified: "
					+ "Loyalty Sales, Spend Lift, Participation Rate, Average Visits, Unique Guests, Loyalty Checkins, Loyalty Avg. Check, "
					+ "Redemptions, Discount, Gross Sales, iOS and Android Downloads");
		}
	}

	@Test(description = "SQ-T5803 Verify that enabling the 'Enable Multiple Redemptions' flag in the cockpit reflects the option to 'Enable Multiple Redemptions on All Locations'."
			+ "SQ-T5805 Verify disabling the multiple redemptions on all locations flag for a business already on R2.0."
			+ "SQ-T6035 Verify the UI changes for Auto-Unlock Period"
			+ "SQ-T5804 Test updating all locations for a business migrating from R1.0 to R2.0.", priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T5803_verifyEnableMultipleRedemptionsFlagReflectsOnAllLocations()
			throws InterruptedException, ParseException {

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to dashboard page
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Multiple Redemptions", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		Boolean f1 = pageObj.dashboardpage().isElementDisplayed("Enable Multiple Redemptions on All Locations");
		Assert.assertFalse(f1, "Enable Multiple Redemptions is not displayed on All Locations");
		utils.logPass("Enable Multiple Redemptions is displayed on All Locations");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Multiple Redemptions", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		Boolean f2 = pageObj.dashboardpage().isElementDisplayed("Enable Multiple Redemptions on All Locations");
		Assert.assertTrue(f2, "Enable Multiple Redemptions is not displayed on All Locations");
		utils.logPass("Enable Multiple Redemptions is displayed on All Locations");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		// navigate to Multiple redemptions tab on redemption page
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		Boolean f3 = pageObj.dashboardpage().isElementDisplayed("Auto-Unlock Period");
		Assert.assertTrue(f3, "Auto-Unlock Period is not displayed on Multiple Redemptions tab");
		utils.logPass("Auto-Unlock Period is displayed on Multiple Redemptions tab");
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