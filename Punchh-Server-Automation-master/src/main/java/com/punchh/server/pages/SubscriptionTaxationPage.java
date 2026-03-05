package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SubscriptionTaxationPage {
	static Logger logger = LogManager.getLogger(SubscriptionTaxationPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, segmentCount, pnCount, smsCount;

	public SubscriptionTaxationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	// Tabs are: Tax Rules, State Level Tax, Location Level Tax and Bulk Update
	public void clickOnGivenTab(String tabName) {
		String xpath = utils.getLocatorValue("subscriptionTaxationPage.taxationTabs").replace("$temp", tabName);
		driver.findElement(By.xpath(xpath)).click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on " + tabName + " tab");
		TestListeners.extentTest.get().info("Clicked on " + tabName + " tab");
	}

	public void selectCountryFromDrpdown(String countryName) {
		WebElement countryDropdown = utils.getLocator("subscriptionTaxationPage.countryDropdown");
		utils.selectDrpDwnValue(countryDropdown, countryName);
		logger.info("Selected country: " + countryName);
		TestListeners.extentTest.get().info("Selected country: " + countryName);
	}

	public void clickOnAddCountryBtn() {
		WebElement addCountryBtn = utils.getLocator("subscriptionTaxationPage.addCountryBtn");
		utils.waitTillElementToBeClickable(addCountryBtn);
		addCountryBtn.click();
		logger.info("Clicked on Add country button");
		TestListeners.extentTest.get().info("Clicked on Add country button");
	}

	public boolean isStateTableAppeared() {
		utils.waitTillCompletePageLoad();
		boolean isStateTableDisplayed = false;
		try {
			WebElement stateTable = utils.getLocator("subscriptionTaxationPage.stateTable");
			isStateTableDisplayed = utils.checkElementPresent(stateTable);
		} catch (Exception e) {
			logger.error("State table not found");
			TestListeners.extentTest.get().info("State table not found");
		}

		return isStateTableDisplayed;

	}

	public String getAlertText() throws InterruptedException {

		Thread.sleep(2000);
		// Check if alert is present
		if (Utilities.isAlertpresent(driver)) {
			String alertText = utils.getPopUpText();
			logger.info("Element: Alert is present");
			TestListeners.extentTest.get().info("Element: Alert is present");
			return alertText;
		} else {
			logger.info("Element: Alert is not present");
			TestListeners.extentTest.get().info("Element: Alert is not present");
			return null;
		}

	}

	public void enterTaxPercentage(String stateCode, String percentage) throws InterruptedException {

		String xpath = utils.getLocatorValue("subscriptionTaxationPage.taxPercentageInputField").replace("$temp",
				stateCode);
		WebElement taxPercentage = driver.findElement(By.xpath(xpath));
		taxPercentage.clear();
		taxPercentage.sendKeys(percentage);
		logger.info("Entered tax percentage: " + percentage + " for state code: " + stateCode);
		TestListeners.extentTest.get().info("Entered tax percentage: " + percentage + " for state code: " + stateCode);
	}

	public void clickOnSaveBtn(String stateCode) throws InterruptedException {
		String xpath = utils.getLocatorValue("subscriptionTaxationPage.saveBtn").replace("$temp", stateCode);
		WebElement saveBtn = driver.findElement(By.xpath(xpath));
		saveBtn.click();
		Thread.sleep(3000);
		logger.info("Clicked on save tax percentage btn for state code: " + stateCode);
		TestListeners.extentTest.get().info("Clicked on save tax percentage btn for state code: " + stateCode);
	}

	public String getEnteredTaxPercentage(String stateCode) throws InterruptedException {

		String xpath = utils.getLocatorValue("subscriptionTaxationPage.taxPercentageInputField").replace("$temp",
				stateCode);
		WebElement taxPercentage = driver.findElement(By.xpath(xpath));
		String value = taxPercentage.getAttribute("value");
		logger.info("Entered tax percentage: " + value + " for state code: " + stateCode);
		TestListeners.extentTest.get().info("Entered tax percentage: " + value + " for state code: " + stateCode);
		return value;
	}

	// Enters the Tax Rate (%)
	public void enterTaxRate(String taxRate) throws InterruptedException {
		WebElement field = utils.getLocator("subscriptionTaxationPage.taxRateField");
		field.isDisplayed();
		field.clear();
		field.sendKeys(taxRate);
		utils.logit("Entered tax rate: " + taxRate);
	}

	public void removeCountry() throws InterruptedException {
		try {
			utils.implicitWait(2);
			WebElement taxPercentage = utils.getLocator("subscriptionTaxationPage.removeCountry");
			taxPercentage.click();
			utils.acceptAlert(driver);
			utils.waitTillPagePaceDone();
			utils.acceptAlert(driver);
			logger.info("Clicked on remove country button");
			TestListeners.extentTest.get().info("Clicked on remove country button");
		} catch (Exception e) {
			logger.error("Remove country button not found");
			TestListeners.extentTest.get().info("Remove country button not found");

		}
		utils.implicitWait(60);
	}

}
