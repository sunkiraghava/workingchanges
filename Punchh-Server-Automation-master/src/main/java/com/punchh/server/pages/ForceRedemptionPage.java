package com.punchh.server.pages;

import java.util.List;
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
public class ForceRedemptionPage {

	static Logger logger = LogManager.getLogger(ForceRedemptionPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public ForceRedemptionPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickForceRedemptionBtn() {
		selUtils.waitTillElementToBeClickable(utils.getLocator("forceRedemptionPage.forceRedemptionBtn"));
		utils.getLocator("forceRedemptionPage.forceRedemptionBtn").click();

	}

	public void forceRedemptionreward(String comment, String reward) {
		utils.getLocator("forceRedemptionPage.commentTextBox").clear();
		utils.getLocator("forceRedemptionPage.commentTextBox").sendKeys(comment);
		utils.getLocator("forceRedemptionPage.rewardDrp").click();
		List<WebElement> elem = utils.getLocatorList("forceRedemptionPage.rewardDrpList");
		utils.selectListDrpDwnValue(elem, reward);
		utils.getLocator("forceRedemptionPage.createRedemptionBtn").click();

	}

	public void clickCreateRedemptionBtn() {
		utils.getLocator("forceRedemptionPage.createRedemptionBtn").click();
	}

	public void forceRedemptionPoints(String comment, String points) {

		utils.getLocator("forceRedemptionPage.commentTextBox").clear();
		utils.getLocator("forceRedemptionPage.commentTextBox").sendKeys(comment);
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").clear();
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").sendKeys(points);
		utils.getLocator("forceRedemptionPage.createRedemptionBtn").click();

	}

	public void forceRedemptionOfPoints(String comment, String forceRedemptionType, String points) {
		utils.getLocator("forceRedemptionPage.commentTextBox").clear();
		utils.getLocator("forceRedemptionPage.commentTextBox").sendKeys(comment);
		utils.getLocator("forceRedemptionPage.forceRedemptionTypeDrp").click();
		List<WebElement> elem = utils.getLocatorList("forceRedemptionPage.forceRedemptionTypeList");
		utils.selectListDrpDwnValue(elem, forceRedemptionType);
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").clear();
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").sendKeys(points);
		utils.getLocator("forceRedemptionPage.createRedemptionBtn").click();
		logger.info("Force redemption is successfully completed");
		TestListeners.extentTest.get().info("Force redemption is successfully completed");
	}

	public void verifyErrorInForceRedemption(String errorMessage) {
		selUtils.implicitWait(5);
		String var = utils.getLocator("forceRedemptionPage.forceRedemptionErrorMessage").getText();
		Assert.assertTrue(var.contains(errorMessage));
		logger.info("verifying Error in creating the forced redemption");
		TestListeners.extentTest.get().pass("verifying Error in creating the forced redemption");
	}

	public void verifyForceRedemption(String errorMessage) {
		utils.waitTillVisibilityOfElement(utils.getLocator("forceRedemptionPage.forceRedemptionErrorMessage"),
				"Force redemption error message");
		String var = utils.getLocator("forceRedemptionPage.forceRedemptionErrorMessage").getText();
		Assert.assertTrue(var.contains(errorMessage));
		logger.info("verifying forced redemption message" + var);
		TestListeners.extentTest.get().pass("verifying forced redemption message" + var);
	}

	public void membershipConfig(String memLevel, String rewardValue, String points) {
		String xpathMemLink = utils.getLocatorValue("forceRedemptionPage.membershipLink").replace("{memLevel}", memLevel);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpathMemLink)));
		utils.waitTillPagePaceDone();
		utils.getLocator("forceRedemptionPage.rewardValue").clear();
		utils.getLocator("forceRedemptionPage.rewardValue").sendKeys(rewardValue);
		utils.getLocator("forceRedemptionPage.redeemptionMark").clear();
		utils.getLocator("forceRedemptionPage.redeemptionMark").sendKeys(points);
		utils.getLocator("forceRedemptionPage.updateMembershipLevel").click();
		logger.info("Updated the Redeemption Mark and Reward Value");
		TestListeners.extentTest.get().info("Updated the Redeemption Mark and Reward Value");
	}

	public void redemptionMarkInCockpit(String redemptionMark) {
		utils.getLocator("forceRedemptionPage.redemptionDisplay").click();
		utils.getLocator("forceRedemptionPage.redemptionMarkInCockpit").clear();
		utils.getLocator("forceRedemptionPage.redemptionMarkInCockpit").sendKeys(redemptionMark);
		utils.getLocator("forceRedemptionPage.updateInRedemption").click();
		logger.info("Updated the Redeemption Mark in Cockpit");
		TestListeners.extentTest.get().info("Updated the Redeemption Mark in Cockpit");
	}

	public void forceRedemptionPointsUpdated(String comment, String forceRedemptionType, String points)
			throws InterruptedException {
		clickDataToggleElipsis();
		utils.getLocator("forceRedemptionPage.forceRedemptionTab").isDisplayed();
		utils.getLocator("forceRedemptionPage.forceRedemptionTab").click();
		utils.getLocator("forceRedemptionPage.commentTextBox").clear();
		utils.getLocator("forceRedemptionPage.commentTextBox").sendKeys(comment);
		utils.getLocator("forceRedemptionPage.forceRedemptionTypeDrpdwn").click();
		List<WebElement> elem = utils.getLocatorList("forceRedemptionPage.forceRedemptionTypeList");
		utils.selectListDrpDwnValue(elem, forceRedemptionType);
		Thread.sleep(3000);
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").clear();
		utils.getLocator("forceRedemptionPage.requestedpunchesTextBox").sendKeys(points);
		utils.getLocator("forceRedemptionPage.createRedemptionBtn").click();
		utils.getLocator("forceRedemptionPage.forceRedemptionSuccessMsg").isDisplayed();
		logger.info("Forced redemption created");
		TestListeners.extentTest.get().pass("Forced redemption created");
		selUtils.implicitWait(50);
	}
	
	public void clickDataToggleElipsis() {
		selUtils.implicitWait(2);
		WebElement elipsis = utils.getLocator("forceRedemptionPage.datatoggleelipsis");
		utils.clickByJSExecutor(driver, elipsis);
		logger.info("Successfully clicked on data toggle ellipsis");
		TestListeners.extentTest.get().info("Successfully clicked on data toggle ellipsis");
	}

	public String verifyTheForceRedemptionPoints(String rewardValue) {
		utils.getLocator("forceRedemptionPage.forceRedemptionlabel").isDisplayed();
		utils.getLocator("forceRedemptionPage.valueVerification").isDisplayed();
		String value = utils.getLocator("forceRedemptionPage.valueVerification").getText().trim()
				.replace("(", "").replace(")", "");
		if (value.contains(rewardValue)) {
			utils.logPass("Successfully verified reward Value: " + rewardValue);
		} else {
			utils.logInfo("Failed to verify reward Value: " + rewardValue);
		}
		return value;
	}

	public void cockpitMultipleRedemption() {
		utils.getLocator("forceRedemptionPage.multipleRedemptions").click();
		utils.getLocator("forceRedemptionPage.processingPriority").clear();
		utils.getLocator("forceRedemptionPage.processingPriority").click();
		utils.getLocator("forceRedemptionPage.couponPriority").click();
		utils.getLocator("forceRedemptionPage.processingPriority").click();
		utils.getLocator("forceRedemptionPage.rewardsPriority").click();
		utils.getLocator("forceRedemptionPage.updateInRedemption").click();
	}
	
	public String getForceRedemptionValueFromAccountHistory()	{
		utils.getLocator("forceRedemptionPage.forceRedemptionlabel").isDisplayed();
		utils.getLocator("forceRedemptionPage.valueVerification").isDisplayed();
		String value = utils.getLocator("forceRedemptionPage.valueVerification").getText();
		logger.info("Force redemption value from Account History: " + value);
		TestListeners.extentTest.get().info("Force redemption value from Account History: " + value);
		return value;
	}
	
}
