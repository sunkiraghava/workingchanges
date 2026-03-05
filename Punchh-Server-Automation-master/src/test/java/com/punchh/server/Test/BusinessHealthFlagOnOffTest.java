package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class BusinessHealthFlagOnOffTest {
	static Logger logger = LogManager.getLogger(BusinessHealthFlagOnOffTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	Properties prop;
	private Utilities utils;
	SeleniumUtilities selUtils;

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

	@Test(description = "SQ-T5062 Verify error message when invalid flag name is given", groups = { "regression",
			"dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5062_invalidFlagNameOnFeatureRollout() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.menupage().navigateToSubMenuItem("SRE", "Feature Rollouts");
		logger.info("Feature Rollout page opened successfully");
		TestListeners.extentTest.get().pass("Feature Rollout page opened successfully");

		pageObj.sidekiqPage().listAllBusinessesDrpDownInFeatureRollouts("sbgdhhjfjrek", "Integer", "5",
				"AutoSeven - PointToCurrency");
		String text = utils.getLocator("featureRolloutsPage.errorMsg").getText();
		Assert.assertEquals(text,
				"Feature Rollout: Flag Name sbgdhhjfjrek entered doesn't match the Business Preferences");
		logger.info("error message verified as : " + text);
		TestListeners.extentTest.get().pass("error message verified as : " + text);

	}

	@Test(description = "SQ-T5061 Verify businesses available when Enable Business Health? flag is disable"
			+ "SQ-T5060 Verify businesses available when Enable Business Health? flag is enable"
			+ "MPC-T807 Verify businesses available when Enable Business Health? flag is enable", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T5061_businessFlagDisabled() throws Exception {

		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabsNew(dataSet.get("dashboardTab"));
		pageObj.dashboardpage().businessHealthTabDropdown(dataSet.get("color"));

		// pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		String query = "select count(*) from businesses";
		pageObj.singletonDBUtilsObj();
		String count = DBUtils.executeQueryAndGetColumnValue(env, query, "count(*)");
		int businessCount = Integer.parseInt(count);
		// int businessCount = pageObj.instanceDashboardPage().getBusinessCount();

		// navigate to Feature Rollouts
		pageObj.menupage().navigateToSubMenuItem("SRE", "Feature Rollouts");
		logger.info("Feature Rollout page opened successfully");
		TestListeners.extentTest.get().pass("Feature Rollout page opened successfully");

		// business health flag disabled
		utils.waitTillPagePaceDone();
		WebElement drpLocation = utils.getLocator("featureRolloutsPage.listAllBusinessesDrpDown");
		Select select = new Select(drpLocation);
		List<WebElement> options = select.getOptions();
		int size = options.size();
		Assert.assertEquals(size, businessCount, "List size does not matched");
		logger.info(
				"List all Businesses dropdown show all the businesses available in the stack and the count is " + size);
		TestListeners.extentTest.get().pass(
				"List all Businesses dropdown show all the businesses available in the stack and the count is " + size);

		// business health flag enabled
		utils.getLocator("featureRolloutsPage.buisnessHealthFlag").click();
		boolean flag = pageObj.dashboardpage().buisnessHealthDrpDwnList(dataSet.get("color"));
		Assert.assertTrue(flag, "Business health dropdown does not show the option selected");
		logger.info("Business health dropdown show the option selected ie : " + dataSet.get("color"));
		TestListeners.extentTest.get()
				.pass("Business health dropdown show the option selected ie :" + dataSet.get("color"));

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
