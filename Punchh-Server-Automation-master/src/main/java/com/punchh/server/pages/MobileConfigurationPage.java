package com.punchh.server.pages;

import static org.testng.Assert.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class MobileConfigurationPage {

	static Logger logger = LogManager.getLogger(MobileConfigurationPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public MobileConfigurationPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public List<String> validateTabLinks() {

		List<WebElement> tabLinks = utils.getLocatorList("mobileConfigurationPage.tabLinks");
		List<String> tabNames = tabLinks.stream().map(s -> s.getText()).collect(Collectors.toList());
		return tabNames;

	}

	public String getPageSource() {
		String data = driver.getPageSource();
		return data;
	}

	public String isFlagChecked(String flagName) {
		String xpath = utils.getLocatorValue("dashboardPage.flagCheckedOrNot").replace("$flagName", flagName);
		WebElement checkbox = driver.findElement(By.xpath(xpath));
		String checkBoxValue = checkbox.getAttribute("checked");
		checkBoxValue = (checkBoxValue == null) ? "" : checkBoxValue;
		return checkBoxValue;
	}

	// It checks or unchecks the Themes color checkbox based on the action specified
	public boolean checkUncheckColorBox(String color, String action) {
		WebElement toastMessage = utils.getLocator("mobileConfigurationPage.publishToastMessage");
		utils.waitTillInVisibilityOfElement(toastMessage, "Toast message");
		String xpath = utils.getLocatorValue("mobileConfigurationPage.colorsCheckbox").replace("temp", color);
		WebElement checkbox = driver.findElement(By.xpath(xpath));
		String checkBoxValue = checkbox.getText();
		boolean status = false;
		if (!checkBoxValue.equals("check_box") && action.equals("uncheck")) {
			logger.info(color + " color box is unchecked and user also want to uncheck. Therefore, did not click.");
			TestListeners.extentTest.get()
					.info(color + " color box is unchecked and user also want to uncheck. Therefore, did not click.");
		} else if (!checkBoxValue.equals("check_box") && action.equals("check")) {
			utils.scrollToElement(driver, checkbox);
			checkbox.click();
			logger.info(color + " color box is unchecked and user want to check it, so clicked");
			TestListeners.extentTest.get()
					.info(color + " color box is unchecked and user want to check it, so clicked");
			status = true;
		} else if (checkBoxValue.equals("check_box") && action.equals("uncheck")) {
			utils.scrollToElement(driver, checkbox);
			checkbox.click();
			logger.info(color + " color box is checked and user want to uncheck it, so clicked");
			TestListeners.extentTest.get()
					.info(color + " color box is checked and user want to uncheck it, so clicked");
		} else if (checkBoxValue.equals("check_box") && action.equals("check")) {
			logger.info(color + " color box is checked and user also want to check. Therefore, did not click.");
			TestListeners.extentTest.get()
					.info(color + " color box is checked and user also want to check. Therefore, did not click");
			status = true;
		} else if (checkBoxValue.equals("check_box") && action.isEmpty()) {
			logger.info(color + " color box is checked");
			TestListeners.extentTest.get().info(color + " color box is checked");
			status = true;
		} else if (!checkBoxValue.equals("check_box") && action.isEmpty()) {
			logger.info(color + " color box is unchecked");
			TestListeners.extentTest.get().info(color + " color box is unchecked");
		}
		return status;
	}

	// It gets or sets the value for the given color checkbox
	public String getSetColorValue(String checkbox, String value) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.colorInputField").replace("temp", checkbox);
		WebElement colorInputField = driver.findElement(By.xpath(xpath));
		String colorValue = colorInputField.getAttribute("value");
		utils.scrollToElement(driver, colorInputField);
		if (!value.isEmpty()) {
			colorInputField.click();
			colorInputField.clear();
			colorInputField.sendKeys(value);
			logger.info("Set the color value for '" + checkbox + "' to #" + value);
			TestListeners.extentTest.get().info("Set the color value for '" + checkbox + "' to #" + value);
		} else {
			logger.info("Retrieved the color value for '" + checkbox + "' as #" + colorValue);
			TestListeners.extentTest.get().info("Retrieved the color value for '" + checkbox + "' as #" + colorValue);
		}
		return colorValue;
	}

	// It saves the changes made on the Themes page
	public void publishChanges() {
		WebElement publishButton = utils.getLocator("mobileConfigurationPage.publishButton");
		utils.StaleElementclick(driver, publishButton);
		utils.getLocator("mobileConfigurationPage.publishModalYesButton").click();
		WebElement toastMessage = utils.getLocator("mobileConfigurationPage.publishToastMessage");
		utils.waitTillInVisibilityOfElement(toastMessage, "Toast message");
		utils.longWaitInSeconds(3); // wait for the toast message to disappear
		logger.info("Publish changes successfully");
		TestListeners.extentTest.get().info("Publish changes successfully");
	}

	// It saves the changes made on the Themes page and return the toast message
	public String publishChangeswithToastMSG() {
		WebElement publishButton = utils.getLocator("mobileConfigurationPage.publishButton");
		utils.StaleElementclick(driver, publishButton);
		utils.getLocator("mobileConfigurationPage.publishModalYesButton").click();
		String toastMessage = pageObj.iframeConfigurationPage()
				.getElementText("mobileConfigurationPage.publishToastMessage", "");
		utils.longWaitInSeconds(3); // wait for the toast message to disappear
		logger.info("Publish changes successfully");
		TestListeners.extentTest.get().info("Publish changes successfully");
		return toastMessage;
	}

	// It clicks on Reset to default button on specified Themes page
	public void clickResetToDefault(String tabName) {
		utils.getLocator("mobileConfigurationPage." + tabName.toLowerCase() + "Tab").click();
		utils.getLocator("mobileConfigurationPage.resetToDefaultButton").click();
		utils.getLocator("mobileConfigurationPage.resetToDefaultModalYesButton").click();
		logger.info("Reset to default successfully for " + tabName + " tab");
		TestListeners.extentTest.get().info("Reset to default successfully for " + tabName + " tab");
	}

	// It selects an option from the Layout bottom navigation dropdown
	public void selectBottomNavOption(String navNumber, String option) {
		String dropdownXpath = utils.getLocatorValue("mobileConfigurationPage.bottomNavDropdown").replace("temp",
				navNumber);
		WebElement dropdownElement = driver.findElement(By.xpath(dropdownXpath));
		dropdownElement.click();
		String optionXpath = utils.getLocatorValue("mobileConfigurationPage.bottomNavDropdownOption")
				.replace("temp1", navNumber).replace("temp2", option);
		WebElement optionElement = driver.findElement(By.xpath(optionXpath));
		utils.StaleElementclick(driver, optionElement);
		TestListeners.extentTest.get().info("Selected option '" + option + "' from bottom navigation " + navNumber);
		logger.info("Selected option '" + option + "' from bottom navigation " + navNumber);
	}

	// It clicks on Add button on Layout bottom Navigation modal
	public void clickNavAddButton() {
		utils.getLocator("mobileConfigurationPage.modalAddNavButton").click();
		TestListeners.extentTest.get().info("Clicked Add Button on Nav items modal");
		logger.info("Clicked Add Button on Nav items modal");
	}

	// It retrieves all options available in the Layout bottom navigation dropdown
	public List<String> getAllBottomNavOptions(String navNumber) {
		String dropdownXpath = utils.getLocatorValue("mobileConfigurationPage.bottomNavDropdown").replace("temp",
				navNumber);
		WebElement dropdownElement = driver.findElement(By.xpath(dropdownXpath));
		utils.waitTillVisibilityOfElement(dropdownElement, "Dropdown " + navNumber);
		utils.longWaitInSeconds(1);
		utils.clickUsingActionsClass(dropdownElement);
		utils.longWaitInSeconds(1);
		List<WebElement> options = utils.getLocatorList("mobileConfigurationPage.bottomNavDropdownOptionList");
		List<String> optionTexts = options.stream().map(WebElement::getText).collect(Collectors.toList());
		TestListeners.extentTest.get().info("Retrieved all bottom navigation options from dropdown: " + optionTexts);
		logger.info("Retrieved all bottom navigation options from dropdown: " + optionTexts);
		return optionTexts;
	}

	// It retrieves the current nav items list in order
	public List<String> getCurrentNavItems(String section) {
		List<WebElement> navItems = null;
		if (section.equals("Layout")) {
			utils.longWaitInSeconds(1);
			navItems = utils.getLocatorList("mobileConfigurationPage.draggableNavItemsList");
		} else if (section.equals("Visualizer")) {
			navItems = utils.getLocatorList("mobileConfigurationPage.visualizerNavNames");
		} else if (section.equals("Icons")) {
			navItems = utils.getLocatorList("mobileConfigurationPage.iconImageNameList");
		}
		List<String> navItemTexts = navItems.stream().map(WebElement::getText).collect(Collectors.toList());
		TestListeners.extentTest.get().info("Retrieved navigation items on " + section + ": " + navItemTexts);
		logger.info("Retrieved navigation items on " + section + ": " + navItemTexts);
		return navItemTexts;
	}

	// It drags and drops a nav item from one position to another
	public List<String> dragAndDropNavItem(List<String> navItems, int fromNavNumber, int toNavNumber) {
		utils.scrollToElement(driver, utils.getLocator("mobileConfigurationPage.layoutTab"));
		String fromItem = navItems.get(fromNavNumber - 1);
		String toItem = navItems.get(toNavNumber - 1);
		String fromXpath = utils.getLocatorValue("mobileConfigurationPage.draggableNavItem").replace("temp", fromItem);
		String toXpath = utils.getLocatorValue("mobileConfigurationPage.draggableNavItem").replace("temp", toItem);
		WebElement fromElement = driver.findElement(By.xpath(fromXpath));
		WebElement toElement = driver.findElement(By.xpath(toXpath));
		Actions actions = new Actions(driver);
		actions.clickAndHold(fromElement).moveToElement(toElement).pause(Duration.ofMillis(500)).release(toElement)
				.build().perform();
		TestListeners.extentTest.get().info("Dragged " + fromItem + " and dropped at " + toItem + "'s position");
		logger.info("Dragged " + fromItem + " and dropped at " + toItem + "'s position");
		navItems = getCurrentNavItems("Layout");
		return navItems;
	}

	// It clicks on the '+ Add New...' button on Themes > Layout page
	public void clickAddNewButton() {
		WebElement addNewButton = utils.getLocator("mobileConfigurationPage.addNewButton");
		utils.StaleElementclick(driver, addNewButton);
		TestListeners.extentTest.get().info("Clicked '+ Add New...' button on Layout section");
		logger.info("Clicked '+ Add New...' button on Layout section");
	}

	// It selects an action on the specified nav item from the navigation items
	public boolean selectActionOnLayout(String item, String action) {
		boolean status = false;
		try {
			utils.implicitWait(1);
			String menuXpath = utils.getLocatorValue("mobileConfigurationPage.draggableNavItemMenu").replace("temp",
					item);
			WebElement menuElement = driver.findElement(By.xpath(menuXpath));
			utils.StaleElementclick(driver, menuElement);
			String optionXpath = utils.getLocatorValue("mobileConfigurationPage.draggableNavItemOption")
					.replace("temp1", item).replace("temp2", action);
			WebElement optionElement = driver.findElement(By.xpath(optionXpath));
			optionElement.click();
			TestListeners.extentTest.get().info("Selecting action '" + action + "' on item '" + item + "'...");
			logger.info("Selecting action '" + action + "' on item '" + item + "'...");
			status = true;
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get().info(
					"Failed to select action '" + action + "' on item '" + item + "' due to: " + e.getRawMessage());
			logger.info("Failed to select action '" + action + "' on item '" + item + "' due to: " + e.getRawMessage());
		}
		utils.implicitWait(50);
		return status;
	}

	// It confirms the action on the modal that appears for Layout nav items
	public boolean confirmActionOnLayout(String item, String action) {
		String modalHeaderXpath, modalDescriptionXpath, modalYesButtonXpath;
		String descriptionText = "", headerText = "", yesButtonText = "";
		WebElement modalHeaderElement, modalDescriptionElement, modalYesButtonElement;
		if (action.equals("Set as default home")) {
			headerText = MessagesConstants.setAsDefaultModalHeader.replace("temp1", item);
			descriptionText = MessagesConstants.setAsDefaultModalDescription.replace("temp1", item);
			yesButtonText = MessagesConstants.setAsDefaultModalYesButton;
		} else if (action.equals("Remove")) {
			headerText = MessagesConstants.removeModalHeader.replace("temp1", item);
			descriptionText = MessagesConstants.removeModalDescription.replace("temp1", item);
			yesButtonText = MessagesConstants.removeModalYesButton;
		}

		modalHeaderXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalHeader").replace("temp2",
				headerText);
		modalHeaderElement = driver.findElement(By.xpath(modalHeaderXpath));
		modalHeaderElement.isDisplayed();
		modalDescriptionXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalDescription").replace("temp2",
				descriptionText);
		modalDescriptionElement = driver.findElement(By.xpath(modalDescriptionXpath));
		modalDescriptionElement.isDisplayed();
		modalYesButtonXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalYesButton").replace("temp2",
				yesButtonText);
		modalYesButtonElement = driver.findElement(By.xpath(modalYesButtonXpath));
		modalYesButtonElement.click();
		TestListeners.extentTest.get().info("Confirmed action '" + action + "' on item: " + item);
		logger.info("Confirmed action '" + action + "' on item: " + item);
		return true;
	}

	// It performs an action on a Layout nav item and confirms it
	public boolean performActionOnLayout(String item, String action) {
		boolean status = selectActionOnLayout(item, action);
		if (status) {
			status = confirmActionOnLayout(item, action);
		}
		return status;
	}

	// It performs an action on an Icon nav item and confirms it
	public boolean performActionOnIcon(String item, String action, String additionalParam) throws InterruptedException {
		boolean status = selectActionOnIcon(item, action);
		if (status) {
			status = confirmActionOnIcon(item, action, additionalParam);
		}
		return status;
	}

	// It selects an action on the specified Icon item from the navigation items
	public boolean selectActionOnIcon(String item, String action) {
		boolean status = false;
		try {
			utils.implicitWait(1);
			String menuXpath = utils.getLocatorValue("mobileConfigurationPage.iconMenu").replace("temp", item);
			WebElement menuElement = driver.findElement(By.xpath(menuXpath));
			utils.StaleElementclick(driver, menuElement);
			String optionXpath = utils.getLocatorValue("mobileConfigurationPage.iconItemOption").replace("temp",
					action);
			WebElement optionElement = driver.findElement(By.xpath(optionXpath));
			if (!action.equals("Upload new image")) {
				optionElement.click();
				TestListeners.extentTest.get().info("Selected action '" + action + "' on item " + item);
				logger.info("Selected action '" + action + "' on item " + item);
			}
			status = true;
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get()
					.info("Failed to select action '" + action + "' on item " + item + " due to: " + e.getRawMessage());
			logger.info("Failed to select action '" + action + "' on item " + item + " due to: " + e.getRawMessage());
		}
		utils.implicitWait(50);
		return status;
	}

	// It confirms the action on the modal that appears for Icon nav items
	public boolean confirmActionOnIcon(String item, String action, String additionalParam) throws InterruptedException {
		String modalHeaderXpath, modalDescriptionXpath, modalYesButtonXpath, modalNoButtonXpath;
		String descriptionText = "", headerText = "";
		WebElement modalDescriptionElement, modalYesButtonElement, modalNoButtonElement;
		if (action.equals("Upload new image")) {
			uploadIcon(item, additionalParam);
		} else if (action.equals("View image")) {
			// verify presence of header, preview image, got it and close button
			modalHeaderXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalHeader").replace("temp2",
					action);
			driver.findElement(By.xpath(modalHeaderXpath)).isDisplayed();
			modalYesButtonXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalYesButton").replace("temp2",
					"Got it");
			driver.findElement(By.xpath(modalYesButtonXpath)).isDisplayed();
			utils.getLocator("mobileConfigurationPage.viewImageModalPreview").isDisplayed();
			utils.getLocator("mobileConfigurationPage.viewImageModalCloseButton").click();
		} else if (action.contains("Remove")) {
			// verify presence of header, description, yes and no button
			headerText = MessagesConstants.removeModalHeader.replace("temp1", "image");
			modalHeaderXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalHeader").replace("temp2",
					headerText);
			driver.findElement(By.xpath(modalHeaderXpath)).isDisplayed();
			descriptionText = MessagesConstants.iconsRemoveImageDescription;
			modalDescriptionXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalDescription")
					.replace("temp2", descriptionText);
			modalDescriptionElement = driver.findElement(By.xpath(modalDescriptionXpath));
			modalDescriptionElement.isDisplayed();
			modalYesButtonXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalYesButton").replace("temp2",
					"Yes, remove");
			modalYesButtonElement = driver.findElement(By.xpath(modalYesButtonXpath));
			modalNoButtonXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalYesButton").replace("temp2",
					"No, keep");
			modalNoButtonElement = driver.findElement(By.xpath(modalNoButtonXpath));
			if (action.equals("Remove") && additionalParam.equals("Yes")) {
				modalYesButtonElement.click();
			} else if (action.equals("Remove") && additionalParam.equals("No")) {
				modalNoButtonElement.click();
			}
		}
		TestListeners.extentTest.get()
				.info("Confirmed action '" + action + "': " + additionalParam + " on item " + item);
		logger.info("Confirmed action '" + action + "': " + additionalParam + " on item " + item);
		return true;
	}

	// It verifies the error modal content for Layout nav items
	public void verifyAddNewModalContent(String headerText, String descriptionText) {
		String modalHeaderXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalHeader").replace("temp2",
				headerText);
		String modalDescriptionXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalDescription")
				.replace("temp2", descriptionText);
		String modalYesButtonXpath = utils.getLocatorValue("mobileConfigurationPage.actionModalYesButton")
				.replace("temp2", "Got it");
		WebElement modalHeaderElement = driver.findElement(By.xpath(modalHeaderXpath));
		modalHeaderElement.isDisplayed();
		WebElement modalDescriptionElement = driver.findElement(By.xpath(modalDescriptionXpath));
		modalDescriptionElement.isDisplayed();
		WebElement modalYesButtonElement = driver.findElement(By.xpath(modalYesButtonXpath));
		modalYesButtonElement.click();
	}

	// It navigates to the specified tab on Themes page
	public void goToThemesTab(String tabName) {
		utils.getLocator("mobileConfigurationPage." + tabName.toLowerCase() + "Tab").click();
		TestListeners.extentTest.get().info("Navigated to '" + tabName + "' tab on Themes page");
		logger.info("Navigated to '" + tabName + "' tab on Themes page");
	}

	// It uploads an icon for the specified Layout nav item
	public boolean uploadIcon(String item, String path) throws InterruptedException {
		String imageInputXpath = utils.getLocatorValue("mobileConfigurationPage.iconImageBrowse").replace("temp", item);
		WebElement imageInput = driver.findElement(By.xpath(imageInputXpath));
		imageInput.sendKeys(path);
		return true;
	}

	// Set the pre-requisite configs for default Bottom Nav Layout options
	public void setThemesPrerequisiteConfigs(String baseUrl) throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Subscriptions?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlagWitoutUpdate("Enable Challenges?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Rich Messaging?", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Deals?", "check");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Mobile Configuration");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous URLs");
		pageObj.mobileconfigurationPage().sendURlsToURLsFields("Order URL", baseUrl);
		pageObj.mobileconfigurationPage().clickUpdateBtn();
		pageObj.dashboardpage().navigateToTabs("Gift Card");
		utils.checkUncheckFlag("Enable Gift Cards?", "check");
		clickUpdateBtn();
	}

	public void clickMiscellaneousURLBtn() {
		utils.getLocator("mobileConfigurationPage.miscellaneousURLBtn").click();
		utils.longWaitInSeconds(3);
		logger.info("Clicked Miscellaneous URL Button");
		TestListeners.extentTest.get().info("Clicked Miscellaneous URL Button");
//		utils.waitTillPagePaceDone();
	}

	public void clickExtendedSettingsBtn() {
		// utils.clickByJSExecutor(driver,
		// utils.getLocator("mobileConfigurationPage.extendedSettingsBtn"));

		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", utils.getLocator("mobileConfigurationPage.extendedSettingsBtn"));

		logger.info("Clicked Extended Settings Button");
	}

	public void clickMiscellaneousFieldsBtn() {
		utils.getLocator("mobileConfigurationPage.miscellaneousFieldsBtn").click();
		utils.longWaitInSeconds(3);
		logger.info("Clicked Miscellaneous Fields Button");

		TestListeners.extentTest.get().info("Clicked Miscellaneous Fields Button");
	}

	public void clickAppStringsBtn() {
		utils.getLocator("mobileConfigurationPage.appStringsBtn").click();
		utils.longWaitInSeconds(3);
		logger.info("Clicked App String Button");
		TestListeners.extentTest.get().info("Clicked App String Button");
	}

	public void clickAppConfigsBtn() {
		utils.getLocator("mobileConfigurationPage.").click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked App String Button");
		TestListeners.extentTest.get().info("Clicked App String Button");
	}

	public void clickVerifyAndUpdateBtn() {
		utils.getLocator("mobileConfigurationPage.verifyAndUpdateBtn").click();
		logger.info("Clicked Verify & Update Button");
		TestListeners.extentTest.get().info("Clicked Verify & Update Button");
		utils.waitTillPagePaceDone();
		// utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"),
		// "");
		// utils.waitTillVisibilityOfElement(utils.getLocator("mobileConfigurationPage.successMsg"),
		// "");
	}

	public void clickUpdateBtn() {
		WebElement updateBtn = utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn");
		utils.waitTillElementToBeClickable(updateBtn);
		utils.StaleElementclick(driver, updateBtn);
		utils.waitTillPagePaceDone();
		logger.info("Clicked on Update button");
		TestListeners.extentTest.get().info("Clicked on Update button");
	}

	public String getSuccessMessage() {
//		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(2);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();

		return messge;
	}

	public String getErrorMessage() {
		// selUtils.implicitWait(60);
		// utils.waitTillPagePaceDone();
		// utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"),
		// "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public String getErrorMessageExtendedSettings() {
		String val = utils.getLocator("mobileConfigurationPage.errorMsg").getText();
		return val;
	}

	public void validateInvalidUpdation() {
		try {
			utils.getLocator("mobileConfigurationPage.faqUrl").clear();
			utils.getLocator("mobileConfigurationPage.faqUrl").sendKeys("qwerty");
			utils.getLocator("mobileConfigurationPage.updateBtn").click();
			utils.getLocator("mobileConfigurationPage.errorMsg").isDisplayed();
			logger.info("Error message displayed");
			TestListeners.extentTest.get().pass("Validated invalid updation");

			utils.getLocator("mobileConfigurationPage.miscellaneousURLBtn").click();
			utils.getLocator("mobileConfigurationPage.faqUrl").clear();
		} catch (Exception e) {

			TestListeners.extentTest.get().fail("invalid updation verification failed");
			Assert.fail("invalid updation verification failed");
		}
	}

	public void validUpdation() {
		utils.getLocator("mobileConfigurationPage.disclaimerBtn").click();
		utils.getLocator("mobileConfigurationPage.gameDisclaimer").clear();
		utils.getLocator("mobileConfigurationPage.gameDisclaimer").sendKeys("Test Game Disclaimer");
		utils.getLocator("mobileConfigurationPage.updateBtn").click();
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();

		// utils.StaleElementclick(driver,
		// utils.getLocator("mobileConfigurationPage.miscellaneousURLBtn"));
		utils.getLocator("mobileConfigurationPage.miscellaneousURLBtn").click();
		utils.getLocator("mobileConfigurationPage.faqUrl").clear();
		utils.getLocator("mobileConfigurationPage.faqUrl").sendKeys("https://testurl.com/");
		utils.getLocator("mobileConfigurationPage.updateBtn").click();
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();

		// utils.StaleElementclick(driver,
		// utils.getLocator("mobileConfigurationPage.extendedSettingsBtn"));
		utils.getLocator("mobileConfigurationPage.extendedSettingsBtn").click();
		utils.getLocator("mobileConfigurationPage.supportEmail").clear();
		utils.getLocator("mobileConfigurationPage.supportEmail").sendKeys("moerewards@moes.com");

		utils.getLocator("mobileConfigurationPage.updateBtn").click();
		try {
			utils.getLocator("mobileConfigurationPage.successMsg").isDisplayed();
			logger.info("Success message displayed");
			TestListeners.extentTest.get().pass("Verified valid updation");
		} catch (Exception e) {
			TestListeners.extentTest.get().fail("valid updation verification failed");
			Assert.fail("valid updation verification failed");
		}

	}

	public void ClickAccountDeletionBtn(String tabName) {
		try {
			String tabXpath = utils.getLocatorValue("dashboardPage.dashboardTab_Xpath").replace("$TabName",
					tabName.trim());
			selUtils.waitTillElementToBeClickable(utils.getXpathWebElements(By.xpath(tabXpath)));
			utils.getXpathWebElements(By.xpath(tabXpath)).click();

		} catch (Exception e) {
			utils.getLocator("dashboardPage.dashboardToggleIcon").click();
			driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li/a[text()='" + tabName + "']")).click();

		}
		selUtils.longWait(200);
		utils.getLocator("mobileConfigurationPage.enButton").click();
		logger.info("Clicked the en language");

	}

	public void updateAppStringsFields(String str, String loyaltyAppStringsJson, String orderingAppStringsJson,
			String giftCardAppStringsJson) {
		WebElement loyaltyAppStrings = utils.getLocator("mobileConfigurationPage.loyaltyAppStrings");
		WebElement orderingAppStrings = utils.getLocator("mobileConfigurationPage.orderingAppStrings");
		WebElement giftCardAppStrings = utils.getLocator("mobileConfigurationPage.giftCardAppStrings");
		utils.waitTillElementToBeClickable(loyaltyAppStrings);
		utils.waitTillElementToBeClickable(orderingAppStrings);
		utils.waitTillElementToBeClickable(giftCardAppStrings);
		utils.scrollToElement(driver, loyaltyAppStrings);

		if (str.equals("Clear")) {
			selUtils.clearTextUsingJS(loyaltyAppStrings);
			selUtils.clearTextUsingJS(orderingAppStrings);
			selUtils.clearTextUsingJS(giftCardAppStrings);
		} else if (str.equals("Append invalid JSON")) {
			loyaltyAppStrings.sendKeys(loyaltyAppStringsJson);
			orderingAppStrings.sendKeys(orderingAppStringsJson);
			giftCardAppStrings.sendKeys(giftCardAppStringsJson);
		} else if (str.contains("Clear & Add Valid JSON")) {
			selUtils.clearTextUsingJS(loyaltyAppStrings);
			loyaltyAppStrings.sendKeys(loyaltyAppStringsJson);
			selUtils.clearTextUsingJS(orderingAppStrings);
			orderingAppStrings.sendKeys(orderingAppStringsJson);
			selUtils.clearTextUsingJS(giftCardAppStrings);
			giftCardAppStrings.sendKeys(giftCardAppStringsJson);
		}
		TestListeners.extentTest.get().info(str + " is done.");
		logger.info(str + " is done.");
	}

	public boolean validateInlineErrorMessage(String locator, String expectedErrorMsg, String expectedHexCode,
			int expectedMsgCount) {
		boolean status = false;
		int actualCount = 0;
		String errorMsg, hexCode;
		String xpath = utils.getLocatorValue(locator).replace("temp", expectedErrorMsg);
		List<WebElement> errors = driver.findElements(By.xpath(xpath));
		for (WebElement error : errors) {
			errorMsg = error.getText();
			hexCode = getTextColour(error);
			if (errorMsg.equals(expectedErrorMsg) && hexCode.equals(expectedHexCode)) {
				actualCount++;
				TestListeners.extentTest.get().info(
						"The error inline message is displayed with text: '" + errorMsg + "' and hex code: " + hexCode);
				logger.info(
						"The error inline message is displayed with text: '" + errorMsg + "' and hex code: " + hexCode);
			}
		}
		if (actualCount == expectedMsgCount) {
			status = true;
		}
		return status;
	}

	public String validateResetLinks() {
		List<WebElement> resetLinks = utils.getLocatorList("mobileConfigurationPage.resetLink");
		for (int i = 0; i < resetLinks.size(); i++) {
			utils.clickByJSExecutor(driver, resetLinks.get(i));
			TestListeners.extentTest.get().info("Reset link at index " + i + " is clicked.");
			logger.info("Reset link at index " + i + " is clicked.");
		}
		clickUpdateBtn();
		utils.longWaitInSeconds(2);
		String message = getSuccessMessage();
		return message;
	}

	public String additionalAppConfig(String operation, String locator, String fieldName, String json) {
		String xpath = utils.getLocatorValue(locator).replace("flag", fieldName);
		String text = "";
		WebElement textArea = driver.findElement(By.xpath(xpath));
		utils.waitTillElementToBeClickable(textArea);
		utils.scrollToElement(driver, textArea);
		if (operation.equals("Update")) {
			selUtils.clearTextUsingJS(textArea);
			textArea.sendKeys(json);
			text = json;
		} else if (operation.equals("GetText")) {
			text = textArea.getText();
		}
		TestListeners.extentTest.get().info(
				"'" + operation + "' is done on '" + fieldName + "' Additional App Config field with text: " + text);
		logger.info(
				"'" + operation + "' is done on '" + fieldName + "' Additional App Config field with text: " + text);
		return text;
	}

	public boolean validateTextAreaHelpMsg(String expectedHelpTextString) {
		List<WebElement> helpTextElements = utils.getLocatorList("mobileConfigurationPage.textAreaHelpMsg");
		List<String> helpTextStrings = helpTextElements.stream().map(s -> s.getText()).collect(Collectors.toList());
		for (String str : helpTextStrings) {
			TestListeners.extentTest.get().info("Help text: " + str);
			logger.info("Help text: " + str);
		}
		List<String> expectedHelpTextList = Arrays.asList(expectedHelpTextString.split(","));
		boolean status = utils.comparingLists(expectedHelpTextList, helpTextStrings);
		return status;
	}

	public void selectMobileConfigurationGivenTab(String tabName) {
		try {
			WebElement ele = driver
					.findElement(By.xpath(("//ul[contains(@class,'nav nav-pills')]//li//a[text()='" + tabName + "']")));
			ele.click();
			TestListeners.extentTest.get().info("Selected tab : " + tabName);
		} catch (Exception e) {
			utils.getLocator("dashboardPage.dashboardToggleIcon").click();
			WebElement ele = driver
					.findElement(By.xpath(("//ul[contains(@class,'nav nav-pills')]//li//a[text()='" + tabName + "']")));
			ele.click();
			TestListeners.extentTest.get().info("Selected tab : " + tabName);
			utils.waitTillPagePaceDone();
		}
	}

	public void setCheckInFrequencyAppPlayStoreRating(String val) {
		utils.getLocator("mobileConfigurationPage.checkinFrequencyAppPlayStoreratingBox").clear();
		utils.getLocator("mobileConfigurationPage.checkinFrequencyAppPlayStoreratingBox").sendKeys(val);
		TestListeners.extentTest.get().info("Check-In Frequency for App/Play Store Rating is set as: " + val);
		logger.info("Check-In Frequency for App/Play Store Rating is set as: " + val);
	}

	public void setRedeemablePointThresholds(String val) {
		utils.getLocator("mobileConfigurationPage.redeemablePointThresholds").clear();
		utils.getLocator("mobileConfigurationPage.redeemablePointThresholds").sendKeys(val);
		TestListeners.extentTest.get().info("Redeemable point threshold is set as: " + val);
		logger.info("Redeemable point threshold is set as: " + val);
	}

	public String verifyAndUpdate() {
		utils.getLocator("mobileConfigurationPage.verifyAndUpdateBtn").click();
		utils.waitTillPagePaceDone();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		TestListeners.extentTest.get().info("clicked verify and update button and success msg is :" + messge);
		return messge;

//		String val = utils.getLocator("mobileConfigurationPage.successMsg").getText();
//		String a[]=val.split("x");
//		String msg = a[1].trim();
	}

	public String checkIFRedeemablePointThresholdsOptional() {
		WebElement ele = utils.getLocator("mobileConfigurationPage.redeemablePointThresholds");
		String val = utils.isOptionalField(ele);
		TestListeners.extentTest.get().info("check if redeemable point thresholds is optional : " + val);
		return val;
	}

	public String getRedeemablePointThresholdsHinttext() {
		String val = utils.getLocator("mobileConfigurationPage.redeemablePointThresholdsHintText").getText().trim();
		TestListeners.extentTest.get().info("Redeemable point thresholds hint text is :" + val);
		return val;
	}

	public String getRedeemablePointThresholdsInvalidInputErrorMsg() {
		String val = utils.getLocator("mobileConfigurationPage.invalidTexterrormsg").getText().trim();
		TestListeners.extentTest.get().info("Redeemable point thresholds invalid input error msg is :" + val);
		return val;
	}

	public void clickOrderingAppConfigBtn() {
		utils.getLocator("mobileConfigurationPage.orderingAppConfigBtn").click();
	}

	public boolean orderingCustomTextPresent() {
		if (utils.getLocator("mobileConfigurationPage.orderingCustomText").isDisplayed()) {
			String attrValue = utils.getLocator("mobileConfigurationPage.orderingCustomText").getAttribute("class");
			if (attrValue.contains("text-bold")) {
				logger.info("ordering custom text is present and is bold");
				return true;
			}
		}
		logger.info("ordering custom text is not present");
		return false;
	}

	// This method selects value from dropdown on Mobile Configuration pages
	public void selectDropdownValue(WebElement dropdownElement, String dropdownValue) {
		utils.waitTillElementToBeClickable(dropdownElement);
		utils.scrollToElement(driver, dropdownElement);
		utils.selectDrpDwnValue(dropdownElement, dropdownValue);
		TestListeners.extentTest.get().info("Selected value '" + dropdownValue + "' from dropdown");
		logger.info("Selected value '" + dropdownValue + "' from dropdown");
	}

	// It checks if the dropdown is single-select or multi-select
	public boolean isSingleSelectDropdown(WebElement dropdownElement, String dropdownName) {
		utils.waitTillElementToBeClickable(dropdownElement);
		utils.scrollToElement(driver, dropdownElement);
		Select select = new Select(dropdownElement);
		boolean isSingleSelect = !select.isMultiple();
		return isSingleSelect;
	}

	public boolean greyAreaOrderingCustomText() {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextTextarea");
		List<WebElement> ele1 = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextDropdownValue");
		for (int i = 0; i < ele.size(); i++) {
			if (!ele.get(i).isEnabled()) {
				if (!ele1.get(i).isEnabled()) {
					continue;
				}
			}
			logger.info("grey out area is not enabled");
			return false;
		}
		logger.info("grey out area is enabled");
		return true;

	}

	public boolean verifyAsterisk() {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.asterisk");
		for (int i = 0; i < ele.size(); i++) {
			if (!ele.get(i).isDisplayed()) {
				logger.info("asterisk is not present");
				return false;
			}
			// logger.info("asterisk is present");
		}
		logger.info("asterisk is present for all");
		TestListeners.extentTest.get().pass("asterisk is present for all");
		return true;
	}

	public String generateRandomString(int length) {
		Random random = new Random();
		StringBuilder sb = new StringBuilder(length);
		String characters = "qwertyuioplkjhgfdsazxcvbnm098766543432123AQZWSXCDERFVHTPLMNBJHUYI";
		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(characters.length());
			char randomChar = characters.charAt(randomIndex);
			sb.append(randomChar);
		}
		return sb.toString();
	}

	public boolean negativeNumber(String str) {
		String regex = "-\\d+(\\.\\d+)?";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		while (matcher.find()) {
			String s = matcher.group();
			// System.out.println(s);
			logger.info("negative value is showing in the counter");
			return true;
		}
		return false;
	}

	public void verifyNegativeCounter(int length) {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextTextarea");
		List<WebElement> counter = utils.getLocatorList("mobileConfigurationPage.textAreaCounter");
		for (int i = 0; i < ele.size(); i++) {
			ele.get(i).clear();
			ele.get(i).sendKeys(generateRandomString(length));
			logger.info("Entered some random value in the textarea");
			String counterText = counter.get(i).getText();
			negativeNumber(counterText);
		}
	}

	public void enterTextinTextArea(int length) {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextTextarea");
		for (int i = 0; i < ele.size(); i++) {
			ele.get(i).clear();
			ele.get(i).sendKeys(generateRandomString(length));
			logger.info("Entered some random value in the textarea");
		}
	}

	public void enterTextinTextArea(String path, String value) {
		List<WebElement> ele = utils.getLocatorList(path);
		for (int i = 0; i < ele.size(); i++) {
			ele.get(i).clear();
			ele.get(i).sendKeys(value);
			logger.info("Entered s in the textarea");
		}
	}

	public String getTextColour(WebElement ele) {
		String rgbFormat = ele.getCssValue("color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		return hexcode;
	}

	public void verifyCounterColor(String hexcode) {
		utils.implicitWait(5);
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.textAreaCounter");
		for (int i = 0; i < ele.size(); i++) {
			Assert.assertEquals(getTextColour(ele.get(i)), hexcode, "hexcode is not match");
		}
	}

	public void textAreaScrollable() {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextTextarea");
		for (int i = 0; i < ele.size(); i++) {
			Actions actions = new Actions(driver);
			actions.moveToElement(ele.get(i));
			utils.scrollToElement(driver, ele.get(i));
			logger.info("TextArea is scrollable");
		}
	}

	public void oderingCustomdropdownValidation() {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextDropdownValue");
		List<String> lst = (Arrays.asList("", "Header (scrollable)", "Footer (sticky)", "Footer (scrollable)"));
		for (int i = 0; i < 1; i++) {
			Select select = new Select(ele.get(i));
			List<String> dropdownValues = select.getOptions().stream().map(element -> element.getText())
					.collect(Collectors.toList());
			Assert.assertEquals(lst, dropdownValues, "dropdown values are not matching");
		}
	}

	public String orderingCustomTextHint(String str) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.orderingCustomTextHintText").replace("$flag",
				str);
		return driver.findElement(By.xpath(xpath)).getText();

	}

	public void enterInvalidDetailsOrderingCustomText() {
		enterTextinTextArea(0);
		clickVerifyAndUpdateBtn();

		String xpath1 = utils.getLocatorValue("mobileConfigurationPage.errorMsg");
		String text1 = driver.findElement(By.xpath(xpath1)).getText();
		Assert.assertEquals(text1, "x\n" + "Error updating Mobile configuration for Ordering",
				"Error message is not equal");
		logger.info("error msg is displayed when invalid details are field for the Ordering Custom Text");

	}

	public String enterValidDetailsOrderingCustomText() {
		utils.implicitWait(5);
		List<WebElement> ele1 = utils.getLocatorList("mobileConfigurationPage.orderingCustomTextDropdownValue");
		for (int i = 0; i < 1; i++) {
			Select select = new Select(ele1.get(i));
			select.selectByIndex(1);
		}

		String randomValue = generateRandomString(10);

		enterTextinTextArea("mobileConfigurationPage.menu_custom_text_textarea", randomValue);
		enterTextinTextArea("mobileConfigurationPage.item_custom_text_textarea", randomValue);
		enterTextinTextArea("mobileConfigurationPage.nested_custom_text_textarea", randomValue);

		clickVerifyAndUpdateBtn();
		utils.longWait(3);
		return randomValue;

	}

	public void youtubeURlField(String url) {
		WebElement element = utils.getLocator("mobileConfigurationPage.youtubeURL");
		utils.waitTillElementToBeClickable(element);
		element.clear();
		utils.waitTillElementToBeClickable(element);
		element.sendKeys(url);
	}

	public void restaurantFeedbackURlField(String url) {
		utils.getLocator("mobileConfigurationPage.resturantfeedbackURL").clear();
		utils.getLocator("mobileConfigurationPage.resturantfeedbackURL").sendKeys(url);
	}

	public String msgColor(String path) {
		WebElement ele = utils.getLocator(path);
		String color = getTextColour(ele);
		return color;
	}

	public String isInvalidMsg() {
		List<WebElement> lst = utils.getLocatorList("mobileConfigurationPage.isInvalidMsg");
		String text = "";
		if (!lst.isEmpty()) {
			for (int i = 0; i < 1; i++) {
				text = lst.get(i).getText();
			}
		}
		return text;
	}

	public List<String> appDelectionTypeDropdown(String str) {
		utils.longwait(2000);
		utils.waitTillVisibilityOfElement(
				driver.findElement(By.xpath("//select[@id='mobile_configuration_" + str + "_guest_account_deletion']")),
				"");
		WebElement ele = driver
				.findElement(By.xpath("//select[@id='mobile_configuration_" + str + "_guest_account_deletion']"));
		Select dropdown = new Select(ele);
		List<WebElement> options = dropdown.getOptions();
		List<String> optionValues = new ArrayList<>();
		for (WebElement option : options) {
			optionValues.add(option.getText());
		}
		return optionValues;
	}

	public void iosAppDelectionType(String value) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.appDelectionTypeDropdownIOS");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

		Select select1 = new Select(driver.findElement(By.xpath(xpath)));
		select1.selectByVisibleText(value);
	}

	public void androidAppDelectionType(String value) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.appDelectionTypeDropdownAndroid");
		// utils.staleElementHandle(driver.findElement(By.xpath(xpath)));
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

		Select select1 = new Select(driver.findElement(By.xpath(xpath)));
		select1.selectByVisibleText(value);
	}

	public void selectAppDelectionTypeDropdown(String drpId, String value) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.appDelectionTypeDropdown").replace("$flag",
				drpId);

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
		driver.navigate().refresh();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

		Select select1 = new Select(driver.findElement(By.xpath(xpath)));
		select1.selectByValue(value);
	}

	public void selectAppDelectionAndroidTypeDropdown(String drpId, String value) {

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		driver.navigate().refresh();
		wait.until(ExpectedConditions
				.presenceOfElementLocated(By.id("mobile_configuration_android_guest_account_deletion")));
		driver.navigate().refresh();
		wait.until(ExpectedConditions
				.presenceOfElementLocated(By.id("mobile_configuration_android_guest_account_deletion")));

		Select select2 = new Select(driver.findElement(By.id("mobile_configuration_android_guest_account_deletion")));
		select2.selectByValue(value);

//		utils.implicitWait(5);
//		WebDriverWait wait=new WebDriverWait(driver,15);
//		WebElement drpEle=wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mobile_configuration_android_guest_account_deletion")));
//		Select select=new Select(drpEle);
//		select.selectByValue(value);
//		driver.navigate().refresh();
//		drpEle=wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mobile_configuration_android_guest_account_deletion")));
//		Select select1=new Select(drpEle);
//		select1.selectByValue(value);
	}

	public boolean isEnabled(String str) {
		WebElement ele = utils.getLocator(str);
		return ele.isEnabled();
	}

	public boolean isDisplayed(String str) {
		return utils.getLocator(str).isEnabled();
	}

	public void refereshPage() {
		utils.refreshPage();
		// utils.longWait(5000);
		// utils.implicitWait(60);
		utils.waitTillCompletePageLoad();
	}

	public void clickFrenchLang() {
		utils.implicitWait(5);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", utils.getLocator("mobileConfigurationPage.frenchLang"));
	}

	public void clickRomaniaLang() {
		utils.implicitWait(5);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", utils.getLocator("mobileConfigurationPage.romaniaLang"));
	}

	public List<String> supportedLangList() {
		List<WebElement> ele = utils.getLocatorList("mobileConfigurationPage.supportedLanguagelist");
		List<String> values = new ArrayList<>();
		for (int i = 0; i < ele.size(); i++) {
			String text = ele.get(i).getAttribute("data-language");
			System.out.println(text);
			values.add(text);
		}
		System.out.println(values);
		return values;
	}

	public List<String> nonActiveTabs() {
		List<WebElement> lst = utils.getLocatorList("mobileConfigurationPage.nonActiveTabs");
		List<String> values = new ArrayList<>();
		for (WebElement ele : lst) {
			String text = ele.getText();
			values.add(text);
		}
		return values;
	}

	public String getFooterTextValue() {
		selUtils.longWait(3000);
		return utils.getLocator("mobileConfigurationPage.footerText").getText();
	}

	public String getchallengeDisclaimer() {
		selUtils.longWait(3000);
		return utils.getLocator("mobileConfigurationPage.challengeDisclaimer").getText();
	}

	public String editFooterText() {
		String text = utils.getAlphaNumericString(10);
		WebElement footerTextElement = utils.getLocator("mobileConfigurationPage.footerText");
		utils.scrollToElement(driver, footerTextElement);
		utils.waitTillElementToBeClickable(footerTextElement);
		footerTextElement.clear();
		footerTextElement.sendKeys(text);
		return text;
	}

	public String getGameDisclaimer() {
		selUtils.longWait(3000);
		return utils.getLocator("mobileConfigurationPage.gameDisclaimerTextarea").getText();
	}

	public String gameDisclaimerEditText() {
		String text = "Test Game Disclaimer";
		utils.getLocator("mobileConfigurationPage.gameDisclaimerTextarea").clear();
		utils.getLocator("mobileConfigurationPage.gameDisclaimerTextarea").sendKeys(text);
		return text;
	}

	public String checkDisclaimerFieldSelected() {
		utils.implicitWait(5);
		return utils.getLocator("mobileConfigurationPage.disclaimerBtn").getAttribute("class");
	}

	public void clickLoyaltyAppConfig() {
		utils.waitTillPagePaceDone();
		utils.getLocator("mobileConfigurationPage.loyaltyAppConfigsBtn").click();
		logger.info("clicked loyalty app config");
		TestListeners.test.info("clicked loyalty app config");
	}

	public void editAdditionalLoyaltyAppConfig(String str) {
		utils.getLocator("mobileConfigurationPage.additionalLoyaltyAppConfig").clear();
		utils.getLocator("mobileConfigurationPage.additionalLoyaltyAppConfig").sendKeys(str);
		clickVerifyAndUpdateBtn();
		selUtils.implicitWait(4);
	}

	public void editAdditionalOrderingAppConfig(String str) {
		utils.getLocator("mobileConfigurationPage.additionalOrderingAppConfig").clear();
		utils.getLocator("mobileConfigurationPage.additionalOrderingAppConfig").sendKeys(str);
		clickVerifyAndUpdateBtn();
		selUtils.implicitWait(4);
	}

	public void clickGiftCardAppBtn() {
		utils.getLocator("mobileConfigurationPage.giftCardAppBtn").click();
		logger.info("clicked gift card app config");
	}

	public void editAdditionalGiftCardAppConfig(String str) {
		utils.getLocator("mobileConfigurationPage.additionalGiftCardAppConfig").clear();
		utils.getLocator("mobileConfigurationPage.additionalGiftCardAppConfig").sendKeys(str);
		clickVerifyAndUpdateBtn();
		selUtils.implicitWait(4);
	}

	public void clickAppMessageBtn() {
		utils.getLocator("mobileConfigurationPage.appMsg").click();
		utils.longWaitInSeconds(3);
		logger.info("Clicked App Message Button");
		TestListeners.extentTest.get().info("Clicked App Message Button");
//		utils.waitTillPagePaceDone();
	}

	public void clickDisclaimerBtn() {
		utils.getLocator("mobileConfigurationPage.disclaimerBtn").click();
		utils.longWaitInSeconds(3);
		logger.info("Clicked disclaimer Button");
		TestListeners.extentTest.get().info("Clicked disclaimer Button");
	}

	public void clickCancelBtn() {
		utils.getLocator("mobileConfigurationPage.cancelBtn").click();
		logger.info("Clicked cancel Button");
	}

	@SuppressWarnings("static-access")
	public void clickToggleAccountDelDropdown() {
		utils.waitTillPagePaceDone();
		utils.clickByJSExecutor(driver, utils.getLocator("mobileConfigurationPage.toggleAccountDelDropdown"));
		utils.implicitWait(50);
		// utils.getLocator("mobileConfigurationPage.toggleDropdown").click();
		logger.info("Clicked on toogle account delection dropdown");
	}

	public String getHintText(String str) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.loyaltyAppConfigFieldHintText").replace("$flag",
				str);
		String text = driver.findElement(By.xpath(xpath)).getText();
		return text;
	}

	public List<String> ExcludedHoursOfOperationDrpDownList() {
		utils.getLocator("mobileConfigurationPage.excludedHoursOfOperationField").click();
		List<WebElement> lst = utils.getLocatorList("mobileConfigurationPage.excludedHoursOfOperationDrpDownList");
		List<String> value = new ArrayList<String>();
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			if (text == "") {
				continue;
			}
			value.add(text);
		}
		return value;
	}

	public void clickMobileConfigUpdateBtn() {
		utils.getLocator("mobileConfigurationPage.updateBtn").click();
		logger.info("Clicked Update Button");
		TestListeners.extentTest.get().info("Clicked Update Button");
		utils.waitTillPagePaceDone();
	}

	public boolean checkAsteriskOnFlag(String str) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.asterik").replace("$flag", str);
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (text.contains("*")) {
			logger.info("Asterisk is visible when flag is enable for " + str);
			TestListeners.extentTest.get().info("Asterisk is visible when flag is enable for " + str);
			return true;
		}
		logger.info("Asterisk is not visible when flag is enable for " + str);
		TestListeners.extentTest.get().info("Asterisk is not visible when flag is enable for " + str);
		return false;
	}

	public String getHintTextForDrpDown(String id) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.loyaltyAppConfigFieldHintTextForDrpDown")
				.replace("$flag", id);
		String text = driver.findElement(By.xpath(xpath)).getText();
		return text;
	}

	public void iosAppDelectionTypeNew(String value) {
		utils.waitTillVisibilityOfElement(utils.getLocator("mobileConfigurationPage.appDelectionTypeDropdownIOS"), "");
		utils.selectDrpDwnValue(utils.getLocator("mobileConfigurationPage.appDelectionTypeDropdownIOS"), value);
		logger.info("selected the drpdown value --" + value);
		TestListeners.extentTest.get().info("selected the drpdown value --" + value);
	}

	/*
	 * public void androidAppDelectionTypeNew(String value) { WebElement ele =
	 * utils.getLocator("mobileConfigurationPage.appDelectionTypeDropdownAndroid");
	 * boolean result = false; int attempts = 0; while (attempts < 5) { try {
	 * utils.refreshPageWithCurrentUrl(); utils.waitTillPagePaceDone();
	 * utils.longWaitInSeconds(1); //select drpdwn value
	 * utils.selectDrpDwnValue(ele, value); result = true; break; } catch
	 * (StaleElementReferenceException e) {
	 * 
	 * } attempts++; } logger.info("selected the drpdown value --" + value);
	 * TestListeners.extentTest.get().info("selected the drpdown value --" + value);
	 * }
	 */

	public void androidAppDelectionTyp(String value) {
		utils.selectDrpDwnValue(utils.getLocator("mobileConfigurationPage.appDelectionTypeDropdownAndroid"), value);
		logger.info("selected the drpdown value --" + value);
		TestListeners.extentTest.get().info("selected the drpdown value --" + value);
	}

	public void androidAppDelectionTypeNew(String value) {
		// utils.selectDrpDwnValue(utils.getLocator("mobileConfigurationPage.appDelectionTypeDropdownAndroid"),
		// value);
		WebElement drp = driver.findElement(
				By.xpath("(//span[contains(@id,'mobile_configuration_android_guest_account_deletion')])[1]"));
		utils.StaleElementclick(driver, drp);
		List<WebElement> elements = driver.findElements(
				By.xpath("//ul[contains(@id,'mobile_configuration_android_guest_account_deletion')]//li"));
		utils.selectListDrpDwnValue(elements, value);
		logger.info("selected the drpdown value --" + value);
		TestListeners.extentTest.get().info("selected the drpdown value --" + value);
	}

	public String clickUpdateButton() {
		utils.longWaitInSeconds(4);
		utils.scrollToElement(driver, utils.getLocator("mobileConfigurationPage.updateBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("mobileConfigurationPage.updateBtn"));
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(2);
		logger.info("Clicked Update Button");
		String messge = "";
		TestListeners.extentTest.get().info("Clicked on update button");
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"),
				"Page is not updated successfully");
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public void checkColorInTranslation(String fieldName, String hexcode) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.translationsField").replace("${fieldName}",
				fieldName);
		String actualHexCode = getTextColour(driver.findElement(By.xpath(xpath)));
		Assert.assertEquals(actualHexCode, hexcode, "field is not in the blue color");
		logger.info("Verified field " + fieldName + " is of blue color");
		TestListeners.extentTest.get().pass("Verified field " + fieldName + " is of blue color");
	}

	public void clickFieldInTranslation(String fieldName) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.translationsField").replace("${fieldName}",
				fieldName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("clicked on the field  " + fieldName);
		TestListeners.extentTest.get().info("clicked on the field  " + fieldName);
	}

	public void checkRequiredFieldsInTranslation(int expectedCount, String fieldName) {
		int count = utils.getLocatorList("mobileConfigurationPage.translationsCount").size();
		Assert.assertEquals(count, expectedCount, "for the field " + fieldName + " expected count is not same ");
		logger.info("Verified for the field " + fieldName + " expected count is coming same");
		TestListeners.extentTest.get().pass("Verified for the field " + fieldName + " expected count is coming same");
	}

	// Verify the topmost audit log entry (in green text) based on the provided
	// section
	public String verifyUpdatedConfigInAuditLogs(String section, String keyName) {
		String auditLogEntryXpath = utils.getLocatorValue("mobileConfigurationPage.auditLogEntryForSection")
				.replace("$flag", section);
		String actualValue = "";
		try {
			WebElement auditLogEntryElement = driver.findElement(By.xpath(auditLogEntryXpath));
			String auditLogEntryText = auditLogEntryElement.getText();

			if (auditLogEntryText.startsWith("{")) {
				JSONObject jsonObject = new JSONObject(auditLogEntryText);
				actualValue = jsonObject.get(keyName).toString();
			} else {
				actualValue = auditLogEntryText;
			}
			TestListeners.extentTest.get()
					.info("Audit log entry for '" + section + "' has value: " + auditLogEntryText);
			logger.info("Audit log entry for '" + section + "' has value: " + auditLogEntryText);
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get()
					.fail("Audit log entry not found for '" + section + "' due to exception: " + e.getMessage());
			logger.error("Audit log entry not found for '" + section + "' due to exception: " + e.getMessage());
		}
		return actualValue;
	}

	public String verifyUpdatedConfigInAuditLogsForSection(int lockVersion, String section, String keyName) {
		// XPath for the latest entry's lock version and section
		String auditLogEntryLockVersionXpath = utils.getLocatorValue("mobileConfigurationPage.auditLogEntryForSection")
				.replace("$flag", "Lock Version");
		String auditLogEntryXpath = utils.getLocatorValue("mobileConfigurationPage.auditLogEntryForSection")
				.replace("$flag", section);

		String actualValue = "";
		String expectedLockVersion = String.valueOf(lockVersion);

		try {
			WebElement auditLogEntryLockVersionElement = driver.findElement(By.xpath(auditLogEntryLockVersionXpath));
			String actualLockVersion = auditLogEntryLockVersionElement.getText();
			/*
			 * Find the latest audit log entry's lock version and check if it matches the
			 * expected lock version from DB.
			 */
			if (actualLockVersion.equalsIgnoreCase(expectedLockVersion)) {

				WebElement auditLogEntryElement = driver.findElement(By.xpath(auditLogEntryXpath));
				String auditLogEntryText = auditLogEntryElement.getText();
				TestListeners.extentTest.get().info("For lock version '" + lockVersion + "' found section '" + section
						+ "' with text: " + auditLogEntryText);
				logger.info("For lock version '" + lockVersion + "' found section '" + section + "' with text: "
						+ auditLogEntryText);
				/*
				 * If it matches, then check if the section is JSON object and parse it to get
				 * the value for the given keyName.
				 */
				if (auditLogEntryText.startsWith("{")) {
					JSONObject jsonObject = new JSONObject(auditLogEntryText);
					actualValue = jsonObject.get(keyName).toString();
				} else {
					actualValue = auditLogEntryText;
				}
			} else {
				// Note that audit log entry will not be found if no new change is made
				TestListeners.extentTest.get().fail("Lock version does not match. Expected: " + expectedLockVersion
						+ ". Found: " + actualLockVersion);
				logger.error("Lock version does not match. Expected: " + expectedLockVersion + ". Found: "
						+ actualLockVersion);
			}
		} catch (NoSuchElementException e) {
			TestListeners.extentTest.get().fail("Audit log entry for lock version:" + lockVersion + " section:"
					+ section + " is not found due to exception: " + e.getMessage());
			logger.error("Audit log entry for lock version:" + lockVersion + " section:" + section
					+ " is not found due to exception: " + e.getMessage());
		}
		return actualValue;
	}

	public void enterValueInTranslationField(String value, String fieldName) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.translationsTextArea").replace("${fieldName}",
				fieldName);
		String xpath2 = utils.getLocatorValue("mobileConfigurationPage.translationsTextAreatwo").replace("${fieldName}",
				fieldName);
		if (driver.findElements(By.xpath(xpath)).size() > 0) {
			driver.findElement(By.xpath(xpath)).clear();
			driver.findElement(By.xpath(xpath)).sendKeys(value);
			logger.info("Enter value " + value + " in field " + fieldName);
			TestListeners.extentTest.get().info("Enter value " + value + " in field " + fieldName);
		} else if (driver.findElements(By.xpath(xpath2)).size() > 0) {
			driver.findElement(By.xpath(xpath2)).clear();
			driver.findElement(By.xpath(xpath2)).sendKeys(value);
			logger.info("Enter value " + value + " in field " + fieldName);
			TestListeners.extentTest.get().info("Enter value " + value + " in field " + fieldName);
		} else {
			logger.info("field " + fieldName + " not found");
			TestListeners.extentTest.get().info("field " + fieldName + " not found");
		}
	}

	public boolean checkTranslationAuditLogs(String fieldName, String value) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.checkTranslationAuditLogs")
				.replace("${fieldName}", fieldName).replace("${value}", value);
		if (driver.findElements(By.xpath(xpath)).size() > 0) {
			logger.info("translation audit logs found");
			TestListeners.extentTest.get().info("translation audit logs found");
			return true;
		}
		logger.info("translation audit logs not found");
		TestListeners.extentTest.get().fail("translation audit logs not found");
		return false;
	}

	// Author=Rajasekhar

	public void clickAppConfigFieldsTab() {
		utils.getLocator("mobileConfigurationPage.appConfigsTab").click();
		logger.info("Clicked on AppConfigFieldsTab");
		utils.getLocator("mobileConfigurationPage.OrderingCustomTextHeader").isDisplayed();
		utils.getLocator("mobileConfigurationPage.MenuCustomTextCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.ItemCustomTextCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.NestedItemCustomTextCheckbox").isDisplayed();
		logger.info("All CustomTextCheckbox are available ");

	}

	public void setMenuCustomTextCheckbox(boolean value) {
		if (utils.getLocator("mobileConfigurationPage.MenuCustomTextCheckbox").isSelected() != value) {
			utils.getLocator("mobileConfigurationPage.MenuCustomTextCheckbox").click();
			logger.info(" MenuCustomTextCheckbox is enabled");
			// utils.getLocator("mobileConfigurationPage.MenuCustomTextdrp").click();
			// utils.getLocator("mobileConfigurationPage.MenuCustomTextdrp1").click();
			// utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox").click();
			// utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox").sendKeys("Menu
			// custom text on mobile App");
			// utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn").click();
			selUtils.longWait(5000);
		}

		else {
			logger.info(" MenuCustomTextCheckbox is already enabled");
		}

	}

	public void onMenuCustomTextCheckbox() {
		WebElement MenuCustomTextCheckbox = driver.findElement(By.xpath("//input[@id='menu_custom_text_en']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, MenuCustomTextCheckbox);
		String val = MenuCustomTextCheckbox.getAttribute("checked");
		if (val == null) {
			utils.getLocator("mobileConfigurationPage.MenuCustomTextCheckbox").click();
			logger.info("MenuCustomTextCheckbox is checked sucessfully");
			utils.getLocator("mobileConfigurationPage.MenuCustomTextdrp1").click();
			utils.getLocator("mobileConfigurationPage.MCTdrp1").click();
			utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox").click();
			utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox")
					.sendKeys("Menu custom text on mobile App");
			utils.getLocator("mobileConfigurationPage.Succussbanner").click();
			String sucmsg = utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn").getText();
			System.out.println(sucmsg);
			assertEquals(sucmsg, "x\r\n" + "Mobile configuration updated for App Configs");

		} else {
			logger.info("MenuCustomTextCheckbox is already selected : " + val);
			utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox").click();
			utils.getLocator("mobileConfigurationPage.MenuCustomTextTextbox")
					.sendKeys("Menu custom text on mobile App");
			logger.info("MenuCustomText is updated");
			utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn").click();
			String sucmsg = utils.getLocator("mobileConfigurationPage.Succussbanner").getText();
			System.out.println(sucmsg);
			assertEquals(sucmsg, "Mobile configuration updated for App Configs");
		}

	}

	public void checkNewFlagsonGCPage() {
		utils.getLocator("mobileConfigurationPage.GiftCardTab").click();
		utils.getLocator("mobileConfigurationPage.EnablePurchaseCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.EnableReloadCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.EnableBalanceTransferCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.EnableCardConsolidationCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.EnableCardSharingCheckbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.EnableSupport0CardCreationCheckbox").isDisplayed();
		logger.info("\n" + "Enable Purchase\n" + "Enable Reload\n" + "Enable Balance Transfer\n"
				+ "Enable Card Consolidation\n" + "Enable Card Sharing\n" + "Supports $0 Card Creation"
				+ " flags are avialble");
		TestListeners.extentTest.get()
				.info("Enable Purchase\n" + "Enable Reload\n" + "Enable Balance Transfer\n"
						+ "Enable Card Consolidation\n" + "Enable Card Sharing\n" + "Supports $0 Card Creation"
						+ " flags are avialble");

	}

	public void checkUncheckFlagMobileConfig(String flagName, String checkBoxFlag) {
		selUtils.longWait(3);
		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn").click();
			utils.waitTillPagePaceDone();
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("mobileConfigurationPage.Verify&UpdateBtn").click();
			utils.waitTillPagePaceDone();
			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}
//		utils.waitTillPagePaceDone();
		selUtils.longWait(1000);

	}

	public void verifyColorsTab() {
		selUtils.longWait(5000);
		logger.info("Clicked on colors Tab");
		utils.getLocator("mobileConfigurationPage.Color1Checkbox").isDisplayed();
		utils.getLocator("mobileConfigurationPage.Color1Picker").isDisplayed();
		utils.getLocator("mobileConfigurationPage.Color1Picker").sendKeys("#1eaec7");

		logger.info("Color1 and color2 values are update ");
	}

	public static String hexToRgb(String hexColor) {
		if (hexColor.startsWith("#")) {
			hexColor = hexColor.replace("#", "");
		}
		int r = Integer.parseInt(hexColor.substring(0, 2), 16);
		int g = Integer.parseInt(hexColor.substring(2, 4), 16);
		int b = Integer.parseInt(hexColor.substring(4, 6), 16);
		return "rgb(" + r + ", " + g + ", " + b + ")";
	}

	public void AllowGiftCardPaymentsinSSF_Flag() {
		utils.getLocator("mobileConfigurationPage.AllowGiftCardPaymentsinSSFCheckbox").isDisplayed();
		WebElement checkboxstatus = utils.getLocator("mobileConfigurationPage.AllowGiftCardPaymentsinSSFCheckbox");
		if (!checkboxstatus.isSelected()) {
			logger.info("AllowGiftCardPaymentsinSSF Checkbox is unchecked by default.");
			TestListeners.extentTest.get().info("AllowGiftCardPaymentsinSSF Checkbox is unchecked by default.");
		}
	}

	public void clearExcludedHoursOfOperationField() {
		Select sel = new Select(utils.getLocator("mobileConfigurationPage.excludeOreationHoursDropdownXpath"));
		sel.deselectAll();
		logger.info("exclude_operation_hours dropdown is cleared ");
		TestListeners.extentTest.get().info("exclude_operation_hours dropdown is cleared ");
	}

	public String clickVerifyMessageOnUpdateBtn() {
		// Clicks the "Verify & Update" button and returns the success message
		utils.getLocator("mobileConfigurationPage.verifyAndUpdateBtn").click();
		utils.waitTillPagePaceDone();
		utils.waitTillVisibilityOfElement(utils.getLocator("mobileConfigurationPage.successMsg"), "");
		return utils.getLocator("mobileConfigurationPage.successMsg").getText();
	}

	public void navigateToTab(String tab) {
		String tabXpath = utils.getLocatorValue("mobileConfigurationPage.mobileConfigurationTab").replace("$tabName",
				tab);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(tabXpath)));
		String cancelBtnOnTabXpath = utils.getLocatorValue("mobileConfigurationPage.cancelBtn");
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(cancelBtnOnTabXpath)));
		logger.info("Clicked " + tab + " Button");
		TestListeners.extentTest.get().info("Clicked " + tab + " Button");
	}

	public void enterAmountsInSeveralGiftCards(String enterThelabel, String amount) {
		String xpath = utils.getLocatorValue("mobileConfigurationPage.enterTheLableName");
		xpath = xpath.replace("${xyz}", enterThelabel);
		WebElement inputElement = driver.findElement(By.xpath(xpath));
		inputElement.clear();
		logger.info("Text Field cleared");
		inputElement.sendKeys(amount);
		logger.info("Set " + enterThelabel + " as: " + amount);
		TestListeners.extentTest.get().info("Set " + enterThelabel + " as: " + amount);
	}

	// Alternate of above
	public void enterAmountsInSeveralGiftCards(String enterThelabelAmount1, String enterThelabelAmount2,
			String enterThelabelAmount3, String amount1, String amount2, String amount3) {
		String giftCardsAmountXpath1 = utils.getLocatorValue("mobileConfigurationPage.enterAmountForAllGiftCards");
		giftCardsAmountXpath1 = giftCardsAmountXpath1.replace("${pqr}", enterThelabelAmount1);
		WebElement inputElement1 = driver.findElement(By.xpath(giftCardsAmountXpath1));
		inputElement1.clear();
		logger.info("Text Field cleared");
		inputElement1.sendKeys(amount1);
		logger.info("Amount sent");
		String giftCardsAmountXpaths2 = utils.getLocatorValue("mobileConfigurationPage.enterAmountForAllGiftCards");
		giftCardsAmountXpaths2 = giftCardsAmountXpaths2.replace("${pqr}", enterThelabelAmount2);
		WebElement inputElement2 = driver.findElement(By.xpath(giftCardsAmountXpaths2));
		inputElement2.clear();
		logger.info("Text Field cleared");
		inputElement2.sendKeys(amount2);
		logger.info("Amount sent");
		String giftCardsAmountXpaths3 = utils.getLocatorValue("mobileConfigurationPage.enterAmountForAllGiftCards");
		giftCardsAmountXpaths3 = giftCardsAmountXpaths3.replace("${pqr}", enterThelabelAmount3);
		WebElement inputElement3 = driver.findElement(By.xpath(giftCardsAmountXpaths3));
		inputElement3.clear();
		logger.info("Text Field cleared");
		inputElement3.sendKeys(amount3);
		logger.info("Amount sent");
	}

	public String verifyTheGiftCardsAmountFieldMessage(String labelName, String errorMessage) {
		String message = "";
		String amountFieldErrorMessage1 = utils
				.getLocatorValue("mobileConfigurationPage.giftcardsAmountFieldErrorMessage");
		String amountFieldErrorMessage2 = amountFieldErrorMessage1.replace("${labelName}", labelName);
		String amountFieldErrorMessage = amountFieldErrorMessage2.replace("${errorMessage}", errorMessage);
		WebElement amountfieldMessage = driver.findElement(By.xpath(amountFieldErrorMessage));
		message = amountfieldMessage.getText();
		logger.info("Error handling message found and message is =>" + message);
		TestListeners.extentTest.get()
				.info("Error handling message found for amount field and message is =>" + message);
		return message;
	}

	public void selectGiftCardsAmountFromDropDownList(String labelName, String dropDownElement) {
		String defaultAmountXpath = utils.getLocatorValue("mobileConfigurationPage.defaultAmounts");
		defaultAmountXpath = defaultAmountXpath.replace("${label}", labelName);
		WebElement defaultAmount = driver.findElement(By.xpath(defaultAmountXpath));
		defaultAmount.isDisplayed();
		logger.info("Web element displayed");
		TestListeners.extentTest.get().info("Web element displayed");
		defaultAmount.click();
		logger.info(" Clickeon amount dropDown");
		TestListeners.extentTest.get().info("Clicked on amount dropDown");
		try {
			List<WebElement> defaultAmountDropDownList = utils
					.getLocatorList("mobileConfigurationPage.preSelectAmountDropDownList");
			for (WebElement ele : defaultAmountDropDownList) {
				if (ele.getText().contains(dropDownElement)) {
					utils.implicitWait(50);
					ele.click();
					logger.info("Web element selected from drop down list");
					TestListeners.extentTest.get().info("Web element selected from drop down list");
				}
			}
		} catch (Exception e) {
			logger.info(e);
			List<WebElement> defaultAmountDropDownList = utils
					.getLocatorList("mobileConfigurationPage.preSelectAmountDropDownList");
			for (WebElement ele : defaultAmountDropDownList) {
				if (ele.getText().contains(dropDownElement)) {
					utils.implicitWait(50);
					ele.click();
					logger.info("Exception handled and Web element selected from drop down list");
					TestListeners.extentTest.get()
							.info("Exception handled and Web element selected from drop down list");
				}
			}
		}
	}

	public void enterRequestEmail(String email) {
		WebElement inputElement = utils.getLocator("mobileConfigurationPage.requestEmail");
		inputElement.clear();
		logger.info("Text Field cleared");
		inputElement.sendKeys(email);
		logger.info("Email has been sent successfully");
		TestListeners.extentTest.get().pass("Email has been sent successfully");
	}

