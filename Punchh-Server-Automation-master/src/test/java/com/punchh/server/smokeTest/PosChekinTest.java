package com.punchh.server.smokeTest;

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

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PosChekinTest {
	static Logger logger = LogManager.getLogger(PosChekinTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
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

	@Test(description = "SQ-T2434 Verify PosCheckin with valid details", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2434_verifyPosCheckin() throws InterruptedException {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set pos_scanner_checkin notification on
		pageObj.menupage().navigateToSubMenuItem("Settings", "Notification Templates");
		pageObj.notificationTemplatePage().setPOSScannerCheckinOn();

		// Api1 User creation
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		// apiUtils.verifyResponse(response, "API 1 user signup");
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Pos api checkin
		String key = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String txn = CreateDateTime.getRandomNumberSixDigit() + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos chekin api");
		Assert.assertEquals(resp.jsonPath().get("email").toString(), userEmail.toLowerCase());
		logger.info("Response time for Pos checkin api in milliseconds is :" + resp.getTime());

		// Verify checkin in guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		Assert.assertTrue(pageObj.guestTimelinePage().verifyPosCheckinInTimeLine(key, dataSet.get("amount"),
				dataSet.get("baseConversionRate")), "Error in verifying POS checkin in guest timeline ");
		// verify pos_scanner_checkin notification
		String posScannerNotification = pageObj.guestTimelinePage()
				.getPosScannerCheckinNotification("pos_scanner_checkin");
		Assert.assertEquals(posScannerNotification, "pos_scanner_checkin",
				"Campaign Name: pos_scanner_checkin did not found on timeline");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
