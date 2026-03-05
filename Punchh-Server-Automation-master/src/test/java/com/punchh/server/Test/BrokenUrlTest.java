package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
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
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class BrokenUrlTest {
	private static Logger logger = LogManager.getLogger(BrokenUrlTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Properties prop;
	Utilities utils;

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
		utils = new Utilities(driver);
	}

	// hardik
	@Test(description = "SQ-T5689 Verify Support Portal Link is working and re-directing to valid page", groups = {
			"nonNightly" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T5689_brokenUrlLink() throws Exception {

		// login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// checking Enable New SideNav, Header and Footer checkbox
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		String checkBox = pageObj.dashboardpage().checkBoxResponse("Enable New SideNav, Header and Footer?");
		logger.info("Enable New SideNav, Header and Footer? flag is " + checkBox);
		TestListeners.extentTest.get().info("Enable New SideNav, Header and Footer? flag is " + checkBox);

		// navigate to Dashboard
		pageObj.menupage().clickDashboardMenu();
		utils.brokenUiLinks();

		pageObj.dashboardpage().clickOnHelpButton();
		pageObj.dashboardpage().verifyHelpPage();

		if (checkBox.equalsIgnoreCase("true")) {

			pageObj.dashboardpage().clickOnSupportPortal();
			pageObj.dashboardpage().verifyHelpPage();

			pageObj.dashboardpage().clickfooterLinkButton("status");
			pageObj.dashboardpage().checkStatusOption();

			pageObj.dashboardpage().clickfooterLinkButton("developers");
			pageObj.dashboardpage().checkDeveloperOption();

			pageObj.dashboardpage().clickfooterLinkButton("contact");
			pageObj.dashboardpage().checkContactOption();

			pageObj.dashboardpage().clickfooterLinkButton("about");
			pageObj.dashboardpage().checkAboutOption();

			pageObj.dashboardpage().clickfooterLinkButton("blog");
			pageObj.dashboardpage().checkBlogOption();

			pageObj.dashboardpage().clickfooterLinkButton("security");
			pageObj.dashboardpage().checkSecurityOption();

			pageObj.dashboardpage().clickfooterLinkButton("privacy");
			pageObj.dashboardpage().checkPrivacyOption();
		}

		else {

			pageObj.dashboardpage().clickfooterLinkButton("Status");
			pageObj.dashboardpage().checkStatusOption();

			pageObj.dashboardpage().clickfooterLinkButton("Developers");
			pageObj.dashboardpage().checkDeveloperOption();

			pageObj.dashboardpage().clickfooterLinkButton("Contact");
			pageObj.dashboardpage().checkContactOption();

			pageObj.dashboardpage().clickfooterLinkButton("About");
			pageObj.dashboardpage().checkAboutOption();

			pageObj.dashboardpage().clickfooterLinkButton("Blog");
			pageObj.dashboardpage().checkBlogOption();

			pageObj.dashboardpage().clickfooterLinkButton("Security");
			pageObj.dashboardpage().checkSecurityOption();

			pageObj.dashboardpage().clickfooterLinkButton("Privacy");
			pageObj.dashboardpage().checkPrivacyOption();
		}

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
