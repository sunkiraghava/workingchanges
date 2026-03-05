package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CollectibleCategoryTest {
	private static Logger logger = LogManager.getLogger(CollectibleCategoryTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env;
	private String run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	private Properties prop;
	private String businessID;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
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
		businessID = null;
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Owner(name = "Shivam Maurya")
	@Test(description = "SQ-T7485 || SQ-T7483 Verify category creation with valid data", groups = "Regression", enabled = true)
	public void SQ_T7483_verifyCategoryCreationWithValidData() {
		businessID = dataSet.get("business_id");
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.collectibleCategoryPage().navigateToCollectibleCategories();
		pageObj.collectibleCategoryPage().clickAddCategoryButton();

		String categoryName = "AutoCat_" + System.currentTimeMillis();
		pageObj.collectibleCategoryPage().enterCategoryName(categoryName);
		String description = "AutoCat_" + System.currentTimeMillis();
		pageObj.collectibleCategoryPage().enterCategoryDescription(description);
		pageObj.mobileconfigurationPage().selectDropdownValue(utils.getLocator("collectibleCategory.statusDropdown"),
				"Active");

		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		String first = pageObj.collectibleCategoryPage().getCategoryName(1);
		Assert.assertTrue(first.startsWith("AutoCat"));

		List<String> all = pageObj.collectibleCategoryPage().getCategoryNames();
		Assert.assertTrue(all.stream().anyMatch(name -> name.startsWith("AutoCat")));

		pageObj.collectibleCategoryPage().clickAddCategoryButton();

		String nameinactive = "AutoCat_" + System.currentTimeMillis();
		pageObj.collectibleCategoryPage().enterCategoryName(nameinactive);
		String descriptionInactive = "AutoCat_" + System.currentTimeMillis();
		pageObj.collectibleCategoryPage().enterCategoryDescription(descriptionInactive);

		pageObj.mobileconfigurationPage().selectDropdownValue(utils.getLocator("collectibleCategory.statusDropdown"),
				"Inactive");

		pageObj.collectibleCategoryPage().clickSaveCategoryButton();

		String firstinactive = pageObj.collectibleCategoryPage().getCategoryName(1);
		Assert.assertTrue(firstinactive.startsWith("AutoCat"));

		List<String> allinactive = pageObj.collectibleCategoryPage().getCategoryNames();
		Assert.assertTrue(allinactive.stream().anyMatch(name -> name.startsWith("AutoCat")));

	}

	@Owner(name = "Shivam Maurya")
	@Test(description = "SQ-T7486 || SQ-T7487 Verify error message for missing mandatory fields,", groups = "Regression", enabled = true)
	public void SQ_T7487_verifyMandatoryFieldsError() {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.collectibleCategoryPage().navigateToCollectibleCategories();
		pageObj.collectibleCategoryPage().clickAddCategoryButton();
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();

		boolean inlineErrorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"collectibleCategory.Inlineerror", MessagesConstants.collectiblecategoryerror,
				dataSet.get("redColorHexCode"), 1);
		String message = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertTrue(inlineErrorMessage);
		Assert.assertEquals(message, MessagesConstants.locationCreateCSVBlankNameError);
		utils.logPass("Mandatory field validation passed");
		pageObj.collectibleCategoryPage().navigateToCollectibleCategories();
		pageObj.collectibleCategoryPage().clickAddCategoryButton();
		String Name = "AutoCat_" + System.currentTimeMillis() + "_" + "A".repeat(260);
		pageObj.collectibleCategoryPage().enterCategoryName(Name);
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();

		boolean errorMessage = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"collectibleCategory.Inlineerror", MessagesConstants.collectiblecategoryfielderror,
				dataSet.get("redColorHexCode"), 1);
		String messages = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertTrue(errorMessage);
		Assert.assertEquals(messages, MessagesConstants.collectiblecategorynameerror);
	}

	@Owner(name = "Shivam Maurya")
	@Test(description = "SQ-T7484 Verify category limit error", groups = "Regression", enabled = true)
	public void SQ_T7484_verifycategorylimitError() {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.collectibleCategoryPage().navigateToCollectibleCategories();
		pageObj.collectibleCategoryPage().clickAddCategoryButton();
		pageObj.collectibleCategoryPage().enterCategoryName("AutoCat_LIMIT_" + System.currentTimeMillis());
		pageObj.collectibleCategoryPage().enterCategoryDescription("Limit test");

		pageObj.mobileconfigurationPage().selectDropdownValue(utils.getLocator("collectibleCategory.statusDropdown"),
				"Active");

		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		String messages = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(messages, MessagesConstants.collectiblelimiterror);
		utils.logPass("Category limit error validation passed");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		DBUtils.deleteCollectibleCategoriesByBusiness(env, businessID);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
