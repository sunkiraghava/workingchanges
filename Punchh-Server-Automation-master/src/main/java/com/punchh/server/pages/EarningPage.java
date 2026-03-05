package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;
import org.testng.Assert;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class EarningPage {

	static Logger logger = LogManager.getLogger(EarningPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;

	private Map<String, By> locators;

	public EarningPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);

		locators = utils.getAllByMap();
	}

	public void selectPendingCheckinStrategy(String value, String delay) throws InterruptedException {
		WebElement checkinUpdateBtn = driver.findElement(locators.get("earningPage.checkinUpdateBtn"));
		checkinUpdateBtn.click();
		Thread.sleep(2000);
		WebElement pendingCheckinStrategy = driver.findElement(locators.get("earningPage.pendingCheckinStrategy"));
		pendingCheckinStrategy.click();
		logger.info("Clicked checkin strategy Dropdown");
		List<WebElement> drpOptions = driver.findElements(locators.get("earningPage.drpOptions"));

		for (int i = 0; i < drpOptions.size(); i++) {
			if (drpOptions.get(i).getText().equalsIgnoreCase(value)) {
				drpOptions.get(i).click();
				logger.info("Slected option :" + value);
				break;
			}
		}
		WebElement delayTextBox = driver.findElement(locators.get("earningPage.delayTextBox"));
		delayTextBox.clear();
		delayTextBox.sendKeys(delay);
		WebElement updateBtn = driver.findElement(locators.get("earningPage.updateBtn"));
		updateBtn.click();
		utils.waitTillPagePaceDone();
	}

	public void setProgramType(String programType) {
		WebElement checkinEarningBtn = driver.findElement(locators.get("earningPage.checkinEarningBtn"));
		checkinEarningBtn.isDisplayed();
		checkinEarningBtn.click();

		WebElement loyaltyProgramType = driver.findElement(locators.get("earningPage.loyaltyProgramType"));
		utils.selectDrpDwnValue(loyaltyProgramType, programType);
	}

	public void setPointsConvertTo(String pointsConvertTo) {
		WebElement pointsConvertToDropdown = driver.findElement(locators.get("earningPage.pointsConvertTo"));
		utils.selectDrpDwnValue(pointsConvertToDropdown, pointsConvertTo);
	}

	public void setBaseConversionRate(String rate) {
		WebElement checkinEarningBtn = driver.findElement(locators.get("earningPage.checkinEarningBtn"));
		checkinEarningBtn.isDisplayed();
		checkinEarningBtn.click();
		WebElement baseConversionRate = driver.findElement(locators.get("earningPage.baseConversionRate"));
		baseConversionRate.isDisplayed();
		baseConversionRate.clear();
		baseConversionRate.sendKeys(rate);
		TestListeners.extentTest.get().info("Base Conversion Rate to Points is set as " + rate);
		logger.info("Base Conversion Rate to Points is set as " + rate);
	}

	public void updateConfiguration() throws InterruptedException {
		WebElement updateBtn = driver.findElement(locators.get("earningPage.updateBtn"));
		updateBtn.isDisplayed();
		updateBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Page is updated after clicking on Update");
		TestListeners.extentTest.get().pass("Page is updated after clicking on Update");
		Thread.sleep(10000);
	}

	public void setGlobalCheckinRateLimit(String noOfReceipt, String noOfHours) throws InterruptedException {
		WebElement receiptField = driver.findElement(locators.get("earningPage.setReceiptInGlobalCheckinRateLimit"));
		receiptField.click();
		receiptField.clear();
		receiptField.sendKeys(noOfReceipt);
		logger.info("Number of Receipt in Global checkin Rate Limit is set as " + noOfReceipt);
		TestListeners.extentTest.get().pass("Number of Receipt in Global checkin Rate Limit is set as " + noOfReceipt);
		WebElement hoursField = driver.findElement(locators.get("earningPage.setHoursInGlobalCheckinRateLimit"));
		hoursField.click();
		hoursField.clear();
		hoursField.sendKeys(noOfHours);
		logger.info("Number of Hours in Global checkin Rate Limit is set as " + noOfHours);
		TestListeners.extentTest.get().pass("Number of Hours in Global checkin Rate Limit is set as " + noOfHours);
		updateConfiguration();
	}

	public void setCheckinRateLimitByChannel(String ChannelName, String rateLimit) {
		String xpath = utils.getLocatorValue("earningPage.setCheckinRateLimitByChannel").replace("$ChannelName",
				ChannelName);
		WebElement checkinRateLimitField = driver.findElement(By.xpath(xpath));
		checkinRateLimitField.click();
		checkinRateLimitField.clear();
		checkinRateLimitField.sendKeys(rateLimit);
		logger.info("Checkin Rate Limit of " + ChannelName + " Channel is set as " + rateLimit);
		TestListeners.extentTest.get().pass("Checkin Rate Limit of " + ChannelName + " Channel is set as " + rateLimit);
	}

	public void navigateToEarningTabs(String tabToNavigate) {
		String xpath = utils.getLocatorValue("earningPage.earningTabsXpath").replace("$TabName", tabToNavigate);
		WebElement earningTab = driver.findElement(By.xpath(xpath));
		earningTab.click();
		Utilities.longWait(4);
		logger.info("User navigate to " + tabToNavigate + " tab ");
		TestListeners.extentTest.get().pass("User navigate to " + tabToNavigate + " tab ");
	}

	public void setExpiresAfter(String choice, int value) {
		WebElement expiresAfterBox = driver.findElement(locators.get("earningPage.setExpiresAfterBox"));
		switch (choice) {
		case "set":
			expiresAfterBox.clear();
			expiresAfterBox.sendKeys(value + "");
			logger.info("Entered Expires After successfully.");
			TestListeners.extentTest.get().pass("Entered Expires After successfully.");
			break;

		case "clear":
			expiresAfterBox.clear();
			logger.info("cleared Expires After successfully.");
			TestListeners.extentTest.get().pass("cleared Expires After successfully.");
			break;
		}
	}

	public void setTransferredPointsExpiryDaysAsFixedDays(int value) throws InterruptedException {
		WebElement expiryStrategyDropdown = driver.findElement(locators.get("earningPage.transferredPointsExpiryStrategyDropDown"));
		utils.selectDrpDwnValue(expiryStrategyDropdown, "Fixed Days");

		logger.info("Fixed Days is selected in dropdown ");
		TestListeners.extentTest.get().pass("Fixed Days is selected in dropdown ");

		WebElement expiryDaysBox = driver.findElement(locators.get("earningPage.transferredPointsExpiryDaysBox"));
		expiryDaysBox.clear();
		expiryDaysBox.sendKeys(value + "");
		logger.info("Entered Expiry Days successfully.");
		TestListeners.extentTest.get().pass("Entered Expiry Days successfully.");

		updateConfiguration();
	}

	public void verifyMessage(String expectedMessage) {
		WebElement messageElement = driver.findElement(locators.get("earningPage.messageXpath"));
		String actualMessage = messageElement.getText();
		Assert.assertEquals(actualMessage, expectedMessage);
		logger.info(
				"expected message i.e. " + expectedMessage + " is matched with actual message i.e. " + actualMessage);
		TestListeners.extentTest.get().pass("expected message i.e. ( " + expectedMessage
				+ " ) is matched with actual message i.e. ( " + actualMessage + " )");
	}

	public void expiryDateStrategy(String strategy) {
		WebElement expiryDaysStrategyDrpDown = driver.findElement(locators.get("earningPage.expiryDaysStrategyDrpDown"));
		expiryDaysStrategyDrpDown.click();
		List<WebElement> expiryDaysStrategyList = driver.findElements(locators.get("earningPage.expiryDaysStrategyList"));
		utils.selectListDrpDwnValue(expiryDaysStrategyList, strategy);
		logger.info("Expiry Date Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get().pass("Expiry Date Strategy is selected as :- " + strategy);
	}

	public void accountReEvaluationStrategy(String strategy) {
		WebElement accountReEvaluationStrategyDrpDown = driver.findElement(locators.get("earningPage.accountReEvaluationStrategyDrpDown"));
		accountReEvaluationStrategyDrpDown.click();
		List<WebElement> accountReEvaluationStrategyList = driver.findElements(locators.get("earningPage.accountReEvaluationStrategyList"));
		utils.selectListDrpDwnValue(accountReEvaluationStrategyList, strategy);
		logger.info("Account Re-evaluation Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get().pass("Account Re-evaluation Strategy is selected as :- " + strategy);
	}

	public void setInactiveDays(String choice, int value) {
		WebElement inactiveDaysBox = driver.findElement(locators.get("earningPage.setInactiveDaysBox"));
		switch (choice) {
		case "set":
			inactiveDaysBox.clear();
			inactiveDaysBox.sendKeys(value + "");
			logger.info("Entered Inactive Days successfully.");
			TestListeners.extentTest.get().pass("Entered Inactive Days successfully.");
			break;

		case "clear":
			inactiveDaysBox.clear();
			logger.info("cleared Inactive Days successfully.");
			TestListeners.extentTest.get().pass("cleared Inactive Daysr successfully.");
			break;
		}
	}

	public void membershipLevelResetStrategy(String strategy) {
		WebElement membershipLevelResetStrategyDrpDown = driver.findElement(locators.get("earningPage.membershipLevelResetStrategyDrpDown"));
		membershipLevelResetStrategyDrpDown.click();
		List<WebElement> membershipLevelResetStrategyList = driver.findElements(locators.get("earningPage.membershipLevelResetStrategyList"));
		utils.selectListDrpDwnValue(membershipLevelResetStrategyList, strategy);
		logger.info("Membership Level Reset Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get().pass("Membership Level Reset Strategy is selected as :- " + strategy);
	}

	public void setExpiryDay(String choice, String value) {
		WebElement expiryDayField = driver.findElement(locators.get("earningPage.setExpiryDay"));
		switch (choice) {
		case "set":
			expiryDayField.clear();
			expiryDayField.sendKeys(value + "");
			logger.info("Expiry Day is set as: " + value);
			TestListeners.extentTest.get().info("Expiry Day is set as: " + value);
			break;

		case "clear":
			expiryDayField.clear();
			logger.info("Cleared Expiry Days successfully.");
			TestListeners.extentTest.get().info("Cleared Expiry Days successfully.");
			break;
		}
	}

	public void setExpiryMonth(String strategy) {
		WebElement setExpiryMonthClick = driver.findElement(locators.get("earningPage.setExpiryMonthClick"));
		setExpiryMonthClick.click();
		WebElement expiryMonth = driver.findElement(locators.get("earningPage.setExpiryMonth"));
		expiryMonth.click();
//		expiryMonth.clear();
		expiryMonth.sendKeys(strategy);
		expiryMonth.sendKeys(Keys.ENTER);
		logger.info("Expiry Month is selected as :- " + strategy);
		TestListeners.extentTest.get().info("Expiry Month is selected as :- " + strategy);
	}

	public void decoupledMembershipLevelEvaluationStrategy(String strategy) {
		WebElement decoupledStrategyDrpDown = driver.findElement(locators.get("earningPage.decoupledMembershipLevelEvaluationStrategyDrpDown"));
		decoupledStrategyDrpDown.click();
		List<WebElement> decoupledStrategyList = driver.findElements(locators.get("earningPage.decoupledMembershipLevelEvaluationStrategyList"));
		utils.selectListDrpDwnValue(decoupledStrategyList, strategy);
		logger.info("Decoupled Membership Level Evaluation Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get()
				.pass("Decoupled Membership Level Evaluation Strategy is selected as :- " + strategy);
	}

	public void selectLoyaltyProgramType(String str) {
		WebElement selectLoyaltyProgramType = driver.findElement(locators.get("earningPage.selectLoyaltyProgramType"));
		utils.selectDrpDwnValue(selectLoyaltyProgramType, str);
		logger.info("selected the value " + str + " from the drp down");
		TestListeners.extentTest.get().info("selected the value " + str + " from the drp down");
	}

	public List<String> loyaltyProgramTypeLst() {
		List<String> lst = new ArrayList<>();
		WebElement clickLoyaltyProgramType = driver.findElement(locators.get("earningPage.clickLoyaltyProgramType"));
		clickLoyaltyProgramType.click();
		List<WebElement> loyaltyProgramTypeList = driver.findElements(locators.get("earningPage.loyaltyProgramTypeLst"));
		for (int i = 0; i < loyaltyProgramTypeList.size(); i++) {
			String text = loyaltyProgramTypeList.get(i).getText();
			lst.add(text);
		}
		return lst;
	}

	public void clickUpdateBtn() {
		WebElement updateButton = driver.findElement(locators.get("earningPage.updateBtn"));
		utils.scrollToElement(driver, updateButton);
		updateButton.isDisplayed();
		updateButton.click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on the Update button");
		TestListeners.extentTest.get().info("Clicked on the Update button");
	}

	public List<String> getErrorOrSuccessMsg() {
		List<String> lst = new ArrayList<>();
		WebElement errorMsg = driver.findElement(locators.get("earningPage.errorMsg"));
		utils.waitTillVisibilityOfElement(errorMsg, "error msg");
		String text = errorMsg.getText();
		String color = errorMsg.getCssValue("color");
		String hexcode = Color.fromString(color).asHex();
		lst.add(text);
		lst.add(hexcode);
		return lst;
	}

	public void loyaltyGoalCompletion(String strategy) {
		WebElement clickLoyaltyGoalCompletion = driver.findElement(locators.get("earningPage.clickLoyaltyGoalCompletion"));
		clickLoyaltyGoalCompletion.click();
		List<WebElement> loyaltyGoalCompletionDrpDown = driver.findElements(locators.get("earningPage.loyaltyGoalCompletionDrpDown"));
		utils.selectListDrpDwnValue(loyaltyGoalCompletionDrpDown, strategy);
		logger.info("Loyalty Goal Completion Strategy is selected as :- " + strategy);
		TestListeners.extentTest.get().info("Loyalty Goal Completion Strategy is selected as :- " + strategy);
	}

	public void setScanningRateLimit(String noOfReceipt) throws InterruptedException {
		WebElement scanningRateLimit = driver.findElement(locators.get("earningPage.setScanningRateLimit"));
		scanningRateLimit.click();
		scanningRateLimit.clear();
		scanningRateLimit.sendKeys(noOfReceipt);
		logger.info("Number of Scanning Rate Limit is set as " + noOfReceipt);
		TestListeners.extentTest.get().pass("Number of Scanning Rate Limit is set as " + noOfReceipt);
		updateConfiguration();
	}

	public void setFinalExpiryDaysFields(String choice, int value, String inputField) {
		switch (choice) {
		case "set":
			WebElement expiryInputField = driver.findElement(
					By.xpath(utils.getLocatorValue("earningPage.expiryInputFields").replace("$temp", inputField)));
			expiryInputField.clear();
			expiryInputField.sendKeys(value + "");
			WebElement updateButton = driver.findElement(locators.get("adminUserPage.updateButton"));
			updateButton.click();
			logger.info("Entered " + inputField + " successfully.");
			TestListeners.extentTest.get().pass("Entered " + inputField + " successfully.");
			break;

		case "clear":
			WebElement expiryFieldToClear = driver.findElement(
					By.xpath(utils.getLocatorValue("earningPage.expiryInputFields").replace("$temp", inputField)));
			expiryFieldToClear.clear();
			WebElement updateBtn = driver.findElement(locators.get("adminUserPage.updateButton"));
			updateBtn.click();
			logger.info("cleared " + inputField + " successfully.");
			TestListeners.extentTest.get().pass("cleared " + inputField + " successfully.");
			break;
		}
	}

	public void setBankedRewardValue(String value) throws InterruptedException {
		WebElement bankedRewardValue = driver.findElement(locators.get("earningPage.bankedRewardValue"));
		bankedRewardValue.click();
		bankedRewardValue.clear();
		bankedRewardValue.sendKeys(value);
		logger.info("Banked reward value is set as " + value);
		TestListeners.extentTest.get().info("Banked reward value is set as " + value);
	}

	public void selectRedeemableOnLoyaltyGoalCompletion(String value) throws InterruptedException {
//		driver.findElement(locators.get("earningPage.baseRedeemable")).click();
//		logger.info("Clicked Base Redeemable Dropdown");
		WebElement redeemableDropdown = driver.findElement(locators.get("earningPage.redeemableListDrpDwn"));
		utils.selectDrpDwnValue(redeemableDropdown, value);
		WebElement updateBtn = driver.findElement(locators.get("earningPage.updateBtn"));
		updateBtn.click();
		utils.waitTillPagePaceDone();
	}
}