//	public String getTextuRLsLabelName(String enterTheURlLabelName) {
//		String urlFieldXpath = utils.getLocatorValue("mobileConfigurationPage.urlsLabelName");
//		urlFieldXpath = urlFieldXpath.replace("${urlLableName}", enterTheURlLabelName);
//		WebElement urlField = driver.findElement(By.xpath(urlFieldXpath));
//		return urlField.getText();
//	}

	public void sendURlsToURLsFields(String enterTheURlLabelName, String sendTheURLs) {
		String urlFieldXpath = utils.getLocatorValue("mobileConfigurationPage.urls");
		urlFieldXpath = urlFieldXpath.replace("${urlFields}", enterTheURlLabelName);
		WebElement urlField = driver.findElement(By.xpath(urlFieldXpath));
		urlField.isEnabled();
		logger.info("URL input field enabled");
		TestListeners.extentTest.get().info("URL input field enable");
		urlField.clear();
		logger.info("URL input field clered");
		urlField.sendKeys(sendTheURLs);
		logger.info("URL sent into url input field");
		TestListeners.extentTest.get().info("URL sent into url input field");
	}

	public String errormessageFromAllTheURlsField(String enterTheURlsFieldLabelName) {
		String errorMessagesXpath = utils.getLocatorValue("mobileConfigurationPage.errorMessageFromURlsFields");
		errorMessagesXpath = errorMessagesXpath.replace("${errorMessage}", enterTheURlsFieldLabelName);
		WebElement errorMessage = driver.findElement(By.xpath(errorMessagesXpath));
		logger.info("Error message found for ->" + enterTheURlsFieldLabelName);
		TestListeners.extentTest.get().info("Error message found for -> " + enterTheURlsFieldLabelName);
		return errorMessage.getText();
	}

	public void clearTheURLsField(String enterTheURlLabelName) {
		String urlFieldXpath = utils.getLocatorValue("mobileConfigurationPage.urls");
		urlFieldXpath = urlFieldXpath.replace("${urlFields}", enterTheURlLabelName);
		WebElement urlField = driver.findElement(By.xpath(urlFieldXpath));
		TestListeners.extentTest.get().info("URL input field enable");
		urlField.clear();
		logger.info("URL input field clered");
		TestListeners.extentTest.get().info("URL sent into url input field");
	}

	// method to interact with Tip input fields
	public void setInputField(String locatorKey, String value) {
		WebElement element = utils.getLocator(locatorKey);
		element.click();
		element.clear();
		element.sendKeys(value);
	}

	// Click on Enable Flat Amount Tipping & Fill Tip amount
	public void clickOnEnableFlatAmountTippingAndFillTipAmount(String strF1, String strF2, String strF3) {

		// Locate the "Enable Flat Amount Tipping" checkbox

		WebElement enableFlatAmountTippingCheckbox = utils
				.getLocator("mobileConfigurationPage.enableFlatAmountTippingCheckbox");
		WebElement enableFlatAmountTippingCheckbox1 = utils
				.getLocator("mobileConfigurationPage.enableFlatAmountTippingCheckbox1");

		if (!enableFlatAmountTippingCheckbox1.isSelected()) {
			enableFlatAmountTippingCheckbox.click();
			logger.info("Enabled 'Enable Flat Amount Tipping' checkbox");

			// Refactored code
			setInputField("mobileConfigurationPage.mobileConfigurationAmountTipOption1", strF1);
			setInputField("mobileConfigurationPage.mobileConfigurationAmountTipOption1", strF2);
			setInputField("mobileConfigurationPage.mobileConfigurationAmountTipOption1", strF3);
			logger.info("Filled tip amounts for Flat Amount Tipping");
		}
	}

