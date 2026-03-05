package com.punchh.server.pages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.aventstack.extentreports.ExtentTest;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class CockpitTransferPage {

	static Logger logger = LogManager.getLogger(CockpitTransferPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private ExtentTest pass;
	public CockpitTransferPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		new PageObj(driver);

	}

	public void transferFlags(String checkBoxFlag) {
		// Navigate to Transfer tab
		utils.getLocator("earningPage.burger_icon").click();
		utils.getLocator("earningPage.transferTab").click();

		// Define all checkbox locators
		Map<String, WebElement> checkboxes = new LinkedHashMap<>();
		checkboxes.put("Point transfer checkbox", utils.getLocator("earningPage.pointTransferFlag"));
		checkboxes.put("Currency transfer checkbox", utils.getLocator("earningPage.currencyTransferFlag"));
		checkboxes.put("Reward transfer checkbox", utils.getLocator("earningPage.rewardTransferFlag"));

		boolean isUpdated = false;

		// Loop through each checkbox and apply logic
		for (Map.Entry<String, WebElement> entry : checkboxes.entrySet()) {
			String name = entry.getKey();
			WebElement checkbox = entry.getValue();
			String checkBoxValue = checkbox.getAttribute("checked");

			if ((checkBoxValue == null) && checkBoxFlag.equalsIgnoreCase("uncheck")) {
				logger.info(name + " is unchecked and should remain unchecked (flag=" + checkBoxFlag + ")");
				TestListeners.extentTest.get().pass(name + " is unchecked and should remain unchecked (flag=" + checkBoxFlag + ")");

			} else if ((checkBoxValue == null) && checkBoxFlag.equalsIgnoreCase("check")) {
				utils.clickByJSExecutor(driver, checkbox);
				isUpdated = true;
				logger.info(name + " was unchecked and now checked as per flag=" + checkBoxFlag);
				TestListeners.extentTest.get().pass(name + " was unchecked and now checked as per flag=" + checkBoxFlag);

			} else if ("true".equalsIgnoreCase(checkBoxValue) && checkBoxFlag.equalsIgnoreCase("uncheck")) {
				utils.clickByJSExecutor(driver, checkbox);
				isUpdated = true;
				logger.info(name + " was checked and now unchecked as per flag=" + checkBoxFlag);
				TestListeners.extentTest.get().pass(name + " was checked and now unchecked as per flag=" + checkBoxFlag);

			} else if ("true".equalsIgnoreCase(checkBoxValue) && checkBoxFlag.equalsIgnoreCase("check")) {
				logger.info(name + " already checked and remains checked (flag=" + checkBoxFlag + ")");
				TestListeners.extentTest.get().pass(name + " already checked and remains checked (flag=" + checkBoxFlag + ")");
			}
		}

		// Click update only once if any checkbox state changed
		if (isUpdated) {
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Updated all modified checkbox states successfully.");
			TestListeners.extentTest.get().pass("Updated all modified checkbox states successfully.");
		} else {
			logger.info("No checkbox state was changed; update button not clicked.");
			TestListeners.extentTest.get().pass("No checkbox state was changed; update button not clicked.");
		}
	}


	public void pointexpirystrategy(int days) {
		utils.getLocator("earningPage.transferTab").click();
		List<WebElement> pointExpiryStrategy = (List<WebElement>) utils.getLocator("earningPage.transferredPointsExpiryStrategyDropDown");
		utils.selectListDrpDwnValue(pointExpiryStrategy, "Fixed Days");
		WebElement expiryDays = utils.getLocator("earningPage.transferredPointsExpiryDaysBox");
		expiryDays.clear();
		expiryDays.sendKeys(String.valueOf(days));
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("Point Expiry Strategy set to Expire after specific days with days: " + days);
		TestListeners.extentTest.get()
		.pass("Point Expiry Strategy set to Expire after specific days with days: " + days);
	}



}
