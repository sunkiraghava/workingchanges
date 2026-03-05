package com.punchh.server.deprecatedTest;

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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class AppConfigOrderingCustumTextTest {

	private static Logger logger = LogManager.getLogger(AppConfigOrderingCustumTextTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
		env = prop.getProperty("environment");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@Test(description = "SQ-T3189 Validate that newly added subsection title Ordering Custom Text (Header/Footer Text for Ordering) on Mobile configuration page.", groups = "Regression", priority = 0)
	public void T3189_VerifyOrderingCustomtextinAppConfigTabofMobileconfigurationPage() throws InterruptedException {
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Menu Items
		Thread.sleep(2000);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().clickAppConfigFieldsTab();
		pageObj.mobileconfigurationPage().onMenuCustomTextCheckbox();

	}

	// Amit // disabled as per discussion with Rahul Garg no need to run in
	// regression
	// @Test(description = "SQ-T4455 To validate the \"Locked rewards\" screen and
	// functionalities", priority = 3)
	@Owner(name = "Amit Kumar")
	public void T4455_validateLockedRewardsScreenPartOne() throws Exception {
		// when the locked redeemable count is equal to 5 View More button should not
		// visible
		String bid = dataSet.get("business_id");
		String redeemaleNameLike = dataSet.get("redeemablenameLike");
		String deleteRedeemableQuery = "delete from redeemables where name like '%" + redeemaleNameLike
				+ "%' and business_id=" + bid + ";";
		DBUtils.executeQuery(env, deleteRedeemableQuery);
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a new user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// nagivate to guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messagePointsToUser("", "", dataSet.get("gifttype"),
				dataSet.get("pointsToGiftUser"), dataSet.get("reason"));

		// login through iframe
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(userEmail);
		pageObj.iframeSingUpPage().clickRedeemReward();

		// when view more is not visible
		boolean visible = pageObj.iframeSingUpPage().viewMoreVisible();
		Assert.assertFalse(visible, "view more button is visible");
		logger.info(
				"Verified when the locked redeemable count is equal to 5 then also View More button is not visible");
		TestListeners.extentTest.get().pass(
				"Verified when the locked redeemable count is equal to 5 then also View More button is not visible");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
