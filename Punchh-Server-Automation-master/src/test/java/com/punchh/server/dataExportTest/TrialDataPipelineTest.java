package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.pages.SchedulePage;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class TrialDataPipelineTest {
	static Logger logger = LogManager.getLogger(TrialDataPipelineTest.class);
	public WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private Properties prop;
	SchedulePage schedulePage;
	String exportName;
	private PageObj pageObj;
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String env, run = "ui";
	private String sTCName;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		schedulePage = new SchedulePage(driver);
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	// hardik
	@SuppressWarnings({ "static-access" })
	@Test(description = "SQ-T5318 Verification of Trial Data Pipeline Creation", priority = 0)
	public void T5318_VerifyDataTrialPipeline() throws Exception {

		boolean messageFlag = false;
		String dataPipelineName = "Auto_DataTrialPipeline_" + CreateDateTime.getTimeDateString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_data_pipeline", "check");
		pageObj.dashboardpage().updateCheckBox();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Data Pipeline");
		boolean flag = pageObj.dataExportPage().checkTrialPipelineVisibility() ;
		if (flag) {

			String availablePipelineName = pageObj.dataExportPage().pipelineName();
			pageObj.dataExportPage().deleteDataPipeline(availablePipelineName);

			String query = "delete from `data_pipelines` where `business_id` = " + dataSet.get("business_id") + ";";
			int rs = DBUtils.executeUpdateQuery(env, query);
			Assert.assertEquals(rs, 1);
			utils.refreshPage();
		}
		else {
			String query2 = "delete from data_pipelines where business_id = '" + dataSet.get("business_id") + "';" ;
			DBUtils.executeUpdateQuery(env, query2);
			utils.refreshPage();
		}

		String currentURL = driver.getCurrentUrl();
		utils.duplicateTab(currentURL);

		utils.switchToParentWindow();

		pageObj.dataExportPage().clickOnStartATrial();

		String currentDate = utils.getCurrentDate("EEEE, dd MMMM yyyy");
		String pastDate = CreateDateTime.getYesterdaysDateInGivenFormate("EEEE, dd MMMM yyyy");
		String futureDate = CreateDateTime.getFutureDateTimeInGivenFormate(31, "EEEE, dd MMMM yyyy");

		pageObj.dataExportPage().validateTrialStartDate("Previous", pastDate, true);
		pageObj.dataExportPage().clickOnTrialPipelineName();
		pageObj.dataExportPage().validateTrialStartDate("After 30 Days", futureDate, true);
		pageObj.dataExportPage().clickOnTrialPipelineName();
		pageObj.dataExportPage().validateTrialStartDate("Current", currentDate, false);

		pageObj.dataExportPage().createDataPipeline(dataPipelineName, dataSet.get("destinationType"),
				dataSet.get("destinationURL"), dataSet.get("destinationFileType"));

		String message = pageObj.dataExportPage().verifySuccessMessage("1");
		if (message.equalsIgnoreCase("Pipeline has been created successfully")
				|| message.equalsIgnoreCase("Create Data Pipeline")) {
			messageFlag = true;
		}
		Assert.assertTrue(messageFlag, "Trial Data Pipeline Success Message is not visible");
		logger.info("Trial Data Pipeline is created successfully and Success message is " + message);
		TestListeners.extentTest.get()
				.pass("Trial Data Pipeline is created successfully and Success message is " + message);

		String currentDateMessage = utils.getCurrentDate("MMM dd, yyyy");
		String futureDateMessage = CreateDateTime.getFutureDateTimeInGivenFormate(13, "MMM dd, yyyy");

		pageObj.dataExportPage().verifyActivationDaysMessage(currentDateMessage, futureDateMessage);
		List<String> setOfWindowIds = selUtils.getAllOpenedWindowId();
		logger.info("Parent id = " + setOfWindowIds.get(1));
		selUtils.switchToWindow(setOfWindowIds.get(1));

		pageObj.dataExportPage().clickOnStartATrial();
		pageObj.dataExportPage().validateTrialStartDate("Current", currentDate, false);
		pageObj.dataExportPage().createDataPipeline(dataPipelineName, dataSet.get("destinationType"),
				dataSet.get("destinationURL"), dataSet.get("destinationFileType"));

		String message2 = pageObj.dataExportPage().verifySuccessMessage("1");
		Assert.assertEquals(message2, "Trial pipeline error : Business is allowed to create only 1 Pipeline.",
				"Duplicating Trial Data Pipeline is Successful");
		logger.info(
				"The system should prevent the creation of a second trial pipeline for the same business, displaying an appropriate error message "
						+ message2);
		TestListeners.extentTest.get().pass(
				"The system should prevent the creation of a second trial pipeline for the same business, displaying an appropriate error message "
						+ message2);

		utils.switchToParentWindow();

		String msg = pageObj.dataExportPage().verifyTestConnection();
		Assert.assertTrue(msg.contains("This is a trial pipeline, running"),
				"Test Connection Success Message did not matched");
		logger.info("Test Connection message is successfully verified i.e. " + msg);
		TestListeners.extentTest.get().pass("Test Connection message is successfully verified i.e. " + msg);

		String pipelineName = pageObj.dataExportPage().pipelineName();
		pageObj.dataExportPage().clickEditPipeline();
		String dataPipelineNameNew = "Auto_DataTrialPipelie_1_" + CreateDateTime.getTimeDateString();
		pageObj.dataExportPage().editNameTrialPipeline(dataPipelineNameNew);
		pageObj.dataExportPage().clickSaveButton();

		String messageUpdation = pageObj.dataExportPage().verifySuccessMessage("1");
		Assert.assertTrue(messageUpdation.contains("Pipeline has been updated successfully"),
				"Trial Data Pipeline Success Message is not visible");
		logger.info("Trial Data Pipeline is updated successfully and Success message is " + messageUpdation);
		TestListeners.extentTest.get()
				.pass("Trial Data Pipeline is updated successfully and Success message is " + messageUpdation);

		utils.refreshPage();
		String pipelineNameNew = pageObj.dataExportPage().pipelineName();

		Assert.assertNotEquals(pipelineName, pipelineNameNew,
				"Trial Data Pipeline is not updated Successful after Edit");
		logger.info("Trial Data Pipeline is Edited successfully");
		TestListeners.extentTest.get().pass("Trial Data Pipeline is Edited successfully");

		// delete data pipeline

		pageObj.dataExportPage().deleteDataPipeline(pipelineNameNew);
		String query = "delete from `data_pipelines` where `business_id` = " + dataSet.get("business_id") + ";";
		int rs = DBUtils.executeUpdateQuery(env, query);
		Assert.assertEquals(rs, 1);
		logger.info("Trial Data Pipeline is Deleted successfully from DB");
		TestListeners.extentTest.get().info("Trial Data Pipeline is Deleted successfully from DB");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
	}
}
