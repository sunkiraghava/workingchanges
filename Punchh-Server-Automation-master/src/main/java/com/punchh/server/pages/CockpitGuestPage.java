package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import net.minidev.json.JSONValue;

@Listeners(TestListeners.class)
public class CockpitGuestPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public CockpitGuestPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void clickMiscConfig() {
		utils.getLocator("CockpitGuestPage.miscConfig").click();
		logger.info("clicked on Miscellaneous Config button");
	}

	public String getPageSource() {
		String data = driver.getPageSource();
		return data;
	}

	public void clickGuestValidationBtn() {
		utils.getLocator("CockpitGuestPage.guestValidationBtn").click();
		logger.info("clicked on guest validation btn");
	}

	public void SelectValGuestMandatoryInputFields(String str) {
		Select select = new Select(utils.getLocator("CockpitGuestPage.guestMandatoryInputFields"));
		select.selectByVisibleText(str);
	}

	public void selectAgeVerificationAdapter(String str) {
		Select select = new Select(utils.getLocator("CockpitGuestPage.ageVerificationAdapter"));
		select.selectByVisibleText(str);
	}

	public void removeSelectValueUsingx(String str) {
		String xpath = utils.getLocatorValue("CockpitGuestPage.inputValue").replace("$flag", str);
		driver.findElement(By.xpath(xpath)).click();
		utils.getLocator("CockpitGuestPage.guestMandatoryInputFieldsEmpty").click();
	}

	public void clearDropdownValue(String drpName) {
		utils.longwait(3);
		String xpathSelectArea = utils.getLocatorValue("CockpitGuestPage.selectArea").replace("$flag", drpName);
		String dropdownValue = (String) pageObj.dashboardpage().getSelectedValueFromDropdown(drpName);

		if (!dropdownValue.isEmpty()) {
			driver.findElement(By.xpath(xpathSelectArea)).click();
			String xpathClearBtn = utils.getLocatorValue("CockpitGuestPage.xValue").replace("$flag", drpName);
			driver.findElement(By.xpath(xpathClearBtn)).click();
			driver.findElement(By.xpath(xpathSelectArea)).click();
			logger.info("Dropdown value of " + drpName + " is removed");
			TestListeners.extentTest.get().info("Dropdown value of " + drpName + " is removed");
		} else {
			logger.info("Dropdown value of " + drpName + " is already empty");
			TestListeners.extentTest.get().info("Dropdown value of " + drpName + " is already empty");
		}
	}

	public void clickUpdateBtn() {
		utils.clickByJSExecutor(driver, utils.getLocator("CockpitGuestPage.updateBtn"));
		logger.info("Clicked On update page button");
		TestListeners.extentTest.get().info("Clicked On update page button");
	}

	public void editUserIncinerateDaysField(String str) {
		utils.getLocator("cockpitPage.userIncinerateDaysField").clear();
		utils.getLocator("cockpitPage.userIncinerateDaysField").sendKeys(str);
		clickUpdateBtn();
		utils.logit("User Incinerate days are set to " + str);
		utils.waitTillPagePaceDone();
	}

	public String updateItemRecSys(WebElement field, String updateVal, String str) {
		String value = "";
		if (str.equals("GetText")) {
			value = field.getText();
			logger.info("Text from the field is returned as: " + value);
			TestListeners.extentTest.get().info("Text from the field is returned as: " + value);
		} else if (str.contains("ClearAndUpdate")) {
			field.clear();
			field.sendKeys(updateVal);
			if (str.equals("ClearAndUpdateMenu")) {
				utils.clickByJSExecutor(driver, utils.getLocator("CockpitGuestPage.updateMenuBtn"));
			} else if (str.equals("ClearAndUpdateModelConfig")) {
				utils.clickByJSExecutor(driver, utils.getLocator("CockpitGuestPage.updateBtn"));
			}
			utils.waitTillPagePaceDone();
			logger.info(str + " is done and Update button is clicked.");
			TestListeners.extentTest.get().info(str + " is done and Update button is clicked.");
		}
		return value;
	}

	public String addValToJson(String oldJson, String addVal) {
		oldJson = oldJson.substring(0, oldJson.length() - 1) + "";
		String newJson = oldJson + addVal;
		boolean valid = JSONValue.isValidJson(newJson);
		Assert.assertTrue(valid, "new created String is not a valid json");
		logger.info("new created String is valid JSON");
		TestListeners.extentTest.get().info("new created String is valid JSON");
		return newJson;
	}

//existing can be removed 	
	public void enterDataInEmbedCodeBusinessLiability(String text) {
		utils.waitTillPagePaceDone();
		utils.clickByJSExecutor(driver, utils.getLocator("CockpitGuestPage.embedCodeBusinessLiability"));
		utils.getLocator("CockpitGuestPage.embedCodeBusinessLiability").clear();
		utils.getLocator("CockpitGuestPage.embedCodeBusinessLiability").sendKeys(text);
		logger.info("Data entered in Embed Code :- " + text);
		TestListeners.extentTest.get().info("Data entered in Embed Code :- " + text);
	}

	public void accountReEvaluationStrategy(String strategy, int actualEleSize) {
		utils.getLocator("CockpitGuestPage.pointsMigrationStrategyDrpDown").click();
		List<WebElement> ele = utils.getLocatorList("CockpitGuestPage.pointsMigrationStrategyList");
		int expectedEleSize = ele.size();
		utils.selectListDrpDwnValue(ele, strategy);
		logger.info("Initial Points Migration Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get().pass("Initial Points Migration Strategy is selected as :- " + strategy);
		Assert.assertEquals(expectedEleSize, actualEleSize);
		logger.info("Verified that Number of Initial Points Migration Strategy present :- " + expectedEleSize);
		TestListeners.extentTest.get()
				.pass("Verified that Number of Initial Points Migration Strategy is selected as :- " + expectedEleSize);
	}

	public boolean verifyInitialPointsMigrationStrategyOptionsValue(String expectedValue) {
		utils.waitTillPagePaceDone();
		List<String> actualList = utils.getAllVisibleTextFromDropdwon(
				utils.getLocator("CockpitGuestPage.initialPointsMigrationStrategyDropdown"));
		boolean result = actualList.contains(expectedValue);
		return result;
	}

	public boolean verifyInitialPointsMigrationStrategy(String expectedHint) {
		List<WebElement> wEleList = utils
				.getLocatorList("CockpitGuestPage.initialPointsMigrationStrategyHintMessageXapth");
		boolean result = false;
		for (WebElement wEle : wEleList) {
			String text = wEle.getText();
			if (text.contains(expectedHint)) {
				result = true;
				break;
			}

		}
		return result;

	}

// Common methods for the report to access the embed code. 
	public void enterDataInEmbedCode(String locator, String text) {
		String xpath = utils.getLocatorValue("CockpitGuestPage.embeddedLinkReport").replace("$temp", locator);
		WebElement linkTab = driver.findElement(By.xpath(xpath));
		utils.clickByJSExecutor(driver, linkTab);
		linkTab.clear();
		linkTab.sendKeys(text);
		utils.getLocator("dashboardPage.updateBtn").click();
		utils.waitTillPagePaceDone();
		logger.info("Data entered in Embed Code :- " + text);
		TestListeners.extentTest.get().info("Data entered in Embed Code :- " + text);
	}
}
