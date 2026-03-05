package com.punchh.server.Test;

import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class FaceboookSignupLoginTest {
	static Logger logger = LogManager.getLogger(FaceboookSignupLoginTest.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	Properties prop;
	String iFrameEmail;
	PageObj pageObj;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod() {
		prop = Utilities.loadPropertiesFile("config.properties");
		// String browserName=prop.getProperty("browserName");
		driver = new BrowserUtilities().launchBrowser();
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		prop = Utilities.loadPropertiesFile("config.properties");
		pageObj = new PageObj(driver);
	}

	@Test
	public void facebookSignUp() {
		logger.info("== Facebook signin and login test ==");
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(prop.getProperty("instanceUrl"));
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(prop.getProperty("slug"));
//		if (pageObj.instanceDashboardPage().verifyGuestsPresence(prop.getProperty("fbUsername")))
//			pageObj.guestTimelinePage().deleteGuest(prop.getProperty("fbUsername"));
		pageObj.iframeSingUpPage().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + prop.getProperty("slug"));
		iFrameEmail = pageObj.iframeSingUpPage().facebookSignUp(prop.getProperty("fbUsername"));
		// driver.close();
	}

	@Test(dependsOnMethods = { "facebookSignUp" })
	public void facebookSignIn() {
		pageObj.iframeSingUpPage().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + prop.getProperty("slug"));
		iFrameEmail = pageObj.iframeSingUpPage().facebookLogin(prop.getProperty("fbUsername"),
				prop.getProperty("fbPassword"));
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		Utilities.screenShotCapture(driver, this.getClass().getName());
		driver.close();
		driver.quit();
	}
}
