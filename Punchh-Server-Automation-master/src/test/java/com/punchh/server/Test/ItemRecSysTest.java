package com.punchh.server.Test;

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
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ItemRecSysTest {
	static Logger logger = LogManager.getLogger(ItemRecSysTest.class);
	public WebDriver driver;
	PageObj pageObj;
	String sTCName, env;
	private String run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	Properties prop;
	private Utilities utils;

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

	@Test(description = "SQ-T5175: Update Item Rec Sys Business settings & Menu in Cockpit; "
			+ "SQ-T5345: Verify switching of Item Recommendation option in Cockpit",groups = {"nonNightly" })
	@Owner(name = "Vaibhav Agnihotri")
	public void T5175_ItemRecSysUIPageUpdate() throws InterruptedException {

		// Move to All businesses page and select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Dashboard, ensure the required flag is turned OFF
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(dataSet.get("flagID"), "uncheck", true);

		// Verify that the Item Rec Sys sub menu is not available
		pageObj.menupage().clickDashboardMenu();
		List<String> cockpitSubMenusList = pageObj.menupage().subMenuItems(NewMenu.menu_Cockpit);
		boolean isItemRecSysSubMenuAvailable = cockpitSubMenusList.contains("Item Rec Sys");
		Assert.assertFalse(isItemRecSysSubMenuAvailable, "Item Rec Sys is available in the sub menu.");
		TestListeners.extentTest.get().info("Item Rec Sys is not available in the sub menu as expected.");
		logger.info("Item Rec Sys is not available in the sub menu as expected.");

		// Go to Cockpit > Dashboard, ensure the required flag is turned ON
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndUpdate(dataSet.get("flagID"), "check", true);

		// Verify that the Item Rec Sys sub menu is available. Go to Cockpit > Item Rec
		// Sys
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Item Rec Sys");
		TestListeners.extentTest.get()
				.info("Item Rec Sys is available in the sub menu. Navigated to the Item Rec Sys page.");
		logger.info("Item Rec Sys is available in the sub menu. Navigated to the Item Rec Sys page.");

		// Verify that Model Config can be updated using a valid JSON
		String modelConfigJSONValue1 = dataSet.get("validModelConfigJSONValue")
				+ CreateDateTime.getUniqueString("automation") + "\"\n}";

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"),
				modelConfigJSONValue1, "ClearAndUpdateModelConfig");
		String currentModelConfigJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"), "", "GetText");
		Assert.assertEquals(currentModelConfigJSONValue, modelConfigJSONValue1, "Model Config Value is not updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("successfullyUpdatedMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Model Config is successfully updated with valid JSON: " + modelConfigJSONValue1);
		logger.info("Item Rec Sys Model Config is successfully updated with valid JSON: " + modelConfigJSONValue1);

		// Verify that Model Config cannot be updated using invalid JSONs
		String modelConfigJSONValue2 = dataSet.get("validModelConfigJSONValue") + ",";
		String modelConfigJSONValue3 = "Invalid JSON";
		String modelConfigJSONValue4 = "";

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"),
				modelConfigJSONValue2, "ClearAndUpdateModelConfig");
		currentModelConfigJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"), "", "GetText");
		Assert.assertNotEquals(currentModelConfigJSONValue, modelConfigJSONValue2, "Model Config Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidSettingsMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Model Config is not updated with invalid JSON: " + modelConfigJSONValue2);
		logger.info("Item Rec Sys Model Config is not updated with invalid JSON: " + modelConfigJSONValue2);

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"),
				modelConfigJSONValue3, "ClearAndUpdateModelConfig");
		currentModelConfigJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"), "", "GetText");
		Assert.assertNotEquals(currentModelConfigJSONValue, modelConfigJSONValue3, "Model Config Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidSettingsMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Model Config is not updated with invalid JSON: " + modelConfigJSONValue3);
		logger.info("Item Rec Sys Model Config is not updated with invalid JSON: " + modelConfigJSONValue3);

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"),
				modelConfigJSONValue4, "ClearAndUpdateModelConfig");
		currentModelConfigJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysModelConfigTextBox"), "", "GetText");
		Assert.assertNotEquals(currentModelConfigJSONValue, modelConfigJSONValue4, "Model Config Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidSettingsMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Model Config is not updated with invalid empty JSON: " + modelConfigJSONValue4);
		logger.info("Item Rec Sys Model Config is not updated with invalid empty JSON: " + modelConfigJSONValue4);

		// Verify that Menu can be updated using a valid JSON
		String menuJSONValue1 = dataSet.get("validMenuJSONValue");

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"),
				menuJSONValue1, "ClearAndUpdateMenu");
		String currentMenuJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"), "", "GetText");
		Assert.assertEquals(currentMenuJSONValue, menuJSONValue1, "Menu Value is not updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("successfullyUpdatedMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Menu is successfully updated with valid JSON: " + menuJSONValue1);
		logger.info("Item Rec Sys Menu is successfully updated with valid JSON: " + menuJSONValue1);

		// Verify that Menu cannot be updated using invalid JSONs
		String menuJSONValue2 = dataSet.get("validMenuJSONValue") + ",";
		String menuJSONValue3 = "Invalid JSON";
		String menuJSONValue4 = "";

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"),
				menuJSONValue2, "ClearAndUpdateMenu");
		currentMenuJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"), "", "GetText");
		Assert.assertNotEquals(currentMenuJSONValue, menuJSONValue2, "Menu Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidMenuMsg"), "Message did not match.");
		TestListeners.extentTest.get().pass("Item Rec Sys Menu is not updated with invalid JSON: " + menuJSONValue2);
		logger.info("Item Rec Sys Menu is not updated with invalid JSON: " + menuJSONValue2);

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"),
				menuJSONValue3, "ClearAndUpdateMenu");
		currentMenuJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"), "", "GetText");
		Assert.assertNotEquals(currentMenuJSONValue, menuJSONValue3, "Menu Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidMenuMsg"), "Message did not match.");
		TestListeners.extentTest.get().pass("Item Rec Sys Menu is not updated with invalid JSON: " + menuJSONValue3);
		logger.info("Item Rec Sys Menu is not updated with invalid JSON: " + menuJSONValue3);

		pageObj.cockpitGuestPage().updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"),
				menuJSONValue4, "ClearAndUpdateMenu");
		currentMenuJSONValue = pageObj.cockpitGuestPage()
				.updateItemRecSys(utils.getLocator("CockpitGuestPage.itemRecSysMenuTextBox"), "", "GetText");
		Assert.assertNotEquals(currentMenuJSONValue, menuJSONValue4, "Menu Value is updated.");
		Assert.assertEquals(utils.getSuccessMessage(), dataSet.get("invalidMenuMsg"), "Message did not match.");
		TestListeners.extentTest.get()
				.pass("Item Rec Sys Menu is not updated with invalid empty JSON: " + menuJSONValue4);
		logger.info("Item Rec Sys Menu is not updated with invalid empty JSON: " + menuJSONValue4);

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