package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
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
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CompressionCampaigTest {

	private static Logger logger = LogManager.getLogger(CompressionCampaigTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String run = "ui";
	private String campaignName;

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
	}

	@Test(description = "SQ-T7260 Compression campaign basic fucntionality", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T7260_verifyCompressionCampaignBasicFucntionality() throws Exception {

		campaignName = CreateDateTime.getUniqueString("Automation CompressionCampaign");
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// set campaign name in what tab
		pageObj.signupcampaignpage().setCampaignName(campaignName);
		pageObj.signupcampaignpage().clickNextBtn();

		// set campaign details in whom tab
		pageObj.signupcampaignpage().setTargetDaysForCheckin(dataSet.get("targetDays"));
		pageObj.signupcampaignpage().setMinMaxRange(dataSet.get("minRange"), dataSet.get("maxRange"));
		pageObj.signupcampaignpage().setSchedulrFor(dataSet.get("scheduleFor"));
		pageObj.signupcampaignpage().setPushNotification(campaignName);
		pageObj.signupcampaignpage().setEmailSubject(dataSet.get("emailSubject"));
		pageObj.signupcampaignpage().setEmailTemplate(dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().clickNextBtn();

		// set campaign details in when tab
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// user signup via api2
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Pos api checkin
		Response resp = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"), "2.0");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(),
				"Status code 200 did not matched for post chekin api");

		String CompressioncamName = pageObj.guestTimelinePage().getCampaignNameByNotificationsAPI(campaignName,
				dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertTrue(CompressioncamName.equalsIgnoreCase(campaignName), "Campaign name did not matched");

		TestListeners.extentTest.get().pass(
				"Compression campaign details: push notification, campaign name validated successfully on timeline");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
