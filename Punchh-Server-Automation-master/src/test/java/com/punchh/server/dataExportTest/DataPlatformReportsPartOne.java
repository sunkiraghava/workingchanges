package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataPlatformReportsPartOne {
	static Logger logger = LogManager.getLogger(DataPlatformReportsPartOne.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String baseUrl;
	private Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeClass(Method method) throws InterruptedException {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T5181 Verify 'iframe' to embed Dumpster Divers Report ", groups = "Regression")
	public void verifyReportsSuperAdmin() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();

		// navigate to the dumpster report in the tableau and enter the embedded link.
		pageObj.cockpitTableauPage().enterEmbeddedLinkInDumpsterDiversReport(dataSet.get("DumpsterLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Dumpster Divers Report");

		// validate the three fields of dumpster divers report:
		boolean dumpsterReportFields = pageObj.cockpitTableauPage()
				.validateFieldsOfDumpsterDiversreport(dataSet.get("date"));
		Assert.assertTrue(dumpsterReportFields, "Dumpster diver report report fields did not match");
		logger.info("The dumpster diver report fields were verified");
		TestListeners.extentTest.get().pass("The dumpster diver report fields were verified");

		// navigate to the promotional report in the tableau and enter the embedded
		// link.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();
		pageObj.cockpitTableauPage().enterEmbeddedLinkInPromotionalReport(dataSet.get("promotionalLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Promotional Report");

		// validate the fields of redemption report:
		boolean promotionalReportFields = pageObj.cockpitTableauPage()
				.validateFieldsOfPromotionalReport(dataSet.get("date"));
		Assert.assertTrue(promotionalReportFields, "Promotional report fields did not match");
		logger.info("The promotional report fields were verified");
		TestListeners.extentTest.get().pass("The promotional report fields were verified");

		// navigate to the redemption report in the tableau and enter the embedded link.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();
		pageObj.cockpitTableauPage().enterEmbeddedLinkInRedemptionReport(dataSet.get("redemptionLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Redemption Report");

		// validate the fields of the redemption report:
		boolean redemptionReportFields = pageObj.cockpitTableauPage()
				.validateDateAndLocationFields(dataSet.get("date"));
		Assert.assertTrue(redemptionReportFields, "Redemption report fields did not match");
		logger.info("The redemption report fields were verified");
		TestListeners.extentTest.get().pass("The redemption report fields were verified");

		// navigate to the location report in the tableau and enter the embedded link.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();
		pageObj.cockpitTableauPage().enterEmbeddedLinkInLocationReport(dataSet.get("locationLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Location Report");

		// validate the fields of location report:
		boolean locationReportFields = pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		Assert.assertTrue(locationReportFields, "location report fields did not match");
		logger.info("The location report fields were verified");
		TestListeners.extentTest.get().pass("The location report fields were verified");

		// navigate to the lift report in the tableau and enter the embedded link.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();
		pageObj.cockpitTableauPage().enterEmbeddedLinkInLiftReport(dataSet.get("liftLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Lift Report");

		// validate the two fields of lift report:
		boolean liftReportFields = pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		Assert.assertTrue(liftReportFields, "Lift report fields did not match");
		logger.info("The lift report fields were verified");
		TestListeners.extentTest.get().pass("The lift report fields were verified");

		// navigate to the coupon report in the tableau and enter the embedded link.
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Tableau Analytics");
		pageObj.cockpitTableauPage().goToCockpitTableau();
		pageObj.cockpitTableauPage().enterEmbeddedLinkInCouponReport(dataSet.get("couponLink"));
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Reports", "Coupon Report");

		// validate the two fields of coupon report:
		boolean couponReportFields = pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		Assert.assertTrue(couponReportFields, "coupon report fields did not match");
		logger.info("The coupon report fields were verified");
		TestListeners.extentTest.get().pass("The coupon report fields were verified");

	}

//	@Test(description = "SQ-T5179 verify 'iframe' is added to embed Lift Report ", groups = "Regression")
	public void verifyReportSiteadmin() throws Exception {
		String userName = prop.getProperty("siteadminusername");
		String password = prop.getProperty("siteadminpassword");
		// Validate the lift report tableau with the site admin with individual location
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(userName, password);
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug1"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Lift Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getIndividualList = pageObj.cockpitTableauPage().getIndividualLocations();
		Assert.assertTrue(getIndividualList, "Individual location did not get displayed");
		logger.info("The Individual location gets displayed");
		TestListeners.extentTest.get().pass("The Individual location gets displayed");

		// Validate the tableau with the site admin with location groups
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Lift Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getLocationGroupList = pageObj.cockpitTableauPage().getLocationGroups();
		Assert.assertTrue(getLocationGroupList, "Location group did not get displayed");
		logger.info("The location group gets displayed");
		TestListeners.extentTest.get().pass("The location group gets displayed");

		// validate the tableau with the site admin with no location access.
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug3"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Lift Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		pageObj.cockpitTableauPage().getNoLocation();
		boolean getNoLocationMessage = pageObj.cockpitTableauPage().getNoLocation();
		Assert.assertTrue(getNoLocationMessage, "No location message did not get displayed");
		logger.info("No location message gets displayed");
		TestListeners.extentTest.get().pass("No location message gets displayed");

		// validate the coupon report tableau with the site admin with individual
		// location
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug1"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Coupon Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getIndividualList1 = pageObj.cockpitTableauPage().getIndividualLocations();
		Assert.assertTrue(getIndividualList1, "Individual location did not get displayed");
		logger.info("The Individual location gets displayed");
		TestListeners.extentTest.get().pass("The Individual location gets displayed");

		// Validate the tableau with the site admin with location groups
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Coupon Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getLocationGroupList1 = pageObj.cockpitTableauPage().getLocationGroups();
		Assert.assertTrue(getLocationGroupList1, "Location group did not get displayed");
		logger.info("The location group gets displayed");
		TestListeners.extentTest.get().pass("The location group gets displayed");

		// validate the tableau with the site admin with no location access.
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug3"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Coupon Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getNoLocationMessage1 = pageObj.cockpitTableauPage().getNoLocation();
		Assert.assertTrue(getNoLocationMessage1, "No location message did not get displayed");
		logger.info("No location message gets displayed");
		TestListeners.extentTest.get().pass("No location message gets displayed");

		// validate the redemption report tableau with the site admin with individual
		// location
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug1"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Redemption Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getIndividualList2 = pageObj.cockpitTableauPage().getIndividualLocations();
		Assert.assertTrue(getIndividualList2, "Individual location did not get displayed");
		logger.info("The Individual location gets displayed");
		TestListeners.extentTest.get().pass("The Individual location gets displayed");

		// Validate the tableau with the site admin with location groups
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug2"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Redemption Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getLocationGroupList2 = pageObj.cockpitTableauPage().getLocationGroups();
		Assert.assertTrue(getLocationGroupList2, "Location group did not get displayed");
		logger.info("The location group gets displayed");
		TestListeners.extentTest.get().pass("The location group gets displayed");

		// validate the tableau with the site admin with no location access.
		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().clickOnSlug(dataSet.get("slug3"));
		pageObj.menupage().navigateToSubMenuItem("Reports", "Redemption Report");
		pageObj.cockpitTableauPage().validateDateAndLocationFields(dataSet.get("date"));
		boolean getNoLocationMessage2 = pageObj.cockpitTableauPage().getNoLocation();
		Assert.assertTrue(getNoLocationMessage2, "No location message did not get displayed");
		logger.info("No location message gets displayed");
		TestListeners.extentTest.get().pass("No location message gets displayed");
	}

	@Test(description = "SQ-T4585 Verify the POS scoreboard works for the Databricks", groups = "Regression")
	public void verifyPOSScoreboard() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.cockpitTableauPage().navigateToPOSScoreboard(dataSet.get("slug"));
		pageObj.cockpitTableauPage().verifyPOSScoreboardPage();
	}

	@AfterMethod
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