// Click on Enable Percentage Tipping & Fill Tip amount
	public void ClickOnEnablePercentageAndFillTipAmount(String strP1, String strP2, String strP3) {

		// Locate the " Enable Percentage Tipping Flag" checkbox
		WebElement enablePercentageTippingCheckbox = utils
				.getLocator("mobileConfigurationPage.enablePercentageTippingCheckbox");

		// enablePercentageTippingFlag
		WebElement enablePercentageTippingCheckbox1 = utils
				.getLocator("mobileConfigurationPage.enablePercentageTippingCheckbox1");

		if (!enablePercentageTippingCheckbox1.isSelected()) {
			enablePercentageTippingCheckbox.click();
			logger.info("Enabled 'Enable Percentage Tipping' checkbox");
		}

		// Refactored code
		setInputField("mobileConfigurationPage.mobileConfigurationPercentageTipOption1", strP1);
		setInputField("mobileConfigurationPage.mobileConfigurationPercentageTipOption2", strP2);
		setInputField("mobileConfigurationPage.mobileConfigurationPercentageTipOption3", strP3);
		logger.info("Filled tip amounts for Percentage Tipping");
	}

// Click on Update Button
	public void clickOnUpdateBtn() {
		utils.getLocator("mobileConfigurationPage.updateButton").click();

	}

	public String getSuccesMessage() {
		// utils.longWaitInSecondSuccess(2);
		String messge = "";
		messge = utils.getLocator("mobileConfigurationPage.SuccesMessage").getText();
		return messge;
	}

	public List<String> getExcludedOrderTypesDrpDownList() {
		utils.getLocator("mobileConfigurationPage.excludedOrderTypesField").click();
		List<WebElement> lst = utils.getLocatorList("mobileConfigurationPage.excludedOrderTypesDrpDownList");
		List<String> values = new ArrayList<String>();
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			if (text.isEmpty()) {
				continue;
			}
			values.add(text);
		}
		return values;
	}

}