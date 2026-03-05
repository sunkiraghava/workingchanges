package com.punchh.server.dataExportTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.Integration2.IntUtils;
import com.punchh.server.pages.PageObj;
import com.punchh.server.pages.SchedulePage;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DataPipelineTableChagnesTest {
	static Logger logger = LogManager.getLogger(DataPipelineTableChagnesTest.class);
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
	private Properties dbConfig;
	private String punchhDBName;
	TestListeners testListObj;
	private IntUtils intUtils;

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
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		exportName = CreateDateTime.getUniqueString("AutoDataExport");
		dbConfig = Utilities.loadPropertiesFile("dbConfig.properties");
		punchhDBName = dbConfig.getProperty(env.toLowerCase() + ".DBName");
		selUtils = new SeleniumUtilities(driver);
		utils = new Utilities(driver);
		intUtils = new IntUtils(driver);
	}

	// shashank
	@Test(description = "SQ-T4546 (1.0)	Verify the SRE changes for the datapipeline for the table changes.", priority = 0)
	public void T4546_VerifySREDataPipelineForTableChanges() throws InterruptedException {

		String dataPipelineName = "AutoDP_" + CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("createDataPipelineURL"));
		pageObj.dataExportPage().createDataPipeline(dataPipelineName, dataSet.get("destinationType"),
				dataSet.get("destinationURL"), dataSet.get("destinationFileType"));
		pageObj.dataExportPage().clickOnSelectTablesActivateButton(dataPipelineName);
		boolean isIncludeResult = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("includeTableName"));
		Assert.assertTrue(isIncludeResult, dataSet.get("includeTableName") + "  data table is not present");

		boolean isIncludeResult2 = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("defualtTableName"));
		Assert.assertTrue(isIncludeResult2, dataSet.get("defualtTableName") + "  data table is not present");

		boolean isIncludeResultSet = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("excludeTableName"));
		Assert.assertFalse(isIncludeResultSet, dataSet.get("excludeTableName") + "  data table is present");

		// delete data pipeline
		pageObj.menupage().navigateToSubMenuItem("Settings", "Data Pipeline");
		pageObj.dataExportPage().deleteDataPipeline(dataPipelineName);

		String dataPipelineName2 = "AutoDP2_" + CreateDateTime.getTimeDateString();
		Thread.sleep(5000);
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness("autofive");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("createDataPipelineURL"));
		pageObj.dataExportPage().createDataPipeline(dataPipelineName2, dataSet.get("destinationType"),
				dataSet.get("destinationURL"), dataSet.get("destinationFileType"));
		pageObj.dataExportPage().clickOnSelectTablesActivateButton(dataPipelineName2);
		boolean isIncludeResult3 = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("includeTableName"));
		Assert.assertFalse(isIncludeResult3, dataSet.get("includeTableName") + "  data table is  present");

		boolean isIncludeResult4 = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("defualtTableName"));
		Assert.assertTrue(isIncludeResult4, dataSet.get("defualtTableName") + "  data table is not present");

		boolean isIncludeResult5 = pageObj.dataExportPage()
				.verifyDataTableIsPresentInDataPipeline(dataSet.get("excludeTableName"));
		Assert.assertTrue(isIncludeResult4, dataSet.get("excludeTableName") + "  data table is not present");

		// delete data pipeline
		pageObj.menupage().navigateToSubMenuItem("Settings", "Data Pipeline");
		pageObj.dataExportPage().deleteDataPipeline(dataPipelineName2);
	}

	// shashank - Commented, this just for POC for data validation in export .
	// @Test(description = "PS-T252 WOW and MOM Report switch Databricks: Verify
	// that Punchh Report option is appearing under stats configuration and can be
	// switched to Databricks."
	// + "DP1-T242 Verify the POS scoreboard works for the Databricks", priority =
	// 0)
	public void PST252_verifyDataBricksValueIsSelectedInPunchreportOption() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.menupage().navigateToSubMenuItem("SRE", "Stats Configuration");
		pageObj.dashboardpage().StatsConfigToDatabrickes("Databricks");
		pageObj.dashboardpage().clickOnUpdateButton();

		String actualValueSelectedPunchhReports = (String) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("Punchh reports");
		Assert.assertEquals(actualValueSelectedPunchhReports, "Databricks",
				"Punchh reports Databricks is not updated ");
		logger.info("Verified that Databricks is selected in Punchh reports drop down ");
		TestListeners.extentTest.get().pass("Verified that Databricks is selected in Punchh reports drop down ");

		String actualValueSelectedPOSSinkScoreboards = (String) pageObj.dashboardpage()
				.getSelectedValueFromDropdown("POS sink scoreboards");
		Assert.assertEquals(actualValueSelectedPOSSinkScoreboards, "Databricks",
				"POS sink scoreboards Databricks is not updated ");
		logger.info("Verified that Databricks is selected in POS sink scoreboards drop down ");
		TestListeners.extentTest.get().pass("Verified that Databricks is selected in POS sink scoreboards drop down ");

		pageObj.dashboardpage().navigateToAllBusinessPage();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Support", "POS Stats");
		pageObj.settingsPage().clickonPOSScoreBoardData();

		String actualDataTableMessage = pageObj.settingsPage().verifyPosScoreBoardDataTableMessage();
		Assert.assertTrue(actualDataTableMessage.startsWith("Data available for last one year not counting today."),
				"Data Table message is not coming ");

		boolean dataIsVisibleResult = pageObj.settingsPage().verifyPosScoreBoardData();
		Assert.assertTrue(dataIsVisibleResult, "Data is not coming in Pos Scoreboard page");

		logger.info("Verified that Data is coming in Pos Scoreboard page ");
		TestListeners.extentTest.get().pass("Verified that Data is coming in Pos Scoreboard page ");

	}

	// Shashank
	@Test(description = "SQ-T5063 Verify success message when valid flag name is given"
			+ "SQ-T5064 Verify error message when invalid datatype is selected")
	public void T808_ValidateSuccessMessageForValidFlagName() throws InterruptedException {

		String expErrorMessage = "Feature Rollout: Flag Name force_index_on_business_anniversary_worker entered doesn't match the Business Preferences";
		String expSuccessMessage = "Successfully set 'copy_to_phone_notification' to 'true' for 1 business(es).";

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.menupage().navigateToSubMenuItem("SRE", "Feature Rollouts");

		pageObj.dashboardpage().createFeaturesRollouts("force_index_on_business_anniversary_worker", "Integer", "5",
				"Demo App");

		String actualMessage = utils.getSuccessMessage();
		Assert.assertEquals(actualMessage, expErrorMessage, expErrorMessage + " expected error message not matched  ");
		logger.info(expErrorMessage + " expected error message  matched  ");
		TestListeners.extentTest.get().pass(expErrorMessage + " expected error message  matched  ");

		pageObj.dashboardpage().createFeaturesRollouts("copy_to_phone_notification", "Boolean", "True", "Demo App");

		String actualMessage1 = utils.getSuccessMessage();
		Assert.assertEquals(actualMessage1, expSuccessMessage,
				expSuccessMessage + " expected success message not matched  ");
		logger.info(expSuccessMessage + " expected success message  matched  ");
		TestListeners.extentTest.get().pass(expSuccessMessage + " expected success message  matched  ");

	}

	@Test(description = "SQ-T3915 Verify Business ID in Data Pipeline Table Config")
	public void T3915_ValidateColumnName() throws InterruptedException {

		String query = "SELECT * FROM data_pipeline_table_configs LIMIT 1";
		boolean columnExists = DBUtils.verifyColumnExistsInQuery(env, query, "business_id");
		Assert.assertTrue(columnExists, "Column 'business_id' does not exist in the query result");
		logger.info("Column 'business_id' exists in the query result");
		TestListeners.extentTest.get().pass("Column 'business_id' exists in the query result");
	}

	@Test(description = "SQ-T3811 Verify list of tables, categories & pipelines are visible on SRE page")
	public void T3811_VerifyDataPipelineListInSRE() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.menupage().navigateToSubMenuItem("SRE", "Data Pipeline");
		pageObj.dataExportPage().verifyDataPipelineList();
		logger.info("Verified that Data Pipeline list is visible on SRE page");
		TestListeners.extentTest.get().pass("Verified that Data Pipeline list is visible on SRE page");
	}

	@Test(description = "SQ-T2879 Verify Data Documentation linked to Data Pipeline")
	public void T2879_VerifyDataDocumentationInDataPipeline() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Data Pipeline");
		pageObj.dataExportPage().verifyDataDictionaryLinkInDataPipeline();
		logger.info("Verified that Data Pipeline list is visible on SRE page");
		TestListeners.extentTest.get().pass("Verified that Data Pipeline list is visible on SRE page");
	}

	@Test(description = "SQ-T3853 New Pipeline: Verify that If an email subscribed email id's are deleted from subscription alert dialog box then it should be deleted from DB as well"
			+ "SQ-T3851 New Pipeline: All the subscribed email id should appear under data pipeline table for particular pipeline id"
			+ "SQ-T3793 Verify the email confirmation following table selection activation request after modifying")
	public void T3853_ValidateEmailSubscriptionDeletion() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Data Pipeline");
		// do not create just click on the pipeline I have already created and is active
		pageObj.dataExportPage().verifyEmailSubscriptionInDataPipeline(dataSet.get("emailId"));
		logger.info("Verified that email subscription works as expected");
		TestListeners.extentTest.get().pass("Verified that email subscription works as expected");
		String query = "SELECT * FROM data_pipelines where id=904";
		boolean isEmailSubscripedPresent = DBUtils.verifyEmailSubscriptionInDB(env, query,
				dataSet.get("emailId"));
		Assert.assertTrue(isEmailSubscripedPresent, "Email subscription is not present in DB");
		logger.info("Verified that email subscription is present in DB");
		TestListeners.extentTest.get().pass("Verified that email subscription is present in DB");
		pageObj.dataExportPage().deleteEmailSubscription();
		logger.info("Deleted email subscription");
		TestListeners.extentTest.get().pass("Deleted email subscription");
		String query1 = "SELECT preferences FROM data_pipelines where id=904";
		boolean isEmailSubscripedDeleted = DBUtils.verifyAlertSubscribersEmptyInDB(env, query1);
		Assert.assertTrue(isEmailSubscripedDeleted, "Email subscription is not deleted in DB");
		logger.info("Verified that email subscription is deleted in DB");
	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		driver.quit();
	}
}
