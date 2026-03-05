package com.punchh.server.smokeTest;

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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeSignUpAndLoginTest {
	private static Logger logger = LogManager.getLogger(IframeSignUpAndLoginTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("config.properties");
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

	@Test(description = "SQ-T2193 Iframe signup signout", groups = { "sanity" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2193_iframeSignup() throws InterruptedException {

		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String iFrameEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();

		// iframe login and logout
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail, prop.getProperty("iFramePassword"));
		pageObj.iframeSingUpPage().iframeSignOut();

		// Verify guest on timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelWebEmail();
		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		Assert.assertEquals(dataSet.get("joinedChannel"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(iFrameEmail, guestTimelineEmail, "Guest email did not matched on timeline header");

	}

	@Test(description = "SQ-T2193 Iframe invalid password login and forgot password", groups = {
			"sanity" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2193_iframeForgotPassword() throws InterruptedException {

		// iFrame Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String FrameEmail = pageObj.iframeSingUpPage().iframeSignUp();
		pageObj.iframeSingUpPage().iframeSignOut();

		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		// iframe invalid login
		String msg = pageObj.iframeSingUpPage().iframeInvalidLogin(FrameEmail, "123456");
		Assert.assertEquals(msg, "Incorrect information submitted. Please retry.",
				"Incorrect Password help message did not matched");
		TestListeners.extentTest.get().pass("Iframe invalid password login validated :" + msg);

		// iframe forgot password
		String resetMsg = pageObj.iframeSingUpPage().forgotPassword(FrameEmail);
		Assert.assertEquals(resetMsg,
				"If an account currently exists within our system, an email will be sent to the associated address with instructions on resetting the password.",
				"Reset Password help message did not matched");
		TestListeners.extentTest.get().pass("Iframe forgot password validated :" + resetMsg);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
