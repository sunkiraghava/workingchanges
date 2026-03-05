package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PosIntegrationPage {
	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public PosIntegrationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void verifyGuestLookUpTypeDrpDownList(List<String> expectedLst, String str) {
		List<WebElement> lst = utils.getLocatorList("PosIntegrationPage.guestLookupType");
		logger.info(lst.size());
		List<String> value = new ArrayList<String>();

		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			if (text == " ") {
				continue;
			}
			value.add(text);
		}
		logger.info(value);
		boolean campareResult = utils.comparingLists(expectedLst, value);
		Assert.assertTrue(campareResult, str + " actual lst is not same as expected lst");
		logger.info(str + " actual lst is same as expected lst");
		TestListeners.extentTest.get().pass(str + " actual lst is  same as expected lst");
	}

	public void singleScanTypeTypeDrpDownList(List<String> expectedLst, String str) {
		List<WebElement> lst = utils.getLocatorList("PosIntegrationPage.singleScanType");
		List<String> value = new ArrayList<String>();
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			if (text == "") {
				continue;
			}
			value.add(text);
		}
		System.out.println(value);
		boolean campareResult = utils.comparingLists(expectedLst, value);
		Assert.assertTrue(campareResult, str + " actual lst is not same as expected lst");
		logger.info(str + " actual lst is same as expected lst");
		TestListeners.extentTest.get().pass(str + " actual lst is  same as expected lst");
	}

	public String getHintText(String str) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.hintText").replace("$flag", str);
		String text = driver.findElement(By.xpath(xpath)).getText();
		return text;
	}

	public void clickUpdateBtn() {
		utils.longWaitInSeconds(1);
		utils.clickByJSExecutor(driver, utils.getLocator("PosIntegrationPage.updateBtn"));
		utils.waitTillPagePaceDone();
		logger.info("click on the update button");
		TestListeners.extentTest.get().info("click on the update button");
	}

	public void removeSelectedField(String str) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.removeSelectedField").replace("$flag", str);
		driver.findElement(By.xpath(xpath)).click();
	}

	public void clickAuditLog() {
		utils.getLocator("PosIntegrationPage.auditLogButton").click();
		selUtils.implicitWait(50);
		TestListeners.extentTest.get().info("click on the audit log button");
	}

	public String auditLogValue(String str) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.auditLogValue").replace("$flag", str);
		String txt = driver.findElement(By.xpath(xpath)).getText();
		return txt;
	}

	public void refreshPage() {
		utils.refreshPage();
		selUtils.implicitWait(50);
	}

	public void selectDrpDownValue(String str, String value) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.selectPosIntegrationDrpDown").replace("$flag", str);
		WebElement ele = driver.findElement(By.xpath(xpath));
		utils.selectDrpDwnValue(ele, value);
	}

	public void deselectDrpDownValue(String str, String value) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.selectPosIntegrationDrpDown").replace("$flag", str);
		WebElement ele = driver.findElement(By.xpath(xpath));
		utils.deselectDrpDwnValue(ele, value);
	}

	public void deselectAllValuesFromDrpDown(String str) {
		String xpath = utils.getLocatorValue("PosIntegrationPage.selectPosIntegrationDrpDown").replace("$flag", str);
		WebElement ele = driver.findElement(By.xpath(xpath));
		utils.deselectAllDrpDownValue(ele);
	}

	public void checkUncheckAnyFlag(String flagName, String checkBoxFlagValue) {

		WebElement checkBoxFlag = driver
				.findElement(By.xpath("//label[contains(text(),'" + flagName + "')]/preceding-sibling::input[1]"));
		String checkBoxFlagCurrentStatus = checkBoxFlag.getAttribute("checked");

		switch (checkBoxFlagValue) {
		case "check":
			if (checkBoxFlagCurrentStatus == null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " checked successfully");
				TestListeners.extentTest.get().info(flagName + " checked successfully");
			} else {
				logger.info(flagName + " is already checked...");
				TestListeners.extentTest.get().info(flagName + " is already checked...");
			}

			break;
		case "uncheck":
			if (checkBoxFlagCurrentStatus != null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " unchecked successfully");
				TestListeners.extentTest.get().info(flagName + " unchecked successfully");
			} else {
				logger.info(flagName + " is already unchecked...");
				TestListeners.extentTest.get().info(flagName + " is already unchecked...");
			}

			break;
		}
	}

	public void selectDriveThruDrpDownValue(String value) {
		WebElement ele = utils.getLocator("PosIntegrationPage.selectDriveThruShortCodeLengthFromDropdown");
		utils.selectDrpDwnValue(ele, value);
	}

	public void deselectDriveThruDrpDownValue() {
		try {
			boolean isDisplayed = pageObj.gamesPage().isPresent("PosIntegrationPage.deSelectDriveThruShortCodeLengthFromDropdown");
			if (isDisplayed) {
				WebElement ele = utils.getLocator("PosIntegrationPage.deSelectDriveThruShortCodeLengthFromDropdown");
				ele.click();
				utils.getLocator("PosIntegrationPage.deSelectDriveThruShortCodeLengthField").click();
			}

			utils.logInfo("Drive Thru Short Code Length deselected successfully");
		} catch (NoSuchElementException | StaleElementReferenceException e) {
			utils.logInfo("Drive Thru Short Code Length is already deselected");
		}
	}

    public void clickDriveThroughUpdateBtn() {
        utils.longWaitInSeconds(1);
        utils.clickByJSExecutor(driver, utils.getLocator("PosIntegrationPage.updateBtn1"));
        utils.waitTillPagePaceDone();
        logger.info("click on the update button");
        TestListeners.extentTest.get().info("click on the update button");
    }
}
