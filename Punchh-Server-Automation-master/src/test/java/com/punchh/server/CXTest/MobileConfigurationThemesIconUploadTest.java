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

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MobileConfigurationThemesIconUploadTest {

	private static Logger logger = LogManager.getLogger(MobileConfigurationThemesIconUploadTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
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
		// move to All Business Page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@SuppressWarnings("static-access")
	@Test(description = "SQ-T6519: Validate Meta v1 API response for Themes changes", priority = 0)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6519_verifyMetaApiThemesChanges() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.mobileconfigurationPage().setThemesPrerequisiteConfigs(baseUrl);

		// When all the Colors, Layouts and Icons are reset to default
		pageObj.dashboardpage().navigateToTabs("Themes");
		pageObj.mobileconfigurationPage().clickResetToDefault("Colors");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.mobileconfigurationPage().clickResetToDefault("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.mobileconfigurationPage().clickResetToDefault("Icons");
		pageObj.mobileconfigurationPage().publishChanges();
		Response cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		Map<String, Object> themeColor = cardsResponse.jsonPath().getMap("[0].themes.theme_color");
		String themeLayout = cardsResponse.jsonPath().getString("[0].themes.layouts");
		Assert.assertEquals(themeLayout, "[]", "Expected empty array but found: " + themeLayout);
		Assert.assertTrue(themeColor.isEmpty(), "Expected empty JSON object but found: " + themeColor);
		TestListeners.extentTest.get()
				.pass("Verified Meta API response when all the Colors, Layouts and Icons are reset to default");
		logger.info("Verified Meta API response when all the Colors, Layouts and Icons are reset to default");

		// When Colors are filled but Layouts and Icons are reset to default
		Map<String, String> checkboxToColorKeyMap = Map.of("Primary", "brand_primary", "Primary Contrast",
				"brand_primary_contrast", "Secondary", "brand_secondary", "Accent", "brand_accent", "Background",
				"brand_background", "Base", "utility_base", "Border", "utility_border", "Default", "utility_default");
		pageObj.mobileconfigurationPage().goToThemesTab("Colors");
		for (String checkbox : checkboxToColorKeyMap.keySet()) {
			// check all the 'Brand' & 'Utility' checkboxes
			pageObj.mobileconfigurationPage().checkUncheckColorBox(checkbox, "check");
			// set custom color values for all the checkboxes
			pageObj.mobileconfigurationPage().getSetColorValue(checkbox,
					dataSet.get(checkbox.toLowerCase().replace(" ", "") + "ColorCode"));
		}
		pageObj.mobileconfigurationPage().publishChanges();
		String rgbColor, apiColor;
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		// verify all the published custom color values in Meta API
		for (String checkbox : checkboxToColorKeyMap.keySet()) {
			rgbColor = pageObj.mobileconfigurationPage()
					.hexToRgb(dataSet.get(checkbox.toLowerCase().replace(" ", "") + "ColorCode"));
			apiColor = cardsResponse.jsonPath().get("[0].themes.theme_color." + checkboxToColorKeyMap.get(checkbox))
					.toString();
			Assert.assertEquals(apiColor, rgbColor, "API color for " + checkbox + " does not match UI color");
		}
		themeLayout = cardsResponse.jsonPath().getString("[0].themes.layouts");
		Assert.assertEquals(themeLayout, "[]", "Expected empty array but found: " + themeLayout);
		TestListeners.extentTest.get()
				.pass("Verified Meta API response when Colors are filled but Layouts and Icons are reset to default");
		logger.info("Verified Meta API response when Colors are filled but Layouts and Icons are reset to default");

		// When all the Colors, Layouts and Icons are filled
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().selectBottomNavOption("4", "Inbox");
		pageObj.mobileconfigurationPage().selectBottomNavOption("5", "Gift Cards");
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.mobileconfigurationPage().goToThemesTab("Icons");
		List<String> expectedOptions = Arrays.asList("Rewards", "More", "Order", "Inbox", "Gift Cards");
		List<String> currentNavItemsIcons = pageObj.mobileconfigurationPage().getCurrentNavItems("Icons");
		Assert.assertEquals(currentNavItemsIcons, expectedOptions);
		String imagePath = System.getProperty("user.dir") + dataSet.get("svgImagePath");
		boolean isIconUploaded, homeDefault;
		// upload icon for all 5 nav items
		for (String navItem : expectedOptions) {
			isIconUploaded = pageObj.mobileconfigurationPage().uploadIcon(navItem, imagePath);
			Assert.assertTrue(isIconUploaded, "Failed to upload icon for " + navItem);
		}
		pageObj.mobileconfigurationPage().publishChanges();
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		Map<String, String> labelToSceneMap = Map.of("Rewards", "RewardScene", "More", "MoreScene", "Order", "Order",
				"Inbox", "RichMessageScene", "Gift Cards", "GiftCardScene");
		String itemLabel, iconUrl, scene;
		for (int i = 0; i < expectedOptions.size(); i++) {
			itemLabel = cardsResponse.jsonPath().getString("[0].themes.layouts[" + i + "].label");
			iconUrl = cardsResponse.jsonPath().getString("[0].themes.layouts[" + i + "].icon_url");
			scene = cardsResponse.jsonPath().getString("[0].themes.layouts[" + i + "].scene");
			homeDefault = cardsResponse.jsonPath().getBoolean("[0].themes.layouts[" + i + "].default");

			Assert.assertEquals(itemLabel, expectedOptions.get(i), "Label does not match for index " + i);
			Assert.assertTrue(iconUrl.contains(dataSet.get("iconUrlPart")), "Icon URL does not match for index " + i);
			Assert.assertEquals(scene, labelToSceneMap.get(itemLabel), "Scene does not match for label: " + itemLabel);
			if (itemLabel.equals("Rewards")) { // "Rewards" should be the default home screen
				Assert.assertTrue(homeDefault, "Default does not match for label: " + itemLabel);
			} else {
				Assert.assertFalse(homeDefault, "Default should not be true for label: " + itemLabel);
			}
		}
		TestListeners.extentTest.get()
				.pass("Verified Meta API response when all the Colors, Layouts and Icons are filled");
		logger.info("Verified Meta API response when all the Colors, Layouts and Icons are filled");

		// When Layouts and Icons are filled but Colors is reset to default
		pageObj.mobileconfigurationPage().goToThemesTab("Colors");
		pageObj.mobileconfigurationPage().clickResetToDefault("Colors");
		pageObj.mobileconfigurationPage().publishChanges();
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		themeColor = cardsResponse.jsonPath().getMap("[0].themes.theme_color");
		Assert.assertTrue(themeColor.isEmpty(), "Expected empty JSON object but found: " + themeColor);
		iconUrl = cardsResponse.jsonPath().getString("[0].themes.layouts[0].icon_url");
		Assert.assertTrue(iconUrl.contains(dataSet.get("iconUrlPart")), "Icon URL does not match");
		TestListeners.extentTest.get()
				.pass("Verified Meta API response when Layouts and Icons are filled but Colors is reset to default");
		logger.info("Verified Meta API response when Layouts and Icons are filled but Colors is reset to default");

		// When Layouts are reset to default, Icons also get reset
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().clickResetToDefault("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.mobileconfigurationPage().goToThemesTab("Icons");
		boolean isIconsSectionEmpty = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.setUpLayoutButton"));
		Assert.assertTrue(isIconsSectionEmpty, "Set up layout button is not present");
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		themeLayout = cardsResponse.jsonPath().getString("[0].themes.layouts");
		Assert.assertEquals(themeLayout, "[]", "Expected empty array but found: " + themeLayout);
		TestListeners.extentTest.get()
				.pass("Verified Meta API response when Layouts are reset to default, Icons also get reset");
		logger.info("Verified Meta API response when Layouts are reset to default, Icons also get reset");

		// When Color checkboxes are just checked without setting any custom values
		pageObj.mobileconfigurationPage().goToThemesTab("Colors");
		for (String checkbox : checkboxToColorKeyMap.keySet()) {
			pageObj.mobileconfigurationPage().checkUncheckColorBox(checkbox, "check");
		}
		pageObj.mobileconfigurationPage().publishChanges();
		cardsResponse = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse.getStatusCode(), 200);
		for (String checkbox : checkboxToColorKeyMap.keySet()) {
			rgbColor = pageObj.mobileconfigurationPage().hexToRgb(dataSet.get("blackColorCode"));
			apiColor = cardsResponse.jsonPath().get("[0].themes.theme_color." + checkboxToColorKeyMap.get(checkbox))
					.toString();
			Assert.assertEquals(apiColor, rgbColor, "API color for " + checkbox + " does not match UI color");
		}
		TestListeners.extentTest.get().pass(
				"Verified Meta API response when Color checkboxes are just checked without setting any custom values, they are defaulted to Black");
		logger.info(
				"Verified Meta API response when Color checkboxes are just checked without setting any custom values, they are defaulted to Black");
	}

	@Test(description = "SQ-T6518: Verify Themes → Icons Tab", priority = 1)
	@Owner(name = "Vaibhav Agnihotri")
	public void T6518_verifyMobileConfigThemesIcons() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.mobileconfigurationPage().setThemesPrerequisiteConfigs(baseUrl);

		// verify Icons > Bottom Navigation in empty state
		pageObj.dashboardpage().navigateToTabs("Themes");
		pageObj.mobileconfigurationPage().goToThemesTab("Icons");
		pageObj.mobileconfigurationPage().clickResetToDefault("Icons");
		pageObj.mobileconfigurationPage().publishChanges();
		boolean isBottomNavigationHeaderPresent = utils
				.checkElementPresent(utils.getLocator("mobileConfigurationPage.bottomNavigation"));
		Assert.assertTrue(isBottomNavigationHeaderPresent, "Bottom Navigation Header is not present");
		String setUpBottomNavigationText = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.setUpBottomNavigationText", "");
		Assert.assertEquals(setUpBottomNavigationText, MessagesConstants.iconsSetUpBottomNavigationText);
		// verify the Visualizer info message
		String visualizerInfo = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.colorsInfoBanner", "");
		Assert.assertEquals(visualizerInfo, MessagesConstants.visualizerInfo);
		// Verify 'Set up layout' takes to Layout tab. Add the items
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.setUpLayoutButton"));
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().selectBottomNavOption("4", "Gift Cards");
		pageObj.mobileconfigurationPage().clickNavAddButton();
		pageObj.gamesPage().clickButton("mobileConfigurationPage.gotItButton");
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().publishChanges();
		pageObj.mobileconfigurationPage().goToThemesTab("Icons");
		// verify the Bottom Navigation description
		String bottomNavDescription = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.headerDescription", "Bottom Navigation");
		Assert.assertEquals(bottomNavDescription, MessagesConstants.iconsBottomNavigationDescription);
		TestListeners.extentTest.get().pass("Verified the headers and info texts in reset to default state");
		logger.info("Verified the headers and info texts in reset to default state");

		// verify the light to dark theme switch and visualizer preview message
		String visualizerThemeColor = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("mobileConfigurationPage.visualizerBottomNav", "background-color", "");
		Assert.assertEquals(visualizerThemeColor, dataSet.get("lightThemeColor"));
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.visualizerDarkThemeCheckbox"));
		visualizerThemeColor = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("mobileConfigurationPage.visualizerBottomNav", "background-color", "");
		Assert.assertEquals(visualizerThemeColor, dataSet.get("darkThemeColor"));
		visualizerInfo = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.visualizerInfo",
				MessagesConstants.visualizerPreviewInfo);
		Assert.assertEquals(visualizerInfo, MessagesConstants.visualizerPreviewInfo);
		TestListeners.extentTest.get().pass("Verified the light to dark theme switch and visualizer preview message");
		logger.info("Verified the light to dark theme switch and visualizer preview message");

		String svg5KbImagePath = System.getProperty("user.dir") + dataSet.get("svgImagePath");
		String iconUploadText, iconSource;
		List<String> expectedOptions = Arrays.asList("Rewards", "More", "Order", "Gift Cards");
		for (String item : expectedOptions) {
			// verify the upload text on Nav section
			iconUploadText = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.iconUploadText",
					item);
			Assert.assertTrue(iconUploadText.contains("Drag and drop image or\nbrowse"));
			// verify the placeholder icon on Visualizer
			iconSource = pageObj.iframeConfigurationPage()
					.getAttributeValue("mobileConfigurationPage.iconVisualizerPreview", "src", item);
			Assert.assertTrue(iconSource.isEmpty(), "Icon source is not empty for " + item);
			// upload a valid SVG image
			pageObj.mobileconfigurationPage().uploadIcon(item, svg5KbImagePath);
			// verify the set icon image on Nav section
			iconSource = pageObj.iframeConfigurationPage().getAttributeValue("mobileConfigurationPage.iconImagePreview",
					"src", item);
			Assert.assertTrue(iconSource.contains(baseUrl), "Failed to verify icon source for " + item);
			// verify the set icon image on Visualizer
			iconSource = pageObj.iframeConfigurationPage()
					.getAttributeValue("mobileConfigurationPage.iconVisualizerPreview", "src", item);
			Assert.assertTrue(iconSource.contains(baseUrl), "Failed to verify icon source for " + item);
		}
		pageObj.mobileconfigurationPage().publishChanges();
		TestListeners.extentTest.get()
				.pass("Verified icon upload dynamically reflects in live preview for all the Nav items");
		logger.info("Verified icon upload dynamically reflects in live preview for all the Nav items");

		boolean isIconViewVerified, isIconRemoveVerified;
		for (String item : expectedOptions) {
			// verify View image workflow
			isIconViewVerified = pageObj.mobileconfigurationPage().performActionOnIcon(item, "View image", "");
			Assert.assertTrue(isIconViewVerified, "Failed to View image for " + item);
			// verify Remove > No workflow
			isIconRemoveVerified = pageObj.mobileconfigurationPage().performActionOnIcon(item, "Remove", "No");
			Assert.assertTrue(isIconRemoveVerified, "Failed to verify Remove > No for " + item);
			// verify that icon is still present
			iconSource = pageObj.iframeConfigurationPage().getAttributeValue("mobileConfigurationPage.iconImagePreview",
					"src", item);
			Assert.assertTrue(iconSource.contains(dataSet.get("iconUrlPart")),
					"Failed to verify icon source for " + item);
			// verify Remove > Yes workflow
			isIconRemoveVerified = pageObj.mobileconfigurationPage().performActionOnIcon(item, "Remove", "Yes");
			Assert.assertTrue(isIconRemoveVerified, "Failed to verify Remove > Yes for " + item);
			// verify the placeholder icon on Visualizer
			iconSource = pageObj.iframeConfigurationPage()
					.getAttributeValue("mobileConfigurationPage.iconVisualizerPreview", "src", item);
			Assert.assertTrue(iconSource.isEmpty(), "Icon source is not empty for " + item);
		}
		pageObj.mobileconfigurationPage().publishChanges();
		TestListeners.extentTest.get()
				.pass("Verified for all nav items: 1) 'View image' opens image overlay, which closes on clicking 'x'. "
						+ "2) 'Remove' prompts confirmation modal, which when closes makes no change to icon. "
						+ "3) 'Remove' > Yes removes the icon and reverts to placeholder icon");
		logger.info("Verified for all nav items: 1) 'View image' opens image overlay, which closes on clicking 'x'. "
				+ "2) 'Remove' prompts confirmation modal, which when closes makes no change to icon. "
				+ "3) 'Remove' > Yes removes the icon and reverts to placeholder icon");

		// try uploading an invalid file type and verify the upload text on Nav section
		String navItem = "More";
		pageObj.mobileconfigurationPage().uploadIcon(navItem, svg5KbImagePath);
		String invalidImagePath = System.getProperty("user.dir") + dataSet.get("pngImagePath");
		pageObj.mobileconfigurationPage().performActionOnIcon(navItem, "Upload new image", invalidImagePath);
		iconUploadText = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.iconErrorText",
				navItem);
		Assert.assertTrue(iconUploadText.contains("Wrong file type -\ntry again"));
		// try publishing the changes
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishButton"));
		String errorMessage = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.publishToastMessage", "");
		Assert.assertTrue(errorMessage.contains(MessagesConstants.publishErrorMessageInvalidFileType),
				"Failed to verify publish error message");
		// clear the uploaded image to try again. upload a valid SVG image again
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.iconUploadTryAgain"));
		pageObj.mobileconfigurationPage().uploadIcon(navItem, svg5KbImagePath);
		// now try an SVG with size > 50KB and verify the upload text on Nav section
		invalidImagePath = System.getProperty("user.dir") + dataSet.get("svgImage1MbPath");
		pageObj.mobileconfigurationPage().performActionOnIcon(navItem, "Upload new image", invalidImagePath);
		iconUploadText = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.iconErrorText",
				navItem);
		Assert.assertTrue(iconUploadText.contains("Image too large -\ntry again"));
		// try publishing the changes
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.publishButton"));
		errorMessage = pageObj.iframeConfigurationPage().getElementText("mobileConfigurationPage.publishToastMessage",
				"");
		Assert.assertTrue(errorMessage.contains(MessagesConstants.publishErrorMessageInvalidFileType),
				"Failed to verify publish error message");
		TestListeners.extentTest.get().pass(
				"Verified the error messages for icon image upload: 1) When uploading an invalid file type like PNG. "
						+ "2) When uploading an SVG image with size > 50KB.");
		logger.info(
				"Verified the error messages for icon image upload: 1) When uploading an invalid file type like PNG. "
						+ "2) When uploading an SVG image with size > 50KB.");

		// verify that item added in the Layout should be synced in the Icons
		pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.iconUploadTryAgain"));
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().clickAddNewButton();
		pageObj.mobileconfigurationPage().selectBottomNavOption("5", "Challenges");
		pageObj.mobileconfigurationPage().clickNavAddButton();
		// Tooltip on Icons tab should appear with a 'Got it' button
		String iconsTabTooltipText = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.iconsTabTooltip", "Got it");
		Assert.assertEquals(iconsTabTooltipText, MessagesConstants.iconsTabTooltipText);
		boolean isIconsTabTooltipClosed = pageObj.segmentsBetaPage()
				.verifyPresenceAndClick(utils.getLocatorValue("mobileConfigurationPage.gotItButton"));
		Assert.assertTrue(isIconsTabTooltipClosed, "Failed to close the Icons tab tooltip");
		expectedOptions = Arrays.asList("Rewards", "More", "Order", "Gift Cards", "Challenges");
		List<String> currentNavItemsIcons = pageObj.mobileconfigurationPage().getCurrentNavItems("Icons");
		Assert.assertEquals(currentNavItemsIcons, expectedOptions);
		List<String> currentNavItemsVisualizer = pageObj.mobileconfigurationPage().getCurrentNavItems("Visualizer");
		Assert.assertEquals(currentNavItemsVisualizer, expectedOptions);
		// verify that item removed in the Layout should be synced in the Icons
		pageObj.mobileconfigurationPage().goToThemesTab("Layout");
		pageObj.mobileconfigurationPage().performActionOnLayout("Challenges", "Remove");
		pageObj.mobileconfigurationPage().goToThemesTab("Icons");
		currentNavItemsIcons = pageObj.mobileconfigurationPage().getCurrentNavItems("Icons");
		expectedOptions = Arrays.asList("Rewards", "More", "Order", "Gift Cards");
		Assert.assertEquals(currentNavItemsIcons, expectedOptions);
		currentNavItemsVisualizer = pageObj.mobileconfigurationPage().getCurrentNavItems("Visualizer");
		Assert.assertEquals(currentNavItemsVisualizer, expectedOptions);
		TestListeners.extentTest.get().pass(
				"Verified: 1) When a layout nav item is added then Icons tab shows a tooltip. Clicking on it will navigate to Icons tab. "
						+ "2) When an item is added or removed in the Layout, it gets synced in the Icons tab.");
		logger.info(
				"Verified: 1) When a layout nav item is added then Icons tab shows a tooltip. Clicking on it will navigate to Icons tab. "
						+ "2) When an item is added or removed in the Layout, it gets synced in the Icons tab.");

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
