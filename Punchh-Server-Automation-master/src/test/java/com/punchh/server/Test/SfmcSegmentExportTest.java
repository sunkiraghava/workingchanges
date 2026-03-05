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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SfmcSegmentExportTest {
	private static Logger logger = LogManager.getLogger(SfmcSegmentExportTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	Utilities utils;
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
		utils = new Utilities();
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Amit
	@Test(description = "SQ-T3586 SFMC Perform E2E test on segment export", groups = { "nonNightly" })
	@Owner(name = "Amit Kumar")
	public void T3586_verifySFMCPerformE2ETestOnSegmentExport() throws Exception {
		String sFMCFolderName = CreateDateTime.getUniqueString("SFMCSegmentExport");
		String segmentName = CreateDateTime.getUniqueString("sfmcAutomation");
//Enable marketing cloud?  cockpit > campaigns> sfmc
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Bento Apps");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable New Segments Homepage?", "uncheck");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.integrationServicesPage().enableMarketingCloud("SFMC");
		// navigate to Whitelabel -> Integration Service >> set SFMC Folder name
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.integrationServicesPage().setSFMCFolderName(sFMCFolderName);

		// navigate to Guests -> Segments >> update segment name
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.segmentsBetaPage().findSegmentAndSelectSegment(dataSet.get("segment"));
		pageObj.segmentsBetaPage().updateSegmentName(segmentName);
		pageObj.segmentsBetaPage().saveSegment(segmentName);
		int segmentDefCount = pageObj.segmentsBetaPage().getSegmentCountSegmentDefination();

		// navigate to support >> schedules and run
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Segment Export Schedules");
		pageObj.schedulespage().findMassCampaignNameandRun(segmentName);

		// wait for schedule export
		pageObj.utils().longWaitInSeconds(20);
		// get SFMC access Token
		Response getTokenResponse = pageObj.endpoints().getSFMCAccessToken(dataSet.get("subdomain"),
				utils.decrypt(dataSet.get("clientId")), utils.decrypt(dataSet.get("clientSecret")));
		Assert.assertEquals(getTokenResponse.getStatusCode(), 200);
		TestListeners.extentTest.get().pass("SFMC access token retrieved successfully");
		String access_token = getTokenResponse.jsonPath().get("access_token").toString();

		// get sfmc segment export details
		Response getSegmentExportDetails = pageObj.endpoints().getSFMCSegmentExportDetails(dataSet.get("subdomain"),
				access_token, segmentName);
		Assert.assertEquals(getSegmentExportDetails.getStatusCode(), 200);
		TestListeners.extentTest.get().pass("Get sfmc segment export details successfull");
		String segName = getSegmentExportDetails.jsonPath().get("customObjectKey").toString();
		String count = getSegmentExportDetails.jsonPath().get("count").toString();
		Assert.assertEquals(segmentName, segName, "Segment name did not match for SFMC export");
		TestListeners.extentTest.get().pass("Segment name match for SFMC export");
		Assert.assertEquals(count, Integer.toString(segmentDefCount),
				"Segment guest count did not match for SFMC export");
		TestListeners.extentTest.get().pass("Segment guest count did not match for SFMC export");

		// Get folder category_id
		Response getSFMCFolderCategoryIDResponse = pageObj.endpoints().getSFMCFolderCategoryID(dataSet.get("subdomain"),
				access_token, segmentName);
		Assert.assertEquals(getSFMCFolderCategoryIDResponse.getStatusCode(), 200);
		// only one test case, hence, using string function to parse not xml parser
		String strResponse = getSFMCFolderCategoryIDResponse.body().asString();
		String categoryID = strResponse.substring(strResponse.indexOf("<CategoryID>") + 12,
				strResponse.indexOf("</CategoryID>"));
		Assert.assertNotNull(categoryID, "Folder categoryID not found");
		logger.info("Folder categoryID retreived as: " + categoryID);
		TestListeners.extentTest.get().pass("Get SFMC folder category_id successful");

		// Delete created folder
		Response deleteSFMCFolderResponse = pageObj.endpoints().deleteSFMCFolder(dataSet.get("subdomain"), access_token,
				categoryID);
		Assert.assertEquals(deleteSFMCFolderResponse.getStatusCode(), 200, "Delete SFMC folder failed");
		// Verify if the response contains "Folder deleted successfully."
		Assert.assertTrue(deleteSFMCFolderResponse.getBody().asString().contains("Folder deleted successfully."),
				"Folder deletion message not found in response");
		TestListeners.extentTest.get().pass("Delete created SFMC folder successful");
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
