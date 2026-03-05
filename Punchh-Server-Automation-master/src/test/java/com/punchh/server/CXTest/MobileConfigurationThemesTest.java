package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class MobileConfigurationThemesTest {

	private static Logger logger = LogManager.getLogger(MobileConfigurationThemesTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;
	String currentTime;

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
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T5308: To validate Mobile configs for core colors in MFW; "
			+ "SQ-T6516: Verify THEMES → COLORS in mobile configurations are in bento design; "
			+ "SQ-T6515: Validate Platform Configuration for MFW Bottom Nav + Update Colors to Bento", groups = {
					"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T5308_verifyMobileConfigThemesColors() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.mobileconfigurationPage().setThemesPrerequisiteConfigs(baseUrl);
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.mobileconfigurationPage().selectMobileConfigurationGivenTab("Themes");

		// verify 'Colors' tab and info banner
		boolean isColorsTabPresent = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.colorsTab"));
		Assert.assertTrue(isColorsTabPresent, "Colors tab is not present or clickable");
		String colorsInfoBanner = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.colorsInfoBanner", "");
		Assert.assertEquals(colorsInfoBanner, MessagesConstants.colorsInfoBanner);
		// verify 'Themes' info banner and description
		String themesInfoBanner = pageObj.iframeConfigurationPage().getElementText(
				"mobileConfigurationPage.themesInfoBanner", "Themes require Mobile Framework version 4.0 or higher");
		Assert.assertEquals(themesInfoBanner, MessagesConstants.themesInfoBanner);
		String themesDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.headerDescription", "Themes");
		Assert.assertEquals(themesDescription, MessagesConstants.themesDescription);
		// verify 'Brand' & 'Utility' description
		String brandDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.headerDescription", "Brand");
		Assert.assertEquals(brandDescription, MessagesConstants.brandDescription);
		String utilityDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.headerDescription", "Utility");
		Assert.assertEquals(utilityDescription, MessagesConstants.utilityDescription);
		// verify color descriptions and tooltips
		verifyColorDescriptionAndTooltip("Primary", MessagesConstants.primaryDescription,
				MessagesConstants.primaryTooltip);
		verifyColorDescriptionAndTooltip("Primary Contrast", MessagesConstants.primaryContrastDescription,
				MessagesConstants.primaryContrastTooltip);
		verifyColorDescriptionAndTooltip("Secondary", MessagesConstants.secondaryDescription,
				MessagesConstants.secondaryTooltip);
		verifyColorDescriptionAndTooltip("Accent", MessagesConstants.accentDescription,
				MessagesConstants.accentTooltip);
		verifyColorDescriptionAndTooltip("Background", MessagesConstants.backgroundDescription,
				MessagesConstants.backgroundTooltip);
		verifyColorDescriptionAndTooltip("Base", MessagesConstants.baseDescription, MessagesConstants.baseTooltip);
		verifyColorDescriptionAndTooltip("Border", MessagesConstants.borderDescription,
				MessagesConstants.borderTooltip);
		verifyColorDescriptionAndTooltip("Default", MessagesConstants.defaultDescription,
				MessagesConstants.defaultTooltip);
		TestListeners.extentTest.get().pass("Verified all the headers, descriptions, tooltips on Colors page");
		logger.info("Verified all the headers, descriptions, banners and tooltips on Colors page");

		// verify 'Reset to default' button functionality and description
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.resetToDefaultButton"));
		boolean isResetToDefaultModalHeaderPresent = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.resetToDefaultModalHeader"));
		Assert.assertTrue(isResetToDefaultModalHeaderPresent, "Reset to default modal header is not present");
		String resetToDefaultModalDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.resetToDefaultModalDescription", "");
		Assert.assertEquals(resetToDefaultModalDescription, MessagesConstants.resetToDefaultModalDescription);
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.resetToDefaultModalYesButton"));
		// verify 'Publish' button functionality
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishButton"));
		boolean isPublishModalHeaderPresent = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.publishModalHeader"));
		Assert.assertTrue(isPublishModalHeaderPresent, "Publish modal header is not present");
		String publishModalDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.publishModalDescription", "");
		Assert.assertEquals(publishModalDescription, MessagesConstants.publishModalDescription);
		// verify 'Yes, publish' saves the changes
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishModalYesButton"));
		// verify the success message for 'Publish' button
		String successMessage = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.publishToastMessage", "");
		Assert.assertTrue(successMessage.contains(MessagesConstants.publishSuccessMessage),
				"Failed to verify publish success message");
		TestListeners.extentTest.get().pass("Verified 'Reset to default' and 'Publish' button functionality");
		logger.info("Verified 'Reset to default' and 'Publish' button functionality");

		Map<String, String> checkboxToColorKeyMap = Map.of("Primary", "brand_primary", "Primary Contrast",
				"brand_primary_contrast", "Secondary", "brand_secondary", "Accent", "brand_accent", "Background",
				"brand_background", "Base", "utility_base", "Border", "utility_border", "Default", "utility_default");
		boolean isColorBoxChecked;
		for (String checkbox : checkboxToColorKeyMap.keySet()) {
			// verify that all 'Brand' & 'Utility' checkboxes are now unchecked
			isColorBoxChecked = pageObj.mobileconfigurationPage().checkUncheckColorBox(checkbox, "");
			Assert.assertFalse(isColorBoxChecked, checkbox + " checkbox is still checked");
			// verify that color input field disappears
			boolean isColorInputFieldPresent = pageObj.segmentsBetaPage().verifyPresenceAndClick(
					utils.getLocatorValue("mobileConfigurationPage.colorInputField").replace("temp", checkbox));
			Assert.assertFalse(isColorInputFieldPresent, checkbox + " color input field is still present");
			// check all the 'Brand' & 'Utility' checkboxes
			isColorBoxChecked = pageObj.mobileconfigurationPage().checkUncheckColorBox(checkbox, "check");
			// verify that default color for all is set to Black
			String colorValue = pageObj.mobileconfigurationPage().getSetColorValue(checkbox, "");
			Assert.assertEquals(colorValue, dataSet.get("blackColorCode"), checkbox + " color is not Black");
			// set custom color values for all the checkboxes
			pageObj.mobileconfigurationPage().getSetColorValue(checkbox,
					dataSet.get(checkbox.toLowerCase().replace(" ", "") + "ColorCode"));
		}
		pageObj.mobileconfigurationPage().publishChanges();
		TestListeners.extentTest.get().pass(
				"Verified that after 'Reset to default', all checkboxes are unchecked, checking them sets their default color to Black, and custom color values can be applied");
		logger.info(
				"Verified that after 'Reset to default', all checkboxes are unchecked, checking them sets their default color to Black, and custom color values can be applied");

		// verify that set configurations are retained on UI
		utils.refreshPage();
		pageObj.dashboardpage().navigateToTabs("Themes");
		String uiColor = pageObj.mobileconfigurationPage().getSetColorValue("Primary", "");
		Assert.assertEquals(uiColor, dataSet.get("primaryColorCode"));
		uiColor = pageObj.mobileconfigurationPage().getSetColorValue("Default", "");
		Assert.assertEquals(uiColor, dataSet.get("defaultColorCode"));
		TestListeners.extentTest.get().pass("Verified that custom color values are retained on UI");
		logger.info("Verified that custom color values are retained on UI");

		// verify that invalid input in color fields will give error message
		pageObj.mobileconfigurationPage().getSetColorValue("Default", "1");
		boolean isInlineErrorMessageVerified = pageObj.mobileconfigurationPage().validateInlineErrorMessage(
				"mobileConfigurationPage.colorInlineError", MessagesConstants.invalidHexInlineError,
				dataSet.get("redColorHexCode"), 1);
		Assert.assertTrue(isInlineErrorMessageVerified, "Failed to verify inline error message for Default color");
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishButton"));
		String errorMessage = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.publishToastMessage", "");
		Assert.assertTrue(errorMessage.contains(MessagesConstants.publishErrorMessageDefault),
				"Failed to verify publish error message");
		TestListeners.extentTest.get().pass("Verified that invalid input in color fields gives error message");
		logger.info("Verified that invalid input in color fields gives error message");

		// Meta API validations are done in T6519_verifyMetaApiThemesChanges()
	}

	public void verifyColorDescriptionAndTooltip(String checkbox, String descriptionText, String tooltipText) {
		String description = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.checkboxDescription", checkbox);
		Assert.assertEquals(description, descriptionText);
		boolean isTooltipVerified = pageObj.segmentsBetaPage()
				.verifyTooltipText(utils.getLocatorValue("mobileConfigurationPage.checkboxTooltip"), tooltipText);
		Assert.assertTrue(isTooltipVerified, "Failed to verify tooltip for " + checkbox);
	}

	@Test(description = "SQ-T6517: Verify THEMES → LAYOUT tab", groups = { "regression" }, priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6517_verifyMobileConfigThemesLayout() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.mobileconfigurationPage().setThemesPrerequisiteConfigs(baseUrl);

		// verify Layout > Bottom Navigation in empty state
		pageObj.dashboardpage().navigateToTabs("Themes");
		pageObj.mobileconfigurationPage().clickResetToDefault("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		boolean isBottomNavigationHeaderPresent = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.bottomNavigation"));
		Assert.assertTrue(isBottomNavigationHeaderPresent, "Bottom Navigation Header is not present");
		String setUpBottomNavigationText = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.setUpBottomNavigationText", "");
		Assert.assertEquals(setUpBottomNavigationText, MessagesConstants.layoutSetUpBottomNavigationText);
		// verify the Visualizer info message
		String visualizerInfo = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.colorsInfoBanner", "");
		Assert.assertEquals(visualizerInfo, MessagesConstants.visualizerInfo);
		TestListeners.extentTest.get().pass("Verified the Bottom Navigation headers and info texts in empty state");
		logger.info("Verified the Bottom Navigation headers and info texts in empty state");

		// click on Add New button and verify the header and info banner
		pageObj.mobileconfigurationPage().clickAddNewButton();
		boolean isAddBottomNavHeaderPresent = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.addBottomNavHeader"));
		Assert.assertTrue(isAddBottomNavHeaderPresent, "Add Bottom Navigation Header is not present");
		String addBottomNavInfoBanner = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.addBottomNavInfoBanner", "");
		Assert.assertEquals(addBottomNavInfoBanner, MessagesConstants.addBottomNavInfoBanner);
		// verify that 'Rewards', 'More', 'Order' are added by default
		String bottomNav1Name = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.bottomNavName", "1");
		Assert.assertEquals(bottomNav1Name, "Rewards");
		String bottomNav2Name = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.bottomNavName", "2");
		Assert.assertEquals(bottomNav2Name, "More");
		String bottomNav3Name = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.bottomNavName", "3");
		Assert.assertEquals(bottomNav3Name, "Order");
		// verify that Nav #4 and #5 are empty
		String bottomNav4Name = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.bottomNavDropdown", "4");
		Assert.assertTrue(bottomNav4Name.contains("Select"));
		String bottomNav5Name = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.bottomNavDropdown", "5");
		Assert.assertTrue(bottomNav5Name.contains("Select"));
		TestListeners.extentTest.get().pass("Verified the Add New functionality and default Nav options");
		logger.info("Verified the Add New functionality and default Nav options");

		// select option for Nav #4 and verify in Layout and Visualizer
		pageObj.mobileconfigurationPage().selectBottomNavOption("4", "Inbox");
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		List<String> expectedOptions = Arrays.asList("Rewards", "More", "Order", "Inbox");
		List<String> currentNavItemsVisualizer = pageObj.mobileconfigurationPage().getCurrentNavItems("Visualizer");
		Assert.assertEquals(currentNavItemsVisualizer, expectedOptions);
		// verify the Bottom Navigation description
		String bottomNavDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.headerDescription", "Bottom Navigation");
		Assert.assertEquals(bottomNavDescription, MessagesConstants.layoutBottomNavigationDescription);
		// select option for Nav #5
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().selectBottomNavOption("5", "Gift Cards");
		// verify remaining available dropdown options
		List<String> currentNavItemsLayout = pageObj.mobileconfigurationPage().getAllBottomNavOptions("4");
		List<String> expectedNavDropdownOptions = Arrays.asList("Earn", "Stores", "Subscription", "Deals",
				"Challenges");
		Assert.assertEquals(currentNavItemsLayout, expectedNavDropdownOptions);
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		// verify current published order of nav items in Layout and Visualizer
		currentNavItemsLayout = pageObj.mobileconfigurationPage().getCurrentNavItems("Layout");
		expectedOptions = Arrays.asList("Rewards", "More", "Order", "Inbox", "Gift Cards");
		Assert.assertEquals(currentNavItemsLayout, expectedOptions);
		currentNavItemsVisualizer = pageObj.mobileconfigurationPage().getCurrentNavItems("Visualizer");
		Assert.assertEquals(currentNavItemsVisualizer, expectedOptions);
		// drag and drop nav item to change the order
		currentNavItemsLayout = pageObj.mobileconfigurationPage().dragAndDropNavItem(currentNavItemsLayout, 5, 1);
		expectedOptions = Arrays.asList("Rewards", "Gift Cards", "More", "Order", "Inbox");
		Assert.assertEquals(currentNavItemsLayout, expectedOptions);
		TestListeners.extentTest.get().pass(
				"Verified the max available options and drag-and-drop functionality to change the order of nav items in Layout and Visualizer");
		logger.info(
				"Verified the max available options and drag-and-drop functionality to change the order of nav items in Layout and Visualizer");

		// verify that 'Rewards' is set as default home screen
		String defaultHomeScreen = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.draggableNavItemMenu", "Rewards");
		Assert.assertEquals(defaultHomeScreen, "Default home screen");
		// verify that 'Order' can be set as default home screen
		pageObj.mobileconfigurationPage().performActionOnLayout("Order", "Set as default home");
		defaultHomeScreen = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.draggableNavItemMenu", "Order");
		Assert.assertEquals(defaultHomeScreen, "Default home screen");
		// verify that 'Rewards' is not set as default home screen anymore
		defaultHomeScreen = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.draggableNavItemMenu", "Rewards");
		Assert.assertNotEquals(defaultHomeScreen, "Default home screen");
		TestListeners.extentTest.get().pass("Verified the default home screen and ability to change it for nav items");
		logger.info("Verified the default home screen and ability to change it for nav items");

		// verify error while adding nav item when max limit is reached
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().verifyAddNewModalContent(MessagesConstants.navMaxLimitReachedHeader,
				MessagesConstants.navMaxLimitReachedDescription);
		TestListeners.extentTest.get()
				.pass("Verified the error message when adding a nav item exceeds the max limit of 5.");
		logger.info("Verified the error message when adding a nav item exceeds the max limit of 5.");

		// remove nav item and verify the order in Layout and Visualizer
		pageObj.mobileconfigurationPage().performActionOnLayout("Inbox", "Remove");
		currentNavItemsLayout = pageObj.mobileconfigurationPage().getCurrentNavItems("Layout");
		expectedOptions = Arrays.asList("Rewards", "Gift Cards", "More", "Order");
		Assert.assertEquals(currentNavItemsLayout, expectedOptions);
		pageObj.mobileconfigurationPage().publishChanges();
		currentNavItemsVisualizer = pageObj.mobileconfigurationPage().getCurrentNavItems("Visualizer");
		Assert.assertEquals(currentNavItemsVisualizer, expectedOptions);
		// verify that 'Rewards', 'More', 'Order' cannot be removed
		boolean status;
		for (String navItem : Arrays.asList("Rewards", "More", "Order")) {
			status = pageObj.mobileconfigurationPage().performActionOnLayout(navItem, "Remove");
			Assert.assertFalse(status, navItem + " is removed");
		}
		TestListeners.extentTest.get().pass(
				"Verified that Rewards, More or Order can't be removed, whereas others like Inbox can be removed");
		logger.info("Verified that Rewards, More or Order can't be removed, whereas others like Inbox can be removed");

		// verify error while removing nav item when min limit is reached
		pageObj.mobileconfigurationPage().clickResetToDefault("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.dashboardpage().navigateToTabs("Miscellaneous URLs");
		pageObj.mobileconfigurationPage().sendURlsToURLsFields("Order URL", "");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		String successMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg, "Mobile configuration updated for Miscellaneous URLs");
		pageObj.dashboardpage().navigateToTabs("Themes");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		//pageObj.mobileconfigurationPage().performActionOnLayout("Gift Cards", "Remove");
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishButton"));
		pageObj.mobileconfigurationPage().verifyAddNewModalContent(MessagesConstants.navMinLimitReachedHeader,
				MessagesConstants.navMinLimitReachedDescription);
		TestListeners.extentTest.get()
				.pass("Verified the error message when removing a nav item reaches the min limit of 3.");
		logger.info("Verified the error message when removing a nav item reaches the min limit of 3.");
	}

	@Test(description = "SQ-T6712 Validate Disable Gift Card Flag with Feature in Layout", groups = {
			"regression" }, priority = 2)
	@Owner(name = "Rajasekhar Reddy")
	public void T6712_verifyMobileConfigThemesLayoutIfFeatureFlagOff() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Gift Card");
		utils.checkUncheckFlag("Enable Gift Cards?", "check");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Themes");

		// Validate Disable Gift Card Flag with Feature in Layout -- Raja
		pageObj.mobileconfigurationPage().clickResetToDefault("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.setUpLayoutButton"));
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().selectBottomNavOption("4", "Gift Cards");
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.dashboardpage().navigateToTabs("Gift Card");
		utils.checkUncheckFlag("Enable Gift Cards?", "uncheck");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Gift Card");
		utils.checkUncheckFlag("Enable Gift Cards?", "uncheck");
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Themes");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		String errorMessage = pageObj.mobileconfigurationPage().publishChangeswithToastMSG();
		logger.info(errorMessage);
		Assert.assertTrue(errorMessage.contains(MessagesConstants.publishErrorMessageifFeatureFlagoff),
				"Failed to verify publish error message");
		//pageObj.mobileconfigurationPage().publishChanges();
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
