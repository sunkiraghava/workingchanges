package com.punchh.server.pages;

import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;


@Listeners(TestListeners.class)
public class RedemptionLogPage {
	static Logger logger = LogManager.getLogger(RedemptionLogPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String scheduleName;

	public RedemptionLogPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public boolean verifyRedemptionLogg(String redemptionCode, String amount) {
		amount = amount.replace(".0", ".");
		try {
			utils.getLocator("redemptionLogPage.redemptionHeading").isDisplayed();
			utils.getLocator("redemptionLogPage.searchTextbox").isDisplayed();
			utils.getLocator("redemptionLogPage.searchTextbox").clear();
			utils.getLocator("redemptionLogPage.searchTextbox").sendKeys(redemptionCode);
			utils.getLocator("redemptionLogPage.searchButton").click();
			String redemptionH4HeaderValue = utils.getLocator("redemptionLogPage.redemptioncodeLabel").getText().trim();
			Assert.assertTrue(redemptionH4HeaderValue.startsWith(redemptionCode));

			Assert.assertTrue(redemptionH4HeaderValue.startsWith(redemptionCode + " Redeemable"));

			logger.info("Verfied redemption log heading: " + driver.findElement(By.xpath(
					utils.getLocatorValue("redemptionLogPage.redemptioncodeLabel").replace("temp", redemptionCode))));
			TestListeners.extentTest.get().info("Verfied redemption log heading: " + driver.findElement(By.xpath(
					utils.getLocatorValue("redemptionLogPage.redemptioncodeLabel").replace("temp", redemptionCode))));
			driver.findElement(
					By.xpath(utils.getLocatorValue("redemptionLogPage.responseLabel").replace("temp", amount)))
					.isDisplayed();
			logger.info("Sucessfully verfied redemption log");
			TestListeners.extentTest.get().pass("Sucessfully verfied redemption log");
			return true;
		} catch (Exception e) {
			logger.error("Error in scheduling new segment export " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return false;
	}

	public boolean verifyRedemptionLog(String redemptionCode, String amount) {
		amount = amount.replace(".0", ".");
		boolean flag = false;
		int attempts = 0;
		while (attempts <= 5) {
			utils.longWaitInSeconds(4);
			try {
				utils.getLocator("redemptionLogPage.redemptionHeading").isDisplayed();
				utils.getLocator("redemptionLogPage.searchTextbox").isDisplayed();
				utils.getLocator("redemptionLogPage.searchTextbox").clear();
				utils.getLocator("redemptionLogPage.searchTextbox").sendKeys(redemptionCode);
				utils.getLocator("redemptionLogPage.searchButton").click();
				String redemptionH4HeaderValue = utils.getLocator("redemptionLogPage.redemptioncodeLabel").getText()
						.trim();
				if (redemptionH4HeaderValue.startsWith(redemptionCode + " Redeemable")) {
					logger.info("Redemption logs matched on the page");
					TestListeners.extentTest.get().pass("Redemption logs matched on the page");
					logger.info("Verfied redemption log heading: "
							+ driver.findElement(By.xpath(utils.getLocatorValue("redemptionLogPage.redemptioncodeLabel")
									.replace("temp", redemptionCode))));
					TestListeners.extentTest.get()
							.info("Verfied redemption log heading: " + driver
									.findElement(By.xpath(utils.getLocatorValue("redemptionLogPage.redemptioncodeLabel")
											.replace("temp", redemptionCode))));
					driver.findElement(
							By.xpath(utils.getLocatorValue("redemptionLogPage.responseLabel").replace("temp", amount)))
							.isDisplayed();
					logger.info("Sucessfully verfied redemption log");
					TestListeners.extentTest.get().pass("Sucessfully verfied redemption log");
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info(
						"Element is not present or Redemption logs did not matched... pooling count is : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		return flag;
	}

}
