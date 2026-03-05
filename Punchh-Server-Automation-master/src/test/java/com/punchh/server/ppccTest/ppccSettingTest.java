/*
 * @author Aman Jain (aman.jain@partech.com)
 * @brief This class contains UI test cases for the POS Control Center Settings tab.
 * @fileName ppccSettingTest.java
 */

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
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

@Listeners(TestListeners.class)
public class ppccSettingTest {
	static Logger logger = LogManager.getLogger(ppccSettingTest.class);
	public WebDriver driver;
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
				pageObj.readData().getJsonFilePath("ui", env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T6168 Verify the search functionality in the settings tab", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6168_verifySearchFunctionalityInSettingsTab() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccSettingsPage().navigateToSettingTab();

		String posItemInSelectPos = pageObj.utils().getLocatorValue("ppccSettingsPage.itemEntryInSelectPosColumn");
		String searchItem = driver.findElement(By.xpath(posItemInSelectPos)).getText();
		boolean isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectPos(searchItem);
		Assert.assertTrue(isItemFound, String.format("Not all List contain '%s'", searchItem));
		pageObj.utils().logPass("All List contain the search item.");

		String posItemInSelectedPos = pageObj.utils().getLocatorValue("ppccSettingsPage.itemEntryInSelectedPosColumn");
		searchItem = driver.findElement(By.xpath(posItemInSelectedPos)).getText();
		isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectedPos(searchItem);
		Assert.assertTrue(isItemFound, String.format("Not all List contain '%s'", searchItem));
		pageObj.utils().logPass("All List contain the search item.");
	}

	@Test(description = "SQ-T6156 Validate the functionality of assigning and unassigning the POS in the settings tab", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6156_verifyAssigningAndUnassigningPosInSettingsTab() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccSettingsPage().navigateToSettingTab();

		String posItemInSelectPos = pageObj.utils().getLocatorValue("ppccSettingsPage.itemEntryInSelectPosColumn");
		String searchItem = driver.findElement(By.xpath(posItemInSelectPos)).getText();
		pageObj.ppccSettingsPage().assignPolicyToBusiness(searchItem);

		boolean isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectedPos(searchItem);
		Assert.assertTrue(isItemFound, String.format("List do not contain '%s'", searchItem));
		pageObj.utils().logPass("List contain the search item.");

		pageObj.ppccSettingsPage().unassignPolicyFromBusiness(searchItem);
		isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectedPos(searchItem);
		Assert.assertFalse(isItemFound, String.format("List contain '%s'", searchItem));
		pageObj.utils().logPass("List do not contain the search item.");

		isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectPos(searchItem);
		Assert.assertTrue(isItemFound, String.format("List do not contain '%s'", searchItem));
		pageObj.utils().logPass("List contain the search item.");
	}

	@Test(description = "SQ-T6157 Validate the message on POS while unassinging it in the settings tab when the pos is attahced to policy.", groups = {
			"regression" }, priority = 1)
	@Owner(name = "Aman Jain")
	public void SQ_T6157_verifyMessageOnPosWhenPosIsAttachedToPolicyWhileUnassinging() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
		pageObj.ppccSettingsPage().navigateToSettingTab();

		String posItemInSelectPos = pageObj.utils().getLocatorValue("ppccSettingsPage.itemEntryInSelectPosColumn");
		String assignedPos = driver.findElement(By.xpath(posItemInSelectPos)).getText();
		pageObj.ppccSettingsPage().assignPolicyToBusiness(assignedPos);

		pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
		pageObj.ppccPolicyPage().clickOnCreatePolicyButton();
		String createdPolicy = pageObj.ppccPolicyPage().defineGeneralSettings(assignedPos);
		pageObj.ppccPolicyPage().clickOnSaveAsDraftButton();
		String matchPolicy = pageObj.ppccPolicyPage().searchPolicy(createdPolicy);
		Assert.assertEquals(matchPolicy, createdPolicy + " " + dataSet.get("expectedStatus"),
				"Policy is not created successfully");

		pageObj.ppccSettingsPage().navigateToSettingTab();
		boolean isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectedPos(assignedPos);
		Assert.assertTrue(isItemFound, String.format("List do not contain '%s'", assignedPos));
		pageObj.utils().logPass("List contain the search item.");

		pageObj.utils().getLocator("ppccSettingsPage.itemEntryInSelectedPosColumn").click();
		String errorMsg = dataSet.get("errorMsg");
		String errorMessageOnUi = pageObj.utils().getLocator("ppccSettingsPage.errorMsg").getText();
		Assert.assertEquals(errorMessageOnUi, errorMsg, "Error message is not displayed");

		String token = pageObj.endpoints().getAuthTokenForPPCC(Integer.parseInt(dataSet.get("businessId")),
				dataSet.get("slug"), dataSet.get("businessName"));
		int id = pageObj.ppccUtilities().getPolicyId(createdPolicy, token);
		pageObj.ppccUtilities().deletePolicy(id, token);

		pageObj.ppccSettingsPage().navigateToSettingTab();
		pageObj.ppccSettingsPage().unassignPolicyFromBusiness(assignedPos);
		isItemFound = pageObj.ppccSettingsPage().validateSearchFunctionalityForSelectedPos(assignedPos);
		Assert.assertFalse(isItemFound, String.format("List contain '%s'", assignedPos));
		pageObj.utils().logPass("List do not contain the search item.");
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
