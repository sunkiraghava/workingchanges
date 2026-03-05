package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class CockpitRedemptionsCodePage {
	static Logger logger = LogManager.getLogger(CockpitRedemptionsCodePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	public CockpitRedemptionsCodePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		new PageObj(driver);
	}

	public void clickedOnGenerateUUIDforOLO(String checkBoxFlag) {

		utils.getLocator("cockpitRedemptionsCodePage.redemptioncodetab").click();
	    WebElement oloUUIDCheckbox = utils.getLocator("cockpitRedemptionsCodePage.oloUUIDcheckbox");
	    String checkBoxValue = oloUUIDCheckbox.getAttribute("checked");
		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("UUID checkbox Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("UUID checkbox Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, oloUUIDCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("UUID checkbox is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("UUID checkbox is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, oloUUIDCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();

			logger.info("UUID checkbox is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("UUID checkbox is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("UUID checkbox is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("UUID checkbox is already checked and user want to check the chekedbox, so do not click");

		}
	}
}
