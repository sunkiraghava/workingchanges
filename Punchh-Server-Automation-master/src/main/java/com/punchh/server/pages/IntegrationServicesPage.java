package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IntegrationServicesPage {
	static Logger logger = LogManager.getLogger(IntegrationServicesPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, pnCount, smsCount;
	int segmentCount;
	String age = "30";

	public IntegrationServicesPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickMenuBtn() {
		utils.getLocator("whitelabelPage.MenuBtn").click();
	}

	public boolean isHashedValue(String path) {
		String attrvalue = utils.getLocator(path).getAttribute("value");
		if (attrvalue.contains("****")) {
			logger.info("value is hashed");
			return true;
		}
		logger.info("value is not hashed");
		return false;
	}

	public void setSFMCFolderName(String name) {
		utils.getLocator("integrationServicesPage.sFMCTab").click();
		utils.getLocator("integrationServicesPage.folderNameTextBox").clear();
		utils.getLocator("integrationServicesPage.folderNameTextBox").sendKeys(name);
		utils.getLocator("integrationServicesPage.updateSFMCBtn").click();
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().pass("Folder name is set as :" + name);
	}

	public void enableBulkSms(boolean bulkSmsEnabled) {
		utils.clickByJSExecutor(driver, utils.getLocator("whitelabelPage.attentiveServices"));
		utils.setCheckboxStateViaCheckBoxText(utils.getLocatorValue("whitelabelPage.attentiveBulkSmsFlag"),
				bulkSmsEnabled);
		utils.clickByJSExecutor(driver, utils.getLocator("whitelabelPage.updateAttentive"));
		logger.info("Updated Attentive credentials");
		TestListeners.extentTest.get().pass("Updated Attentive credentials");
	}

	public void enableMarketingCloud(String cloudName) {
		WebElement ele = utils.getLocator("integrationServicesPage.enableMarketingCloudDrp");
		utils.selectDrpDwnValue(ele, cloudName);
		utils.getLocator("cockpitEarningPage.updateBtn").click();
		logger.info("Updated Enable marketing cloud? on cockpit > campaigns : " + cloudName);
		TestListeners.extentTest.get().pass("Updated Enable marketing cloud? on cockpit > campaigns : " + cloudName);
	}

}
