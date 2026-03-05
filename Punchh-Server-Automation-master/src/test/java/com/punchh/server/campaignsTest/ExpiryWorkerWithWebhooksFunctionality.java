package com.punchh.server.campaignsTest;

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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ExpiryWorkerWithWebhooksFunctionality {

	private static Logger logger = LogManager.getLogger(ExpiryWorkerWithWebhooksFunctionality.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
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
		utils = new Utilities();

	}

	// shashank -- incomplete script .. will commit again after completion
	@Test(description = "MPC-T828  (SQ-T5065) / Verify UpdateRewardExpiryWorker functionality when enable_synchronous_reward_publish: ON and enable_webhooks_management: ON"
			+ "SQ-T5065 Verify UpdateRewardExpiryWorker functionality when Bulk Reward publishing batch size: 100, enable_synchronous_reward_publish: OFF and enable_webhooks_management: ON")
	public void T828_VerifyUpdateRewardExpiryWorkerFunctionalityEnableSynchronousRewardFlagOn() throws Exception {

		// user signup using api2
		String iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));

		String query3 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query3, "preferences");

		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_synchronous_reward_publish", dataSet.get("business_id"));
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_webhooks_management", dataSet.get("business_id"));

		String massCampaignName = "AutomationMassCampaignForWebhookTest_" + CreateDateTime.getTimeDateString();

		String dateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to cockpit -> earning
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(massCampaignName, dataSet.get("giftType"),
				dataSet.get("redeemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Schedule created successfully Success message did not displayed....");

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");

		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().findMassCampaignNameandRun(massCampaignName);

		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");

        pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		boolean result = pageObj.webhookManagerPage().verifyTheSrtatusLogs("campaign_name", massCampaignName);
		Assert.assertTrue(result, massCampaignName + " value is not visible or not matched in json from UI");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
