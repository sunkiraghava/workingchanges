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

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class SsoSignupTest {
	private static Logger logger = LogManager.getLogger(SsoSignupTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String ssoEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
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

	@Test(description = "SQ-T2256 SSO SignUp Validation", groups = { "sanity" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2256_ssoSignUpValidation() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		// SSO signup
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to menu -> submenu
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "OAuth Apps");
		pageObj.oAuthAppPage().goToSsoSignUp();
		ssoEmail = pageObj.iframeSingUpPage().generateEmail();
		pageObj.ssoLoginSingupPage().signUpViaSso(ssoEmail);
		// verify guest on timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(ssoEmail);

		String guestTimelineEmail = pageObj.guestTimelinePage().getGuestTimelineEmail();
		String joiningchannel = pageObj.guestTimelinePage().getGuestJoiningChannelWebEmail();
		Assert.assertEquals(dataSet.get("joinedChannel"), joiningchannel,
				"Joined channel guest details did not matched");
		Assert.assertEquals(ssoEmail, guestTimelineEmail, "Guest email did not matched on timeline header");
		pageObj.utils().logPass("Successfully verified guest email and joined channel");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
