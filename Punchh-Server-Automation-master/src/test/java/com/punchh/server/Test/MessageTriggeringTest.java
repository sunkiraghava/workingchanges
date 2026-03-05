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
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MessageTriggeringTest {

	static Logger logger = LogManager.getLogger(MessageTriggeringTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
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
		logger.info(sTCName + " ==> " + dataSet);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2804 INTD-441_Verify message triggering via API- Failure", groups = { "regression",
			"dailyrun" }, priority = 0)
	@Owner(name = "Ashwini Shetty")
	public void T2802_VerifyMessageTriggering() throws InterruptedException {
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().clickCockPitMenu();
		// pageObj.menupage().clickCockpitDashboardLink();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().enableSMS();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");

		pageObj.dashboardpage().smsAdapter(dataSet.get("adapter"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");

		// SignupAPI
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		long phoneNumber = (long) (Math.random() * Math.pow(10, 10));
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"), phoneNumber);
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");
		String phone = signUpResponse.jsonPath().get("user.phone");
		logger.info("User phone number is :- +91" + phone);
		TestListeners.extentTest.get().info("User phone number is :- +91" + phone);

		// Check the user using phone number
		pageObj.menupage().navigateToSubMenuItem("Support", "Integration Services Logs");
		String phone1 = "+91" + phone;
		pageObj.dashboardpage().integrationServiceLogs();
		boolean status = pageObj.dashboardpage().failureSMSLogs(phone1);
		Assert.assertTrue(status, "Failure SMS Logs Displayed");
		logger.info("SMS Logs Displayed in Integration Sevrice Logs");
		TestListeners.extentTest.get().pass("SMS Logs Displayed in Integration Sevrice Logs");
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
