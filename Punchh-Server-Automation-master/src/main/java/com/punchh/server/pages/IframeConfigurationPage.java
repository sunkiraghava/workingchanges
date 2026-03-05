package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeConfigurationPage {
	static Logger logger = LogManager.getLogger(IframeFieldValidationPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String iframeEmail, iframeEmailinvalid, temp = "temp";
	String iframeEmailvalid;
	String iframeEmaildata;

	public IframeConfigurationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickEClubWidgetBtn() {
		utils.getLocator("iframeConfigurationPage.eClubWidgetBtn").click();
		logger.info("clicked on eclub widget button");
		TestListeners.extentTest.get().info("clicked on eclub widget button");
	}

	public void editEClubPostRegistrationRedirectURL(String str) {
		utils.getLocator("iframeConfigurationPage.eClubPostRegistrationRedirectURL").clear();
		utils.getLocator("iframeConfigurationPage.eClubPostRegistrationRedirectURL").sendKeys(str);
	}

	public String getErrorMessage() {
		selUtils.implicitWait(60);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public String getSuccessMessage() {
		selUtils.implicitWait(60);
		// utils.waitTillCompletePageLoad();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

	public void clickUpdateBtn() {
		utils.getLocator("iframeConfigurationPage.updateBtn").click();
		logger.info("clicked on the update btn");
	}

	public String getTextColour(WebElement ele) {
		String rgbFormat = ele.getCssValue("color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		return hexcode;
	}

	public String getCssPropertyValue(String locator, String property, String tempValue) {
		WebElement element = null;
		if (tempValue.isEmpty()) {
			element = utils.getLocator(locator);
		} else {
			String newXpath = utils.getLocatorValue(locator).replace("temp", tempValue);
			element = driver.findElement(By.xpath(newXpath));
		}
		String value = element.getCssValue(property);
		logger.info("The value of the CSS property '" + property + "' is: " + value);
		TestListeners.extentTest.get().info("The value of the CSS property '" + property + "' is: " + value);
		return value;
	}

	public String getElementText(String locator, String tempValue) {
		WebElement element = null;
		String xpath = "";
		if (!tempValue.isEmpty()) {
			xpath = utils.getLocatorValue(locator).replace("temp", tempValue);
		} else {
			xpath = utils.getLocatorValue(locator);
		}
		element = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(element, "Heading");
		utils.scrollToElement(driver, element);
		logger.info("Found text: " + element.getText());
		TestListeners.extentTest.get().info("Found text: " + element.getText());
		return element.getText();
	}

	// It verifies the color of language support for a field after updating it
	public void verifyLanguageSupportAfterUpdate(String fieldName, String languageName, String inputText,
			String rgbColor) {
		String languageXpath = utils.getLocatorValue("iframeConfigurationPage.languageSupport")
				.replace("temp1", fieldName).replace("temp2", languageName);
		driver.findElement(By.xpath(languageXpath)).click();
		String textFieldXpath = utils.getLocatorValue("iframeConfigurationPage.inputField").replace("temp1", fieldName)
				.replace("temp2", languageName);
		logger.info("Clicked " + languageName + " of " + fieldName);
		TestListeners.extentTest.get().info("Clicked " + languageName + " of " + fieldName);
		WebElement textField = driver.findElement(By.xpath(textFieldXpath));
		textField.clear();
		textField.sendKeys(inputText);
		clickUpdateBtn();
		utils.waitTillPagePaceDone();
		String languageSupportColor = driver.findElement(By.xpath(languageXpath)).getCssValue("color");
		if (languageSupportColor.equals(rgbColor)) {
			logger.info("Language support for " + fieldName + " in " + languageName + " is updated. " + languageName
					+ " color is as expected.");
			TestListeners.extentTest.get().info("Language support for " + fieldName + " in " + languageName
					+ " is updated. " + languageName + " color is as expected.");
		} else {
			logger.error("Language support for " + fieldName + " in " + languageName + " could not be verified.");
			TestListeners.extentTest.get()
					.info("Language support for " + fieldName + " in " + languageName + " could not be verified.");
		}
	}

	public void selectReward(String rewardName) {
		String rewardRadioXpath = utils.getLocatorValue("iframePage.selectReward_RadioButton");
		rewardRadioXpath = rewardRadioXpath.replace("${rewardName}", rewardName);
		driver.findElement(By.xpath(rewardRadioXpath)).click();
		logger.info("Selected the reward: " + rewardName);
		TestListeners.extentTest.get().info("Selected the reward: " + rewardName);
	}

	public String getAttributeValue(String locator, String attribute, String tempValue) {
		WebElement element = null;
		String xpath, value = "";
		try {
			utils.implicitWait(1);
			if (!tempValue.isEmpty()) {
				xpath = utils.getLocatorValue(locator).replace("temp", tempValue);
			} else {
				xpath = utils.getLocatorValue(locator);
			}
			element = driver.findElement(By.xpath(xpath));
			value = element.getAttribute(attribute);
			logger.info("The value of the attribute '" + attribute + "' is: " + value);
			TestListeners.extentTest.get().info("The value of the attribute '" + attribute + "' is: " + value);
		} catch (NoSuchElementException e) {
			logger.error("Element not found: " + e.getRawMessage());
			TestListeners.extentTest.get().info("Element not found: " + e.getRawMessage());
		}
		utils.implicitWait(50);
		return value;
	}

	public String msgColor(String path) {
		WebElement ele = utils.getLocator(path);
		String color = getTextColour(ele);
		return color;
	}

	public void clickWhitelabelURLBtn() {
		utils.getLocator("iframeConfigurationPage.whiteLabelURLs").click();
		logger.info("clicked on whitelabel url button");
		TestListeners.extentTest.get().info("clicked on whitelabel url button");
	}

	public void editInputField(String inputFieldName, String path, String str) {
		utils.getLocator(path).clear();
		utils.getLocator(path).sendKeys(str);
		logger.info("Update the input field " + inputFieldName);
		TestListeners.extentTest.get().info("Update the input field " + inputFieldName);
	}

	public void clickAccountDeleteTab() {
		utils.getLocator("iframeConfigurationPage.AccountDelationTab").isDisplayed();
		utils.getLocator("iframeConfigurationPage.AccountDelationTab").click();
		logger.info("Clicked on the Account deletion tab btn");
		TestListeners.extentTest.get().info("Clicked on the Account deletion tab btn");
	}

	public void clickCheckinTab() {
		utils.getLocator("iframeConfigurationPage.CheckinTab").isDisplayed();
		utils.getLocator("iframeConfigurationPage.CheckinTab").click();
		logger.info("Clicked on the CheckinTab  btn");
		TestListeners.extentTest.get().info("Clicked on the CheckinTab btn");
	}

	public void ClickUserSignupEditProfileTab() {
		utils.getLocator("iframeConfigurationPage.UserSignupEditProfileTab").isDisplayed();
		utils.getLocator("iframeConfigurationPage.UserSignupEditProfileTab").click();
		logger.info("Clicked on the UserSignupEditProfileTab  btn");
		TestListeners.extentTest.get().info("Clicked on the UserSignupEditProfileTab btn");
	}

	public void verifyEmailConfirmationFields(String confirmtxt1, String confirmtxt2) {
		utils.getLocator("iframeConfigurationPage.EmailVerificationConfirmationTxtbox").isDisplayed();
		utils.getLocator("iframeConfigurationPage.EmailVerificationAlreadyConfirmedTxtbox").isDisplayed();
		logger.info("EmailVerificationConfirmation fileds are availble in iframe configurations");
		TestListeners.extentTest.get()
				.info("EmailVerificationConfirmation fileds are availble in iframe configurations");
		utils.getLocator("iframeConfigurationPage.EmailVerificationConfirmationTxtbox").clear();
		utils.getLocator("iframeConfigurationPage.EmailVerificationAlreadyConfirmedTxtbox").clear();
		;
		utils.getLocator("iframeConfigurationPage.EmailVerificationConfirmationTxtbox").sendKeys(confirmtxt1);
		utils.getLocator("iframeConfigurationPage.EmailVerificationAlreadyConfirmedTxtbox").sendKeys(confirmtxt2);
		clickUpdateBtn();
		logger.info("EmailVerificationConfirmation fileds updated with custom text");
		TestListeners.extentTest.get().info("EmailVerificationConfirmation fileds updated with custom text");

	}

	public void clickOnPageTab(String tabName) {

		// Try direct tab click
		try {
			WebElement tabElement = driver
					.findElement(By.xpath("//li[@class='nav-item']//a[normalize-space(text())='" + tabName + "']"));

			if (tabElement.isDisplayed()) {
				tabElement.click();
				logger.info("Tab with name '" + tabName + "' found on page directly and clicked.");
				TestListeners.extentTest.get()
						.pass("Tab with name '" + tabName + "' found on page directly and clicked.");
				return;
			}
		} catch (NoSuchElementException e) {
			logger.info("Tab '" + tabName + "' not found directly. Trying hamburger menu...");
		}

		// Try via hamburger
		try {
			WebElement hamburgerIcon = utils.getLocator("instanceDashboardPage.hamburgerTabsIcon");
			if (hamburgerIcon != null && hamburgerIcon.isDisplayed()) {
				hamburgerIcon.click();
				utils.implicitWait(2);

				WebElement hiddenTab = driver
						.findElement(By.xpath("//li[@class='nav-item']//a[normalize-space(text())='" + tabName + "']"));

				if (hiddenTab.isDisplayed()) {
					hiddenTab.click();
					logger.info("Tab with name '" + tabName + "' found under hamburger and clicked.");
					TestListeners.extentTest.get()
							.pass("Tab with name '" + tabName + "' found under hamburger and clicked.");
					return;
				}
			}
		} catch (NoSuchElementException e) {
			logger.warn("Tab '" + tabName + "' not found under hamburger.");
		}
	}

}
