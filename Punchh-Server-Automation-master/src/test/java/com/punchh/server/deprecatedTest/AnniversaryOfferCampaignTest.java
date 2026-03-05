package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.text.ParseException;
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
public class AnniversaryOfferCampaignTest {
	private static Logger logger = LogManager.getLogger(AnniversaryOfferCampaignTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private String campaignName;

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
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

// as discussed with campaigns team to be disabled
	@Test(description = "SQ-T6823 Verify user cannot create another schedules for anniversary campaigns when 1 activate schedule is present", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Piyush Kumar")
	public void T6823_verifyCreationOfAnniversaryCamScheduleWhen1activeSchedulePresent() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();

		boolean isAlreadyActivated = pageObj.schedulespage().activateScheduleIfdeactivated();
		if (!isAlreadyActivated) {
			pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
			pageObj.schedulespage().clickOnEditSchedule();
		}

		pageObj.schedulespage().modifyURLAndRedirect();
		pageObj.schedulespage().createSchedule();
		String actualErrorMsg = pageObj.schedulespage().getErrorMsgOnSchedulePage();
		String expectedErrorMsg = dataSet.get("errorMsg");
		Assert.assertEquals(actualErrorMsg, expectedErrorMsg, "Error message did not match");
		pageObj.utils().logPass("Error message is displayed successfully as " + actualErrorMsg);

	}

	// as discussed with campaigns team to be disabled
	@Test(description = "SQ-6827 Verify user cannot create another schedules for anniversary campaigns when 1 deactivate schedule is present ", groups = {
			"regression", "dailyrun" }, priority = 4)
	@Owner(name = "Piyush Kumar")
	public void T6827_verifyCreationOfAnniversaryCamScheduleWhen1DeactiveSchedulePresent() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();
		boolean isAlreadyDeactivated = pageObj.schedulespage().deactivateScheduleIfActivated();
		if (!isAlreadyDeactivated) {
			pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
			pageObj.schedulespage().clickOnEditSchedule();
		}

		pageObj.schedulespage().modifyURLAndRedirect();
		pageObj.schedulespage().createSchedule();
		String actualErrorMsg = pageObj.schedulespage().getErrorMsgOnSchedulePage();
		String expectedErrorMsg = dataSet.get("errorMsg");
		Assert.assertEquals(actualErrorMsg, expectedErrorMsg, "Error message did not match");
		pageObj.utils().logPass("Error message is displayed successfully as " + actualErrorMsg);

		pageObj.schedulespage().selectScheduleType("Anniversary Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();
		pageObj.schedulespage().activateScheduleIfdeactivated();
	}

	// as discussed with campaigns team to be disabled
	@Test(description = "SQ-T6824 Verify creation of new recall schedule when 1 active schedule is present ", groups = {
			"regression", "dailyrun" }, priority = 6)
	@Owner(name = "Shubham Gupta")
	public void T6824_verifyCreationOfRecallCamScheduleWhen1ActiveSchedulePresent()
			throws InterruptedException, ParseException {

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();

		boolean isAlreadyActivated = pageObj.schedulespage().activateScheduleIfdeactivated();
		if (!isAlreadyActivated) {
			pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
			pageObj.schedulespage().clickOnEditSchedule();
		}

		pageObj.schedulespage().modifyURLAndRedirect();
		pageObj.schedulespage().createSchedule();
		String actualErrorMsg = pageObj.schedulespage().getErrorMsgOnSchedulePage();
		String expectedErrorMsg = dataSet.get("errorMsg");
		Assert.assertEquals(actualErrorMsg, expectedErrorMsg, "Error message did not match");
		pageObj.utils().logPass("Error message is displayed successfully as " + actualErrorMsg);

	}

	// as discussed with campaigns team to be disabled
	@Test(description = "SQ-6825 Verify creation of new recall schedule when 1 deactivate schedule is present ", groups = {
			"regression", "dailyrun" }, priority = 7)
	@Owner(name = "Shubham Gupta")
	public void T6825_verifyCreationOfRecallCamScheduleWhen1DeactiveSchedulePresent()
			throws InterruptedException, ParseException {

//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate recall schedule
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		// utils.waitTillPagePaceDone();
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();
		boolean isAlreadyDeactivated = pageObj.schedulespage().deactivateScheduleIfActivated();
		if (!isAlreadyDeactivated) {
			pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
			pageObj.schedulespage().clickOnEditSchedule();
		}

		pageObj.schedulespage().modifyURLAndRedirect();
		pageObj.schedulespage().createSchedule();
		String actualErrorMsg = pageObj.schedulespage().getErrorMsgOnSchedulePage();
		String expectedErrorMsg = dataSet.get("errorMsg");
		Assert.assertEquals(actualErrorMsg, expectedErrorMsg, "Error message did not match");
		pageObj.utils().logPass("Error message is displayed successfully as " + actualErrorMsg);

		// activate the schedule again
		pageObj.schedulespage().selectScheduleType("Recall Campaign Schedule");
		pageObj.schedulespage().clickOnEditSchedule();
		pageObj.schedulespage().activateScheduleIfdeactivated();
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().deleteCampaignFromDb(campaignName, env);
		driver.quit();
		logger.info("Browser closed");
	}
}